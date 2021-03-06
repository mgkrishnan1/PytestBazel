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

#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif

#include <windows.h>

#include "src/main/native/jni.h"

/*
 * Class:     Java_com_google_devtools_build_lib_platform_SystemThermalModule
 * Method:    registerJNI
 * Signature: ()V
 */
extern "C" JNIEXPORT void JNICALL
Java_com_google_devtools_build_lib_platform_SystemThermalModule_registerJNI(
    JNIEnv *env, jobject local_object) {
  // Currently not implemented.
}

/*
 * Class:     Java_com_google_devtools_build_lib_platform_SystemThermalModule
 * Method:    thermalLoad
 * Signature: ()I
 */
extern "C" JNIEXPORT int JNICALL
Java_com_google_devtools_build_lib_platform_SystemThermalModule_thermalLoad(
    JNIEnv *env, jobject local_object) {
  // Currently not implemented.
  return 0;
}

