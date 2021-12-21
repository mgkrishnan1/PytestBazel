// Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.android.desugar.testdata;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test class carefully constructed so javac emits a lambda body method called lambda$filter$0,
 * which is exactly the name used for the lambda body method generated by javac for the superclass.
 */
public class LambdaInOverride extends OuterReferenceLambda {
  public LambdaInOverride(List<String> names) {
    super(names);
  }

  public List<String> filter(List<String> names) {
    return super.filter(names).stream()
        .filter(n -> !reference.contains(n))
        .collect(Collectors.toList());
  }
}
