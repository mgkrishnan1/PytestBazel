// Copyright 2019 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.actionsketch;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ActionSketch}. */
@RunWith(JUnit4.class)
public final class ActionSketchTest {

  @Test
  public void serialization() {
    roundTrip(
        ActionSketch.builder()
            .setTransitiveSourceHash(HashAndVersion.create(BigInteger.ONE, /*version=*/ 0L))
            .build());
  }

  private static void roundTrip(ActionSketch sketch) {
    assertThat(ActionSketch.fromBytes(sketch.toBytes())).isEqualTo(sketch);
  }

  @Test
  public void canonicalNullInstance() {
    ActionSketch sketch1 =
        ActionSketch.builder()
            .setTransitiveSourceHash(HashAndVersion.create(null, /*version=*/ Long.MAX_VALUE))
            .build();
    ActionSketch sketch2 =
        ActionSketch.builder()
            .setTransitiveSourceHash(HashAndVersion.create(null, /*version=*/ Long.MAX_VALUE))
            .build();

    assertThat(sketch1).isNotNull();
    assertThat(sketch1).isSameInstanceAs(sketch2);
  }
}
