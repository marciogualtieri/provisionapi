package actions

import dal.token.TokenSlickRepository
import javax.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class AuthenticatedUserAction @Inject() (tokenRepository: TokenSlickRepository,
                                         parser: BodyParsers.Default)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) {

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    request.headers.get("X-AUTH") match {
      case None =>
        Future.successful(Forbidden("MISSING AUTHENTICATION TOKEN."))
      case Some(xAuth) =>
        handleAuthentication(request, block, xAuth)
    }
  }

  private def handleAuthentication[A](request: Request[A], block: Request[A] => Future[Result],
                                           xAuth: String) = {
    val token = Await.result(tokenRepository.get(xAuth), Duration.Inf)
    if (token.isDefined) block(request)
    else Future.successful(Forbidden("INVALID AUTHENTICATION TOKEN."))
  }
}
