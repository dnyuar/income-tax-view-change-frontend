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

package controllers

import java.time.LocalDate

import assets.BaseTestConstants
import config.ItvcErrorHandler
import config.featureswitch.{FeatureSwitching, ObligationsPage}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.reportDeadlines._
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.Helpers._
import services.ReportDeadlinesService

import scala.concurrent.Future

class PreviousObligationsControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching{

  class Setup(featureSwitchEnabled: Boolean = true) {
    val reportDeadlinesService: ReportDeadlinesService = mock[ReportDeadlinesService]

    val controller = new PreviousObligationsController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      reportDeadlinesService,
      appConfig,
      ec,
      messagesApi
    )

    if(featureSwitchEnabled) {
      enable(ObligationsPage)
    } else {
      disable(ObligationsPage)
    }
  }

  val date: LocalDate = LocalDate.now

  "getPreviousObligations" should {
    s"redirect ($SEE_OTHER) to the home page when the feature switch is off" in new Setup(false) {
      mockSingleBusinessIncomeSource()

      val result: Future[Result] = controller.getPreviousObligations(fakeRequestWithActiveSession)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
    }

    s"return $OK with html" in new Setup {
      mockBothIncomeSourcesBusinessAligned()
      when(reportDeadlinesService.getReportDeadlines(matches(true))(any(), any()))
          .thenReturn(Future.successful(ObligationsModel(Seq(
            ReportDeadlinesModel(BaseTestConstants.testSelfEmploymentId, List(ReportDeadlineModel(date, date, date, "Quarterly", Some(date), "#001"))),
            ReportDeadlinesModel(BaseTestConstants.testPropertyIncomeId, List(ReportDeadlineModel(date, date, date, "EOPS", Some(date), "EOPS")))
          ))))

      val result: Result = await(controller.getPreviousObligations(fakeRequestWithActiveSession))
      
      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    s"return $OK with html when previous deadlines returns an error response" in new Setup {
      mockBothIncomeSourcesBusinessAligned()
      when(reportDeadlinesService.getReportDeadlines(matches(true))(any(), any()))
          .thenReturn(Future.successful(ReportDeadlinesErrorModel(500, "error")))

      val result: Result = await(controller.getPreviousObligations(fakeRequestWithActiveSession))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

}
