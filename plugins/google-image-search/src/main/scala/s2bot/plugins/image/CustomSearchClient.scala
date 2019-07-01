package s2bot.plugins.image

import play.api.libs.json.{Json, OFormat, Reads}
import s2bot.plugins.image.CustomSearchClient._

import scala.concurrent.{ExecutionContext, Future}

class CustomSearchClient(cseId: String, apiKey: String)(implicit executionContext: ExecutionContext) {


  def searchImage(
      q: String,
      safe: Safe = High,
      animate: Boolean = false
  ): Future[List[Image]] = {
    request[Response[Image]](
      Map(
        "q" -> q,
        "searchType" -> SearchImage.value,
        "safe" -> safe.value,
        "fields" -> "items(link)"
      ) ++ (
          if (animate) {
            Map(
              "fileType" -> "gif",
              "hq" -> "animated",
              "tbs" -> "itp:animated"
            )
          } else {
            Map.empty
          }
          )
    ).map(_.items.getOrElse(Nil))
  }

  private def request[T](query: Map[String, String])(implicit reads: Reads[T]): Future[T] = {
    import dispatch._

    val req =
      url(endpoint)
          .setQueryParameters(query.mapValues(Seq(_)))
          .addQueryParameter("cx", cseId)
          .addQueryParameter("key", apiKey)
          .GET

    Http.default(req OK as.String).map(Json.parse(_).as[T])
  }
}

object CustomSearchClient {

  private val endpoint = "https://www.googleapis.com/customsearch/v1"

  sealed abstract class SearchType(val value: String)
  case object SearchImage extends SearchType("image")

  sealed abstract class Safe(val value: String)
  case object High extends Safe("high")

  case class Response[T](items: Option[List[T]])
  case class Image(link: String)

  implicit def responseFormat[T](implicit format: OFormat[T]): OFormat[Response[T]] = Json.format[Response[T]]
  implicit lazy val imageFormat: OFormat[Image] = Json.format[Image]
}