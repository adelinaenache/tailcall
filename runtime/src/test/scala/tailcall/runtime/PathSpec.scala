package tailcall.runtime

import tailcall.runtime.ast.Path
import tailcall.runtime.ast.Path.Segment.{Literal, Param}
import tailcall.runtime.internal.TestGen
import zio.ZIO
import zio.schema.DynamicValue
import zio.test.Assertion.equalTo
import zio.test._

object PathSpec extends ZIOSpecDefault {
  val syntax = Path.syntax.route

  override def spec =
    suite("path")(
      test("segments") {
        val input: Seq[(String, List[Path.Segment])] = Seq(
          "/a"                 -> (Literal("a") :: Nil),
          "/a/b"               -> (Literal("a") :: Literal("b") :: Nil),
          "/a/b/c"             -> (Literal("a") :: Literal("b") :: Literal("c") :: Nil),
          "/a/b/{{c}}"         -> (Literal("a") :: Literal("b") :: Param("c") :: Nil),
          "/a/{{b}}/{{c}}"     -> (Literal("a") :: Param("b") :: Param("c") :: Nil),
          "/{{a}}/{{b}}/{{c}}" -> (Param("a") :: Param("b") :: Param("c") :: Nil),
          "/{{a}}/{{b}}"       -> (Param("a") :: Param("b") :: Nil),
          "/{{a}}"             -> (Param("a") :: Nil)
        )
        checkAll(Gen.fromIterable(input)) { case (input, expected) =>
          val parsed = ZIO.fromEither(syntax.parseString(input)).map(_.segments)
          assertZIO(parsed)(equalTo(expected))
        }
      },
      test("evaluate") {
        val inputs = List(
          "/{{a}}/{{b}}/{{c}}" -> DynamicValue(Map("a" -> "a", "b" -> "b", "c" -> "c")),
          "/{{a.b.c}}/b/c"     -> DynamicValue(Map("a" -> Map("b" -> Map("c" -> "a"))))
        )

        checkAll(Gen.fromIterable(inputs)) { case (path, input) =>
          val string = ZIO.fromEither(syntax.parseString(path)).map(_.evaluate(input))
          assertZIO(string)(equalTo("/a/b/c"))
        }
      },
      test("encoding") {
        check(TestGen.genPath) { path =>
          val program = path.encode
          assertTrue(program.isRight)
        }
      }
    )
}