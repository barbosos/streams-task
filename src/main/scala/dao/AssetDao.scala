package dao

import cats.effect.IO
import models.{Asset, Min}

import scala.util.Random

object AssetDao {

  def findAssetsForDevice(deviceId: Int): IO[List[Asset]] =
    IO {
      List(Asset(Random.nextInt(100), Min, deviceId))
    }

  def findAssetsForDevices(deviceIds: Seq[Int]): IO[Seq[Asset]] =
    IO {
      deviceIds.flatMap { deviceId =>
        List(Asset(Random.nextInt(100), Min, deviceId))
      }
    }
}
