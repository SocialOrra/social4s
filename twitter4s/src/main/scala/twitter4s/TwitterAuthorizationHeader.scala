package twitter4s

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

import http.client.method.PostMethod
import http.client.request.{ PostRequest, Request }

import scala.compat.Platform

class TwitterAuthorizationHeader(oauthConsumerKey: String, oauthToken: String, oauthConsumerSecret: String, oauthTokenSecret: String) {

  protected val ENCODING = "UTF-8"
  protected def oauthNonce = scala.util.Random.alphanumeric.take(16).mkString
  protected val oauthSignatureMethod = "HMAC-SHA1"
  protected def oauthTimestamp: String = Platform.currentTime / 1000 toString ()
  protected val oauthVersion = "1.0"

  /** Generates a Twitter Authorization header according to:
   *  https://dev.twitter.com/oauth/overview/authorizing-requests
   *
   *  with a signature according to:
   *  https://dev.twitter.com/oauth/overview/creating-signatures
   *
   *  with percent encoding from Netflix according to:
   *  https://dev.twitter.com/oauth/overview/percent-encoding-parameters
   *  http://oauth.googlecode.com/svn/code/java/core/commons/src/main/java/net/oauth/OAuth.java
   *
   *  @param request the request to generate the header for
   *  @return a Tuple of 2 strings, the header's name, and the value
   */
  def generate(request: Request): (String, String) = {
    val fieldsWithoutSignature = Map(
      "oauth-consumer-key" -> oauthConsumerKey,
      "oauth-token" -> oauthToken,
      "oauth-nonce" -> oauthNonce,
      "oauth-signature-method" -> oauthSignatureMethod,
      "oauth-timestamp" -> oauthTimestamp,
      "oauth-version" -> oauthVersion)

    val fields = fieldsWithoutSignature ++
      Map("oauth-signature" -> oauthSignature(request, fieldsWithoutSignature))

    val encodedFields = fields map (kv ⇒ { percentEncode(kv._1) + "=\"" + percentEncode(kv._2) + "\"" })
    val oauthHeader = "Authorization" -> s"OAuth ${encodedFields.mkString(",")}"

    oauthHeader
  }

  protected def percentEncode(s: String): Option[String] = {
    try {
      Some(URLEncoder.encode(s, ENCODING)
        // OAuth encodes some characters differently:
        .replace("+", "%20").replace("*", "%2A")
        .replace("%7E", "~"))
      // This could be done faster with more hand-crafted code.
    } catch {
      case e: UnsupportedEncodingException ⇒
        println(s"Unsupported encoding: ${e.getMessage} ${e.getStackTraceString}")
        None
    }
  }

  private def queryStringToSeq(fields: Map[String, Seq[String]]): Seq[(String, String)] =
    fields.flatMap(keyAndValues ⇒ {
      val key = keyAndValues._1
      keyAndValues._2.map(value ⇒ (key, value)).toList
    }).toSeq

  private def createBodyParams(request: Request): Seq[(String, String)] = {
    if (request.method.equals(PostMethod)) {
      val body = request.asInstanceOf[PostRequest[_]].body.map(_.toString())

      val params = body
        // get lines
        .map(b ⇒ {
          val lines = b.split("\n")
          val kvs = lines.map(line ⇒ line.split("="))
          kvs.map(kv ⇒ (kv(0), kv(1)))
        })

      params.getOrElse(Array.empty).toSeq

    } else { Seq.empty }
  }

  private def createParameterString(request: Request, oauthFields: Map[String, String]): String = {

    // TODO: clean this up, really ugly
    val bodyParams = createBodyParams(request)

    (queryStringToSeq(request.queryString) ++ oauthFields.toSeq ++ bodyParams)
      // percent encode keys and values
      .map { kv ⇒ percentEncode(kv._1).getOrElse("") -> percentEncode(kv._2).getOrElse("") }
      // sort by the key names
      .sortBy(_._1)
      // joing key and value with "="
      .map(kv ⇒ { kv._1 + "=" + kv._2 })
      // join key/value pairs with &
      .mkString("&")
  }

  private def createSignatureBaseString(request: Request, parameterString: String): String = {
    val method = request.method.name.toUpperCase
    val url = percentEncode(s"https://api.twitter.com/${request.relativeUrl}") // TODO: fetch from config or param
    val pString = percentEncode(parameterString)

    val signatureBaseString = method + "&" + url + "&" + pString
    signatureBaseString
  }

  protected def oauthSignature(request: Request, oauthFields: Map[String, String]): String = {
    val method = request.method.name
    val baseUrl = s"https://api.twitter.com/${request.relativeUrl}" // TODO: fetch from config or param
    val parameterString = createParameterString(request, oauthFields)
    val signatureString = createSignatureBaseString(request, parameterString)
    val signingKey = percentEncode(oauthConsumerSecret) + "&" + percentEncode(oauthTokenSecret)
    // TODO: calculate HMAC
    ""
  }

}
