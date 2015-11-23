package auction

import akka.actor._
import akka.util.Timeout
import auction.Notifier.Notify
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import akka.pattern.ask

class NotifierRequest(publisher: ActorRef, notify: Notify) extends Actor {
  implicit val timeout = Timeout(5 seconds)
  val future = publisher ? notify
  Await.result(future, timeout.duration)

  override def receive: Receive = {
    case _ =>
  }
}
