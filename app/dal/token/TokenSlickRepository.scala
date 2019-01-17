package dal.token

import javax.inject.{Inject, Singleton}
import models._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TokenSlickRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends TokenRepository {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val rowToToken: ((Long, String)) => Token = {
    case (id, value) => Token(id, value)
  }

  val tokenToRow: Token => Option[(Long, String)] =
    t => Some((t.id, t.value))

  private class TokenTable(tag: Tag) extends Table[Token](tag, "token") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def value = column[String]("value")

    def * = (id, value) <> (rowToToken, tokenToRow)
  }

  private val tokens = TableQuery[TokenTable]

  override def get(value: String): Future[Option[Token]] = db.run {
    tokens.filter(_.value === value).result.headOption
  }

}
