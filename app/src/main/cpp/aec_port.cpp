//
// Created by rubbe on 31.10.2021.
//

#include <jni.h>

#include "speex/speex_echo.h"

extern "C"
JNIEXPORT void JNICALL
Java_ru_sberdigitalauto_loudspeaker_EchoCancelationNativeHelper_closeAEC(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jlong aec_handler) {
    auto *echoState = (SpeexEchoState *)aec_handler;
    speex_echo_state_destroy(echoState);
}
extern "C"
JNIEXPORT void JNICALL
Java_ru_sberdigitalauto_loudspeaker_EchoCancelationNativeHelper_cancelEcho(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jlong aec_handler,
                                                                           jbyteArray mic_input,
                                                                           jbyteArray echo_input,
                                                                           jbyteArray echo_out) {
    auto *echoState = (SpeexEchoState *)aec_handler;
    jbyte* inputDataPtr = env->GetByteArrayElements(mic_input, NULL);
    jbyte* echoDataPtr = env->GetByteArrayElements(echo_input, NULL);

    //это место надо бы сделать более аккуратно
    auto *pData = new jbyte[env->GetArrayLength(echo_out)];

    speex_echo_cancellation(echoState, (short *)inputDataPtr, (short *)echoDataPtr, (short *)pData);

    env->SetByteArrayRegion(echo_out, 0, env->GetArrayLength(echo_out), pData);
    delete[] pData;

    env->ReleaseByteArrayElements(echo_input, echoDataPtr, JNI_ABORT);
    env->ReleaseByteArrayElements(mic_input, inputDataPtr, JNI_ABORT);
}

extern "C"
JNIEXPORT jlong JNICALL
Java_ru_sberdigitalauto_loudspeaker_EchoCancelationNativeHelper_initAEC(JNIEnv *env, jobject thiz,
                                                                        jint buffer_size,
                                                                        jint filter_size) {
    SpeexEchoState *echoState = speex_echo_state_init(buffer_size, filter_size);
    return (jlong )echoState;
}