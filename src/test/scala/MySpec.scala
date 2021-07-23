import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.persistence.  testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.PersistenceTestKit
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.DurationInt

object MyStore {
  def apply(): Behavior[Command] = EventSourcedBehavior[Command, Event, State](
    persistenceId = PersistenceId.ofUniqueId("myId"),
    emptyState = State(),
    commandHandler = (state, command) => handleCommand(state, command),
    eventHandler = (state, event) => handleEvent(state, event)
  )

  sealed trait Command
  case class AddCmd(s: String, replyTo: ActorRef[List[String]]) extends Command
  case class ReadCmd(replyTo: ActorRef[List[String]])           extends Command

  sealed trait Event
  case class AddEvent(s: String) extends Event

  case class State(values: List[String] = List())

  def handleCommand(state: State, command: Command): ReplyEffect[Event, State] = command match {
    case AddCmd(s, replyTo) => Effect.persist(AddEvent(s)).thenReply(replyTo)(updatedState => updatedState.values)
    case ReadCmd(replyTo)   => Effect.reply(replyTo)(state.values)
  }

  def handleEvent(state: State, event: Event): State = event match {
    case AddEvent(s) => state.copy(values = s :: state.values)
  }
}

object MySpec {
  val configuration: Config = {
    val serializationConfigString = "akka.actor.allow-java-serialization = on"
    val serialization             = ConfigFactory.parseString(serializationConfigString).resolve()
    val persistence               = PersistenceTestKitPlugin.config
    serialization.withFallback(persistence)
  }
}

class MySpec extends AnyFunSuite with Matchers with ScalatestRouteTest with BeforeAndAfterEach {
  import MyStore._
  import akka.http.scaladsl.server.Directives._

  val persistenceTestKit: PersistenceTestKit = PersistenceTestKit(system)

  val route: Route = {
    import akka.actor.typed.scaladsl.AskPattern._
    import akka.actor.typed.scaladsl.adapter._
    implicit val typedSystem: ActorSystem[Nothing] = system.toTyped
    implicit val timeout: Timeout                  = 3.seconds

    val actor: ActorRef[Command] =
      system.spawn(behavior = MyStore(), name = "MyStore", props = Props.empty)

    get {
      val result = actor.ask(replyTo => ReadCmd(replyTo)).map(_.mkString(";"))
      complete(result)
    } ~ (post & entity(as[String])) { newRecord =>
      val result = actor.ask(replyTo => AddCmd(newRecord, replyTo)).map(_ => "OK")
      complete(result)
    }
  }

  override def createActorSystem(): akka.actor.ActorSystem =
    akka.actor.ActorSystem("MySystem", MySpec.configuration)

  override def beforeEach(): Unit = {
    persistenceTestKit.clearAll()
  }

  private def add(s: String) = {
    Post("/", s) ~> route ~> check {
      responseAs[String] shouldEqual "OK"
    }
  }

  test("Add two elements") {
    add("One")
    add("Two")

    Get() ~> route ~> check {
      responseAs[String] shouldEqual "Two;One"
    }
  }

  test("Add another two element") {
    add("Three")
    add("Four")

    Get() ~> route ~> check {
      responseAs[String] shouldEqual "Four;Three"
    }
  }
}
