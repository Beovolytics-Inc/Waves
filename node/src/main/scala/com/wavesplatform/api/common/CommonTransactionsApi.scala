package com.wavesplatform.api.common

import com.wavesplatform.account.{Address, AddressOrAlias}
import com.wavesplatform.api.{BlockMeta, common}
import com.wavesplatform.block
import com.wavesplatform.block.Block
import com.wavesplatform.block.Block.TransactionProof
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.lang.ValidationError
import com.wavesplatform.state.diffs.FeeValidation
import com.wavesplatform.state.diffs.FeeValidation.FeeDetails
import com.wavesplatform.state.{Blockchain, Diff, Height, InvokeScriptResult}
import com.wavesplatform.transaction.smart.InvokeScriptTransaction
import com.wavesplatform.transaction.smart.script.trace.TracedResult
import com.wavesplatform.transaction.{Asset, CreateAliasTransaction, Transaction}
import com.wavesplatform.utx.UtxPool
import com.wavesplatform.wallet.Wallet
import monix.reactive.Observable
import org.iq80.leveldb.DB

trait CommonTransactionsApi {
  import CommonTransactionsApi._

  def aliasesOfAddress(address: Address): Observable[(Height, CreateAliasTransaction)]

  def transactionsByAddress(
      subject: AddressOrAlias,
      sender: Option[Address],
      transactionTypes: Set[Byte],
      fromId: Option[ByteStr] = None
  ): Observable[(Height, Transaction)]

  def transactionById(txId: ByteStr): Option[TransactionMeta]

  def unconfirmedTransactions: Seq[Transaction]

  def unconfirmedTransactionById(txId: ByteStr): Option[Transaction]

  def calculateFee(tx: Transaction): Either[ValidationError, (Asset, Long, Long)]

  def broadcastTransaction(tx: Transaction): TracedResult[ValidationError, Boolean]

  def invokeScriptResults(
      subject: AddressOrAlias,
      sender: Option[Address],
      transactionTypes: Set[Byte],
      fromId: Option[ByteStr] = None
  ): Observable[TransactionMeta]

  def transactionProofs(transactionIds: List[ByteStr]): List[TransactionProof]
}

object CommonTransactionsApi {
  type TransactionMeta = (Height, Either[Transaction, (InvokeScriptTransaction, Option[InvokeScriptResult])])

  def apply(
      maybeDiff: => Option[(Height, Diff)],
      db: DB,
      blockchain: Blockchain,
      utx: UtxPool,
      wallet: Wallet,
      publishTransaction: Transaction => TracedResult[ValidationError, Boolean],
      blockAt: Int => Option[(BlockMeta, Seq[Transaction])]
  ): CommonTransactionsApi = new CommonTransactionsApi {
    private def resolve(subject: AddressOrAlias): Option[Address] = blockchain.resolveAlias(subject).toOption

    override def aliasesOfAddress(address: Address): Observable[(Height, CreateAliasTransaction)] = common.aliasesOfAddress(db, maybeDiff, address)

    override def transactionsByAddress(
        subject: AddressOrAlias,
        sender: Option[Address],
        transactionTypes: Set[Byte],
        fromId: Option[ByteStr] = None
    ): Observable[(Height, Transaction)] = resolve(subject).fold(Observable.empty[(Height, Transaction)]) { subjectAddress =>
      common.addressTransactions(db, maybeDiff, subjectAddress, sender, transactionTypes, fromId)
    }

    override def transactionById(transactionId: ByteStr): Option[TransactionMeta] =
      blockchain.transactionInfo(transactionId).map {
        case (height, ist: InvokeScriptTransaction) =>
          Height(height) ->
            Right(
              ist -> maybeDiff.flatMap(_._2.scriptResults.get(transactionId)).orElse(AddressTransactions.loadInvokeScriptResult(db, transactionId))
            )
        case (height, tx) => Height(height) -> Left(tx)

      }

    override def unconfirmedTransactions: Seq[Transaction] = utx.all

    override def unconfirmedTransactionById(transactionId: ByteStr): Option[Transaction] =
      utx.transactionById(transactionId)

    override def calculateFee(tx: Transaction): Either[ValidationError, (Asset, Long, Long)] =
      FeeValidation
        .getMinFee(blockchain, tx)
        .map {
          case FeeDetails(asset, _, feeInAsset, feeInWaves) =>
            (asset, feeInAsset, feeInWaves)
        }

    override def broadcastTransaction(tx: Transaction): TracedResult[ValidationError, Boolean] = publishTransaction(tx)

    override def invokeScriptResults(
        subject: AddressOrAlias,
        sender: Option[Address],
        transactionTypes: Set[Byte],
        fromId: Option[ByteStr] = None
    ): Observable[TransactionMeta] =
      resolve(subject).fold(Observable.empty[TransactionMeta]) { subjectAddress =>
        common.invokeScriptResults(db, maybeDiff, subjectAddress, sender, transactionTypes, fromId)
      }

    override def transactionProofs(transactionIds: List[ByteStr]): List[TransactionProof] =
      for {
        transactionId           <- transactionIds
        (height, transaction)   <- blockchain.transactionInfo(transactionId)
        (meta, allTransactions) <- blockAt(height) if meta.header.version >= Block.ProtoBlockVersion
        transactionProof        <- block.transactionProof(transaction, allTransactions)
      } yield transactionProof
  }
}
