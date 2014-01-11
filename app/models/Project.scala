package models

import scala.language.postfixOps
import play.api.libs.json.{JsObject, Json}
import scala.concurrent.Future
import com.geteit.rcouch.views.Query
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

case class Project(id: String, folder: String, name: String, members: List[User] = Nil)

object Project {

  implicit val reads = Json.reads[Project]
  implicit val writes = Json.writes[Project].transform(v => v.asInstanceOf[JsObject] ++ Json.obj("docType" -> "Project"))


  /**
   * Retrieve a Project from id.
   */
  def findById(id: String): Future[Option[Project]] = Couch.bucket.flatMap(_.get[Project](id))

  /**
   * Retrieve project for user
   */
  def findInvolving(email: String): Future[List[Project]] = for {
    b <- Couch.bucket
    v <- Couch.Projects.byMember
    projects <- b.list[Project](v, Query(Some(email)))
  } yield projects.flatten

  /**
   * Update a project.
   */
  def rename(id: String, newName: String) = Couch.bucket.flatMap(_.update[Project](id, (p: Project) => Some(p.copy(name = newName))))

  /**
   * Delete a project.
   */
  def delete(id: String) = Couch.bucket.flatMap(_.delete(id))

  /**
   * Delete all project in a folder
   */
  def deleteInFolder(folder: String) = for {
    b <- Couch.bucket
    v <- Couch.Projects.byFolder
    projects <- b.list[Project](v, Query(Some(folder)))
    _ <- Future.sequence(projects.flatten.map(p => b.delete(p.id)))
  } yield ()
  
  /**
   * Rename a folder
   */
  def renameFolder(folder: String, newName: String) = for {
    b <- Couch.bucket
    v <- Couch.Projects.byFolder
    projects <- b.list[Project](v, Query(Some(folder)))
    _ <- Future.sequence(projects.flatten.map(p => b.update[Project](p.id, (p: Project) => Some(p.copy(folder = newName)))))
  } yield ()

  /**
   * Retrieve project member
   */
  def membersOf(project: String): Future[List[User]] = findById(project).map {
    case Some(p) => p.members
    case None => Nil
  }

  /**
   * Add a member to the project team.
   */
  def addMember(project: String, user: String) = for {
    b <- Couch.bucket
    u <- User.findByEmail(user)
    res <- u.fold(Future.successful(None: Option[Project])) { us =>
      b.update[Project](project, (p: Project) => Some(p.copy(members = us :: p.members)))
    }
  } yield res

  /**
   * Remove a member from the project team.
   */
  def removeMember(project: String, user: String): Future[Option[Project]] =
    Couch.bucket.flatMap(_.update[Project](project, (p: Project) => Some(p.copy(members = p.members.filter(_.email != user)))))

  /**
   * Check if a user is a member of this project
   */
  def isMember(project: String, user: String): Future[Boolean] =
    Couch.bucket.flatMap(_.get[Project](project)).map(_.exists(_.members.exists(_.email == user)))

  /**
   * Create a Project.
   */
  def create(project: Project, members: Seq[String]): Future[Project] = for {
    b <- Couch.bucket
    users <- Future.sequence(members.map(User.findByEmail))
    p = project.copy(id = UUID.randomUUID().toString, members = users.toList.flatten)
    _ <- b.add[Project](p.id, p)
  } yield p
}
