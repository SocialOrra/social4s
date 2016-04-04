package http.client.connection.impl

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import com.github.bucket4j.Buckets
import http.client.connection.HttpConnection
import http.client.request.{BatchRequest, GetRequest, PostRequest}
import http.client.response.HttpResponse
import play.api.http.Writeable

trait ThrottledHttpConnection extends HttpConnection {

  protected val actorSystem: ActorSystem
  protected val connection: HttpConnection
  protected val actorProps = Props(classOf[ThrottlingActor])
  lazy protected val actor = actorSystem.actorOf(actorProps, "throttling-actor")

  // TODO: make configurable
  protected val requestTimeoutDuration = 5.seconds
  protected val shutdownTimeoutDuration = 20.seconds

  override def get(getRequest: GetRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled flatMap { _ ⇒
      connection.get(getRequest)
    }
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, writeable: Writeable[T]): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled flatMap { _ ⇒
      connection.post(postRequest)
    }
  }

  override def batch(batchRequest: BatchRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled flatMap { _ ⇒
      connection.batch(batchRequest)
    }
  }

  override def shutdown(): Unit = {
    implicit val timeout = new Timeout(shutdownTimeoutDuration)
    val f = actor ask Shutdown
    Await.result(f, shutdownTimeoutDuration)
    connection.shutdown()
  }
}
case object Throttled
case object Shutdown

class ThrottlingActor extends Actor with ActorLogging {

  // TODO: make this configurable
  protected val bucket = Buckets.withNanoTimePrecision()
    .withLimitedBandwidth(80, TimeUnit.MINUTES, 1)
    .build()

  def receive = {
    case Throttled ⇒
      log.debug("Being asked to check if throttled.")
      bucket.consume(1)
      log.debug("Throttling completed.")
      sender ! Throttled
    case Shutdown ⇒
      log.info(s"Shutting down ${getClass.getName}")
      sender ! Shutdown
    case x ⇒
      log.warning(s"Improper message received, ignoring: $x")
      sender ! None
  }
}

