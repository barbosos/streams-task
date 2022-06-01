import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.parser._
import models.AssetEvent
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object AssetEventConsumerApp extends App {
  implicit val system: ActorSystem = ActorSystem("consumer-sys")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(
    consumerConfig,
    new StringDeserializer,
    new StringDeserializer
  )

  val consume = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("asset_event"))
    .runWith(Sink.foreach(v => println(decode[AssetEvent](v.value()))))

  consume onComplete {
    case Success(_)   => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}
