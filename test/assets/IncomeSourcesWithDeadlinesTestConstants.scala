package assets

import assets.BusinessDetailsTestConstants.{business2, obligationsDataSuccessModel}
import assets.ReportDeadlinesTestConstants.reportDeadlinesDataPropertySuccessModel
import models.reportDeadlines.ObligationsModel

object IncomeSourcesWithDeadlinesTestConstants {
  val businessAndPropertyIncomeWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel,
    obligationsDataSuccessModel.copy(identification = business2.incomeSourceId), reportDeadlinesDataPropertySuccessModel))
  val singleBusinessIncomeWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel))
  val propertyIncomeOnlyWithDeadlines = ObligationsModel(Seq(reportDeadlinesDataPropertySuccessModel))
  val businessAndPropertyAlignedWithDeadlines = ObligationsModel(Seq(obligationsDataSuccessModel, reportDeadlinesDataPropertySuccessModel))
  val noIncomeDetailsWithNoDeadlines = ObligationsModel(Seq.empty)
}
