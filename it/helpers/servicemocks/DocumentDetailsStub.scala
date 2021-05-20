
package helpers.servicemocks

import java.time.LocalDate

import assets.BaseIntegrationTestConstants.taxYear
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate}

object DocumentDetailsStub {
  def docDetail(documentDescription:String): DocumentDetail = DocumentDetail(
    taxYear = taxYear,
    transactionId = "1040000124",
    documentDescription = Some(documentDescription),
    originalAmount = Some(10.34),
    outstandingAmount = Some(1.2)
  )

  def docDateDetail(dueDate: String, chargeType: String): DocumentDetailWithDueDate = DocumentDetailWithDueDate(
    documentDetail = docDetail(chargeType),
    dueDate = Some(LocalDate.parse(dueDate))
  )
}
