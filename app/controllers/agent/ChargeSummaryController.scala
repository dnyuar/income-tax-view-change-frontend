/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.agent

import audit.AuditingService
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.Inject
import models.chargeHistory.ChargeHistoryModel
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetailsModel, PaymentsWithChargeType}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{FinancialDetailsService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.ChargeSummary

import scala.concurrent.{ExecutionContext, Future}


class ChargeSummaryController @Inject()(chargeSummaryView: ChargeSummary,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        financialDetailsService: FinancialDetailsService,
                                        incomeSourceDetailsService: IncomeSourceDetailsService,
                                        auditingService: AuditingService,
                                       )(implicit val appConfig: FrontendAppConfig,
                                         val languageUtils: LanguageUtils,
                                         mcc: MessagesControllerComponents,
                                         dateFormatter: ImplicitDateFormatterImpl,
                                         implicit val ec: ExecutionContext,
                                         val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(documentDetailWithDueDate: DocumentDetailWithDueDate, chargeHistoryOpt: Option[List[ChargeHistoryModel]], latePaymentInterestCharge: Boolean,
                   backLocation: Option[String], taxYear: Int, paymentAllocations: List[PaymentsWithChargeType], paymentAllocationEnabled: Boolean, payments: FinancialDetailsModel)
                  (implicit request: Request[_]): Html = {

    chargeSummaryView(
      documentDetailWithDueDate = documentDetailWithDueDate,
      chargeHistoryOpt = chargeHistoryOpt,
      latePaymentInterestCharge = latePaymentInterestCharge,
      backUrl = backUrl(backLocation, taxYear),
      paymentAllocations,
      paymentAllocationEnabled,
      payments
    )
  }

  def showChargeSummary(taxYear: Int, chargeId: String, isLatePaymentCharge: Boolean = false): Action[AnyContent] = {
		Authenticated.async { implicit request =>
			implicit user =>
				getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
					financialDetailsService.getAllFinancialDetails(mtdItUser, implicitly, implicitly) flatMap { financialResponses =>
						val payments = financialResponses.collect {
							case (_, model: FinancialDetailsModel) => model.filterPayments()
						}.foldLeft(FinancialDetailsModel(List(), List()))((merged, next) => merged.merge(next))

						val matchingYear = financialResponses.collect {
							case (year, response) if year == taxYear => response
						}

						matchingYear.headOption match {
							case Some(success: FinancialDetailsModel) if success.documentDetails.exists(_.transactionId == chargeId) =>
								val backLocation = mtdItUser.session.get(SessionKeys.chargeSummaryBackPage)

								val docDateDetail: DocumentDetailWithDueDate = success.findDocumentDetailByIdWithDueDate(chargeId).get
								val financialDetails = success.financialDetails.filter(_.transactionId.contains(chargeId))

								val paymentAllocationEnabled: Boolean = isEnabled(PaymentAllocation)
								val paymentAllocations: List[PaymentsWithChargeType] =
									if (paymentAllocationEnabled) {
										financialDetails.flatMap(_.allocation)
									} else Nil

								getChargeHistory(chargeId, isLatePaymentCharge).map { chargeHistoryOpt =>
									auditChargeSummary(chargeId, success)
									Ok(view(docDateDetail, chargeHistoryOpt, isLatePaymentCharge, backLocation, taxYear,
										paymentAllocations = paymentAllocations,
										paymentAllocationEnabled = paymentAllocationEnabled, payments))
								}
							case Some(_: FinancialDetailsModel) =>
								Logger.warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
								Future.successful(Redirect(controllers.agent.routes.HomeController.show().url))
							case _ =>
								Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
								Future.successful(itvcErrorHandler.showInternalServerError())
						}
					}
				}
			}
	}

	def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
		case Some("taxYearOverview") => controllers.agent.routes.TaxYearOverviewController.show(taxYear).url + "#payments"
		case Some("paymentDue") => controllers.agent.nextPaymentDue.routes.PaymentDueController.show().url
		case _ => controllers.agent.routes.HomeController.show().url
	}

	private def getChargeHistory(chargeId: String, isLatePaymentCharge: Boolean)(implicit req: Request[_]): Future[Option[List[ChargeHistoryModel]]] = {
		ChargeHistory.fold(
			ifDisabled = Future.successful(None),
			ifEnabled = if (isLatePaymentCharge) Future.successful(Some(Nil)) else {
				financialDetailsService.getChargeHistoryDetails(getClientMtditid, chargeId)
					.map(historyListOpt => historyListOpt.map(sortHistory) orElse Some(Nil))
			}
		)
	}

  private def auditChargeSummary(chargeId: String, financialDetailsModel: FinancialDetailsModel)
                                (implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Unit = {
    if (isEnabled(TxmEventsApproved)) {
      val documentDetailWithDueDate: DocumentDetailWithDueDate = financialDetailsModel.findDocumentDetailByIdWithDueDate(chargeId).get
      auditingService.extendedAudit(ChargeSummaryAudit(
        mtdItUser = mtdItUser,
        docDateDetail = documentDetailWithDueDate,
        agentReferenceNumber = mtdItUser.arn
      ))
    }
  }

    private def sortHistory(list: List[ChargeHistoryModel]): List[ChargeHistoryModel] = {
      list.sortBy(chargeHistory => chargeHistory.reversalDate)
    }



}
