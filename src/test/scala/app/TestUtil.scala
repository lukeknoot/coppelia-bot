package app

import zio._
import scala.io.Source

trait TestUtil {
  def managedResource(resourceName: String): ZManaged[Any, Nothing, String] = ZManaged
    .make {
      ZIO.effectTotal(Source.fromResource(resourceName))
    } { source => ZIO.effectTotal(source.close) }
    .mapM { source =>
      ZIO.effectTotal(source.getLines.mkString)
    }
}
