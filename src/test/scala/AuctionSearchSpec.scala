import akka.actor._
import akka.testkit._
import auction.AuctionSearch
import org.scalatest._

import scala.collection.mutable
import scala.collection.mutable.Seq

class AuctionSearchSpec extends TestKit(ActorSystem("AuctionSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  
  override def afterAll() = {
    system.terminate()
  }
  
  "An AuctionSearch" must {
    
    "return NotFound when no Auctions fulfilling criteria" in {
      val auctionSearch = system.actorOf(Props[AuctionSearch])
      auctionSearch ! AuctionSearch.Search("test")
      expectMsg(AuctionSearch.NotFound)
    }
    
    "return all Auctions fulfilling criteria" in {
      val auctionSearch = system.actorOf(Props[AuctionSearch])
      val auction1 = TestProbe()
      val auction2 = TestProbe()
      auction1 send(auctionSearch, AuctionSearch.Register(Seq("ziemniaki", "buraki")))
      auction2 send(auctionSearch, AuctionSearch.Register(Seq("owoce", "ziemniaki")))
      auctionSearch ! AuctionSearch.Search("ziemniaki")
      expectMsg(AuctionSearch.SearchResponse(mutable.Seq(auction1.ref, auction2.ref)))
      auctionSearch ! AuctionSearch.Search("owoce")
      expectMsg(AuctionSearch.SearchResponse(mutable.Seq(auction2.ref)))
    }
  }
}