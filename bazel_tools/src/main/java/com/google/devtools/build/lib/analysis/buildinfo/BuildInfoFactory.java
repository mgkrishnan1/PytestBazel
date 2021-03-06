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
package com.google.devtools.build.lib.analysis.buildinfo;

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactRoot;
import com.google.devtools.build.lib.analysis.config.BuildConfigurationValue;
import com.google.devtools.build.lib.vfs.PathFragment;

/**
 * A factory for language-specific build-info files.
 *
 * <p>The goal of the build info system is to "stamp" non-hermetic information into output files,
 * for example, the time and date of the build that resulted in the output, the hostname it was run
 * on or the set of sources the output was built from.
 *
 * <p>This non-hermetic data gets into the action graph by calling the script specified in the
 * <code>--workspace_status_command</code> command line argument, which results in a text file which
 * containts a build info entry in each line, with its name ("key") and value separated by a space.
 * This script is unconditionally invoked on every build and therefore should be very fast.
 *
 * <p>Build info keys come in two kinds: volatile and non-volatile. The difference is that the
 * former is expected to change very frequently (e.g. current time) and therefore changes to it
 * should not invalidate downstream actions whereas a rebuild is required if a non-volatile build
 * info entry changes.
 *
 * <p>This is accomplished by splitting the build info file emitted by the workspace status command
 * into two files, a volatile and a non-volatile. The former kind of artifact is special-cased in
 * the execution phase machinery so that changes to it never trigger a rebuild. This artifact is
 * marked by {@link
 * com.google.devtools.build.lib.actions.Artifact.SpecialArtifactType#CONSTANT_METADATA}.
 *
 * <p>The invocation of the workspace status command and splitting its output into two is done in
 * descendants of {@link com.google.devtools.build.lib.analysis.WorkspaceStatusAction} .
 *
 * <p>However, this is not enough because the workspace status files cannot always be ingested by
 * the actions that need them; for example, if a C++ file wants to incorporate build info, the
 * compiler cannot process build info text files, therefore the data needs to be transformed into a
 * format that the compiler likes.
 *
 * <p>This is done for each language by an implementation of {@link BuildInfoFactory}: rules can
 * call {@link com.google.devtools.build.lib.analysis.AnalysisEnvironment#getBuildInfo(boolean,
 * BuildInfoKey, BuildConfigurationValue)} with the language-specific build info key, which then
 * invokes {@link com.google.devtools.build.lib.skyframe.BuildInfoCollectionFunction}, which in turn
 * calls the language-specific implementation of {@link BuildInfoFactory}, which creates the
 * language-specific actions and artifacts. These are then returned to the caller of {@code
 * getBuildInfo()} (This could probably be replaced by an implicit dependency on a language-specific
 * special rule does all this; there are only historical reasons why it works this way)
 *
 * <p>{@link com.google.devtools.build.lib.skyframe.BuildInfoCollectionValue} is a thin wrapper
 * around the data structure {@code BuildInfoFactory} returns (a set of artifacts and actions). Its
 * purpose is to allow Skyframe to look up the generating actions of build info artifacts. This is
 * done by implementing {@link com.google.devtools.build.lib.actions.ActionLookupValue}. It is
 * necessary because actions are usually generated by configured targets or aspects, but not build
 * info actions which are instead created by the mechanism described above.
 *
 * <p>Build info factories are registered in {@link
 * com.google.devtools.build.lib.analysis.ConfiguredRuleClassProvider}.
 */
public interface BuildInfoFactory {
  /**
   * Type of the build-data artifact.
   */
  enum BuildInfoType {
    /**
     * Ignore changes to this file for the purposes of determining whether an action needs to be
     * re-executed. I.e., the action is only re-executed if at least one other input has changed.
     */
    NO_REBUILD,

    /**
     * Changes to this file trigger re-execution of actions, similar to source file changes.
     */
    FORCE_REBUILD_IF_CHANGED;
  }

  /**
   * Context for the creation of build-info artifacts.
   */
  interface BuildInfoContext {
    Artifact getBuildInfoArtifact(
        PathFragment rootRelativePath, ArtifactRoot root, BuildInfoType type);
  }

  /** Create actions and artifacts for language-specific build-info files. */
  BuildInfoCollection create(
      BuildInfoContext context,
      BuildConfigurationValue config,
      Artifact buildInfo,
      Artifact buildChangelist);

  /**
   * Returns the key for the information created by this factory.
   */
  BuildInfoKey getKey();

  /**
   * Returns false if this build info factory is disabled based on the configuration (usually by
   * checking if all required configuration fragments are present).
   */
  boolean isEnabled(BuildConfigurationValue config);
}
