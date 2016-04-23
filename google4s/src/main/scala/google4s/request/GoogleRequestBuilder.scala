package google4s

import scala.collection.JavaConverters._
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.{GoogleJsonError, GoogleJsonResponseException}
import com.google.api.client.http.{HttpHeaders, HttpTransport}
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.DataStoreFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.analytics.Analytics
import com.google.api.services.analytics.AnalyticsScopes
import com.google.api.services.analytics.model._
import com.google.api.services.analytics.model.GaData.ColumnHeaders
import java.io.IOException
import java.io.InputStreamReader
import java.util.Collections
import java.util.List

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback

package twitter4s.request

import http.client.connection.HttpConnection
import http.client.request.Request
import http.client.response.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

abstract class GoogleRequest extends Request {

  val paginated: Boolean

  override def toJson(extraQueryStringParams: Map[String, Seq[String]]): String = "{}"

  def nextRequest(response: HttpResponse): GoogleRequest
}

object GoogleRequesBuilderApp

class GoogleRequestBuilder(connection: HttpConnection) {

  def shutdown() = connection.shutdown()

  def makeRequest[R <: GoogleRequest](request: GoogleRequest): Future[(Request, Seq[HttpResponse])] = {
    executeWithPagination(request)
  }

  def executeWithPagination[R <: GoogleRequest](request: GoogleRequest)(implicit ec: ExecutionContext): Future[(Request, Seq[HttpResponse])] = {
    val f = _executeWithPagination(request) map { requestAndResponseParts ⇒
      // TODO: could there be no parts?
      val request = requestAndResponseParts._1
      val parts = requestAndResponseParts._2
      (request, parts)
    }
    f
  }

  private def _executeWithPagination[R <: GoogleRequest](
    request:                GoogleRequest,
    completedResponseParts: Seq[HttpResponse] = Seq.empty)(
    implicit
    ec: ExecutionContext): Future[(GoogleRequest, Seq[HttpResponse])] = {

    val responseF = connection.makeRequest(request)

    responseF.map { response ⇒

      val newRequest = if (!isRequestComplete(request, response))
        Some(newRequestFromIncompleteRequest(request, response))
      else None

      (response, newRequest)

    } flatMap {
      case (responsePart, Some(newRequest)) ⇒
        _executeWithPagination(newRequest, completedResponseParts ++ Seq(responsePart))
      case (responsePart, None) ⇒
        Future.successful { (request, completedResponseParts ++ Seq(responsePart)) }
    }
  }

  private def isRequestComplete[R <: GoogleRequest](request: GoogleRequest, response: HttpResponse): Boolean = {

    if (response.status == 200) {
      request.isComplete(response)
    } else {
      // error
      true
    }
  }

  protected def newRequestFromIncompleteRequest[R <: GoogleRequest](request: GoogleRequest, response: HttpResponse): GoogleRequest = {
    request.nextRequest(response)
  }
}


object GoogleRequestBuilderApp2 { // extends App {
  /** Be sure to specify the name of your application. If the application name is {@code null} or
   *  blank, the application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private val APPLICATION_NAME = ""

  /** Directory to store user credentials. */
  private val DATA_STORE_DIR =
    new java.io.File(System.getProperty("user.home"), ".store/analytics_sample")

  /** Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
   *  globally shared instance across your application.
   */
  private var dataStoreFactory: FileDataStoreFactory = _

  /** Global instance of the HTTP transport. */
  private var httpTransport: HttpTransport = _

  /** Global instance of the JSON factory. */
  private val JSON_FACTORY = JacksonFactory.getDefaultInstance()

  try {
    httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR)
    val analytics = initializeAnalytics()
    val profileId = getFirstProfileId(analytics)

    if (profileId == null) {
      System.err.println("No profiles found.")
    } else {
      val gaData = executeDataQuery(analytics, profileId)
      printGaData(gaData)
    }

