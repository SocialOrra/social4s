package http.client.connection.impl

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import com.github.bucket4j.Buckets
import com.typesafe.config.ConfigFactory
import http.client.connection.HttpConnection
import http.client.request.HttpRequest
import http.client.response.HttpResponse

trait ThrottledHttpConnection extends HttpConnection {

  protected val actorSystem: ActorSystem
  protected val connection: HttpConnection

  protected val config = ConfigFactory.load()
  protected val numberOfRequests: scala.Long = config.getLong("http.client.connection.throttled.number-of-requests")
  protected val periodInSeconds: scala.Long = config.getLong("http.client.connection.throttled.period-in-seconds")

  protected val actorProps = Props(classOf[ThrottlingActor], numberOfRequests, TimeUnit.SECONDS, periodInSeconds)
  lazy protected val actor = actorSystem.actorOf(actorProps)

  // TODO: make configurable
  protected val requestTimeoutDuration = 60.seconds
  protected val shutdownTimeoutDuration = 20.seconds

  /** This can be overridden in order to throttle subsequent requestes based on the response
   *  of the last made request.
   */
  protected def throttleNextRequest(request: HttpRequest, response: HttpResponse): Boolean = false

  override def makeRequest(request: HttpRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled(request) flatMap { _ ⇒
      connection.makeRequest(request)
    } map { response ⇒

      // if this response forces us to slow down, we send a throttling message
      // so that the next request will be throttled
      if (throttleNextRequest(request, response)) actor ! ThrottleImmediately

      response
    }
  }

  override def shutdown(): Unit = {
    implicit val timeout = new Timeout(shutdownTimeoutDuration)
    val f = actor ask Shutdown
    Await.result(f, shutdownTimeoutDuration)
    connection.shutdown()
  }
}
case class Throttled(request: HttpRequest)
case object ThrottleImmediately
case object Shutdown

class ThrottlingActor(numRequests: Long, timeUnit: TimeUnit, period: Long) extends Actor with ActorLogging {

  log.info(s"Throttling to $numRequests per $period ${timeUnit.toString}")
  protected val bucket = Buckets.withNanoTimePrecision()
    .withLimitedBandwidth(numRequests, timeUnit, period)
    .build()

  // TODO: make rate limiting pluggable, perhaps based on rules (ex: twitter per advertiser rate limiting)
  def receive = {
    case t @ Throttled(request) ⇒
      log.debug("Being asked to check if throttled.")
      bucket.consume(1)
      log.debug("Throttling completed.")
      sender ! t
    case t @ ThrottleImmediately ⇒
      log.debug("Being asked to throttle immediately.")
      bucket.consumeAsMuchAsPossible()
      sender ! t

    case Shutdown ⇒
      log.info(s"Shutting down ${getClass.getName}")
      sender ! Shutdown
    case x ⇒
      log.warning(s"Improper message received, ignoring: $x")
      sender ! None
  }
}

