package auction

import akka.testkit._
import akka.actor._
import org.scalatest._
import scala.concurrent.duration._
import scala.collection.mutable.Seq

class AuctionSpec extends TestKit(ActorSystem("AuctionSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  
  override def afterAll = {
    system.shutdown()
  }
  
  "An Auction" must {
    
    "register in AuctionSearch after starting" in {
      val auctionSearch = TestProbe()
      val auction = system.actorOf(Props(new Auction("sprzedam opla", self, auctionSearch.testActor.path.toString())))
      auction ! Auction.Start
      auctionSearch expectMsg(AuctionSearch.Register(Seq("sprzedam", "opla")))
    }
    
    "only accept rising bids" in {
      val auctionSearch = TestProbe()
      val auction = system.actorOf(Props(new Auction("sprzedam opla", self, auctionSearch.testActor.path.toString())))
      auction ! Auction.Start
      auction ! Auction.Bid(10)
      auction ! Auction.Bid(1)
      expectMsg(Auction.BidTopped(10))
      auction ! Auction.Bid(20)
      expectMsg(Auction.BidTopped(20))
    }
    
    "notify Seller and Buyer after winning" in {
      val auctionSearch = TestProbe()
      val seller = TestProbe()
      val auction = system.actorOf(Props(new Auction("sprzedam opla", seller.ref, auctionSearch.testActor.path.toString())))
      auction ! Auction.Start
      auction ! Auction.Bid(10)
      auction ! Auction.BidExpired
      expectMsg(Buyer.AuctionWon("sprzedam opla"))
      seller expectMsg(Auction.AuctionEnded)
    }
  }
}