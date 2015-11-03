package auction

import akka.actor._
import collection.mutable._
import akka.event.LoggingReceive
import scala.concurrent.duration._
import scala.util.Random

class AuctionSearch extends Actor {
  import AuctionSearch._
  import context._
  
  val auctions = Map[String, Seq[ActorRef]]() withDefaultValue Seq()
  
  def receive: Receive = LoggingReceive {
    case Search(name) => 
      val random = Random
      if (auctions contains name) sender ! SearchResponse(auctions(name))
      else sender ! NotFound
    case Register(words) => 
      words.foreach { x => auctions(x) = auctions(x) :+ sender }
      println(s"Auction '${words mkString " "}' registered.")
      println(auctions)
  }
}

object AuctionSearch {
  case class Search(word: String)
  case class Register(words: Seq[String])
  case class SearchResponse(ref: Seq[ActorRef])
  case object NotFound
}
