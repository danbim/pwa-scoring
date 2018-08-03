# PWA Windsurfing Contest Live Scoring
Allows to score Windsurfing contests and provides (i.e. will provide) an interface for judges as well as an
interface for live stream viewers to show the current scores.
In theory, it is an alternative to the live scoring system the Professional Windsurfing Association
([PWA](https://www.pwaworldtour.com/)) uses for scoring their events
(see [livestream.com](https://livestream.com/accounts/9351246) for past events).

This application is (at the current state) mostly a learning/teaching project. It models the Windsurfing contest domain
using events and commands and implements this model using an event sourcing architecture. Furthermore, it acts as a
foundation / learning playground for several Meetup talks I gave / will give in the future.

Technologies used include:
* [Scala](https://www.scala-lang.org/)
* [Akka Typed](https://doc.akka.io/docs/akka/current/typed/index.html)
  * [Akka Typed Cluster Sharding](https://doc.akka.io/docs/akka/current/typed/cluster-sharding.html)
  * [Akka Typed Persistence](https://doc.akka.io/docs/akka/current/typed/persistence.html)
* [Akka HTTP](https://doc.akka.io/docs/akka/current/persistence.html?language=scala)
* [Spray JSON](https://github.com/spray/spray-json)
* [ScalaCheck](https://github.com/scalacheck/scalacheck) for Property-based testing
* [Mill Build Tool](https://github.com/lihaoyi/mill)

------------------------------------------------------------------------

# Meetup Talks

* Event Storming
  * [Softwerkskammer Lübeck Meetup 07/2017](https://www.meetup.com/Softwerkskammer-Luebeck/events/250458021/)
  * [inoio](https://inoio.de)-internal knowledge transfer
* Event Sourcing
  * [Softwerkskammer Lübeck Meetup 08/2018](https://www.meetup.com/Softwerkskammer-Luebeck/events/gjsxslyxlbdb/): "Event Sourcing und CQRS am Beispiel" --> [Slides](https://t.co/4aY5Uyf9IV)
* Property-based Testing with ScalaCheck
  * Softwerkskammer Lübeck Meetup
* Akka Typed, Typed Cluster Sharding and Typed Persistence
* Akka HTTP Basics, SSE / WebSockets
