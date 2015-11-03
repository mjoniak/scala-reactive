package auction;

import akka.actor._
import akka.event._
import scala.util.Random
import scala.concurrent.duration._
import scala.BigDecimal
import scala.math.BigDecimal.int2bigDecimal
import auction.Auction.BidTopped

class Buyer(auctionSearchPath: String, maxBid: BigDecimal, words: String*) extends Actor {
  import Buyer._
  import context._
    
  def receive: Actor.Receive = {
    case Start =>
      words.foreach { system.actorSelection(auctionSearchPath) ! AuctionSearch.Search(_) }   
      context become involved
  }
  
  def involved: Actor.Receive = {
    case AuctionSearch.SearchResponse(auctions) =>
      println(s"${self.path.name} found auctions and bids 1.")
      auctions foreach { _ ! Auction.Bid(BigDecimal(1)) }
    case AuctionWon(item) => println(s"${self.path.name} bought $item")
    case BidTopped(value) => 
      val random = Random
      if (value < maxBid) {
        val currentBid = maxBid.min(value + random.nextInt(9) + 1)
        println(s"${self.path.name} sees that his bid was topped. New bid: ${currentBid}")
        sender ! Auction.Bid(currentBid)
      } else {
        println(s"${self.path.name} sees that his bid was topped but can't do anything.")
      }
  }
}

object Buyer {
  case object Start
  case class AuctionWon(item: String)
  case object Failed
}