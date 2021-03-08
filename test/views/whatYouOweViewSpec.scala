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

package views

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName, testTaxYear}
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.MessagesLookUp.{Breadcrumbs => breadcrumbMessages, WhatYouOwe => whatYouOwe}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.FinancialDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import assets.FinancialDetailsTestConstants.testFinancialDetailsModel

import java.time.LocalDate

class whatYouOweViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("individual"))(FakeRequest())

  class Setup(financialDetails: FinancialDetailsModel = FinancialDetailsModel(List()), yearOfMigration: Option[String] = None,
              paymentEnabled: Boolean = false) {
    val html: HtmlFormat.Appendable = views.html.whatYouOwe(financialDetails,yearOfMigration,
      paymentEnabled, mockImplicitDateFormatter)(FakeRequest(), implicitly, mockAppConfig)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  val financialDetailsWithAllDataBeyond30DaysOfDueDate = testFinancialDetailsModel(List(Some("Balancing Charge debit"), Some("POA1"), Some("POA2")),
    List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    List(Some(25), Some(50), Some(75)))

  val financialDetailsWithAllDataWithin30DaysOfDueDate = testFinancialDetailsModel(List(Some("Balancing Charge debit"), Some("POA1"), Some("POA2")),
    List(Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    List(Some(25), Some(50), Some(75)))

  val financialDetailsWithAllDataAfterDueDate = testFinancialDetailsModel(List(Some("Balancing Charge debit"), Some("POA1"), Some("POA2")),
    List(Some(LocalDate.now().minusDays(30).toString), Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(25), Some(50), Some(75)))

  val financialDetailsWithMixDate = testFinancialDetailsModel(List(Some("POA1"), Some("POA1"), Some("POA2")),
    List(Some(LocalDate.now().plusDays(35).toString), Some(LocalDate.now().plusDays(30).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(25), Some(50), Some(75)))

  "The What you owe view with financial details model" should {
    "when the user has charges for year 1 of migration and access viewer before 30 days of due date" should {
      s"have the title '${whatYouOwe.title}' and page header and notes" in new Setup(
        financialDetails = financialDetailsWithAllDataBeyond30DaysOfDueDate, Some("2021"), true) {

        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.getElementById("sa-note").text shouldBe whatYouOwe.saNote
        pageDocument.getElementById("outstanding-charges-note").text shouldBe whatYouOwe.osChargesNote
      }
      s"have the remaining balance title, table header " in new Setup(
        financialDetails = financialDetailsWithAllDataBeyond30DaysOfDueDate, Some("2021"), true) {

        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments("2020", "2021")
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue
      }
      s"remaining balance row data exists and should not contain hyperlink and overdue tag " in new Setup(
        financialDetails = financialDetailsWithAllDataBeyond30DaysOfDueDate, Some("2021"), true) {

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        remainingBalanceTable.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("balancing-charge-type-0-link") shouldBe null
        pageDocument.getElementById("balancing-charge-type-0-overdue") shouldBe null
      }
      s"payment type drop down and content exists" in new Setup(
        financialDetails = financialDetailsWithAllDataBeyond30DaysOfDueDate, Some("2021"), true) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1

        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments
      }

      s"table header and data for future payments" in new Setup(
        financialDetails = financialDetailsWithAllDataBeyond30DaysOfDueDate, Some("2021"), true) {
        val futurePaymentsHeader: Element = pageDocument.select("tr").get(2)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(45).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText("2019", "2020")
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2020, "1040000124").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null

        val futurePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        futurePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
        futurePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2Text + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        futurePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("future-payments-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2019, "1040000125").url
        pageDocument.getElementById("future-payments-type-1-overdue") shouldBe null

        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(2500).url

        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }

    "when the user has charges for year 1 of migration and access viewer within 30 days of due date" should {
      s"have the title '${whatYouOwe.title}' and notes" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {

        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.getElementById("sa-note").text shouldBe whatYouOwe.saNote
        pageDocument.getElementById("outstanding-charges-note").text shouldBe whatYouOwe.osChargesNote
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments("2020", "2021")
      }
      s"have the remaining balance header and table data" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        remainingBalanceTable.select("td").last().text() shouldBe "£25.00"
      }
      s"have payment type drop down details" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1

        pageDocument.getElementById("balancing-charge-type-0-link") shouldBe null
        pageDocument.getElementById("balancing-charge-type-0-overdue") shouldBe null
      }

      s"have table header and data for due within 30 days" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {
        pageDocument.getElementById("due-in-thirty-days-payments-heading").text shouldBe whatYouOwe.dueInThirtyDays

        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText("2019", "2020")
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2020, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have data with POA2 with hyperlink and no overdue" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {
        val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(4)
        dueWithInThirtyDaysTableRow2.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
        dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe whatYouOwe.poa2Text + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        dueWithInThirtyDaysTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("due-in-thirty-days-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2019, "1040000125").url
        pageDocument.getElementById("due-in-thirty-days-type-1-overdue") shouldBe null
      }

      s"have payment details and should not contain future payments and overdue payment headers" in new Setup(
        financialDetails = financialDetailsWithAllDataWithin30DaysOfDueDate, Some("2021"), true) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("over-due-payments-heading") shouldBe null
      }
    }
    "when the user has charges for year 1 of migration and access viewer after due date" should {
      s"have the title '${whatYouOwe.title}' and notes" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {

        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.getElementById("sa-note").text shouldBe whatYouOwe.saNote
        pageDocument.getElementById("outstanding-charges-note").text shouldBe whatYouOwe.osChargesNote
      }
      s"have the mtd payments header, table header and data with remaining balance data with no hyperlink but have overdue tag" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {
        pageDocument.getElementById("pre-mtd-payments-heading").text shouldBe whatYouOwe.preMtdPayments("2020", "2021")
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        remainingBalanceHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(30).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.remainingBalance + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        remainingBalanceTable.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("balancing-charge-type-0-link") shouldBe null
        pageDocument.getElementById("balancing-charge-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have payment type dropdown details" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {
        pageDocument.getElementById("payment-type-dropdown-title").text shouldBe whatYouOwe.dropDownInfo
        pageDocument.getElementById("payment-details-content-0").text shouldBe whatYouOwe.remainingBalance + " " + whatYouOwe.remainingBalanceLine1
        pageDocument.getElementById("payment-details-content-1").text shouldBe whatYouOwe.poaHeading + " " + whatYouOwe.poaLine1
      }

      s"have overdue payments header and data with POA1 charge type" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {
        pageDocument.getElementById("over-due-payments-heading").text shouldBe whatYouOwe.overduePayments

        val overdueTableHeader: Element = pageDocument.select("tr").get(2)
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText("2019", "2020")
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2020, "1040000124").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {
        val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
        overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow2.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2019, "1040000125").url
        pageDocument.getElementById("over-due-type-1-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have payments data with button" in new Setup(
        financialDetails = financialDetailsWithAllDataAfterDueDate, Some("2021"), true) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(2500).url

        pageDocument.getElementById("future-payments-heading") shouldBe null
        pageDocument.getElementById("due-in-thirty-days-payments-heading") shouldBe null
      }
    }

    "when the user has charges for year 1 of migration and access viewer with mixed dates" should {
      s"have the title '${whatYouOwe.title}' and notes" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {

        pageDocument.title() shouldBe whatYouOwe.title
        pageDocument.getElementById("sa-note").text shouldBe whatYouOwe.saNote
        pageDocument.getElementById("outstanding-charges-note").text shouldBe whatYouOwe.osChargesNote
      }
      s"not have MTD payments heading" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        val overdueTableHeader: Element = pageDocument.select("tr").first()
        overdueTableHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        overdueTableHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.overdueTag + " " + whatYouOwe.poa2Text + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        pageDocument.getElementById("over-due-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2019, "1040000125").url
        pageDocument.getElementById("over-due-type-0-overdue").text shouldBe whatYouOwe.overdueTag
      }
      s"have due within thirty days header and data with hyperlink and no overdue tag" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        val dueWithInThirtyDaysHeader: Element = pageDocument.select("tr").get(2)
        dueWithInThirtyDaysHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        dueWithInThirtyDaysHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        dueWithInThirtyDaysHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText("2019", "2020")
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-in-thirty-days-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2020, "1040000124").url
        pageDocument.getElementById("due-in-thirty-days-type-0-overdue") shouldBe null
      }
      s"have future payments with table header, data and hyperlink without overdue tag" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        pageDocument.getElementById("future-payments-heading").text shouldBe whatYouOwe.futurePayments

        val futurePaymentsHeader: Element = pageDocument.select("tr").get(4)
        futurePaymentsHeader.select("th").first().text() shouldBe whatYouOwe.dueDate
        futurePaymentsHeader.select("th").get(1).text() shouldBe whatYouOwe.paymentType
        futurePaymentsHeader.select("th").last().text() shouldBe whatYouOwe.amountDue

        val futurePaymentsTableRow1: Element = pageDocument.select("tr").get(5)
        futurePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
        futurePaymentsTableRow1.select("td").get(1).text() shouldBe whatYouOwe.poa1Text + " " + whatYouOwe.taxYearForChargesText("2018", "2019")
        futurePaymentsTableRow1.select("td").last().text() shouldBe "£25.00"

        pageDocument.getElementById("future-payments-type-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showChargeSummary(2019, "1040000123").url
        pageDocument.getElementById("future-payments-type-0-overdue") shouldBe null
      }
      s"have payment data with button" in new Setup(
        financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        pageDocument.getElementById("payment-days-note").text shouldBe whatYouOwe.paymentDaysNote
        pageDocument.getElementById("credit-on-account").text shouldBe whatYouOwe.creditOnAccount
        pageDocument.getElementById("payment-button").text shouldBe whatYouOwe.payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(7500).url

        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }

    }
    "have a breadcrumb trail" in new Setup(
      financialDetails = financialDetailsWithMixDate, Some("2021"), true) {
        pageDocument.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        pageDocument.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        pageDocument.getElementById("breadcrumb-what-you-owe").text shouldBe breadcrumbMessages.whatYouOwe
      }
    }
}
