
package assets

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus}
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import play.api.libs.json.{JsValue, Json}

object CitizenDetailsTestConstants {
  val testValidCitizenDetailsModelJson: JsValue = Json.obj(
    "name" -> Json.obj(
      "current" -> Json.obj(
        "firstName" -> "John",
        "lastName" -> "Smith"),
      "previous" -> ""
    ),
    "ids" -> Json.obj("nino" -> "AA055075C"),
    "dateOfBirth" -> "11121971"
  )

  val testValidCitizenDetailsModel: CitizenDetailsModel = CitizenDetailsModel(Some("John"), Some("Smith"), Some("AA055075C"))

  val testInvalidCitizenDetailsJson: JsValue = Json.obj(
    "ids" -> Json.obj("nino" -> 123.4)
  )

  val testCitizenDetailsErrorModelParsing: CitizenDetailsErrorModel = CitizenDetailsErrorModel(
    testErrorStatus, "Json Validation Error. Parsing Citizen Details Data Response")

  val testCitizenDetailsErrorModel: CitizenDetailsErrorModel = CitizenDetailsErrorModel(testErrorStatus, testErrorMessage)
  val testCitizenDetailsErrorModelJson: JsValue = Json.obj(
    "code" -> testErrorStatus,
    "message" -> testErrorMessage
  )
}
