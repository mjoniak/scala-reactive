import akka.actor._
import akka.testkit._
import auction.{Auction, AuctionSearch, Buyer}
import org.scalatest._

import scala.collection.mutable
import scala.collection.mutable.Seq

class AuctionSpec extends TestKit(ActorSystem("AuctionSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  
  override def afterAll() = {
    system.terminate()
  }

  val publisher = TestProbe()
  
  "An Auction" must {
    
    "register in AuctionSearch after starting" in {
      val auctionSearch = TestProbe()
      val auction = system.actorOf(Props(new Auction("sprzedam opla", self, auctionSearch.testActor.path.toString, publisher.ref)))
      auction ! Auction.Start
      auctionSearch expectMsg AuctionSearch.Register(mutable.Seq("sprzedam", "opla"))
    }
    
    "only accept rising bids" in {
      val auctionSearch = TestProbe()
      val auction = system.actorOf(Props(new Auction("sprzedam opla", self, auctionSearch.testActor.path.toString, publisher.ref)))
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
      val auction = system.actorOf(Props(new Auction("sprzedam opla", seller.ref, auctionSearch.testActor.path.toString, publisher.ref)))
      auction ! Auction.Start
      auction ! Auction.Bid(10)
      auction ! Auction.BidExpired
      expectMsg(Buyer.AuctionWon("sprzedam opla"))
      seller expectMsg Auction.AuctionEnded
    }
  }
}