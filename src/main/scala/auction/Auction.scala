package auction;

import akka.actor._
import akka.event.LoggingReceive
import akka.persistence.{RecoveryCompleted, PersistentActor}
import auction.Auction.PersistentState.PersistentState
import scala.concurrent.duration._
import auction.Buyer.AuctionWon
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal

class Auction(title: String, seller: ActorRef, auctionSearchPath: String) extends PersistentActor {
  import Auction._
  import context._
   
  var topBid = BigDecimal(0)
  var winner: Option[ActorRef] = None
  var bidTimerScheduleTime: Option[Long] = None
  var bidTimeoutRecovery: Option[Long] = None

  override def receiveCommand: Receive = LoggingReceive {
    case Start =>
      context.actorSelection(auctionSearchPath) ! AuctionSearch.Register(title split " ")
      bidTimerScheduleTime = Option(System.currentTimeMillis())
      system.scheduler.scheduleOnce(bidTimeout, self, BidExpired)

      persist(EvtNewState(PersistentState.Created)) { _ =>
        context become created
      }
  }
  
  def created: Receive = LoggingReceive {    
    case Bid(amount) =>
      persist(EvtNewBid(amount)) { _ =>
        topBid = amount
      }

      persist(EvtNewWinner(Option(sender()))) { _ =>
        winner = Option(sender())
        persist(EvtNewState(PersistentState.Activated)) { _ =>
          context become activated
        }
      }
    case BidExpired =>
      system.scheduler.scheduleOnce(deleteTimeout, self, DeleteExpired)
      persist(EvtNewState(PersistentState.Ignored)) { _ =>
        context become ignored
      }
    case _ => sender ! Failed
  }
  
  def ignored: Receive = LoggingReceive {
    case DeleteExpired =>
      context stop self
    case Relist =>
      bidTimerScheduleTime = Option(System.currentTimeMillis())
      system.scheduler.scheduleOnce(bidTimeout, self, BidExpired)
      persist(EvtNewState(PersistentState.Created)) { _ =>
        context become created
      }
    case _ => sender ! Failed
  }
    
  def activated: Receive = LoggingReceive {
    case Bid(amount) =>
      if (amount > topBid) {
        persist(EvtNewBid(amount)) { _ =>
          topBid = amount
          persist(EvtNewWinner(Option(sender()))) { _ =>
            winner foreach { _ ! BidTopped(amount) }
            winner = Option(sender())
          }
        }
      } else {
        sender ! BidTopped(topBid)
      }  
    case BidExpired =>
      bidTimerScheduleTime = Option(System.currentTimeMillis())
      system.scheduler.scheduleOnce(bidTimeout, self, DeleteExpired)
      winner foreach { _ ! AuctionWon(title) }
      seller ! AuctionEnded
      persist(EvtNewState(PersistentState.Sold)) { _ =>
        context become sold
      }
    case _ => sender ! Failed
  }
  
  def sold: Receive = LoggingReceive {
    case DeleteExpired =>
      persist(EvtNewState(PersistentState.Deleted)) { _ =>
        context become deleted
      }
    case _ => sender ! Failed
  }
  
  def deleted: Receive = {
    case _ => sender ! Failed
  }

  override def receiveRecover: Receive = LoggingReceive {
    case EvtNewBid(value) =>
      topBid = value
    case EvtNewWinner(ref) =>
      winner = ref
    case EvtBidTimeout(timeout) =>
      bidTimeoutRecovery = Option(timeout)
    case EvtNewState(e) => e match {
      case PersistentState.Activated => context become activated
      case PersistentState.Created => context become created
      case PersistentState.Deleted => context become deleted
      case PersistentState.Ignored => context become ignored
      case PersistentState.Sold => context become sold
    }
    case RecoveryCompleted =>
      bidTimeoutRecovery foreach { timeout => system.scheduler.scheduleOnce(timeout seconds, self, BidExpired) }
  }

  override def persistenceId: String = s"auction-$title"

  override def postStop() = {
    bidTimerScheduleTime foreach { time =>
      val currentTime = System.currentTimeMillis()
      persist(EvtBidTimeout(currentTime - time)) {_ => }
    }
  }
}

object Auction {
  case object Start
  case class Bid(amount: BigDecimal) {
    require(amount > 0)
  }
  case object Relist
  case object Failed
  case object BidExpired
  case object DeleteExpired
  case object AuctionEnded
  
  case class BidTopped(value: BigDecimal)
  
  val bidTimeout = Duration(10, SECONDS)
  val deleteTimeout = Duration(10, SECONDS)

  case class EvtNewBid(value: BigDecimal)
  case class EvtNewWinner(ref: Option[ActorRef])
  case class EvtBidTimeout(timeout: Long)
  case class EvtNewState(state : PersistentState)

  object PersistentState extends Enumeration {
    type PersistentState = Value
    val Created, Ignored, Activated, Sold, Deleted = Value
  }
}

object AuctionApp extends App {
  val system = ActorSystem("AuctionSystem")
  
  val auctionSearch = system.actorOf(Props[AuctionSearch], "auction_search")
  
  val path = auctionSearch.path.toString
  
  val seller1 = system.actorOf(Props(new Seller(path, "bag of potatoes")), "bag_of_potatoes")
  val seller2 = system.actorOf(Props(new Seller(path, "bike pump")), "bike_pump")
  val seller3 = system.actorOf(Props(new Seller(path, "unwanted tomato")), "unwanted_tomato")
  
  seller1 ! Seller.Start
  seller2 ! Seller.Start
  seller3 ! Seller.Start
  
  val buyer1 = system.actorOf(
      Props(new Buyer(path, 150.0, "bag", "pump")), 
      "Stanislaw_Kaloryfer")
  val buyer2 = system.actorOf(
      Props(new Buyer(path, 60.0, "potatoes")), 
      "Andrzej_Seler")
  val buyer3 = system.actorOf(
      Props(new Buyer(path, 120.0, "bike")), 
      "Maria_Pudelko")
  val buyer4 = system.actorOf(
      Props(new Buyer(path, 150.0, "potatoes", "bike")), 
      "Filip_Mleko")
  val buyer5 = system.actorOf(
      Props(new Buyer(path, 80.0, "bag")), 
      "Wojciech_Karabin")
            
  buyer1 ! Buyer.Start
  buyer2 ! Buyer.Start
  buyer3 ! Buyer.Start
  buyer4 ! Buyer.Start
  buyer5 ! Buyer.Start  
}
