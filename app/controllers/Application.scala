package controllers

import actors.DataStream
import actors.DataStream._
import akka.actor.Props
import global.Global
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.EventSource
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import streak.Streak
import scala.concurrent.Future

/**
 * Created by pnagarjuna on 29/06/15.
 */

object Application extends Controller with Secured {

  def index = withAuth(parse.anyContent) { implicit key => implicit request =>
    Future(Ok(views.html.index("Dealflow")(request.session)))
  }

  def boxes = withAuth(parse.anyContent){ implicit key => request =>
    /**
    Streak.allBoxes.map { response => {
      Ok(s"${response.body}")
    }}.recover { case th => Ok(s"failed due to ${th.printStackTrace()}")} **/
    Streak.boxes.map { response => {
      val (status, body) = response
      if (status == 200) {
        val json = Json.parse(body)
        val pipelines = (json \\ "pipelineKey").length
        Logger.info(s"pipelines $pipelines")
      }
      Ok(body)
    }}.recover { case th => Ok(s"${th.getCause}")}
  }

  def dealflow = withAuth(parse.anyContent) { implicit key => request =>
    Streak.pipelines.flatMap { response =>
      val (status, body) = response
      val pipelines = Json.parse(body).as[List[JsValue]]
      val dealflow = pipelines.filter { json => (json \ "name").as[String] == "Dealflow"}
      val dealflowKey = (dealflow(0) \ "pipelineKey").as[String]
      Streak.getPipelineBoxes(dealflowKey).map { response =>
        val (status, body) = response
        println("deal flow " + body.length)
        Ok(s"${Json.prettyPrint(Json.parse(body))}")
      }.recover { case th => Ok(s"${th.getMessage}")}
    }.recover { case th => Ok(s"${th.getMessage}")}
  }

  def pipelines = withAuth(parse.anyContent) { implicit key => request =>
    Streak.pipelines.map { response =>
      val (status, body) = response
      val names = Json.parse(body).as[List[JsValue]]
      val name = names.filter { json => (json \ "name").as[String] == "Dealflow"}
      Ok(s"${Json.prettyPrint(name(0))}")
    }.recover { case th => Ok(s"${th.getMessage}")}
  }

  def stages = withAuth(parse.anyContent) { implicit key => request =>
    Streak.pipelines.flatMap { response =>
      val (status, body) = response
      val pipelines = Json.parse(body).as[List[JsValue]]
      val dealflow = pipelines.filter { json => (json \ "name").as[String] == "Dealflow"}
      val dealflowKey = (dealflow(0) \ "pipelineKey").as[String]
      Streak.getPipelineStages(dealflowKey).map { response =>
        val (status, body) = response
        Ok(s"${Json.prettyPrint(Json.parse(body))}")
      }.recover { case th => Ok(s"${th.getMessage}")}
    }.recover { case th => Ok(s"${th.getMessage}")}
  }

  //case class Box(name: String, count: Long)

  def stream = withAuth(parse.anyContent) { key => request =>
    val dataStream =  Global.system.actorOf(Props(new DataStream(key)))
    Future {
      val enum = Concurrent.unicast[String](
        channel => dataStream ! Stream(channel),
        dataStream ! Stop,
        (elem, input) => Unit)
      Ok.stream(enum &> EventSource()).as(EVENT_STREAM)
    }
  }

  val form = Form(single("key" -> nonEmptyText))

  def start = Action { implicit request =>
    Ok(views.html.start(form)(request.session))
  }

  def startPost = Action { implicit request =>
    form.bindFromRequest().fold(
      hasErrors => BadRequest(views.html.start(hasErrors)(request.session)),
      success => Redirect(routes.Application.index()).withNewSession.withSession("key" -> success)
    )
  }

  def takeMeOut() = withAuth(parse.anyContent) { key => request =>
    Future(Redirect(routes.Application.start()).withNewSession)
  }

}