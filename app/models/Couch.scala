package models

import com.geteit.rcouch.CouchbaseClient
import play.api.libs.concurrent.Akka
import com.geteit.rcouch.views.DesignDocument.{MapFunction, ViewDef, DocumentDef}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.Play.current

/**
  */
object Couch {

  val couch = new CouchbaseClient(_system = Akka.system)
  val bucket = for {
    b <- couch.bucket("default")
    _ <- b.saveDesignDocument("zentasks", DocumentDef(Map(
      "users" -> ViewDef(MapFunction("function (doc, meta) { if (meta.type == 'json' && doc.docType && doc.docType == 'User') emit(null, null); }")),
      "project_by_member" -> ViewDef(MapFunction(
        """function (doc, meta) {
          |  if (meta.type == 'json' && doc.docType && doc.docType == 'Project' && doc.members) {
          |    for(var i = 0; i < doc.members.length; i++) {
          |      emit(doc.members[i].email, null);
          |    }
          |  }
          |}""".stripMargin)),
      "project_by_folder" -> ViewDef(MapFunction(
        """function (doc, meta) {
          |  if (meta.type == 'json' && doc.docType && doc.docType == 'Project' && doc.folder) {
          |    emit(doc.folder, null);
          |  }
          |}""".stripMargin)),
      "tasks" -> ViewDef(MapFunction(
        """function (doc, meta) {
          |  if (meta.type == 'json' && doc.docType && doc.docType == 'Task' && doc.project) {
          |    emit([doc.done, doc.project, doc.folder], null);
          |  }
          |}""".stripMargin))
    )))
  } yield b

  object Users {

    val all = bucket.flatMap(_.getView("users", "zentasks"))
  }

  object Projects {
    val byMember = bucket.flatMap(_.getView("project_by_member", "zentasks"))
    val byFolder = bucket.flatMap(_.getView("project_by_folder", "zentasks"))
  }

  object Tasks {
    val all = bucket.flatMap(_.getView("tasks", "zentasks"))
  }
}
