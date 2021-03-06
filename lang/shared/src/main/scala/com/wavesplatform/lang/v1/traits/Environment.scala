package com.wavesplatform.lang.v1.traits

import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.lang.v1.traits.domain._
import shapeless._

import scala.language.higherKinds

object Environment {
  type InputEntity = Tx :+: Ord :+: PseudoTx :+: CNil
}

trait Environment[F[_]] {
  def chainId: Byte
  def inputEntity: Environment.InputEntity
  def tthis: Recipient.Address
  def height: F[Long]
  def transactionById(id: Array[Byte]): F[Option[Tx]]
  def transferTransactionById(id: Array[Byte]): F[Option[Tx]]
  def transactionHeightById(id: Array[Byte]): F[Option[Long]]
  def assetInfoById(id: Array[Byte]): F[Option[ScriptAssetInfo]]
  def lastBlockOpt(): F[Option[BlockInfo]]
  def blockInfoByHeight(height: Int): F[Option[BlockInfo]]
  def data(addressOrAlias: Recipient, key: String, dataType: DataType): F[Option[Any]]
  def resolveAlias(name: String): F[Either[String, Recipient.Address]]
  def accountBalanceOf(addressOrAlias: Recipient, assetId: Option[Array[Byte]]): F[Either[String, Long]]
  def multiPaymentAllowed: Boolean
  def txId: ByteStr
  def transferTransactionFromProto(b: Array[Byte]): Option[Tx.Transfer]
}
