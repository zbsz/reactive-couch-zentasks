import play.api._

import models._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    Await.result(InitialData.insert(), 5.seconds)
  }
  
}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
  
  def insert() = User.findAll flatMap {
    case users if users.isEmpty =>
      for {
        users <- Future.sequence(Seq(
            User("guillaume@sample.com", "Guillaume Bort", "secret"),
            User("maxime@sample.com", "Maxime Dantec", "secret"),
            User("sadek@sample.com", "Sadek Drobi", "secret"),
            User("erwan@sample.com", "Erwan Loisant", "secret")
          ).map(User.create))
        projects <- Future.sequence(Seq(
            Project("", "Play framework", "Play 2.0") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
            Project("", "Play framework", "Play 1.2.4") -> Seq("guillaume@sample.com", "erwan@sample.com"),
            Project("", "Play framework", "Website") -> Seq("guillaume@sample.com", "maxime@sample.com"),
            Project("", "Zenexity", "Secret project") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
            Project("", "Zenexity", "Playmate") -> Seq("maxime@sample.com"),
            Project("", "Personal", "Things to do") -> Seq("guillaume@sample.com"),
            Project("", "Zenexity", "Play samples") -> Seq("guillaume@sample.com", "maxime@sample.com"),
            Project("", "Personal", "Private") -> Seq("maxime@sample.com"),
            Project("", "Personal", "Private") -> Seq("guillaume@sample.com"),
            Project("", "Personal", "Private") -> Seq("erwan@sample.com"),
            Project("", "Personal", "Private") -> Seq("sadek@sample.com")
          ).map {
            case (project,members) => Project.create(project, members)
          })
        tasks <- Future(Seq(
            Task("", "Todo", projects(0).id, "Fix the documentation", false, None, Some("guillaume@sample.com")),
            Task("", "Urgent", projects(0).id, "Prepare the beta release", false, Some(date("2011-11-15")), None),
            Task("", "Todo", projects(8).id, "Buy some milk", false, None, None),
            Task("", "Todo", projects(1).id, "Check 1.2.4-RC2", false, Some(date("2011-11-18")), Some("guillaume@sample.com")),
            Task("", "Todo", projects(6).id, "Finish zentask integration", true, Some(date("2011-11-15")), Some("maxime@sample.com")),
            Task("", "Todo", projects(3).id, "Release the secret project", false, Some(date("2012-01-01")), Some("sadek@sample.com"))
          ).map(Task.create))
      } yield ()
    case _ => Future.successful(Unit)
  }
}
