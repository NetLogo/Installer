#pragma once

#include <jni.h>

extern "C"
{
    JNIEXPORT jobject JNICALL Java_org_nlogo_installer_IconExt_extractIcon(JNIEnv* env, jobject obj, jstring path);
}
