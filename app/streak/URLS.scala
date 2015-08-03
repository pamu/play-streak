package streak

/**
 * Created by pnagarjuna on 29/06/15.
 */

object URLS {
  val boxes = "https://www.streak.com/api/v1/boxes"
  val main = "https://www.streak.com/api/v1/"
  val pipelines = "https://www.streak.com/api/v1/pipelines"
  def pipelineStages(pipelineKey: String) = main + s"pipelines/$pipelineKey/stages"
  def pipelineBoxes(pipelineKey: String) = main + s"pipelines/$pipelineKey/boxes"
}
