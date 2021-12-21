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

package com.google.devtools.build.lib.sandbox;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.devtools.build.lib.actions.LocalHostCapacity;
import com.google.devtools.build.lib.util.OptionsUtils;
import com.google.devtools.build.lib.util.ResourceConverter;
import com.google.devtools.build.lib.vfs.FileSystem;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.Converters.TriStateConverter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionMetadataTag;
import com.google.devtools.common.options.OptionsBase;
import com.google.devtools.common.options.OptionsParsingException;
import com.google.devtools.common.options.TriState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Options for sandboxed execution. */
public class SandboxOptions extends OptionsBase {

  /**
   * A converter for customized path mounting pair from the parameter list of a bazel command
   * invocation. Pairs are expected to have the form 'source:target'.
   */
  public static final class MountPairConverter
      implements Converter<ImmutableMap.Entry<String, String>> {

    @Override
    public ImmutableMap.Entry<String, String> convert(String input) throws OptionsParsingException {

      List<String> paths = Lists.newArrayList();
      for (String path : input.split("(?<!\\\\):")) { // Split on ':' but not on '\:'
        if (path != null && !path.trim().isEmpty()) {
          paths.add(path.replace("\\:", ":"));
        } else {
          throw new OptionsParsingException(
              "Input "
                  + input
                  + " contains one or more empty paths. "
                  + "Input must be a single path to mount inside the sandbox or "
                  + "a mounting pair in the form of 'source:target'");
        }
      }

      if (paths.size() < 1 || paths.size() > 2) {
        throw new OptionsParsingException(
            "Input must be a single path to mount inside the sandbox or "
                + "a mounting pair in the form of 'source:target'");
      }

      return paths.size() == 1
          ? Maps.immutableEntry(paths.get(0), paths.get(0))
          : Maps.immutableEntry(paths.get(0), paths.get(1));
    }

    @Override
    public String getTypeDescription() {
      return "a single path or a 'source:target' pair";
    }
  }

  @Option(
      name = "ignore_unsupported_sandboxing",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "Do not print a warning when sandboxed execution is not supported on this system.")
  public boolean ignoreUnsupportedSandboxing;

  @Option(
      name = "sandbox_debug",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Enables debugging features for the sandboxing feature. This includes two things: first, "
              + "the sandbox root contents are left untouched after a build (and if sandboxfs is "
              + "in use, the file system is left mounted); and second, prints extra debugging "
              + "information on execution. This can help developers of Bazel or Starlark rules "
              + "with debugging failures due to missing input files, etc.")
  public boolean sandboxDebug;

  @Option(
      name = "sandbox_base",
      oldName = "experimental_sandbox_base",
      defaultValue = "",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Lets the sandbox create its sandbox directories underneath this path. Specify a path on"
              + " tmpfs (like /run/shm) to possibly improve performance a lot when your build /"
              + " tests have many input files. Note: You need enough RAM and free space on the"
              + " tmpfs to hold output and intermediate files generated by running actions.")
  public String sandboxBase;

  @Option(
      name = "sandbox_fake_hostname",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "Change the current hostname to 'localhost' for sandboxed actions.")
  public boolean sandboxFakeHostname;

  @Option(
      name = "sandbox_fake_username",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "Change the current username to 'nobody' for sandboxed actions.")
  public boolean sandboxFakeUsername;

  @Option(
      name = "sandbox_block_path",
      allowMultiple = true,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "For sandboxed actions, disallow access to this path.")
  public List<String> sandboxBlockPath;

  @Option(
      name = "sandbox_tmpfs_path",
      allowMultiple = true,
      converter = OptionsUtils.AbsolutePathFragmentConverter.class,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "For sandboxed actions, mount an empty, writable directory at this absolute path"
              + " (if supported by the sandboxing implementation, ignored otherwise).")
  public List<PathFragment> sandboxTmpfsPath;

  @Option(
      name = "sandbox_writable_path",
      allowMultiple = true,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "For sandboxed actions, make an existing directory writable in the sandbox"
              + " (if supported by the sandboxing implementation, ignored otherwise).")
  public List<String> sandboxWritablePath;

  @Option(
      name = "sandbox_add_mount_pair",
      allowMultiple = true,
      converter = MountPairConverter.class,
      defaultValue = "null",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help = "Add additional path pair to mount in sandbox.")
  public List<ImmutableMap.Entry<String, String>> sandboxAdditionalMounts;

  @Option(
      name = "experimental_use_sandboxfs",
      converter = TriStateConverter.class,
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Use sandboxfs to create the actions' execroot directories instead of building a symlink "
              + "tree. If \"yes\", the binary provided by --experimental_sandboxfs_path must be "
              + "valid and correspond to a supported version of sandboxfs. If \"auto\", the binary "
              + "may be missing or not compatible.")
  public TriState useSandboxfs;

  @Option(
      name = "experimental_sandboxfs_path",
      defaultValue = "sandboxfs",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Path to the sandboxfs binary to use when --experimental_use_sandboxfs is true. If a "
              + "bare name, use the first binary of that name found in the PATH.")
  public String sandboxfsPath;

  @Option(
      name = "experimental_sandboxfs_map_symlink_targets",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "If true, maps the targets of symbolic links specified as action inputs into the "
              + "sandbox. This feature exists purely to workaround buggy rules that do not do "
              + "this on their own and should be removed once all such rules are fixed.")
  public boolean sandboxfsMapSymlinkTargets;

  @Option(
      name = "experimental_use_windows_sandbox",
      converter = TriStateConverter.class,
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Use Windows sandbox to run actions. "
              + "If \"yes\", the binary provided by --experimental_windows_sandbox_path must be "
              + "valid and correspond to a supported version of sandboxfs. If \"auto\", the binary "
              + "may be missing or not compatible.")
  public TriState useWindowsSandbox;

