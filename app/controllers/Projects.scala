package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._


import models._
import views._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Manage projects related operations.
 */
object Projects extends Controller with Secured {

  /**
   * Display the dashboard.
   */
  def index = IsAuthenticated { username => _ =>
    User.findByEmail(username).flatMap {
      case Some(user) =>
        for {
          projects <- Project.findInvolving(username)
          tasks <- Task.findTodoInvolving(username)
        } yield Ok(html.dashboard(projects, tasks, user))
      case None => Future.successful(Forbidden)
    }
  }

  // -- Projects

  /**
   * Add a project.
   */
  def add = IsAuthenticated { username => implicit request =>
    Form("group" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      folder =>
        Project.create(Project("", folder, "New project"), Seq(username)) map { p =>
          Ok(views.html.projects.item(p))
        }
    )
  }

  /**
   * Delete a project.
   */
  def delete(project: String) = IsMemberOf(project) { username => _ =>
    Project.delete(project).map(_ => Ok)
  }

  /**
   * Rename a project.
   */
  def rename(project: String) = IsMemberOf(project) { _ => implicit request =>
    Form("name" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      newName => { 
        Project.rename(project, newName).map(_ => Ok(newName))
      }
    )
  }

  // -- Project groups

  /**
   * Add a new project group.
   */
  def addGroup = IsAuthenticated { _ => _ =>
    Future.successful(Ok(html.projects.group("New group")))
  }

  /**
   * Delete a project group.
   */
  def deleteGroup(folder: String) = IsAuthenticated { _ => _ =>
    Project.deleteInFolder(folder).map(_ => Ok)
  }

  /**
   * Rename a project group.
   */
  def renameGroup(folder: String) = IsAuthenticated { _ => implicit request =>
    Form("name" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      newName => Project.renameFolder(folder, newName).map(_ => Ok(newName))
    )
  }

  // -- Members

  /**
   * Add a project member.
   */
  def addUser(project: String) = IsMemberOf(project) { _ => implicit request =>
    Form("user" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      user => Project.addMember(project, user).map(_ => Ok)
    )
  }

  /**
   * Remove a project member.
   */
  def removeUser(project: String) = IsMemberOf(project) { _ => implicit request =>
    Form("user" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      user => Project.removeMember(project, user).map(_ => Ok)
    )
  }

}

