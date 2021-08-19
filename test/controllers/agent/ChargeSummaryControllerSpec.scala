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

import java.time.LocalDate

import assets.BaseTestConstants.testAgentAuthRetrievalSuccess
import assets.ChargeHistoryTestConstants.testValidChargeHistoryModel
import assets.FinancialDetailsTestConstants._
import audit.mocks.MockAuditingService
import config.ItvcErrorHandler
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation}
import implicits.ImplicitDateFormatterImpl
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService}
import mocks.views.agent.MockChargeSummary
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryModel}
import models.financialDetails.FinancialDetailsModel
import org.mockito.ArgumentMatchers.{any, eq => ameq}
import org.mockito.Mockito.{never, verify, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import play.twirl.api.Html
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockFinancialDetailsService
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with MockChargeSummary {

		class Setup() {

			disable(ChargeHistory)
			disable(PaymentAllocation)

			mockChargeSummary()(Html("<html><head><title>Test title</title></head></html>"))

			val chargeSummaryController: ChargeSummaryController = new ChargeSummaryController(
				chargeSummary,
				mockAuthService,
				mockFinancialDetailsService,
				mockIncomeSourceDetailsService,
				mockAuditingService
			)(appConfig,
				app.injector.instanceOf[LanguageUtils],
				app.injector.instanceOf[MessagesControllerComponents],
				app.injector.instanceOf[ImplicitDateFormatterImpl],
				app.injector.instanceOf[ExecutionContext],
				app.injector.instanceOf[ItvcErrorHandler]
			)

			val currentYear: Int = LocalDate.now().getYear
			val errorHeading = "Sorry, there is a problem with the service"
			val currentFinancialDetails: FinancialDetailsModel = chargesWithAllocatedPaymentModel(2018)
			val lateInterestSuccessHeading = "Tax year 6 April 2017 to 5 April 2018 Late payment interest on payment on account 1 of 2"
		}

		".showChargeSummary" when {
			"getFinancialDetails returns a FinancialDetailsModel1" when {
				"the model contains a transaction for the charge provided" should {
					"show a charge summary with the correct title" in new Setup() {

						setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

						setupMockGetAllFinancialDetails(currentFinancialDetails)
						mockSingleBusinessIncomeSource()


						val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = false)(fakeRequestConfirmedClient()))

						status(await(result)) shouldBe OK
						JsoupParse(result).toHtmlDocument.title() shouldBe "Test title"

					}
				}

				"the model contains a transaction for the charge with LPI set to true" should {
					"show a charge summary with the correct title" in new Setup() {

						setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

						setupMockGetAllFinancialDetails(currentFinancialDetails)

						mockSingleBusinessIncomeSource()

						val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = true)
						(fakeRequestConfirmedClient()))

						status(await(result)) shouldBe OK
						JsoupParse(result).toHtmlDocument.title() shouldBe "Test title"

					}
				}

				"the model does not contains a transaction for the charge provided" should {
					"redirect to home page" in new Setup() {

						mockSingleBusinessIncomeSource()
						setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
						setupMockGetAllFinancialDetails(currentFinancialDetails)

						val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000126", isLatePaymentCharge = false)
						(fakeRequestConfirmedClient()))

						status(result) shouldBe SEE_OTHER
						redirectLocation(result) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/income-tax-account")

					}
				}
			}

			"getFinancialDetails does not returns a successful FinancialDetailsModel" when {

				"show an internal server error with the correct title" in new Setup() {

					mockSingleBusinessIncomeSource()
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					setupMockGetAllFinancialDetails(testFinancialDetailsErrorModelParsing)

					val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = false)
					(fakeRequestConfirmedClient()))

					status(await(result)) shouldBe INTERNAL_SERVER_ERROR
					JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading

				}

			}

			"the ChargeHistory feature switch is enabled and the page can be viewed" should {
				"pass to the view a retrieved charge history list in ascending order" in new Setup() {

					enable(ChargeHistory)
					enable(PaymentAllocation)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					mockSingleBusinessIncomeSource()
					setupMockGetAllFinancialDetails(currentFinancialDetails)
					mockGetChargeHistoryDetails(response = Future.successful(testValidChargeHistoryModel.chargeHistoryDetails))

					val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = false)
					(fakeRequestConfirmedClient()))

					status(result) shouldBe OK
				}

				"pass to the view an empty charge history list" when {
					"charge history list does not exist" in new Setup() {
						enable(ChargeHistory)
						enable(PaymentAllocation)
						setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
						setupMockGetAllFinancialDetails(currentFinancialDetails)
						mockSingleBusinessIncomeSource()

						mockGetChargeHistoryDetails(response = None)

						val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = false)
						(fakeRequestConfirmedClient()))

						status(result) shouldBe OK
						verify(chargeSummary).apply(any(), chargeHistoryOpt = ameq(Some(Nil)), any(), any(), any(), any(), any())(any(), any(), any(), any())
					}

					"viewing a Late Payment Interest summary" in new Setup() {
						enable(ChargeHistory)
						enable(PaymentAllocation)
						setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
						setupMockGetAllFinancialDetails(currentFinancialDetails)
						mockSingleBusinessIncomeSource()

						val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = true)
						(fakeRequestConfirmedClient()))

						status(result) shouldBe OK
						verify(chargeSummary).apply(any(), chargeHistoryOpt = ameq(Some(Nil)), any(), any(), any(), any(), any())(any(), any(), any(), any())
						verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
					}
				}
			}
			"the ChargeHistory feature switch is not enabled and the page can be viewed" should {
				"not pass any charge history list to the view" in new Setup() {
					disable(ChargeHistory)
					disable(PaymentAllocation)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					setupMockGetAllFinancialDetails(currentFinancialDetails)
					mockSingleBusinessIncomeSource()

					val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = false)
					(fakeRequestConfirmedClient()))

					status(result) shouldBe OK
					verify(chargeSummary).apply(any(), chargeHistoryOpt = ameq(None), any(), any(), any(), any(), any())(any(), any(), any(), any())
					verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
				}

				"not pass any charge history list to the view when viewing a Late Payment Interest summary" in new Setup() {
					disable(ChargeHistory)
					disable(PaymentAllocation)
					setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
					setupMockGetAllFinancialDetails(currentFinancialDetails)
					mockSingleBusinessIncomeSource()

					val result: Result = await(chargeSummaryController.showChargeSummary(2018, "1040000123", isLatePaymentCharge = true)
					(fakeRequestConfirmedClient()))

					status(result) shouldBe OK
					verify(chargeSummary).apply(any(), chargeHistoryOpt = ameq(None), any(), any(), any(), any(), any())(any(), any(), any(), any())
					verify(mockFinancialDetailsService, never).getChargeHistoryDetails(any(), any())(any())
				}
			}
		}

		".backUrl" when {

			"the user comes from what you owe page" should {
				"return what you owe page url string result" in new Setup() {

					val result: String = chargeSummaryController.backUrl(backLocation = Some("paymentDue"), currentYear)

					result shouldBe "/report-quarterly/income-and-expenses/view/agents/payments-owed"

				}
			}

			"the user comes from tax overview page payment tab" should {
				"return payment tab url string result" in new Setup() {

					val result: String = chargeSummaryController.backUrl(backLocation = Some("taxYearOverview"), currentYear)

					result shouldBe "/report-quarterly/income-and-expenses/view/agents/calculation/2021#payments"

				}
			}

			"the user comes is not from tax overview payment tab and is not from what you owe page" should {
				"return agent home page url string result" in new Setup() {

					val result: String = chargeSummaryController.backUrl(backLocation = Some("_"), currentYear)

					result shouldBe "/report-quarterly/income-and-expenses/view/agents/income-tax-account"

				}
			}
		}
	}