  @Option(
      name = "experimental_windows_sandbox_path",
      defaultValue = "BazelSandbox.exe",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Path to the Windows sandbox binary to use when --experimental_use_windows_sandbox is"
              + " true. If a bare name, use the first binary of that name found in the PATH.")
  public String windowsSandboxPath;

  public ImmutableSet<Path> getInaccessiblePaths(FileSystem fs) {
    List<Path> inaccessiblePaths = new ArrayList<>();
    for (String path : sandboxBlockPath) {
      Path blockedPath = fs.getPath(path);
      try {
        inaccessiblePaths.add(blockedPath.resolveSymbolicLinks());
      } catch (IOException e) {
        // It's OK to block access to an invalid symlink. In this case we'll just make the symlink
        // itself inaccessible, instead of the target, though.
        inaccessiblePaths.add(blockedPath);
      }
    }
    return ImmutableSet.copyOf(inaccessiblePaths);
  }

  @Option(
      name = "experimental_collect_local_sandbox_action_metrics",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "When enabled, execution statistics (such as user and system time) are recorded for "
              + "locally executed actions which use sandboxing")
  public boolean collectLocalSandboxExecutionStatistics;

  @Option(
      name = "experimental_enable_docker_sandbox",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "Enable Docker-based sandboxing. This option has no effect if Docker is not installed.")
  public boolean enableDockerSandbox;

  @Option(
      name = "experimental_docker_image",
      defaultValue = "",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "Specify a Docker image name (e.g. \"ubuntu:latest\") that should be used to execute a"
              + " sandboxed action when using the docker strategy and the action itself doesn't"
              + " already have a container-image attribute in its remote_execution_properties in"
              + " the platform description. The value of this flag is passed verbatim to 'docker"
              + " run', so it supports the same syntax and mechanisms as Docker itself.")
  public String dockerImage;

  @Option(
      name = "experimental_docker_use_customized_images",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If enabled, injects the uid and gid of the current user into the Docker image before"
              + " using it. This is required if your build / tests depend on the user having a name"
              + " and home directory inside the container. This is on by default, but you can"
              + " disable it in case the automatic image customization feature doesn't work in your"
              + " case or you know that you don't need it.")
  public boolean dockerUseCustomizedImages;

  @Option(
      name = "experimental_docker_verbose",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If enabled, Bazel will print more verbose messages about the Docker sandbox strategy.")
  public boolean dockerVerbose;

  @Option(
      name = "experimental_docker_privileged",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If enabled, Bazel will pass the --privileged flag to 'docker run' when running actions. "
              + "This might be required by your build, but it might also result in reduced "
              + "hermeticity.")
  public boolean dockerPrivileged;

  @Option(
      name = "sandbox_default_allow_network",
      oldName = "experimental_sandbox_default_allow_network",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
      effectTags = {OptionEffectTag.UNKNOWN},
      help =
          "Allow network access by default for actions; this may not work with all sandboxing "
              + "implementations.")
  public boolean defaultSandboxAllowNetwork;

  @Option(
      name = "experimental_sandbox_async_tree_delete_idle_threads",
      defaultValue = "0",
      converter = AsyncTreeDeletesConverter.class,
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If 0, delete sandbox trees as soon as an action completes (causing completion of the "
              + "action to be delayed). If greater than zero, execute the deletion of such threes"
              + " on an asynchronous thread pool that has size 1 when the build is running and"
              + " grows to the size specified by this flag when the server is idle.")
  public int asyncTreeDeleteIdleThreads;

  @Option(
      name = "experimental_delay_virtual_input_materialization",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If set to true, creates virtual inputs (like params files) only inside the sandbox, "
              + "not in the execroot, which fixes a race condition when using the dynamic "
              + "scheduler. This flag exists purely to support rolling this bug fix out.")
  public boolean delayVirtualInputMaterialization;

  @Option(
      name = "incompatible_legacy_local_fallback",
      defaultValue = "true",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      metadataTags = {OptionMetadataTag.INCOMPATIBLE_CHANGE},
      help =
          "If set to true, enables the legacy implicit fallback from sandboxed to local strategy."
              + " This flag will eventually default to false and then become a no-op. Use"
              + " --strategy, --spawn_strategy, or --dynamic_local_strategy to configure fallbacks"
              + " instead.")
  public boolean legacyLocalFallback;

  @Option(
      name = "experimental_reuse_sandbox_directories",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If set to true, directories used by sandboxed non-worker execution may be reused to"
              + " avoid unnecessary setup costs.")
  public boolean reuseSandboxDirectories;

  @Option(
      name = "experimental_use_hermetic_linux_sandbox",
      defaultValue = "false",
      documentationCategory = OptionDocumentationCategory.EXECUTION_STRATEGY,
      effectTags = {OptionEffectTag.EXECUTION},
      help =
          "If set to true, do not mount root, only mount whats provided with "
              + "sandbox_add_mount_pair. Input files will be hardlinked to the sandbox instead of "
              + "symlinked to from the sandbox. "
              + "If action input files are located on a filesystem different from the sandbox, "
              + "then the input files will be copied instead.")
  public boolean useHermetic;

  /** Converter for the number of threads used for asynchronous tree deletion. */
  public static final class AsyncTreeDeletesConverter extends ResourceConverter {
    public AsyncTreeDeletesConverter() {
      super(
          () -> (int) Math.ceil(LocalHostCapacity.getLocalHostCapacity().getCpuUsage()),
          0,
          Integer.MAX_VALUE);
    }
  }
}