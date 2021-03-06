// Copyright 2014 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.io;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.vfs.RootedPath;
import com.google.devtools.build.skyframe.EmptySkyValue;
import com.google.devtools.build.skyframe.SkyFunction;
import com.google.devtools.build.skyframe.SkyKey;
import com.google.devtools.build.skyframe.SkyValue;

/**
 * Given a "cycle" of {@link RootedPath} files, emits an error message for this cycle. The keys for
 * this SkyFunction are assumed to deduplicate cycles that differ only in which element of the cycle
 * they start at, so multiple paths to the cycle will be reported by a single execution of this
 * function.
 *
 * <p>The cycle need not actually be a cycle -- any iterable exhibiting an error that is independent
 * of the iterable's starting point can be an argument to this function.
 */
abstract class AbstractFileChainUniquenessFunction implements SkyFunction {
  protected abstract String getConciseDescription();

  protected abstract String getHeaderMessage();

  protected abstract String getFooterMessage();

  protected abstract String elementToString(RootedPath path);

  @Override
  public SkyValue compute(SkyKey skyKey, Environment env) {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append(getConciseDescription() + " detected\n");
    errorMessage.append(getHeaderMessage() + "\n");
    @SuppressWarnings("unchecked")
    ImmutableList<RootedPath> chain = (ImmutableList<RootedPath>) skyKey.argument();
    for (RootedPath elt : chain) {
      errorMessage.append(elementToString(elt) + "\n");
    }
    errorMessage.append(getFooterMessage() + "\n");
    // The purpose of this SkyFunction is the side effect of emitting an error message exactly
    // once per build per unique error.
    env.getListener().handle(Event.error(errorMessage.toString()));
    return EmptySkyValue.INSTANCE;
  }

  /**
   * Creates a canonicalized representation of the cycle specified by {@code chain}. {@code chain}
   * must be non-empty. The representation may not be unique if cycle has duplicate elements.
   */
  static ImmutableList<RootedPath> canonicalize(ImmutableList<RootedPath> cycle) {
    int minPos = 0;
    RootedPath min = cycle.get(0);
    for (int i = 1; i < cycle.size(); i++) {
      RootedPath cur = cycle.get(i);
      if (cur.compareTo(min) < 0) {
        minPos = i;
        min = cur;
      }
    }
    return ImmutableList.<RootedPath>builderWithExpectedSize(cycle.size())
        .addAll(cycle.subList(minPos, cycle.size()))
        .addAll(cycle.subList(0, minPos))
        .build();
  }
}
