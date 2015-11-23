package auction

import akka.actor._
import akka.event.LoggingReceive
import scala.concurrent.duration._

class Seller(auctionSearchPath: String, titles: String*) extends Actor  {
  import Seller._
  import context._
  
  var auctions = Seq.empty[ActorRef]
  
  def receive: Receive = {
    case Start =>
      auctions = titles.map { 
        x => system.actorOf(Props(new Auction(x, self, auctionSearchPath)), s"auction_${x.replace(" ", "_") }") 
      }
      auctions foreach { _ ! Auction.Start }
    case Auction.AuctionEnded =>
      println(s"Auction ${sender.path.name} has ended.")
  }  
}

object Seller {
  case object Start
}