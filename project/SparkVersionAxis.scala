import sbt.VirtualAxis

case class SparkVersionAxis(idSuffix: String, directorySuffix: String) extends VirtualAxis.StrongAxis {
  def axisValues: Seq[VirtualAxis] = Seq(this, VirtualAxis.jvm)
}
