import akka.testkit.typed.scaladsl.BehaviorTestKit
import akka.testkit.typed.scaladsl.TestInbox
import com.bimschas.pwascoring.Contest
import com.bimschas.pwascoring.Contest.HeatAlreadyStarted
import com.bimschas.pwascoring.Contest.HeatId
import com.bimschas.pwascoring.Contest.HeatStarted
import com.bimschas.pwascoring.Contest.StartHeat
import org.scalatest.WordSpec

class ContestSpec extends WordSpec {

  "HeatActor" when {
    "sent a StartHeat command" must {
      "start the heat if heat is not yet running" in {
        val testKit = BehaviorTestKit(Contest.initialBehavior)
        val inbox = TestInbox[Either[HeatAlreadyStarted, HeatStarted]]()
        testKit.run(StartHeat(HeatId(1, Some('a')), inbox.ref))
        inbox.receiveMessage() match {
          case Left(heatAlreadyStarted) => fail(s"unexpected $heatAlreadyStarted")
          case Right(_) => // expected
        }
      }
      "respond that heat has already started if heat already started" in {

        val testKit = BehaviorTestKit(Contest.initialBehavior)
        val inbox = TestInbox[Either[HeatAlreadyStarted, HeatStarted]]()
        val heatId = HeatId(1, Some('a'))

        testKit.run(StartHeat(heatId, inbox.ref))
        inbox.receiveMessage() // ignore, should start heat

        testKit.run(StartHeat(heatId, inbox.ref))
        inbox.receiveMessage() match {
          case Left(_) => // expected
          case Right(heatStarted) => fail(s"unexpected $heatStarted")
        }
      }
    }
  }
}
