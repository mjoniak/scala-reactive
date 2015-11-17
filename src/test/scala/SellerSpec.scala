import akka.actor._
import akka.testkit._
import auction.{AuctionSearch, Seller}
import org.scalatest._

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
      auctionSearch expectMsgAllOf(AuctionSearch.Register(Seq("jablka")), AuctionSearch.Register(Seq("gruszki")))
    }
  }
}