package assets

import assets.BaseTestConstants.{testMtditid, testPaymentRedirectUrl}
import models.core.PaymentDataModel
import play.api.libs.json.{JsValue, Json}

object PaymentDataTestConstants {

  val testTaxType = "mtdfb-itsa"
  val testAmountInPence = 10000

  val testPaymentDataModel: PaymentDataModel = PaymentDataModel(testTaxType, testMtditid, testAmountInPence, testPaymentRedirectUrl)

  val testPaymentDataJson: JsValue =
    Json.obj(
      "taxType" -> testTaxType,
      "taxReference" -> testMtditid,
      "amountInPence" -> testAmountInPence,
      "returnUrl" -> testPaymentRedirectUrl
    )

}
