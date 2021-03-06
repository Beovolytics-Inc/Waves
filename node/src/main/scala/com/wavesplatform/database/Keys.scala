package com.wavesplatform.database

import com.google.common.primitives.{Ints, Longs}
import com.wavesplatform.account.{Address, Alias}
import com.wavesplatform.api.BlockMeta
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.common.utils.EitherExt2
import com.wavesplatform.lang.script.Script
import com.wavesplatform.state._
import com.wavesplatform.transaction.Asset.IssuedAsset
import com.wavesplatform.transaction.Transaction
import com.wavesplatform.transaction.serialization.impl.TransferTxSerializer
import com.wavesplatform.transaction.transfer.TransferTransaction
import com.wavesplatform.utils._

object Keys {
  import KeyHelpers._
  import KeyTags.{InvokeScriptResult => InvokeScriptResultTag, _}

  val version: Key[Int]               = intKey(Version, default = 1)
  val height: Key[Int]                = intKey(Height)
  def score(height: Int): Key[BigInt] = Key(Score, h(height), Option(_).fold(BigInt(0))(BigInt(_)), _.toByteArray)

  def heightOf(blockId: ByteStr): Key[Option[Int]] = Key.opt[Int](HeightOf, blockId.arr, Ints.fromByteArray, Ints.toByteArray)

  def wavesBalanceHistory(addressId: BigInt): Key[Seq[Int]] = historyKey(WavesBalanceHistory, addressId.toByteArray)

  def wavesBalance(addressId: BigInt)(height: Int): Key[Long] =
    Key(WavesBalance, hAddr(height, addressId), Option(_).fold(0L)(Longs.fromByteArray), Longs.toByteArray)

  def assetBalanceHistory(addressId: BigInt, asset: IssuedAsset): Key[Seq[Int]] =
    historyKey(AssetBalanceHistory, addressId.toByteArray ++ asset.id.arr)
  def assetBalance(addressId: BigInt, asset: IssuedAsset)(height: Int): Key[Long] =
    Key(
      AssetBalance,
      hBytes(asset.id.arr ++ addressId.toByteArray, height),
      Option(_).fold(0L)(Longs.fromByteArray),
      Longs.toByteArray
    )

  def assetDetailsHistory(asset: IssuedAsset): Key[Seq[Int]] = historyKey(AssetDetailsHistory, asset.id.arr)
  def assetDetails(asset: IssuedAsset)(height: Int): Key[(AssetInfo, AssetVolumeInfo)] =
    Key(AssetDetails, hBytes(asset.id.arr, height), readAssetDetails, writeAssetDetails)

  def leaseBalanceHistory(addressId: BigInt): Key[Seq[Int]] = historyKey(LeaseBalanceHistory, addressId.toByteArray)
  def leaseBalance(addressId: BigInt)(height: Int): Key[LeaseBalance] =
    Key(LeaseBalance, hAddr(height, addressId), readLeaseBalance, writeLeaseBalance)
  def leaseStatusHistory(leaseId: ByteStr): Key[Seq[Int]] = historyKey(LeaseStatusHistory, leaseId.arr)
  def leaseStatus(leaseId: ByteStr)(height: Int): Key[Boolean] =
    Key(LeaseStatus, hBytes(leaseId.arr, height), _(0) == 1, active => Array[Byte](if (active) 1 else 0))

  def filledVolumeAndFeeHistory(orderId: ByteStr): Key[Seq[Int]] = historyKey(FilledVolumeAndFeeHistory, orderId.arr)
  def filledVolumeAndFee(orderId: ByteStr)(height: Int): Key[VolumeAndFee] =
    Key(FilledVolumeAndFee, hBytes(orderId.arr, height), readVolumeAndFee, writeVolumeAndFee)

  def changedAddresses(height: Int): Key[Seq[BigInt]] = Key(ChangedAddresses, h(height), readBigIntSeq, writeBigIntSeq)

  def addressIdOfAlias(alias: Alias): Key[Option[BigInt]] = Key.opt(AddressIdOfAlias, alias.bytes.arr, BigInt(_), _.toByteArray)

  val lastAddressId: Key[Option[BigInt]] = Key.opt(LastAddressId, Array.emptyByteArray, BigInt(_), _.toByteArray)

  def addressId(address: Address): Key[Option[BigInt]] = Key.opt(AddressId, address.bytes.arr, BigInt(_), _.toByteArray)
  def idToAddress(id: BigInt): Key[Address]            = Key(IdToAddress, id.toByteArray, Address.fromBytes(_).explicitGet(), _.bytes.arr)

  def addressScriptHistory(addressId: BigInt): Key[Seq[Int]] = historyKey(AddressScriptHistory, addressId.toByteArray)
  def addressScript(addressId: BigInt)(height: Int): Key[Option[AccountScriptInfo]] =
    Key.opt(AddressScript, hAddr(height, addressId), readAccountScriptInfo, writeAccountScriptInfo)

  val approvedFeatures: Key[Map[Short, Int]]  = Key(ApprovedFeatures, Array.emptyByteArray, readFeatureMap, writeFeatureMap)
  val activatedFeatures: Key[Map[Short, Int]] = Key(ActivatedFeatures, Array.emptyByteArray, readFeatureMap, writeFeatureMap)

  def dataKeyChunkCount(addressId: BigInt): Key[Int] =
    Key(DataKeyChunkCount, addr(addressId), Option(_).fold(0)(Ints.fromByteArray), Ints.toByteArray)
  def dataKeyChunk(addressId: BigInt, chunkNo: Int): Key[Seq[String]] =
    Key(DataKeyChunk, addr(addressId) ++ Ints.toByteArray(chunkNo), readStrings, writeStrings)