    val callback = new JsonBatchCallback[Object]() {

      override def onSuccess(obj: Object, responseHeaders: HttpHeaders): Unit = {
        println(s"got object: $obj")
      }

      override def onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders): Unit = {
        println("Error Message: " + e.getMessage())
      }
    }

    val batch = new BatchRequest(httpTransport, /* httpRequestInitializer not needed */ null)

    analytics.management().accounts().list.queue(batch, callback.asInstanceOf[JsonBatchCallback[Accounts]])
    analytics.management().accountSummaries().list.queue(batch, callback.asInstanceOf[JsonBatchCallback[AccountSummaries]])
    analytics.data().mcf()

    batch.execute()

    println("Waiting 5000 millis...")
    Thread.sleep(5000)

  } catch {
    case e: GoogleJsonResponseException ⇒
      println("There was a service error: " + e.getDetails().getCode() + " : " + e.getDetails().getMessage())

    case e: Exception ⇒
      e.printStackTrace()
  }

  /** Authorizes the installed application to access user's protected data. */
  @throws[Exception]
  def authorize(): Credential = {
    // load client secrets
    val clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(getClass.getResourceAsStream("/client_secrets.json")))

    if (clientSecrets.getDetails().getClientId().startsWith("Enter") || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
      println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=analytics into google4s/src/main/resources/client_secrets.json")
      sys.exit(1)
    }

    // set up authorization code flow
    val flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(AnalyticsScopes.ANALYTICS_READONLY))
      .setDataStoreFactory(dataStoreFactory)
      .build()
    // authorize
    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user")
  }

  /** Performs all necessary setup steps for running requests against the API.
   *
   *  @return An initialized Analytics service object.
   *
   *  @throws Exception if an issue occurs with OAuth2Native authorize.
   */
  @throws[Exception]
  def initializeAnalytics(): Analytics = {
    // Authorization.
    val credential = authorize()

    // Set up and return Google Analytics API client.
    new Analytics.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build()
  }

  /** Returns the first profile id by traversing the Google Analytics Management API. This makes 3
   *  queries, first to the accounts collection, then to the web properties collection, and finally
   *  to the profiles collection. In each request the first ID of the first entity is retrieved and
   *  used in the query for the next collection in the hierarchy.
   *
   *  @param analytics the analytics service object used to access the API.
   *  @return the profile ID of the user's first account, web property, and profile.
   *  @throws IOException if the API encounters an error.
   */
  @throws[IOException]
  def getFirstProfileId(analytics: Analytics): String = {

    var profileId: String = null

    // Query accounts collection.
    val accounts = analytics.management().accounts().list().execute()

    if (accounts.getItems().isEmpty()) {
      println("No accounts found")
    } else {
      val firstAccountId = accounts.getItems().get(0).getId()

      // Query webproperties collection.
      val webproperties = analytics.management().webproperties().list(firstAccountId).execute()

      if (webproperties.getItems().isEmpty()) {
        println("No Webproperties found")
      } else {
        val firstWebpropertyId = webproperties.getItems().get(0).getId()

        // Query profiles collection.
        val profiles = analytics.management().profiles().list(firstAccountId, firstWebpropertyId).execute()

        if (profiles.getItems().isEmpty()) {
          println("No profiles found")
        } else {
          profileId = profiles.getItems().get(0).getId()
        }
      }
    }

    profileId
  }

  /** Returns the top 25 organic search keywords and traffic source by visits. The Core Reporting API
   *  is used to retrieve this data.
   *
   *  @param analytics the analytics service object used to access the API.
   *  @param profileId the profile ID from which to retrieve data.
   *  @return the response from the API.
   *  @throws IOException tf an API error occured.
   */
  @throws[Exception]
  def executeDataQuery(analytics: Analytics, profileId: String): GaData = {
    analytics.data().ga().get(
      "ga:" + profileId, // Table Id. ga: + profile id.
      "2012-01-01", // Start date.
      "2012-01-14", // End date.
      "ga:visits") // Metrics.
      .setDimensions("ga:source,ga:keyword")
      .setSort("-ga:visits,ga:source")
      .setFilters("ga:medium==organic")
      .setMaxResults(25)
      .execute()

    /*

    // https://www.googleapis.com/analytics/v3
    // /data/ga
    // ?
    // ids=ga:84943435 &
    // start-date=2015-12-01 &
    // end-date=2016-01-01 &
    // metrics=ga:sessions &
    // dimensions=ga:socialNetwork &
    // max-results=1 &
    // access_token=.....

    {
    	"kind": "analytics#gaData",
    	"id": "https://www.googleapis.com/analytics/v3/data/ga?ids=ga:84943435&dimensions=ga:socialNetwork&metrics=ga:sessions&start-date=2015-12-01&end-date=2016-01-01&max-results=1",
    	"query": {
    		"start-date": "2015-12-01",
    		"end-date": "2016-01-01",
    		"ids": "ga:84943435",
    		"dimensions": "ga:socialNetwork",
    		"metrics": ["ga:sessions"],
    		"start-index": 1,
    		"max-results": 1
    	},
    	"itemsPerPage": 1,
    	"totalResults": 7,
    	"selfLink": "https://www.googleapis.com/analytics/v3/data/ga?ids=ga:84943435&dimensions=ga:socialNetwork&metrics=ga:sessions&start-date=2015-12-01&end-date=2016-01-01&max-results=1",
    	"nextLink": "https://www.googleapis.com/analytics/v3/data/ga?ids=ga:84943435&dimensions=ga:socialNetwork&metrics=ga:sessions&start-date=2015-12-01&end-date=2016-01-01&start-index=2&max-results=1",
    	"profileInfo": {
    		"profileId": "84943435",
    		"accountId": "50132120",
    		"webPropertyId": "UA-50132120-1",
    		"internalWebPropertyId": "82039823",
    		"profileName": "All Web Site Data",
    		"tableId": "ga:84943435"
    	},
    	"containsSampledData": false,
    	"columnHeaders": [{
    		"name": "ga:socialNetwork",
    		"columnType": "DIMENSION",
    		"dataType": "STRING"
    	}, {
    		"name": "ga:sessions",
    		"columnType": "METRIC",
    		"dataType": "INTEGER"
    	}],
    	"totalsForAllResults": {
    		"ga:sessions": "433"
    	},
    	"rows": [
    		["(not set)", "353"]
    	]
    }
      */
  }

  /** Prints the output from the Core Reporting API. The profile name is printed along with each
   *  column name and all the data in the rows.
   *
   *  @param results data returned from the Core Reporting API.
   */
  def printGaData(results: GaData): Unit = {
    println("printing results for profile: " + results.getProfileInfo.getProfileName)

    if (results.getRows == null || results.getRows.isEmpty) {
      println("No results Found.")
    } else {

      // Print column headers.
      results.getColumnHeaders.asScala.foreach(header ⇒ printf("%30s", header.getName))
      println()

      // Print actual data.
      results.getRows.asScala.foreach { row ⇒
        row.asScala.foreach(column ⇒ printf("%30s", column))
        println("")
      }

      println("")
    }
  }
}