//
// Created by Daniel DeCovnick on 2/8/19.
//

//#include "native-lib.h"
#include <jni.h>
#include <string>
#include <iostream>

extern "C" JNIEXPORT void JNICALL
Java_com_buglife_crashlife_android_example_MainActivity_crashMe(JNIEnv *, jobject) {
    std::string hello = "Hello from C++";
    std::cout << "Goodbye from C++" << std::endl;
    abort();
}