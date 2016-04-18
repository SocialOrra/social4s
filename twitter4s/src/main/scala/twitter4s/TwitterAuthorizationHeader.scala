package twitter4s

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.ning.http.util.Base64
import http.client.method.PostMethod
import http.client.request.Request

import scala.compat.Platform

object TwitterAuthorizationHeader {

  private val ENCODING = "UTF-8"
  val oauthSignatureMethod = "HMAC-SHA1"
  val oauthVersion = "1.0"

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
  def generate(
    oauthConsumerKey:    String,
    oauthToken:          String,
    oauthConsumerSecret: String,
    oauthTokenSecret:    String,
    oauthNonce:          String = scala.util.Random.alphanumeric.take(16).mkString,
    oauthTimestamp:      String = (Platform.currentTime / 1000).toString)(
    baseUrl: String,
    request: Request): (String, String) = {

    val fieldsWithoutSignature = createOauthFieldsWithoutSignature(
      oauthConsumerKey,
      oauthToken,
      oauthConsumerSecret,
      oauthTokenSecret,
      oauthNonce,
      oauthTimestamp)

    val fields = fieldsWithoutSignature ++
      Map("oauth_signature" → oauthSignature(baseUrl, request, fieldsWithoutSignature, oauthConsumerSecret, oauthTokenSecret))

    val encodedFields = fields
      .toSeq
      // sort them alphabetically since that's what most Twitter tools do, easier for testing and debugging
      .sortBy(_._1)
      .map(kv ⇒ { percentEncode(kv._1).getOrElse(s"INVALID_KEY_${kv._1}") + "=\"" + percentEncode(kv._2).getOrElse(s"INVALID_VALUE_${kv._2}") + "\"" })

    val oauthHeader = "Authorization" → s"OAuth ${encodedFields.mkString(", ")}"

    oauthHeader
  }

  private[twitter4s] def createOauthFieldsWithoutSignature(
    oauthConsumerKey:    String,
    oauthToken:          String,
    oauthConsumerSecret: String,
    oauthTokenSecret:    String,
    oauthNonce:          String,
    oauthTimestamp:      String): Map[String, String] = {
    Map(
      "oauth_consumer_key" → oauthConsumerKey,
      "oauth_token" → oauthToken,
      "oauth_nonce" → oauthNonce,
      "oauth_signature_method" → oauthSignatureMethod,
      "oauth_timestamp" → oauthTimestamp,
      "oauth_version" → oauthVersion)
  }

  private[twitter4s] def percentEncode(s: String): Option[String] = {
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
    // TODO: clean this up, really ugly
    if (request.method.equals(PostMethod)) {

      // TODO: this only supports strings and byte arrays
      val body = request.body.map {
        case a: Array[Byte] ⇒ new String(a, "utf-8")
        case x              ⇒ x.toString
      }

      val params = body
        // get lines
        .map(b ⇒ {
          val lines = b.split("\n")
          val kvs = lines.map(line ⇒ line.split("="))
          // TODO: this will fail if the body doesn't have key=value format and we'll get array out of bounds
          kvs.map(kv ⇒ (kv(0), kv(1)))
        })

      params.getOrElse(Array.empty).toSeq

    } else { Seq.empty }
  }

  private[twitter4s] def createParameterString(request: Request, oauthFields: Map[String, String]): String = {

    val bodyParams = createBodyParams(request)

    (queryStringToSeq(request.queryString) ++ oauthFields.toSeq ++ bodyParams)
      // percent encode keys and values
      .map { kv ⇒ percentEncode(kv._1).getOrElse("") → percentEncode(kv._2).getOrElse("") }
      // sort by the key names
      .sortBy(_._1)
      // joing key and value with "="
      .map(kv ⇒ { kv._1 + "=" + kv._2 })
      // join key/value pairs with &
      .mkString("&")
  }

  private[twitter4s] def createSignatureBaseString(baseUrl: String, request: Request, parameterString: String): String = {
    val method = request.method.name.toUpperCase
    val url = percentEncode(s"$baseUrl${request.relativeUrl}").getOrElse("INVALID_URL")
    val pString = percentEncode(parameterString).getOrElse("INVALID_PARAMETER_STRING")

    val signatureBaseString = method + "&" + url + "&" + pString
    signatureBaseString
  }

  private val HMACSHA1 = "HmacSHA1"

  private def hmac(secret: String, toEncode: String): String = {
    val signingKey = new SecretKeySpec(secret.getBytes(), HMACSHA1)
    val mac = Mac.getInstance(HMACSHA1)
    mac.init(signingKey)
    // NOTE: if we use utf-8 then the signature will not be compatible with Twitter
    val rawHmac = mac.doFinal(toEncode.getBytes("utf-8"))
    new String(Base64.encode(rawHmac))
  }

  private[twitter4s] def createSigningKey(oauthConsumerSecret: String, oauthTokenSecret: String): String = {
    percentEncode(oauthConsumerSecret).getOrElse("INVALID_CONSUMER_SECRET") + "&" + percentEncode(oauthTokenSecret).getOrElse("INVALID_TOKEN_SECRET")
  }

  private def oauthSignature(baseUrl: String, request: Request, oauthFields: Map[String, String], oauthConsumerSecret: String, oauthTokenSecret: String): String = {
    val parameterString = createParameterString(request, oauthFields)
    val signatureBaseString = createSignatureBaseString(baseUrl, request, parameterString)
    val signingKey = createSigningKey(oauthConsumerSecret, oauthTokenSecret)
    val oauthSignatureString = hmac(signingKey, signatureBaseString)

    oauthSignatureString
  }

}
