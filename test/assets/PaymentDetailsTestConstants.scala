package assets

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus}
import models.paymentAllocations.{PaymentAllocationsError, PaymentDetails}
import play.api.libs.json.{JsValue, Json}

object PaymentDetailsTestConstants {

  val testValidPaymentDetailsJson: JsValue = Json.obj(
    "paymentDetails" -> Json.arr(PaymentAllocationsTestConstants.testValidPaymentAllocationsModelJson)
  )

  val testValidPaymentDetails: PaymentDetails = PaymentDetails(
    paymentDetails = Seq(PaymentAllocationsTestConstants.testValidPaymentAllocationsModel)
  )

  val testInvalidPaymentAllocationsModelJson: JsValue = Json.obj(
    "amount" -> "invalidAmount",
    "payMethod" -> "Payment by Card",
    "valDate" -> "2019-05-27"
  )

  val testPaymentAllocationsErrorModelParsing: PaymentAllocationsError = PaymentAllocationsError(
    testErrorStatus, "Json Validation Error. Parsing Payment Allocations Data Response")

  val testPaymentAllocationsErrorModel: PaymentAllocationsError = PaymentAllocationsError(testErrorStatus, testErrorMessage)
  val testPaymentAllocationsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )
}
