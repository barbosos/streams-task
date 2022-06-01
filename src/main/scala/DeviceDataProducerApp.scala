import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import io.circe.generic.auto._
import io.circe.syntax._
import models.DeviceData
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object DeviceDataProducerApp extends App {
  implicit val system: ActorSystem = ActorSystem("producer-sys")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()
  val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val produce: Future[Done] =
    Source(1 to 100)
      .map(value =>
        new ProducerRecord[String, String](
          "device_data",
          DeviceData(value, 1, value + 100).asJson.noSpaces
        )
      )
      .throttle(1, 10.seconds)
      .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_)   => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}
