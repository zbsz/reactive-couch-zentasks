package models

import java.util.{UUID, Date}


import scala.language.postfixOps
import play.api.libs.json.{JsObject, Json}
import scala.concurrent.Future
import com.geteit.rcouch.views.Query
import scala.concurrent.ExecutionContext.Implicits.global

case class Task(id: String, folder: String, project: String, title: String, done: Boolean, dueDate: Option[Date], assignedTo: Option[String])

object Task {
  
  // -- Parsers

  implicit val reads = Json.reads[Task]
  implicit val writes = Json.writes[Task].transform(v => v.asInstanceOf[JsObject] ++ Json.obj("docType" -> "Task"))


  // -- Queries
  
  /**
   * Retrieve a Task from the id.
   */
  def findById(id: String): Future[Option[Task]] = Couch.bucket.flatMap(_.get[Task](id))

  /**
   * Retrieve todo tasks for the user.
   */
  def findTodoInvolving(email: String): Future[List[(Task, Project)]] = for {
    b <- Couch.bucket
    byMember <- Couch.Projects.byMember
    projects <- b.list[Project](byMember, Query(key = Some(email))) // TODO: add implicit from any value (<% Key) to single key Query
    tView <- Couch.Tasks.all
    tasks <- Future.sequence(
      projects.flatten.map { p =>
        b.query(tView, Query(startKey = Some((false, p.id)), endKey = Some((false, p.id, {})))) // TODO: add Query.range helper method
        findByProject(p.id).map(_.map(t => t -> p))
      }
    )
  } yield tasks.flatten

  /**
   * Find tasks related to a project
   */
  def findByProject(project: String): Future[List[Task]] = for {
    b <- Couch.bucket
    tView <- Couch.Tasks.all
    active <- b.list[Task](tView, Query(startKey = Some((false, project)), endKey = Some((false, project, {}))))
    done <- b.list[Task](tView, Query(startKey = Some((true, project)), endKey = Some((true, project, {}))))
  } yield active.flatten ++ done.flatten

  /**
   * Delete a task
   */
  def delete(id: String) = Couch.bucket.flatMap(_.delete(id))

  /**
   * Delete all task in a folder.
   */
  def deleteInFolder(project: String, folder: String) = for {
    b <- Couch.bucket
    tView <- Couch.Tasks.all
    tasks <- b.list[Task](tView, Query(keys = List((false, project, folder), (true, project, folder))))
    _ <- Future.sequence(tasks.flatten.map(t => b.delete(t.id))) // TODO: add helper method to delete all documents returned from query
  } yield ()
  
  /**
   * Mark a task as done or not
   */
  def markAsDone(taskId: String, done: Boolean) =
    Couch.bucket.flatMap(_.update[Task](taskId, (t : Task) => Some(t.copy(done = done))))

  /**
   * Rename a folder.
   */
  def renameFolder(project: String, folder: String, newName: String) = for {
    b <- Couch.bucket
    tView <- Couch.Tasks.all
    tasks <- b.list[Task](tView, Query(keys = List((false, project, folder), (true, project, folder))))
    _ <- Future.sequence(tasks.flatten.map(t => b.update[Task](t.id, (t : Task) => Some(t.copy(folder = newName)))))
  } yield ()

  /**
   * Check if a user is the owner of this task
   */
  def isOwner(task: String, email: String): Future[Boolean] = for {
    b <- Couch.bucket
    t <- b.get[Task](task)
    p <- t.fold(Future.successful(None: Option[Project]))(t => b.get[Project](t.project))
  } yield p.exists(_.members.exists(_.email == email))

  /**
   * Create a Task.
   */
  def create(task: Task): Future[Task] = {
    val t = task.copy(id = UUID.randomUUID().toString)
    Couch.bucket.flatMap(_.add[Task](t.id, t)).map(_ => t)
  }
}
