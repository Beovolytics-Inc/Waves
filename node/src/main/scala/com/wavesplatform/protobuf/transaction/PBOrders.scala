package com.wavesplatform.protobuf.transaction
import com.google.protobuf.ByteString
import com.wavesplatform.account.{AddressScheme, PublicKey}
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.protobuf.order.AssetPair
import com.wavesplatform.{transaction => vt}

object PBOrders {
  import com.wavesplatform.protobuf.utils.PBImplicitConversions._

  def vanilla(order: PBOrder, version: Int = 0): VanillaOrder = {
    VanillaOrder(
      if (version == 0) order.version.toByte else version.toByte,
      PublicKey(order.senderPublicKey.toByteArray),
      PublicKey(order.matcherPublicKey.toByteArray),
      vt.assets.exchange
        .AssetPair(PBAmounts.toVanillaAssetId(order.getAssetPair.amountAssetId), PBAmounts.toVanillaAssetId(order.getAssetPair.priceAssetId)),
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
      PBAmounts.toVanillaAssetId(order.getMatcherFee.assetId),
      order.proofs.map(_.toByteArray: ByteStr)
    )
  }

  def protobuf(order: VanillaOrder): PBOrder = {
    PBOrder(
      AddressScheme.current.chainId,
      ByteString.copyFrom(order.senderPublicKey),
      ByteString.copyFrom(order.matcherPublicKey),
      Some(AssetPair(PBAmounts.toPBAssetId(order.assetPair.amountAsset), PBAmounts.toPBAssetId(order.assetPair.priceAsset))),
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
