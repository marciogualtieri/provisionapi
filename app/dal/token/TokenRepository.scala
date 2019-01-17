package dal.token

import models.Token

import scala.concurrent.Future

trait TokenRepository {
  def get(value: String): Future[Option[Token]]
}
