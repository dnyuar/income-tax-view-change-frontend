package assets

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus, testNino}
import models.core.{Nino, NinoResponseError}
import play.api.libs.json.{JsValue, Json}

object NinoLookupTestConstants {
  val testNinoModel: Nino = Nino(nino = testNino)
  val testNinoModelJson: JsValue = Json.obj(
    "nino" -> testNino
  )

  val testNinoErrorModel: NinoResponseError = NinoResponseError(testErrorStatus, testErrorMessage)
  val testNinoErrorModelJson: JsValue = Json.obj(
    "status" -> testErrorStatus,
    "reason" -> testErrorMessage
  )
}
