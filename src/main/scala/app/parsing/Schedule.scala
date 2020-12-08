package app.parsing

import zio._
import org.jsoup.Jsoup
import scala.util.Try
import scala.jdk.CollectionConverters._
import cats.implicits._
import org.jsoup.nodes.Element

object Schedule {

  val groupSelector = "div[class=item-group]"
  val daySelector   = "div[class=item-group__title]"
  val timeSelector  = "div[class=item__datetime]"

  type DateStr = String

  private def getDateTimeFromGroups(groups: List[Element]): Try[List[DateStr]] =
    groups.map { group =>
      for {
        day  <- Try(group.select(daySelector).text().split(' ').last)
        time <- Try(group.select(timeSelector).text().split('-').head.trim())
      } yield {
        s"$day $time"
      }
    }.sequence

  def getStartTimes(html: String): ZIO[Any, Throwable, List[DateStr]] =
    ZIO.fromTry(
      for {
        parsedHtml <- Try(Jsoup.parse(html))
        groups     <- Try(
                        parsedHtml
                          .select(groupSelector)
                          .iterator()
                          .asScala
                          .toList
                      )
        dateTimes  <- getDateTimeFromGroups(groups)
      } yield {
        dateTimes
      }
    )

}
