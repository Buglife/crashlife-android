//
// Created by Daniel DeCovnick on 2/8/19.
//

//#include "native-lib.h"
#include <jni.h>
#include <string>
#include <iostream>

void doStuff();
void doAnotherThing();
static void andAThird();

extern "C" void doSomeOtherThing();
extern "C" void doSomeStuff();

extern "C" JNIEXPORT void JNICALL
Java_com_buglife_crashlife_android_example_MainActivity_crashMe(JNIEnv *, jobject) {
    std::string hello = "Hello from C++";
    std::cout << "Goodbye from C++" << std::endl;
    doStuff();
}

void doStuff() {
    doAnotherThing();
}

void doAnotherThing() {
    doSomeStuff();
}

static void andAThird() {
    abort();
}