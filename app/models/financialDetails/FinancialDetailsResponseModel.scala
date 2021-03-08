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

package models.financialDetails

import auth.MtdItUser
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

sealed trait FinancialDetailsResponseModel

case class FinancialDetailsModel(financialDetails: List[Charge]) extends FinancialDetailsResponseModel {
  def withYears(): Seq[ChargeModelWithYear] = financialDetails.map(fd => ChargeModelWithYear(fd, fd.taxYear.toInt))

  def findChargeForTaxYear(taxYear: Int): Option[Charge] = financialDetails.find(_.taxYear.toInt == taxYear)
  def isAllPaid()(implicit user: MtdItUser[_]): Boolean = financialDetails.forall(_.isPaid)

  def whatYourOwePageDataExists(charge: Charge): Boolean = charge.chargeType.isDefined && charge.due.isDefined && charge.outstandingAmount.isDefined

  def getDueWithinThirtyDaysList(migrationYear: String, yearsLimit: Int): List[Charge] = financialDetails.filter(charge => whatYourOwePageDataExists(charge)
    && (charge.chargeType.get == "POA1" || charge.chargeType.get == "POA2")
    && LocalDate.now().isAfter(charge.due.get.minusDays(31))
    && LocalDate.now().isBefore(charge.due.get.plusDays(1))
    && charge.due.get.getYear >= migrationYear.toInt - yearsLimit
  )

  def getFuturePaymentsList(migrationYear: String, yearsLimit: Int): List[Charge] = financialDetails.filter(charge => whatYourOwePageDataExists(charge)
    && (charge.chargeType.get == "POA1" || charge.chargeType.get == "POA2")
    && LocalDate.now().isBefore(charge.due.get.minusDays(30))
    && charge.due.get.getYear >= migrationYear.toInt - yearsLimit
  )

  def getOverduePaymentsList(migrationYear: String, yearsLimit: Int): List[Charge] = financialDetails.filter(charge => whatYourOwePageDataExists(charge)
    && (charge.chargeType.get == "POA1" || charge.chargeType.get == "POA2")
    && charge.due.get.isBefore(LocalDate.now())
    && charge.due.get.getYear >= migrationYear.toInt - yearsLimit
  )

  def getBalancingChargeTypeList(migrationYear: String, yearsLimit: Int): List[Charge] = financialDetails.filter(charge => whatYourOwePageDataExists(charge)
    && charge.chargeType.get == "Balancing Charge debit"
    && charge.due.get.getYear >= migrationYear.toInt - yearsLimit
  )

  def getLatestChargeByDueDate(migrationYear: String, yearsLimit: Int): Option[Charge] = {

    implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

    val listOfAllCharges = getBalancingChargeTypeList(migrationYear, yearsLimit) ++ getOverduePaymentsList(migrationYear, yearsLimit) ++
      getDueWithinThirtyDaysList(migrationYear, yearsLimit) ++ getFuturePaymentsList(migrationYear, yearsLimit)
    listOfAllCharges.sortBy(charge => charge.due.get).headOption
  }
}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
