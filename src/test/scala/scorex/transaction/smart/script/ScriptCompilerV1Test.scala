package scorex.transaction.smart.script

import cats.implicits._
import com.wavesplatform.lang.v1.FunctionHeader
import com.wavesplatform.lang.v1.compiler.Terms._
import com.wavesplatform.lang.v1.compiler.Terms.{BOOLEAN, LONG}
import com.wavesplatform.state.EitherExt2
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, PropSpec}
import scorex.transaction.smart.script.v1.ScriptV1

class ScriptCompilerV1Test extends PropSpec with PropertyChecks with Matchers {

  property("compile script with specified version") {
    val script = scriptWithVersion("1".some)
    ScriptCompiler(script) shouldBe Right((expectedScript, 13))
  }

  property("use version 1 if not specified") {
    val script = scriptWithVersion(none)
    ScriptCompiler(script) shouldBe Right((expectedScript, 13))
  }

  property("fails on unsupported version") {
    val script = scriptWithVersion("2".some)
    ScriptCompiler(script) shouldBe Left("Unsupported language version")
  }

  property("fails on incorrect version value") {
    val script = scriptWithVersion("oOooOps".some)
    ScriptCompiler(script) shouldBe Left("Can't parse language version")
  }

  private val expectedExpr = BLOCK(
    LET("x", CONST_LONG(10)),
    FUNCTION_CALL(
      FunctionHeader("l=l"),
      List(
        CONST_LONG(20),
        FUNCTION_CALL(
          FunctionHeader("l+l"),
          List(REF("x", LONG), REF("x", LONG)),
          LONG
        )
      ),
      BOOLEAN
    ),
    BOOLEAN
  )

  private val expectedScript = ScriptV1(expectedExpr).explicitGet()

  private def scriptWithVersion(versionStr: Option[String]): String = {
    val directive =
      versionStr
        .map(v => s"{-# LANGUAGE_VERSION $v #-}")
        .getOrElse("")

    s"""
      | $directive
      |
      | let x = 10
      | 20 == x + x
      |
      """.stripMargin
  }
}
