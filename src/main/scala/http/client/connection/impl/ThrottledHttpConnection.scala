package http.client.connection.impl

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import akka.pattern.ask
import akka.actor.{ Actor, ActorSystem, Props }
import akka.util.Timeout
import http.client.connection.HttpConnection
import http.client.request.{ BatchRequest, GetRequest, PostRequest }
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
      println("throttling done, making get")
      connection.get(getRequest)
    }
  }

  override def post[T](postRequest: PostRequest[T])(implicit ec: ExecutionContext, writeable: Writeable[T]): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled flatMap { _ ⇒
      println("throttling done, making post")
      connection.post(postRequest)
    }
  }

  override def batch(batchRequest: BatchRequest)(implicit ec: ExecutionContext): Future[HttpResponse] = {
    implicit val timeout = new Timeout(requestTimeoutDuration)
    actor ask Throttled flatMap { _ ⇒
      println("throttling done, making batch")
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

class ThrottlingActor extends Actor {
  def receive = {
    case Throttled ⇒
      println("Being asked if a command can run, sleeping...")
      Thread.sleep(100)
      println("Waking up!")
      sender ! Throttled
    case Shutdown ⇒
      println(s"Shutting down ${getClass.getName}")
      sender ! Shutdown
    case x ⇒
      println(s"Dunno what to do with message, ignoring: $x")
      sender ! None
  }
}

