package models

import play.api.libs.json.{JsObject, Json}
import scala.concurrent.Future
import com.geteit.rcouch.views.Query
import scala.concurrent.ExecutionContext.Implicits.global

case class User(email: String, name: String, password: String)

object User {
  
  // -- Parsers
  implicit val reads = Json.reads[User]
  implicit val writes = Json.writes[User].transform(v => v.asInstanceOf[JsObject] ++ Json.obj("docType" -> "User"))

  def userId(email: String) = "user_" + email

  // -- Queries
  
  /**
   * Retrieve a User from email.
   */
  def findByEmail(email: String): Future[Option[User]] = Couch.bucket.flatMap(_.get[User](userId(email)))

  /**
   * Retrieve all users.
   */
  def findAll: Future[List[User]] = for {
    b <- Couch.bucket
    v <- Couch.Users.all
    users <- b.list[User](v, Query())
  } yield users.flatten

  /**
   * Authenticate a User.
   */
  def authenticate(email: String, password: String): Future[Option[User]] = for {
    b <- Couch.bucket
    u <- b.get[User](userId(email))
  } yield u.filter(_.password == password)

  /**
   * Create a User.
   */
  def create(user: User): Future[Boolean] = Couch.bucket.flatMap(_.add[User](userId(user.email), user))
}
