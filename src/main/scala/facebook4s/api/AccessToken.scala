package facebook4s.api

/** Represents a Facebook API access token.
 *  @param token the token itself
 *  @param expires the expiry time
 */
case class AccessToken(token: String, expires: Long)
