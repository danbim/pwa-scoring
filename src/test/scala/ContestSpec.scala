import java.util.concurrent.atomic.AtomicInteger

import akka.actor.typed.Props
import akka.cluster.typed.ClusterSingleton
import akka.cluster.typed.ClusterSingletonSettings
import akka.testkit.typed.scaladsl.ActorTestKit
import akka.testkit.typed.scaladsl.TestProbe
import com.bimschas.pwascoring.Contest
import com.bimschas.pwascoring.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.Contest.HeatStarted
import com.bimschas.pwascoring.Contest.StartHeat
import com.bimschas.pwascoring.Model.HeatContestants
import com.bimschas.pwascoring.Model.HeatId
import com.bimschas.pwascoring.Model.RiderId
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.WordSpec

object IdGenerator {
  private val lastId = new AtomicInteger(0)
  def nextId(): Int = lastId.incrementAndGet()
}

class ContestSpec extends WordSpec with ActorTestKit with BeforeAndAfterAll with BeforeAndAfterEach {

  override protected def afterAll(): Unit =
    shutdownTestKit()

  //noinspection TypeAnnotation
  private abstract class ContestScenario {
    protected val singletonManager = ClusterSingleton(system)
    protected val contestActor = singletonManager.spawn(
      behavior = Contest.behavior,
      "Contest",
      Props.empty,
      ClusterSingletonSettings(system),
      Contest.PassivateContest
    )
    protected val heatId = {
      val uniquePersistenceId = IdGenerator.nextId()
      HeatId(uniquePersistenceId, Some('a'))
    }
    protected val contestants = HeatContestants(RiderId(sailNr = "USA-1"), RiderId(sailNr = "G-901"))
    protected val probe = TestProbe[Either[HeatAlreadyStarted, HeatStarted]]()
  }

  // TODO shouldn't we shut down the singleton manager after each test?

  "ContestActor" when {
    "sent a StartHeat command" must {
      "start the heat if heat is not yet running" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[HeatAlreadyStarted, HeatStarted]]
        }
      }
      "respond that heat has already started if heat already started" in {
        new ContestScenario {
          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Right[HeatAlreadyStarted, HeatStarted]]

          contestActor ! StartHeat(heatId, contestants, probe.ref)
          probe.expectMessageType[Left[HeatAlreadyStarted, HeatStarted]]
        }
      }
    }
  }
}
