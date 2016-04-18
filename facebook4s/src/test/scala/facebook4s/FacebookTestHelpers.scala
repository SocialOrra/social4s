package facebook4s

import facebook4s.response._
import http.client.response.HttpHeader
import play.api.libs.json._

object FacebookTestHelpers {

  val NUM_SUCCESSES = 3
  val NUM_ERRORS = 3
  val JSON_ERROR_CODE = 190
  val HTTP_SUCCESS_CODE = 200
  val HTTP_ERROR_CODE = 403
  val NAME = "Some Name"

  def jsonPartResponse(id: Int): String =
    s"""
       |{
       |  "code": $HTTP_SUCCESS_CODE,
       |  "headers": [ { "name": "Content-Type", "value": "text/javascript; charset=UTF-8" } ],
       |  "body": "${jsonSuccessBody(id).replaceAll("\n", "").replaceAll("""\"""", """\\"""")}"
       |}""".stripMargin

  def jsonSuccessBody(id: Int): String = {
    s"""
       |{
       |  "name": "$NAME",
       |  "id": "$id"
       |}""".stripMargin
  }

  def jsonErrorBody(id: Int): String = {
    s"""
       |{
       |  "error": {
       |    "message": "Message $id",
       |    "type": "OAuthException",
       |    "code": $JSON_ERROR_CODE,
       |    "error_subcode": 460,
       |    "error_user_title": "A title",
       |    "error_user_msg": "A message",
       |    "fbtrace_id": "EJplcsCHuLu"
       |  }
       |}""".stripMargin
  }

  def jsonPartErrorResponse(id: Int) =
    s"""
       | { "code": $HTTP_ERROR_CODE,
       |   "headers": [ { "name": "Content-Type", "value": "text/javascript; charset=UTF-8" } ],
       |   "body": "${jsonErrorBody(id).replaceAll("\n", "").replaceAll("""\"""", """\\"""")}"
       | }""".stripMargin

  def makeJsonBody(numSuccess: Int, numErrors: Int) =
    (1 to numSuccess).map { jsonPartResponse } ++ (1 to numErrors).map { jsonPartErrorResponse }

  def makeJsonResponse(numSuccess: Int, numErrors: Int) =
    "[" + makeJsonBody(numSuccess, numErrors).mkString(",") + "]"

  val defaultHeaders = Seq(HttpHeader("Content-Type", "text/javascript; charset=UTF-8"))
  val defaultPartHeaders = Seq(HttpHeader("Content-Type", "text/javascript; charset=UTF-8"))

  def makeBatchResponsePartBodyData(name: String = "data-name", period: String = "day", value: JsArray): JsObject = Json.obj(
    "name" → name,
    "period" → period,
    "values" → value)

  def makeBatchResponsePartBody(data: Seq[JsObject], paging: FacebookTimePaging): JsObject = Json.obj(
    "data" → data,
    "paging" → Json.toJson(paging))

  def makeBatchResponsePart(code: Int = 200, headers: Seq[HttpHeader] = defaultPartHeaders, body: JsObject): FacebookBatchResponsePart = {
    FacebookBatchResponsePart(code, headers, body.toString)
  }

  def makeBatchResponse(code: Int = 200, headers: Seq[HttpHeader] = defaultHeaders, parts: Seq[FacebookBatchResponsePart]): FacebookBatchResponse = {
    FacebookBatchResponse(code, code.toString, headers, Array.empty, Json.parse("{}"), parts)
  }

  def jsonDataPartResponse(id: Int): String =
    s"""
       |{
       |  "code": $HTTP_SUCCESS_CODE,
       |  "headers": [ { "name": "Content-Type", "value": "text/javascript; charset=UTF-8" } ],
       |  "body": "${makeJsonData(id).replaceAll("\n", "").replaceAll("""\"""", """\\"""")}"
       |}""".stripMargin

  def makeJsonData(numResponse: Int) =
    s"""
     | {
     |   "data": [ ${(1 to numResponse).map { jsonDataPart }.mkString(",")} ],
     |   "paging": {
     |     "previous": "since=0&until=1",
     |     "next": "since=2&until=3"
     |   }
     | }
   """.stripMargin

  def jsonDataPart(id: Int): String =
    s"""
       |{
       |  "name": "data-name",
       |  "period": "day",
       |  "values": [${jsonDataBody(id).replaceAll("\n", "")}]
       |}""".stripMargin

  def jsonDataBody(id: Int): String = {
    s"""
       |{
       |  "value": "$id"
       |}""".stripMargin
  }
}
