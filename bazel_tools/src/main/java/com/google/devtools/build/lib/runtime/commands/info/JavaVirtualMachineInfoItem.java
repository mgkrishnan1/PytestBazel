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

import com.google.common.base.Supplier;
import com.google.devtools.build.lib.analysis.config.BuildConfigurationValue;
import com.google.devtools.build.lib.runtime.CommandEnvironment;
import com.google.devtools.build.lib.runtime.InfoItem;

/** Info item for the name and version of the Java VM. */
public final class JavaVirtualMachineInfoItem extends InfoItem {
  public JavaVirtualMachineInfoItem() {
    super("java-vm", "Name and version of the current Java virtual machine.", false);
  }

  @Override
  public byte[] get(
      Supplier<BuildConfigurationValue> configurationSupplier, CommandEnvironment env) {
    return print(
        String.format(
            "%s (build %s, %s) by %s",
            System.getProperty("java.vm.name", "Unknown VM"),
            System.getProperty("java.vm.version", "unknown"),
            System.getProperty("java.vm.info", "unknown"),
            System.getProperty("java.vm.vendor", "unknown")));
  }
}
