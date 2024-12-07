/*
 * Copyright 2024 Xebia Netherlands
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
