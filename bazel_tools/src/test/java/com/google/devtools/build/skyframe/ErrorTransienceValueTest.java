// Copyright 2015 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.skyframe;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ErrorTransienceValue}. */
@RunWith(JUnit4.class)
public final class ErrorTransienceValueTest {

  @Test
  public void testHashCodeNotSupported() {
    assertThrows(UnsupportedOperationException.class, ErrorTransienceValue.INSTANCE::hashCode);
  }

  @Test
  public void testToStringWorks() {
    assertThat(ErrorTransienceValue.INSTANCE.toString()).isEqualTo("ErrorTransienceValue");
  }

  @Test
  @SuppressWarnings("SelfEquals")
  public void testIsntEqualToSelf() {
    // assertThat(...).isNotEqualTo(...) does ref equality check, so use equals explicitly.
    assertThat(ErrorTransienceValue.INSTANCE.equals(ErrorTransienceValue.INSTANCE)).isFalse();
  }
}

