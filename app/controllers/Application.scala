package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  // -- Authentication

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    )
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action.async { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold(
      formWithErrors => Future.successful(BadRequest(html.login(formWithErrors))),
      user => User.authenticate(user._1, user._2) map {
        case Some(u) => Redirect(routes.Projects.index).withSession("email" -> user._1)
        case _ => BadRequest(html.login(form.withGlobalError("Invalid email or password")))
      }
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  // -- Javascript routing

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        Projects.add, Projects.delete, Projects.rename,
        Projects.addGroup, Projects.deleteGroup, Projects.renameGroup,
        Projects.addUser, Projects.removeUser, Tasks.addFolder, 
        Tasks.renameFolder, Tasks.deleteFolder, Tasks.index,
        Tasks.add, Tasks.update, Tasks.delete
      )
    ).as("text/javascript") 
  }

}

/**
 * Provide security features
 */
trait Secured {
  
  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("email")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)
  
  // --
  
  /** 
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Future[SimpleResult]) = Security.Authenticated(username, onUnauthorized) { user =>
    Action.async(request => f(user)(request))
  }

  /**
   * Check if the connected user is a member of this project.
   */
  def IsMemberOf(project: String)(f: => String => Request[AnyContent] => Future[SimpleResult]) = IsAuthenticated { user => request =>
    Project.isMember(project, user) flatMap {
      case true => f(user)(request)
      case _ => Future.successful(Results.Forbidden)
    }
  }

  /**
   * Check if the connected user is a owner of this task.
   */
  def IsOwnerOf(task: String)(f: => String => Request[AnyContent] => Future[SimpleResult]) = IsAuthenticated { user => request =>
    Task.isOwner(task, user) flatMap {
      case true => f(user)(request)
      case false => Future.successful(Results.Forbidden)
    }
  }
}

