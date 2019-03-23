//
// Created by Daniel DeCovnick on 3/22/19.
//

#include <jni.h>
#include <string>
#include <iostream>

extern "C" JNIEXPORT void doSomeOtherThing();
extern "C" JNIEXPORT void doSomeStuff();
static void andAThirdStuff();

extern "C" JNIEXPORT void JNICALL
Java_com_buglife_crashlife_android_example_MainActivity_crashMeHarder(JNIEnv *, jobject) {
    std::string hello = "Hello from C++";
    std::cout << "Goodbye from C++" << std::endl;
    doSomeStuff();
}

void doSomeStuff() {
    doSomeOtherThing();
}

void doSomeOtherThing() {
    andAThirdStuff();
}

static void andAThirdStuff() {
    abort();
}