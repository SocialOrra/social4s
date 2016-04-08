package http.client.connection.impl

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import akka.pattern.ask
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.util.Timeout
import com.github.bucket4j.Buckets
import com.typesafe.config.ConfigFactory
import http.client.connection.HttpConnection
import http.client.request.{ BatchRequest, GetRequest, PostRequest, Request }
import http.client.response.HttpResponse
import play.api.http.Writeable

trait ThrottledHttpConnection extends HttpConnection {

  protected val actorSystem: ActorSystem
  protected val connection: HttpConnection

  protected val config = ConfigFactory.load()
  protected val numberOfRequests: scala.Long = config.getLong("http.client.connection.throttled.number-of-requests")
  protected val periodInSeconds: scala.Long = config.getLong("http.client.connection.throttled.period-in-seconds")

  protected val actorProps = Props(classOf[ThrottlingActor], numberOfRequests, TimeUnit.SECONDS, periodInSeconds)
  lazy protected val actor = actorSystem.actorOf(actorProps, "throttling-actor")

  // TODO: make configurable
  protected val requestTimeoutDuration = 5.seconds
  protected val shutdownTimeoutDuration = 20.seconds

  override def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled(getRequest) flatMap { _ ⇒
      connection.get(getRequest)
    }
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, writeable: Writeable[T]): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled(postRequest) flatMap { _ ⇒
      connection.post(postRequest)
    }
  }

  override def shutdown(): Unit = {
    implicit val timeout = new Timeout(shutdownTimeoutDuration)
    val f = actor ask Shutdown
    Await.result(f, shutdownTimeoutDuration)
    connection.shutdown()
  }
}
case class Throttled(request: Request)
case object Shutdown

class ThrottlingActor(numRequests: Long, timeUnit: TimeUnit, period: Long) extends Actor with ActorLogging {

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
    case Shutdown ⇒
      log.info(s"Shutting down ${getClass.getName}")
      sender ! Shutdown
    case x ⇒
      log.warning(s"Improper message received, ignoring: $x")
      sender ! None
  }
}

