package actors

import actors.DataStream.{Failure, Success}
import akka.actor.{Status, ActorLogging, Actor}
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{Writes, JsObject, Json, JsValue}
import streak.Streak
import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern.pipe
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.util.Random

/**
 * Created by pnagarjuna on 29/06/15.
 */

object DataStream {
  case object Stop
  case object Tick
  case class Interval(time: Int)
  case class Stream(channel: Channel[String])
  case class Success(msg: String)
  case class Failure(msg: String)
}

case class Stage(name: String, count: Long)

class DataStream(streakKey: String) extends Actor with ActorLogging {

  override def preStart() = log.info("DataStream Actor Started ... ")

  override def postStop() = log.info("DataStream Actor Stopped ... ")

  import DataStream._

  var channelOption: Option[Channel[String]] = None

  val defaultInterval = 5

  var interval: Option[Int] = Some(defaultInterval)

  override def receive = {
    case Stream(channel) =>
      channelOption = Some(channel)
      self ! Tick

    case Interval(time) =>
      interval = Some(time)

    case Tick =>
      log.info("Got a Tick")

      Utils.getData(streakKey) pipeTo self

      /**
      val limit = 2000
      Future {
        scala.concurrent.blocking {
          Success(Json.obj(
            "total" -> s"${Random.nextInt(limit)}",
            "stages" -> Json.arr(
              Json.obj("name" -> "Passing", "count" -> s"${Random.nextInt(limit)}"),
              Json.obj("name" -> "Closed", "count" -> s"${Random.nextInt(limit)}"),
              Json.obj("name" -> "Buffer Time", "count" -> s"${Random.nextInt(limit)}"),
              Json.obj("name" -> "Closed Deal", "count" -> s"${Random.nextInt(limit)}"),
              Json.obj("name" -> "DueD", "count" -> s"${Random.nextInt(limit)}")
            )
          ).toString())
        }
      }.recover {case th => Failure(Json.obj("failure" -> true, "reason" -> th.getMessage).toString()) } pipeTo self **/

      //Dummy testing code
      /**
      implicit val stageWrites: Writes[Stage] = new Writes[Stage] {
        override def writes(o: Stage): JsValue = {
          Json.obj("name" -> o.name, "count" -> o.count)
        }
      }
      val data = List(Stage("apple", Random.nextInt), Stage("ball", Random.nextInt()))
      Future(Success(Json.obj("total" -> 1, "stages" -> Json.toJson(data)).toString)) pipeTo self  **/

    case Success(msg) =>
      channelOption.map(_ push msg)

      if (interval.isEmpty) interval = Some(defaultInterval)
      interval.map(time => context.system.scheduler.scheduleOnce(time seconds, self, Tick))

    case Failure(msg) =>
      channelOption.map(_ push msg)

      if (interval.isEmpty) interval = Some(defaultInterval)
      interval.map(time => context.system.scheduler.scheduleOnce(time seconds, self, Tick))

    case Status.Failure(cause) =>
      log.info("Future execution failed due to {}", cause.getMessage)
      channelOption.map(_ push (Json.obj("failure" -> true, "reason" -> cause.getMessage).toString))

      if (interval.isEmpty) interval = Some(defaultInterval)
      interval.map(time => context.system.scheduler.scheduleOnce(time seconds, self, Tick))

    case Stop =>
      context stop self

    case x => log.info("Unknown message {} of type {}", x, x getClass)
  }
}

object Utils {

  def getData(key: String) = {
    implicit val implicitKey = key
    Streak.pipelines.flatMap { response =>
      val (status, body) = response
      val pipelines = Json.parse(body).as[List[JsValue]]
      val dealflow = pipelines.filter { json => (json \ "name").as[String] == "Dealflow"}
      val dealflowKey = (dealflow(0) \ "pipelineKey").as[String]
      Streak.getPipelineStages(dealflowKey).flatMap { response =>
        val (status, body) = response
        val stages = Json.parse(body).as[JsObject]
        val stageMap = stages.fieldSet.map { stage => (stage._2 \ "key").as[String] -> (stage._2 \ "name").as[String]}.toMap[String, String]

        Streak.getPipelineBoxes(dealflowKey).map { response =>
          val (status, body) = response
          val boxes = Json.parse(body).as[List[JsValue]]
          val totalDealflow = boxes.length
          val list = boxes.map { box => {
            val stageKey = (box \ "stageKey").as[String]
            stageKey.toLong
          }}

          val countMap = list.foldLeft(ListMap.empty[Long, Long]){ (r, c) => {
            if (r contains c) r ++ ListMap(c -> (r(c) + 1L))
            else r ++ ListMap(c -> 1L)
          }}

          val ranks = Map[String, Int]("Fund II Portfolio" -> 4, "Pending Close" -> 3)

          val sumSet = Set[String]("Lead", "Prelim Interest", "IC Discussion", "Partners interested", "Thumbsup, Termsheet to go out")

          //(lead+ prelim interest+ IC discussion + Partners interested + thumbsup)

          val stageList: List[Stage] = stageMap.map { pair => {
            if (countMap contains pair._1.toLong) {
              Stage(pair._2, countMap(pair._1.toLong))
            } else {
              Stage(pair._2, 0L)
            }
          }}.toList

          Logger.info(s"StageList: ${stageList.mkString(" ")}")

          val stageSumList: List[Stage] =  stageList.filter(stage => sumSet.contains(stage.name))

          Logger.info(s"Stage Sum List: ${stageSumList.mkString(" ")}")

          val sum: Long = stageSumList.foldLeft(0L)((r, c) => r + c.count)

          val stageRankMap: List[(Stage, Int)] = stageList.filter(stage => ranks.contains(stage.name)).map(stage => (stage, ranks(stage.name))) ++ List(Stage("In Pipeline", sum) -> 2)

          val jeffList: List[Stage] = stageRankMap.sortWith((a, b) => a._2 < b._2).map(_._1)

          Logger.info(s"JeffList: ${jeffList.mkString(" ")}")

          /**
          val finalMap = countMap.map { pair => {
            if (stageMap contains pair._1.toString) {
              (stageMap(pair._1.toString) -> pair._2)
            } else {
              (pair._1.toString -> pair._2)
            }
          }} **/

          implicit val stageWrites: Writes[Stage] = new Writes[Stage] {
            override def writes(o: Stage): JsValue = {
              Json.obj("name" -> o.name, "count" -> o.count)
            }
          }

          val finalJson = Json.obj("total" -> totalDealflow.toLong, "stages" -> Json.toJson(jeffList))

          Success(finalJson.toString)

        }.recover { case th => th.printStackTrace();
          Failure(Json.obj("failure" -> true, "reason" -> s"${th.getMessage.take(50)} ... ").toString) }
      }.recover { case th => th.printStackTrace();
        Failure(Json.obj("failure" -> true, "reason" -> s"${th.getMessage.take(50)} ... " ).toString) }
    }.recover { case th => th.printStackTrace();
      Failure(Json.obj("failure" -> true, "reason" -> s"${th.getMessage.take(50)} ... ").toString) }
  }
}
