package io.github.mavenrain

import caliban.GraphQL.graphQL
import caliban.RootResolver
import io.r2dbc.spi.ConnectionFactory
import scala.util.chaining.scalaUtilChainingOps
import zio.Runtime.default.unsafeRun
import zio.Task
import zio.Task.{effect, succeed}
import zio.IO
import zio.IO.fromOption

case class Character(name: String, age: Int)
given characterStore: ConnectionFactory =
  "characters".toDatabase.connectionFactory
val _ = unsafeRun(
  for
    _ <- Seq(
      "create table characters (name TEXT, age INT);",
      "insert into characters values('boss man', 37);",
      "insert into characters values('big man', 38);"
    ).toCommands.execute
  yield ()
)
object NoCharacters extends Throwable
def getCharacters: Task[Seq[Character]] =
  for
    rawCharacters <- "select name, age from characters".toQuery.results
    characters <- rawCharacters.map(character => Character(
      name = character.get("name", classOf[String]),
      age = character.get("age", classOf[Integer]).toInt
    )).pipe(effect(_))
  yield characters.toSeq
def getCharacter(name: String): Task[Character] =
  for
    characters <- s"select name, age from characters where name = $name".toQuery.results
    rawCharacter <- characters.headOption.pipe(fromOption(_)).mapError(_ => NoCharacters)
    character <- Character(
      name = rawCharacter.get("name", classOf[String]),
      age = rawCharacter.get("age", classOf[Integer]).toInt
    ).pipe(effect(_))
  yield character

case class CharacterName(name: String)
case class Queries(
  characters: Task[Seq[Character]],
  character: CharacterName => Task[Character]
)
case class CharacterArgs(name: String)
case class Mutations(deleteCharacter: CharacterArgs => Task[Boolean])
val mutations = Mutations(_ => succeed(true))

enum Color:
  case Red, Blue, Green

case class ColorQueries(colors: Seq[Color])

val queries = Queries(getCharacters, _.name.pipe(getCharacter(_)))
val api = RootResolver(queries, mutations).pipe(graphQL(_))

val colorQueries = ColorQueries(Seq(Color.Red, Color.Green, Color.Blue))
val colorApi = RootResolver(colorQueries).pipe(graphQL(_))