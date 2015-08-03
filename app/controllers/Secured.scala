package controllers

import constants.Constants
import play.api.mvc._

import scala.concurrent.Future

/**
 * Created by pnagarjuna on 29/06/15.
 */

trait Secured {
  def key(requestHeader: RequestHeader) = requestHeader.session.get(Constants.SESSION_KEY)
  def onUnAuthorised(requestHeader: RequestHeader) = Results.Redirect(routes.Application.start)
  def withAuth[A](parser: BodyParser[A])(f: String => Request[A] => Future[Result]) = Security.Authenticated(key, onUnAuthorised) { key =>
    Action.async(parser) { request => f(key)(request)}
  }
}
