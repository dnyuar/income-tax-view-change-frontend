/*
 * Copyright 2019 HM Revenue & Customs
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

package models.calculation

import models._
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{Json, _}

sealed trait CalculationDataResponseModel

case class CalculationDataModel(
                                 nationalRegime: Option[String] = None,
                                 totalTaxableIncome: BigDecimal,
                                 totalIncomeTaxNicYtd: BigDecimal,
                                 personalAllowance: BigDecimal,
                                 taxReliefs: BigDecimal,
                                 totalIncomeAllowancesUsed: BigDecimal,
                                 incomeReceived: IncomeReceivedModel,
                                 savingsAndGains: SavingsAndGainsModel,
                                 dividends: DividendsModel,
                                 nic: NicModel,
                                 eoyEstimate: Option[EoyEstimate] = None,
                                 payAndPensionsProfitBands: List[TaxBandModel] = Nil
                               ) extends CalculationDataResponseModel {

  val taxableDividendIncome: BigDecimal = dividends.totalAmount

  val taxableIncomeTaxIncome: BigDecimal = totalTaxableIncome - taxableDividendIncome
  val savingsAllowanceSummaryData: BigDecimal = savingsAndGains.startBand.taxableIncome + savingsAndGains.zeroBand.taxableIncome
  val additionalAllowances: BigDecimal = totalIncomeAllowancesUsed - personalAllowance - savingsAllowanceSummaryData
  val taxablePayPensionsProfit: BigDecimal = payAndPensionsProfitBands.map(_.income).sum
  val taxableSavingsInterest : BigDecimal = savingsAndGains.startBand.taxableIncome + savingsAndGains.zeroBand.taxableIncome + savingsAndGains.basicBand.taxableIncome + savingsAndGains.higherBand.taxableIncome + savingsAndGains.additionalBand.taxableIncome

  def hasDividensAtSpecfiedRate(taxAmount: BigDecimal): Boolean = taxAmount > 0


  def srtSiITCalc :BigDecimal = savingsAndGains.startBand.taxableIncome
  def srtSiITAmount : BigDecimal  = savingsAndGains.startBand.taxAmount
  def zrtSiITCalc :BigDecimal = savingsAndGains.zeroBand.taxableIncome
  def zrtSiITAmount : BigDecimal  = savingsAndGains.zeroBand.taxAmount
  def brtSiITCalc :BigDecimal = savingsAndGains.basicBand.taxableIncome
  def brtSiITAmount : BigDecimal  = savingsAndGains.basicBand.taxAmount
  def hrtSiITCalc : BigDecimal = savingsAndGains.higherBand.taxableIncome
  def hrtSiITAmount : BigDecimal = savingsAndGains.higherBand.taxAmount
  def artSiITCalc : BigDecimal = savingsAndGains.additionalBand.taxableIncome
  def artSiITAmount : BigDecimal = savingsAndGains.additionalBand.taxAmount
}

case class IncomeReceivedModel(selfEmployment: BigDecimal,
                               ukProperty: BigDecimal,
                               bankBuildingSocietyInterest: BigDecimal,
                               ukDividends: BigDecimal
                              ) {
  def estimateBuisnessProfit: BigDecimal = selfEmployment + ukProperty + bankBuildingSocietyInterest

  def buisnessProfit: BigDecimal = selfEmployment + ukProperty
}

case class SavingsAndGainsModel(startBand: BandModel,
                                zeroBand: BandModel,
                                basicBand: BandModel,
                                higherBand: BandModel,
                                additionalBand: BandModel)

case class DividendsModel(totalAmount: BigDecimal,
                          dividendsBands: Seq[DividendsBandModel])

case class DividendsBandModel(name: String,
                              rate: BigDecimal,
                              threshold: Option[Int],
                              apportionedThreshold: Option[Int],
                              income: BigDecimal,
                              amount : BigDecimal)

case class BandModel(taxableIncome: BigDecimal,
                     taxRate: BigDecimal,
                     taxAmount: BigDecimal)

case class TaxBandModel(name: String, rate: BigDecimal, income: BigDecimal, amount: BigDecimal)

object TaxBandModel {
  implicit val format: Format[TaxBandModel] = Json.format[TaxBandModel]
}

case class NicModel(class2: BigDecimal,
                    class4: BigDecimal)

case class EoyEstimate(incomeTaxNicAmount: BigDecimal)


object CalculationDataModel {
  val defaultZero: JsPath => Reads[BigDecimal] = _.read[BigDecimal].orElse(Reads.pure[BigDecimal](0.00))
  val logFieldError: JsPath => Reads[Option[String]] = _.readNullable[String] map {
    case None => Logger.error(s"[CalculationDataResponseModel][CalculationDataModel] - National Regime field is missing from json");None
    case data => data
  }

  implicit val reads: Reads[CalculationDataModel] = (
    logFieldError(__ \ "nationalRegime") and
    defaultZero(__ \ "totalIncomeOnWhichTaxIsDue") and
      (__ \ "incomeTaxYTD").read[BigDecimal] and
      defaultZero(__ \ "proportionAllowance") and
      defaultZero(__ \ "totalAllowancesAndReliefs") and
      defaultZero(__ \ "totalIncomeAllowancesUsed") and
      __.read[IncomeReceivedModel] and
      __.read[SavingsAndGainsModel] and
      __.read[DividendsModel] and
      __.read[NicModel] and
      (__ \ "eoyEstimate").readNullable[EoyEstimate] and
      (__ \ "incomeTax" \ "payAndPensionsProfit" \ "band").read[List[TaxBandModel]].orElse(Reads.pure(Nil))
    ) (CalculationDataModel.apply _)

  implicit val writes: Writes[CalculationDataModel] = (
    (__ \ "nationalRegime").writeNullable[String] and
    (__ \ "totalTaxableIncome").write[BigDecimal] and
      (__ \ "totalIncomeTaxNicYtd").write[BigDecimal] and
      (__ \ "personalAllowance").write[BigDecimal] and
      (__ \ "taxReliefs").write[BigDecimal] and
      (__ \ "totalIncomeAllowancesUsed").write[BigDecimal] and
      (__ \ "incomeReceived").write[IncomeReceivedModel] and
      (__ \ "savingsAndGains").write[SavingsAndGainsModel] and
      (__ \ "dividends").write[DividendsModel] and
      (__ \ "nic").write[NicModel] and
      (__ \ "eoyEstimate").writeNullable[EoyEstimate] and
      (__ \ "incomeTax" \ "payAndPensionsProfit" \ "band").write[List[TaxBandModel]]
    )(unlift(CalculationDataModel.unapply))
}

object IncomeReceivedModel {
  implicit val reads: Reads[IncomeReceivedModel] = (
    defaultZero(__ \ "profitFromSelfEmployment") and
      defaultZero(__ \ "profitFromUkLandAndProperty") and
      defaultZero(__ \ "interestReceivedFromUkBanksAndBuildingSocieties") and
      defaultZero(__ \ "dividendsFromUkCompanies")
    ) (IncomeReceivedModel.apply _)
  implicit val writes: Writes[IncomeReceivedModel] = Json.writes[IncomeReceivedModel]
}

object SavingsAndGainsModel {
  implicit val reads: Reads[SavingsAndGainsModel] = (
    __.read[BandModel](BandModel.interestReadsStartingRate) and
      __.read[BandModel](BandModel.interestReadsZeroRate) and
      __.read[BandModel](BandModel.interestReadsBRT) and
      __.read[BandModel](BandModel.interestReadsHRT) and
      __.read[BandModel](BandModel.interestReadsART)
    ) (SavingsAndGainsModel.apply _)
  implicit val writes: Writes[SavingsAndGainsModel] = Json.writes[SavingsAndGainsModel]
}

object DividendsModel {
  implicit val reads: Reads[DividendsModel] = (
    defaultZero(__ \ "incomeTax" \ "dividends" \ "totalAmount") and
      (__ \ "incomeTax" \ "dividends" \ "band").read[Seq[DividendsBandModel]]
    ) (DividendsModel.apply _)
  implicit val writes: Writes[DividendsModel] = Json.writes[DividendsModel]
}

object NicModel {
  implicit val reads: Reads[NicModel] = (
    defaultZero(__ \ "nationalInsuranceClass2Amount") and
      defaultZero(__ \ "totalClass4Charge")
    ) (NicModel.apply _)
  implicit val writes: Writes[NicModel] = Json.writes[NicModel]
}

object BandModel {

  // SE Business and Property Reads
  val payPensionsProfitReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtBRT")
    ) (BandModel.apply _)
  val payPensionsProfitReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtHRT")
    ) (BandModel.apply _)
  val payPensionsProfitReadsART: Reads[BandModel] = (
    defaultZero(__ \ "payPensionsProfitAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnPayPensionsProfitAtART")
    ) (BandModel.apply _)

  // Bank and Building Society Interest Reads
  val interestReadsStartingRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtStartingRate") and Reads.pure[BigDecimal](0) and defaultZero(__ \ "incomeTaxOnInterestReceivedAtStartingRate")
    ) (BandModel.apply _)
  val interestReadsZeroRate: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtZeroRate") and Reads.pure[BigDecimal](0) and defaultZero(__ \ "incomeTaxOnInterestReceivedAtZeroRate")
    ) (BandModel.apply _)
  val interestReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtBRT") and defaultZero(__ \ "rateBRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtBRT")
    ) (BandModel.apply _)
  val interestReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtHRT") and defaultZero(__ \ "rateHRT") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtHRT")
    ) (BandModel.apply _)
  val interestReadsART: Reads[BandModel] = (
    defaultZero(__ \ "interestReceivedAtART") and defaultZero(__ \ "rateART") and defaultZero(__ \ "incomeTaxOnInterestReceivedAtART")
    ) (BandModel.apply _)

  // Dividends Reads
  val dividendsReadsBRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtBRT") and defaultZero(__ \ "dividendBRT") and defaultZero(__ \ "incomeTaxOnDividendsAtBRT")
    ) (BandModel.apply _)
  val dividendsReadsHRT: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtHRT") and defaultZero(__ \ "dividendHRT") and defaultZero(__ \ "incomeTaxOnDividendsAtHRT")
    ) (BandModel.apply _)
  val dividendsReadsART: Reads[BandModel] = (
    defaultZero(__ \ "dividendsAtART") and defaultZero(__ \ "dividendART") and defaultZero(__ \ "incomeTaxOnDividendsAtART")
    ) (BandModel.apply _)

  // Implicit Writes
  implicit val writes: Writes[BandModel] = Json.writes[BandModel]
}

object EoyEstimate {
  implicit val format: Format[EoyEstimate] = Json.format[EoyEstimate]
}

case class CalculationDataErrorModel(code: Int, message: String) extends CalculationDataResponseModel

object CalculationDataErrorModel {
  implicit val format: Format[CalculationDataErrorModel] = Json.format[CalculationDataErrorModel]
}

object DividendsBandModel {
  implicit val writes: Writes[DividendsBandModel] = Json.writes[DividendsBandModel]

  implicit val reads: Reads[DividendsBandModel] = (
    (__ \ "name").read[String] and
      (__ \ "rate").read[BigDecimal] and
      (__ \ "threshold").readNullable[Int] and
      (__ \ "apportionedThreshold").readNullable[Int] and
      (__ \ "income").read[BigDecimal] and
      (__ \ "amount").read[BigDecimal]
    ) (DividendsBandModel.apply _)

}
