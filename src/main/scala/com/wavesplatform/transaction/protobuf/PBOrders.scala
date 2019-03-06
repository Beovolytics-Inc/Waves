package com.wavesplatform.transaction.protobuf
import com.google.protobuf.ByteString
import com.wavesplatform.account.PublicKeyAccount
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.{transaction => vt}

object PBOrders {
  import PBInternalImplicits._

  def vanilla(order: PBOrder, version: Int = 0): VanillaOrder = {
    VanillaOrder(
      PublicKeyAccount(order.senderPublicKey.toByteArray),
      PublicKeyAccount(order.matcherPublicKey.toByteArray),
      vt.assets.exchange.AssetPair(Some(order.getAssetPair.amountAssetId.toByteArray), Some(order.getAssetPair.priceAssetId.toByteArray)),
      order.orderSide match {
        case PBOrder.Side.BUY             => vt.assets.exchange.OrderType.BUY
        case PBOrder.Side.SELL            => vt.assets.exchange.OrderType.SELL
        case PBOrder.Side.Unrecognized(v) => throw new IllegalArgumentException(s"Unknown order type: $v")
      },
      order.amount,
      order.price,
      order.timestamp,
      order.expiration,
      order.getMatcherFee.longAmount,
      order.proofs.map(_.toByteArray: ByteStr),
      if (version == 0) order.version.toByte else version.toByte
    )
  }

  def protobuf(order: VanillaOrder): PBOrder = {
    PBOrder(
      ByteString.copyFrom(order.senderPublicKey.publicKey),
      ByteString.copyFrom(order.matcherPublicKey.publicKey),
      Some(PBOrder.AssetPair(order.assetPair.amountAsset.get, order.assetPair.priceAsset.get)),
      order.orderType match {
        case vt.assets.exchange.OrderType.BUY  => PBOrder.Side.BUY
        case vt.assets.exchange.OrderType.SELL => PBOrder.Side.SELL
      },
      order.amount,
      order.price,
      order.timestamp,
      order.expiration,
      Some((order.matcherFeeAssetId, order.matcherFee)),
      order.version,
      order.proofs.map(bs => bs: ByteString)
    )
  }
}