  def dataHistory(addressId: BigInt, key: String): Key[Seq[Int]] =
    historyKey(DataHistory, addressId.toByteArray ++ key.utf8Bytes)
  def data(addressId: BigInt, key: String)(height: Int): Key[Option[DataEntry[_]]] =
    Key.opt(Data, hBytes(addressId.toByteArray ++ key.utf8Bytes, height), readDataEntry(key), writeDataEntry)

  def sponsorshipHistory(asset: IssuedAsset): Key[Seq[Int]] = historyKey(SponsorshipHistory, asset.id.arr)
  def sponsorship(asset: IssuedAsset)(height: Int): Key[SponsorshipValue] =
    Key(Sponsorship, hBytes(asset.id.arr, height), readSponsorship, writeSponsorship)

  def carryFee(height: Int): Key[Long] = Key(CarryFee, h(height), Option(_).fold(0L)(Longs.fromByteArray), Longs.toByteArray)

  def assetScriptHistory(asset: IssuedAsset): Key[Seq[Int]] = historyKey(AssetScriptHistory, asset.id.arr)
  def assetScript(asset: IssuedAsset)(height: Int): Key[Option[(Script, Long)]] =
    Key.opt(AssetScript, hBytes(asset.id.arr, height), readAssetScript, writeAssetScript)
  def assetScriptPresent(asset: IssuedAsset)(height: Int): Key[Option[Unit]] =
    Key.opt(AssetScript, hBytes(asset.id.arr, height), _ => (), _ => Array[Byte]())

  val safeRollbackHeight: Key[Int] = intKey(SafeRollbackHeight)

  def changedDataKeys(height: Int, addressId: BigInt): Key[Seq[String]] =
    Key(ChangedDataKeys, hAddr(height, addressId), readStrings, writeStrings)

  def blockMetaAt(height: Height): Key[Option[BlockMeta]] =
    Key.opt(BlockInfoAtHeight, h(height), readBlockMeta(height), writeBlockMeta)

  def blockInfoBytesAt(height: Height): Key[Option[Array[Byte]]] =
    Key.opt(
      BlockInfoAtHeight,
      h(height),
      identity,
      unsupported("Can not explicitly write block bytes")
    )

  def transactionAt(height: Height, n: TxNum): Key[Option[Transaction]] =
    Key.opt[Transaction](
      NthTransactionInfoAtHeight,
      hNum(height, n),
      readTransaction,
      writeTransaction
    )

  def transferTransactionAt(height: Height, n: TxNum): Key[Option[TransferTransaction]] =
    Key(
      NthTransactionInfoAtHeight,
      hNum(height, n),
      TransferTxSerializer.tryParseTransfer,
      unsupported("Can not explicitly write transfer transaction")
    )

  def addressTransactionSeqNr(addressId: AddressId): Key[Int] =
    bytesSeqNr(AddressTransactionSeqNr, addressId.toByteArray)

  def addressTransactionHN(addressId: AddressId, seqNr: Int): Key[Option[(Height, Seq[(Byte, TxNum)])]] =
    Key.opt(
      AddressTransactionHeightTypeAndNums,
      hBytes(addressId.toByteArray, seqNr),
      readTransactionHNSeqAndType,
      writeTransactionHNSeqAndType
    )

  def transactionHNById(txId: TransactionId): Key[Option[(Height, TxNum)]] =
    Key.opt(
      TransactionHeightAndNumsById,
      txId.arr,
      readTransactionHN,
      writeTransactionHN
    )

  def blockTransactionsFee(height: Int): Key[Long] =
    Key(
      BlockTransactionsFee,
      h(height),
      Longs.fromByteArray,
      Longs.toByteArray
    )

  def invokeScriptResult(height: Int, txNum: TxNum): Key[InvokeScriptResult] =
    Key(InvokeScriptResultTag, hNum(height, txNum), InvokeScriptResult.fromBytes, InvokeScriptResult.toBytes)

  def blockReward(height: Int): Key[Option[Long]] =
    Key.opt(BlockReward, h(height), Longs.fromByteArray, Longs.toByteArray)

  def wavesAmount(height: Int): Key[BigInt] = Key(WavesAmount, h(height), Option(_).fold(BigInt(0))(BigInt(_)), _.toByteArray)

  def hitSource(height: Int): Key[Option[ByteStr]] = Key.opt(HitSource, h(height), ByteStr(_), _.arr)

  val disabledAliases: Key[Set[Alias]] = Key(
    DisabledAliases,
    Array.emptyByteArray,
    b => readStrings(b).map(s => Alias.create(s).explicitGet()).toSet,
    as => writeStrings(as.map(_.name).toSeq)
  )

  def assetStaticInfo(asset: IssuedAsset): Key[Option[AssetStaticInfo]] =
    Key.opt(AssetStaticInfo, asset.id.arr, readAssetStaticInfo, writeAssetStaticInfo)

  def nftCount(addressId: BigInt): Key[Int] =
    Key(NftCount, addr(addressId), Option(_).fold(0)(Ints.fromByteArray), Ints.toByteArray)

  def nftAt(addressId: BigInt, index: Int, assetId: IssuedAsset): Key[Option[Unit]] =
    Key.opt(NftPossession, addressId.toByteArray ++ Longs.toByteArray(index) ++ assetId.id.arr, _ => (), _ => Array.emptyByteArray)
}
