// Copyright 2020 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.runtime.commands.info;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.devtools.build.lib.analysis.config.BuildConfigurationValue;
import com.google.devtools.build.lib.runtime.CommandEnvironment;
import com.google.devtools.build.lib.runtime.InfoItem;

/**
 * Info item for the default package. It is deprecated, it still works, when explicitly requested,
 * but are not shown by default. It prints multi-line messages and thus don't play well with grep.
 * We don't print them unless explicitly requested.
 */
// TODO(lberki): Try to remove this using an incompatible flag.
@Deprecated
public final class DefaultsPackageInfoItem extends InfoItem {
  public DefaultsPackageInfoItem() {
    super("defaults-package", "Obsolete. Retained for backwards compatibility.", true);
  }

  @Override
  public byte[] get(
      Supplier<BuildConfigurationValue> configurationSupplier, CommandEnvironment env) {
    checkNotNull(env);
    return print("");
  }
}
