/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pegasys.artemis.reference.bls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.errorprone.annotations.MustBeClosed;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import kotlin.Pair;
import org.apache.tuweni.bytes.Bytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pegasys.artemis.reference.TestSuite;
import tech.pegasys.artemis.util.mikuli.G2Point;

class g2_uncompressed extends TestSuite {
  private static String testFile = "**/g2_uncompressed.yaml";

  @ParameterizedTest(name = "{index}. message hash to G2 uncompressed {0} -> {1}")
  @MethodSource("readMessageHashG2Uncompressed")
  void messageHashToG2Uncompressed(Bytes message, Bytes domain, G2Point g2PointExpected) {
    G2Point g2PointActual = G2Point.hashToG2(message, domain);
    assertEquals(g2PointExpected, g2PointActual);
  }

  @MustBeClosed
  static Stream<Arguments> readMessageHashG2Uncompressed() throws IOException {
    List<Pair<Class, List<String>>> arguments = new ArrayList<Pair<Class, List<String>>>();
    arguments.add(getParams(Bytes.class, Arrays.asList("input", "message")));
    arguments.add(getParams(Bytes.class, Arrays.asList("input", "domain")));
    arguments.add(getParams(G2Point.class, Arrays.asList("output")));

    return findTests(testFile, arguments);
  }
}
