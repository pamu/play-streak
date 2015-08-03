package global

import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.{Logger, Application, GlobalSettings}

/**
 * Created by pnagarjuna on 29/06/15.
 */

object Global extends GlobalSettings {

  lazy val system = Akka.system

  override def onStart(app: Application): Unit = {
    super.onStart(app)
    Logger.info("Application Started.")
  }

  override def onStop(app: Application): Unit = {
    super.onStop(app)
    Logger.info("Application Stopped.")
  }

}
