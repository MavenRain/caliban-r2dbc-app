package io.github.mavenrain

import io.r2dbc.h2.{H2ConnectionConfiguration, H2ConnectionFactory}
import io.r2dbc.spi.{Batch, Connection, ConnectionFactory, Row}
import scala.util.chaining.scalaUtilChainingOps
import zio.{Chunk, Task, UIO, ZIO}
import zio.interop.reactivestreams.publisherToStream
import zio.stream.{Sink, Stream}

opaque type Query = String
opaque type Command = String
opaque type Commands = Seq[String]
opaque type Database = String
opaque type RowsUpdated = Int

extension (text: String)
  def toQuery: Query = text
  def toCommand: Command = text
  def toDatabase: Database = text
extension (texts: Seq[String])
  def toCommands: Commands = texts
extension (database: Database)
  def connectionFactory: ConnectionFactory = H2ConnectionFactory(
    H2ConnectionConfiguration
      .builder()
      .inMemory(database)
      .username("sa")
      .password("")
      .build()
  )
extension (query: Query)
  def results(using connectionFactory: ConnectionFactory): Task[Chunk[Row]] =
    UIO(connectionFactory.create().toStream()).bracket(_.run(Sink.foreach(_.close().toStream().runDrain)).either) {
      connectionStream =>
        (for
          queries <- connectionStream.map(_.createStatement(query))
          results <- queries.execute().toStream()
          rows <- results.map { (row, _) => row }.toStream()
        yield rows)
          .run(Sink.collectAll[Row])
    }
extension (command: Command)
  def execute(using connectionFactory: ConnectionFactory): Task[RowsUpdated] =
    UIO(connectionFactory.create().toStream()).bracket(_.run(Sink.foreach(_.close().toStream().runDrain)).either) {
      connectionStream =>
        (for
          statements <- connectionStream.map(_.createStatement(command))
          result <- statements.execute().toStream()
          rowsUpdated <- result.getRowsUpdated().toStream().map(_.toInt)
        yield rowsUpdated)
          .run(Sink.sum[Int])
    }
extension (commands: Commands)
  def execute(using connectionFactory: ConnectionFactory): Task[RowsUpdated] =
    UIO(connectionFactory.create().toStream()).bracket(_.run(Sink.foreach(_.close().toStream().runDrain)).either) {
      connectionStream =>
        (for
          statements <- connectionStream.map(_.createBatch()).tap(batch => commands.foreach(batch.add).pipe(ZIO.effect(_)))
          result <- statements.execute().toStream()
          rowsUpdated <- result.getRowsUpdated().toStream().map(_.toInt)
        yield rowsUpdated)
          .run(Sink.sum[Int])
    }

  