package io.github.mavenrain

import caliban.ZHttpAdapter.{makeHttpService, makeWebSocketService}
import caliban.Macros.gqldoc
import io.github.mavenrain.removeElem
import scala.util.chaining.scalaUtilChainingOps
import zhttp.http.{/, ->, CORS, Http, Root}
import zhttp.http.Http.route
import zhttp.service.Server
import zio.Runtime.default.unsafeRun
import zio.Schedule.spaced
import zio.ZIO.{never, succeed}
import zio.console.putStrLn
import zio.duration.durationInt

private val query = gqldoc("""
  {
    characters {
      name
    }
    colors
  }
""")

@main
def run =
  println("Hello world!")
  println(("Hi", true, 2).removeElem[Int]._2)
  println((api |+| colorApi).render)
  (for
    interpreter <- (api |+| colorApi).interpreter
    queryResults <- interpreter.execute(query)
    _ <- putStrLn(queryResults.data.toString)
    _ <- Server.start(
      8088,
      route {
        case _ -> Root / "api" => CORS(makeHttpService(interpreter))
        case _ -> Root / "ws" => CORS(makeWebSocketService(interpreter))
      }
    ).forever
  yield ()).pipe(unsafeRun(_))