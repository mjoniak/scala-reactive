package auction

import akka.actor.Actor

class AuctionPublisher extends Actor {
  import Notifier.Notify

  override def receive: Receive = {
    case Notify(title, winner, topBid) =>
      println(s"AuctionPublished received new bid $topBid for auction: $title.")
      winner foreach { w => println(s"New winner is ${w.path}") }
      sender ! "Ok"
  }
}
