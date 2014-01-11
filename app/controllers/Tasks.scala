package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Manage tasks related operations.
 */
object Tasks extends Controller with Secured {

  /**
   * Display the tasks panel for this project.
   */
  def index(project: String) = IsMemberOf(project) { _ => implicit request =>
    Project.findById(project) flatMap {
      case Some(p) =>
        for {
          tasks <- Task.findByProject(project)
          team <- Project.membersOf(project)
          users <- User.findAll
        } yield Ok(html.tasks.index(p, tasks, team, users.diff(team)))
      case None => Future.successful(NotFound)
    }
  }

  val taskForm = Form(
    tuple(
      "title" -> nonEmptyText,
      "dueDate" -> optional(date("MM/dd/yy")),
      "assignedTo" -> optional(text)
    )
  )

  // -- Tasks

  /**
   * Create a task in this project.
   */  
  def add(project: String, folder: String) =  IsMemberOf(project) { _ => implicit request =>
    taskForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      {
        case (title, dueDate, assignedTo) =>
          Task.create(Task("", folder, project, title, false, dueDate, assignedTo)) map { task =>
            Ok(html.tasks.item(task))
          }
      }
    )
  }

  /**
   * Update a task
   */
  def update(task: String) = IsOwnerOf(task) { _ => implicit request =>
    Form("done" -> boolean).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      isDone => Task.markAsDone(task, isDone).map(_ => Ok)
    )
  }

  /**
   * Delete a task
   */
  def delete(task: String) = IsOwnerOf(task) { _ => implicit request =>
    Task.delete(task).map(_ => Ok)
  }

  // -- Task folders

  /**
   * Add a new folder.
   */
  def addFolder = Action {
    Ok(html.tasks.folder("New folder"))
  }

  /**
   * Delete a full tasks folder.
   */
  def deleteFolder(project: String, folder: String) = IsMemberOf(project) { _ => implicit request =>
    Task.deleteInFolder(project, folder).map(_ => Ok)
  }

  /**
   * Rename a tasks folder.
   */
  def renameFolder(project: String, folder: String) = IsMemberOf(project) { _ => implicit request =>
    Form("name" -> nonEmptyText).bindFromRequest.fold(
      errors => Future.successful(BadRequest),
      newName => Task.renameFolder(project, folder, newName).map(_ => Ok(newName))
    )
  }
}

