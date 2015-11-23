package auction

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

class Notifier(publisher: ActorRef) extends Actor {
  import Notifier._

  override val supervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: Exception => Restart
    }
  }

  override def receive: Receive = {
    case msg: Notify =>
      context.actorOf(Props(new NotifierRequest(publisher, msg)))
  }
}

object Notifier {
  case class Notify(title: String, winner: Option[ActorRef], topBid: BigDecimal)
}
