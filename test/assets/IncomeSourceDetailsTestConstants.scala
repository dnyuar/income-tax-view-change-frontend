
package assets

import java.time.LocalDate

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testMigrationYear2019}
import assets.BusinessDetailsTestConstants._
import assets.PropertyDetailsTestConstants.propertyDetails
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}

object IncomeSourceDetailsTestConstants {
  val businessesAndPropertyIncome = IncomeSourceDetailsModel(testMtdItId, None, List(business1, business2), Some(propertyDetails))
  val singleBusinessIncome = IncomeSourceDetailsModel(testMtdItId, Some("2017"), List(business1), None)
  val singleBusinessIncomeWithCurrentYear = IncomeSourceDetailsModel(testMtdItId, Some(LocalDate.now().getYear.toString), List(business1), None)
  val businessIncome2018and2019 = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), None)
  val propertyIncomeOnly = IncomeSourceDetailsModel(testMtdItId, None, List(), Some(propertyDetails))
  val businessAndPropertyAligned = IncomeSourceDetailsModel(testMtdItId, None, List(alignedBusiness), Some(propertyDetails))
  val singleBusinessAndPropertyMigrat2019 = IncomeSourceDetailsModel(testMtdItId, Some(testMigrationYear2019), List(alignedBusiness), Some(propertyDetails))
  val noIncomeDetails = IncomeSourceDetailsModel(testMtdItId, None, List(), None)
  val errorResponse = IncomeSourceDetailsError(testErrorStatus, testErrorMessage)
  val businessIncome2018and2019AndProp = IncomeSourceDetailsModel(testMtdItId, None, List(business2018, business2019), Some(propertyDetails))
  val oldUserDetails = IncomeSourceDetailsModel(testMtdItId, None, List(oldUseralignedBusiness), Some(propertyDetails))
}
