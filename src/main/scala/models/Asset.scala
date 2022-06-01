package models

sealed trait Metadata

case object Avg extends Metadata
case object Min extends Metadata
case object Max extends Metadata

final case class Asset(assetId: Int, metadata: Metadata, deviceId: Int)
