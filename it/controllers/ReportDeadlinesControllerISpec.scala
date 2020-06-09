/*
 * Copyright 2017 HM Revenue & Customs
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
package controllers

import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.ReportDeadlinesIntegrationTestConstants._
import assets.messages.{ReportDeadlinesMessages => messages}
import config.featureswitch.{FeatureSwitching, ObligationsPage, ReportDeadlines}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import implicits.ImplicitDateFormatter
import models.reportDeadlines.ObligationsModel
import play.api.http.Status._

class ReportDeadlinesControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with FeatureSwitching {


  "Calling the ReportDeadlinesController" when {

    "the ReportDeadlines Feature is enabled" when {

      unauthorisedTest("/obligations")

      "the obligations feature switch is enabled" when {

        "the user has a eops property income obligation only" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(singleObligationEOPSPropertyModel)))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyReportDeadlinesCall(testNino, testPropertyIncomeId, testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays one eops property income obligation")
          res should have(
            elementTextBySelector("#eops-return-section-0 #eops-return-period")("6 April 2017 to 5 July 2018"),
            elementTextBySelector("#eops-return-section-0 #eops-return-due-date")("1 January 2018"),
            isElementVisibleById("eops-return-section-1")(expectedValue = false)
          )
        }

        "the user has no obligations" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          IncomeTaxViewChangeStub.stubGetReportDeadlinesNotFound(testMtditid, testNino)

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyReportDeadlinesCall(testNino, testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.title)
          )

          Then("the page displays no obligation dates")
          res should have(
            isElementVisibleById("eops-return-section-0")(expectedValue = false)
          )
        }

        "the user has a quarterly property income obligation only" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testPropertyIncomeId, testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays the property obligation dates")
          res should have(
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-period")(expectedValue = "6 April 2017 to 5 July 2017"),
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-due-date")(expectedValue = "1 January 2018"),
            isElementVisibleById("quarterly-return-section-1")(expectedValue = false)
          )
        }

        "the user has a quarterly business income obligation only" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId))))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyReportDeadlinesCall(testNino, testPropertyIncomeId, testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays the property obligation dates")
          res should have(
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-period")(expectedValue = "6 April 2017 to 5 July 2017"),
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-due-date")(expectedValue = "1 January 2018"),
            isElementVisibleById("quarterly-return-section-1")(expectedValue = false)
          )
        }

        "the user has multiple quarterly business income obligations only" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId),
            singleObligationQuarterlyModel(otherTestSelfEmploymentId))))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testPropertyIncomeId, testMtditid)

          Then("the view displays the correct title, username and links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays all the business obligation dates")
          res should have(
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-period")(expectedValue = "6 April 2017 to 5 July 2017"),
            elementTextBySelector("#quarterly-return-section-0 #quarterly-return-due-date")(expectedValue = "1 January 2018"),
            elementTextBySelector("#quarterly-return-section-1 #quarterly-return-period")(expectedValue = "6 April 2017 to 5 July 2017"),
            elementTextBySelector("#quarterly-return-section-1 #quarterly-return-due-date")(expectedValue = "1 January 2018"),
            isElementVisibleById("quarterly-return-section-2")(expectedValue = false)
          )
        }

        "the user has a eops SE income obligation only" in {
          enable(ObligationsPage)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(SEIncomeSourceEOPSModel(testSelfEmploymentId))))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testMtditid)

          Then("the view displays the correct title")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays SE income source obligation dates")
          res should have(
            elementTextBySelector("#eops-return-section-0 #eops-return-period")("6 April 2017 to 5 April 2018"),
            elementTextBySelector("#eops-return-section-0 #eops-return-due-date")("31 January 2018")
          )

          Then("the page displays no property obligation dates")
          res should have(
            isElementVisibleById("eops-return-section-1")(expectedValue = false)
          )

        }

        "the user has a Crystallised obligation only" in {
          enable(ObligationsPage)

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

          IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(noObligationsModel(testSelfEmploymentId), crystallisedEOPSModel)))

          val res = IncomeTaxViewChangeFrontend.getReportDeadlines

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyReportDeadlinesCall(testNino, testSelfEmploymentId, testMtditid)

          Then("the view displays the correct title")
          res should have(
            httpStatus(OK),
            pageTitle(messages.obligationsTitle)
          )

          Then("the page displays crystallised obligation information")
          res should have(
            elementTextBySelector("#crystallised-section-0 #crystallised-title")(expectedValue = "Whole tax year (final check)"),
            elementTextBySelector("#crystallised-section-0 #crystallised-due-on-title")(expectedValue = "Due on:"),
            elementTextBySelector("#crystallised-section-0 #crystallised-period")("6 April 2017 to 5 April 2018"),
            elementTextBySelector("#crystallised-section-0 #crystallised-due-date")("31 January 2019")
          )

          Then("the page displays no property or business obligation dates")
          res should have(
            isElementVisibleById("eops-return-section-0")(expectedValue = false),
            isElementVisibleById("quarterly-return-section-0")(expectedValue = false)
          )
        }
      }
    }

    "the ReportDeadlines Feature is disabled" should {

      "Redirect to the Income Tax View Change Home Page" in {


        disable(ReportDeadlines)

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub a single business obligation response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino, ObligationsModel(Seq(singleObligationOverdueModel(testSelfEmploymentId))))

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testNino, testSelfEmploymentId)

        Then("the result should have a HTTP status of SEE_OTHER (303) and redirect to the Income Tax home page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }

    }

  }
}
