
package assets

import models.calculation.BillsViewModel

object BillsTestConstants {
  val paidBillsViewModel: BillsViewModel = BillsViewModel(
    currentBill = 123.45,
    isPaid = true,
    taxYear = 2018
  )

  val unpaidBillsViewModel: BillsViewModel = BillsViewModel(
    currentBill = 123.45,
    isPaid = false,
    taxYear = 2018
  )
}
