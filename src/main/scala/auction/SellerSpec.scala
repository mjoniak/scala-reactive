package auction

import akka.testkit._
import org.scalatest._
import akka.actor._
import scala.collection.mutable.Seq

class SellerSpec extends TestKit(ActorSystem("SellerSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {
  
  override def afterAll = {
    system.shutdown()
  }
  
  "A Seller" must {
    "start Auctions when started" in {
      val auctionSearch = TestProbe()
      val seller = system.actorOf(Props(
          new Seller(auctionSearch.testActor.path.toString(), "jablka", "gruszki")));
      seller ! Seller.Start
      auctionSearch expectMsg(AuctionSearch.Register(Seq("jablka")))
      auctionSearch expectMsg(AuctionSearch.Register(Seq("gruszki")))
    }
  }
}