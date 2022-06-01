import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import cats.effect.unsafe.IORuntime
import com.typesafe.config.ConfigFactory
import dao.AssetDao
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import models._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{
  StringDeserializer,
  StringSerializer
}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object ProcessingApp extends App {

  implicit val ioRuntime: IORuntime = IORuntime.global
  implicit val system: ActorSystem = ActorSystem("processing-sys")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val producerConfig = config.getConfig("akka.kafka.producer")
  val consumerSettings = ConsumerSettings(
    consumerConfig,
    new StringDeserializer,
    new StringDeserializer
  )
  val producerSettings =
    ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  val produce: Future[Done] = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("device_data"))
    .mapConcat(dd => decode[DeviceData](dd.value()).toSeq)
    .groupBy(10, _.deviceId)
    .groupedWithin(300, 5.minutes)
    .mapAsync(3)(ddList =>
      AssetDao
        .findAssetsForDevices(ddList.map(_.deviceId).distinct)
        .map { allAssets =>
          for {
            (deviceId, dds) <- ddList.groupBy(_.deviceId)
            assetsByDevice = allAssets.groupBy(_.deviceId)
            deviceAssets <- assetsByDevice.get(deviceId).toSeq
            assetEvent <- assetEventsForDevice(dds, deviceAssets)
          } yield {
            new ProducerRecord[String, String](
              "asset_event",
              assetEvent.asJson.noSpaces
            )
          }
        }
        .unsafeToFuture()
    )
    .mapConcat(identity)
    .mergeSubstreams
    .runWith(Producer.plainSink(producerSettings))

  produce onComplete {
    case Success(_)   => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }

  private def calculateValueForDeviceDatas(
      deviceDatas: Seq[DeviceData],
      metadata: Metadata
  ): Double = {
    val payloads = deviceDatas.map(_.payload)
    metadata match {
      case Min => payloads.min
      case Max => payloads.max
      case Avg => payloads.sum / payloads.length
    }
  }

  private def assetEventsForDevice(
      deviceDatas: Seq[DeviceData],
      deviceAssets: Seq[Asset]
  ): Seq[AssetEvent] = {
    deviceAssets.map { asset =>
      AssetEvent(
        asset.assetId,
        calculateValueForDeviceDatas(deviceDatas, asset.metadata),
        deviceDatas.map(_.timestamp).max
      )
    }
  }
}
