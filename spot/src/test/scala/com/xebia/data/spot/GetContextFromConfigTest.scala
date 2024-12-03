package com.xebia.data.spot

import org.scalatest.flatspec.AnyFlatSpecLike
import scala.collection.JavaConverters._

class GetContextFromConfigTest extends AnyFlatSpecLike {
  import org.scalatest.matchers.should.Matchers._

  behavior of "GetContextFromConfigTest"

  it should "only return keys in the spark.com.xebia.data.spot namespace, with prefix removed" in new ContextFromConfigTest {
    val keys = getContextFromConfig.keys(spotConfig).asScala
    keys should contain only ("abc", "xyz")
  }

  it should "get values by applying the spark.com.xebia.data.spot prefix" in new ContextFromConfigTest {
    getContextFromConfig.get(spotConfig, "abc") should equal("abc")
    getContextFromConfig.get(spotConfig, "xyz") should equal("xyz")
  }
}

private[this] trait ContextFromConfigTest {
  val getContextFromConfig = new GetContextFromConfig()
  val spotConfig: Map[String, String] = Map(
    "spark.driver.cores" -> "2",
    "spark.executor.cores" -> "8",
    "abc" -> "def",
    "xyz" -> "123",
    "spark.com.xebia.data.spot.abc" -> "abc",
    "spark.com.xebia.data.spot.xyz" -> "xyz"
  )
}
