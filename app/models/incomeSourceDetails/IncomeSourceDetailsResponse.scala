/*
 * Copyright 2020 HM Revenue & Customs
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

package models.incomeSourceDetails

import models.core.AccountingPeriodModel
import play.api.libs.json.{Format, JsValue, Json}

sealed trait IncomeSourceDetailsResponse {
  def toJson: JsValue
}

case class IncomeSourceDetailsModel(businesses: List[BusinessDetailsModel],
                                    property: Option[PropertyDetailsModel]) extends IncomeSourceDetailsResponse {

  override def toJson: JsValue = Json.toJson(this)

  val accountingPeriods: List[AccountingPeriodModel] = businesses.map(_.accountingPeriod) ++ property.map(_.accountingPeriod)
  val orderedTaxYears: List[Int] = accountingPeriods.map(_.determineTaxYear).sortWith(_ < _).distinct

  val hasPropertyIncome: Boolean = property.nonEmpty
  val hasBusinessIncome: Boolean = businesses.nonEmpty

  val earliestTaxYear: Option[Int] = orderedTaxYears.headOption

}

case class IncomeSourceDetailsError(status: Int, reason: String) extends IncomeSourceDetailsResponse {
  override def toJson: JsValue = Json.toJson(this)
}

object IncomeSourceDetailsModel {
  implicit val format: Format[IncomeSourceDetailsModel] = Json.format[IncomeSourceDetailsModel]
}

object IncomeSourceDetailsError {
  implicit val format: Format[IncomeSourceDetailsError] = Json.format[IncomeSourceDetailsError]
}
