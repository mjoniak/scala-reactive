import akka.actor._
import akka.testkit._
import auction.{Auction, AuctionSearch, Buyer}
import org.scalatest._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

class BuyerSpec extends TestKit(ActorSystem("BuyerSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  "A Buyer" must {
    val auctionSearch = TestProbe()
      val buyer = system.actorOf(Props(new Buyer(
          auctionSearch.testActor.path.toString,
          11.0,
          "gruszki")))
    
    "search for the auction" in {
      buyer ! Buyer.Start
      auctionSearch expectMsg AuctionSearch.Search("gruszki")
    }
    
    val auction = TestProbe()
    
    "bid for 1" in {
      auctionSearch reply AuctionSearch.SearchResponse(mutable.Seq(auction.ref))
      auction expectMsg Auction.Bid(1)
    }
    
    "bid for more when topped" in {
      auction reply Auction.BidTopped(10)
      auction expectMsg Auction.Bid(11)
    }
    
    "stop bidding when current max bid is over his max bid value" in {
      auction reply Auction.BidTopped(11)
      auction expectNoMsg(10 millis)
    }
  }
}