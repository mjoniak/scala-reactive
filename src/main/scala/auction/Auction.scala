package auction

import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.duration._
import auction.Buyer.AuctionWon
import scala.language.postfixOps
import scala.math.BigDecimal.double2bigDecimal
import scala.math.BigDecimal.int2bigDecimal

class Auction(title: String, seller: ActorRef, auctionSearchPath: String) extends Actor {
  import Auction._
  import context._
   
  var topBid = BigDecimal(0)
  var winner: Option[ActorRef] = None

  override def receive: Receive = {
    case Start =>
      context.actorSelection(auctionSearchPath) ! AuctionSearch.Register(title split " ")
      system.scheduler.scheduleOnce(bidTimeout, self, BidExpired)
      context become created
  }
  
  def created: Receive = {
    case Bid(amount) =>
      topBid = amount
      winner = Option(sender())
      context become activated
    case BidExpired =>
      system.scheduler.scheduleOnce(deleteTimeout, self, DeleteExpired)
      context become ignored
    case _ => sender ! Failed
  }
  
  def ignored: Receive = {
    case DeleteExpired => context stop self
    case Relist =>
      system.scheduler.scheduleOnce(bidTimeout, self, BidExpired)
      context become created
    case _ => sender ! Failed
  }
    
  def activated: Receive = {
    case Bid(amount) =>
      if (amount > topBid) {
        topBid = amount
        winner foreach { _ ! BidTopped(amount) }
        winner = Option(sender())
      } else {
        sender ! BidTopped(topBid)
      }  
    case BidExpired =>
      system.scheduler.scheduleOnce(bidTimeout, self, DeleteExpired)
      winner foreach { _ ! AuctionWon(title) }
      seller ! AuctionEnded
      context become sold
    case _ => sender ! Failed
  }
  
  def sold: Receive = {
    case DeleteExpired =>
        context become deleted
    case _ => sender ! Failed
  }
  
  def deleted: Receive = {
    case _ => sender ! Failed
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

  val buyer1 = system.actorOf(Props(new Buyer(path, 150.0, "bag", "pump")), "Stanislaw_Kaloryfer")
  val buyer2 = system.actorOf(Props(new Buyer(path, 60.0, "potatoes")), "Andrzej_Seler")
  val buyer3 = system.actorOf(Props(new Buyer(path, 120.0, "bike")), "Maria_Pudelko")
  val buyer4 = system.actorOf(Props(new Buyer(path, 150.0, "potatoes", "bike")), "Filip_Mleko")
  val buyer5 = system.actorOf(Props(new Buyer(path, 80.0, "bag")), "Wojciech_Karabin")
            
  buyer1 ! Buyer.Start
  buyer2 ! Buyer.Start
  buyer3 ! Buyer.Start
  buyer4 ! Buyer.Start
  buyer5 ! Buyer.Start
}
