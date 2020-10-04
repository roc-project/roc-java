#include "org_rocstreaming_roctoolkit_Receiver.h"

#include "common.h"
#include "channel_set.h"
#include "frame_encoding.h"
#include "resampler_profile.h"
#include "address.h"

#include <roc/receiver.h>

#define RECEIVER_CLASS              PACKAGE_BASE_NAME "/Receiver"
#define RECEIVER_CONFIG_CLASS       PACKAGE_BASE_NAME "/ReceiverConfig"

char receiver_config_unmarshall(JNIEnv *env, roc_receiver_config* config, jobject jconfig) {
    jobject tempObject;
    jclass  receiverConfigClass;
    char err = 0;

    receiverConfigClass = env->FindClass(RECEIVER_CONFIG_CLASS);
    assert(receiverConfigClass != NULL);

    config->frame_sample_rate = get_uint_field_value(env, receiverConfigClass, jconfig, "frameSampleRate", &err);
    if (err) return err;

    tempObject = get_object_field(env, receiverConfigClass, jconfig, "frameChannels", "L" CHANNEL_SET_CLASS ";");
    if (tempObject == NULL) return 1;
    config->frame_channels = (roc_channel_set) get_channel_set(env, tempObject);

    tempObject = get_object_field(env, receiverConfigClass, jconfig, "frameEncoding", "L" FRAME_ENCODING_CLASS ";");
    if (tempObject == NULL) return 1;
    config->frame_encoding = (roc_frame_encoding) get_frame_encoding(env, tempObject);

    config->automatic_timing = get_boolean_field_value(env, receiverConfigClass, jconfig, "automaticTiming", &err);
    if (err) return err;

    tempObject = get_object_field(env, receiverConfigClass, jconfig, "resamplerProfile", "L" RESAMPLER_PROFILE_CLASS ";");
    config->resampler_profile = (roc_resampler_profile) get_resampler_profile(env, tempObject);

    config->target_latency = get_ullong_field_value(env, receiverConfigClass, jconfig, "targetLatency", &err);
    if (err) return err;
    config->max_latency_overrun = get_ullong_field_value(env, receiverConfigClass, jconfig, "maxLatencyOverrun", &err);
    if (err) return err;
    config->max_latency_underrun = get_ullong_field_value(env, receiverConfigClass, jconfig, "maxLatencyUnderrun", &err);
    if (err) return err;
    config->no_playback_timeout = get_llong_field_value(env, receiverConfigClass, jconfig, "noPlaybackTimeout", &err);
    if (err) return err;
    config->broken_playback_timeout = get_llong_field_value(env, receiverConfigClass, jconfig, "brokenPlaybackTimeout", &err);
    if (err) return err;
    config->breakage_detection_window = get_ullong_field_value(env, receiverConfigClass, jconfig, "breakageDetectionWindow", &err);
    return err;
}

JNIEXPORT jlong JNICALL Java_org_rocstreaming_roctoolkit_Receiver_open(JNIEnv * env, jclass receiverClass,
                    jlong contextPtr, jobject jconfig) {
    roc_context*            context;
    roc_receiver_config     receiverConfig;
    roc_receiver*           receiver;

    context = (roc_context*) contextPtr;

    if (receiver_config_unmarshall(env, &receiverConfig, jconfig) != 0) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return (jlong) NULL;
    }

    if ((receiver = roc_receiver_open(context, &receiverConfig)) == NULL) {
        jclass exceptionClass = env->FindClass(EXCEPTION);
        env->ThrowNew(exceptionClass, "Error opening receiver");
        return (jlong) NULL;
    }

    return (jlong) receiver;
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Receiver_bind(JNIEnv * env, jobject thisObj, jlong receiverPtr,
                    jint type, jint protocol, jobject jaddress) {
    roc_receiver*   receiver    = NULL;
    roc_address     address;
    int             port        = 0;

    receiver = (roc_receiver*) receiverPtr;

    if (address_unmarshall(env, &address, jaddress) != 0) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return;
    }

    if ((port = roc_address_port(&address)) < 0) {
        jclass exceptionClass = env->FindClass(EXCEPTION);
        env->ThrowNew(exceptionClass, "Error binding receiver");
        return;
    }

    if (roc_receiver_bind(receiver, (roc_port_type) type, (roc_protocol) protocol, &address) != 0) {
        jclass exceptionClass = env->FindClass(EXCEPTION);
        env->ThrowNew(exceptionClass, "Error binding receiver");
        return;
    }

    // ephemeral port
    if (!port) {
        port = roc_address_port(&address);
        address_set_port(env, jaddress, port);
    }
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Receiver_readFloats(JNIEnv *env, jobject thisObj, jlong receiverPtr, jfloatArray jsamples) {
    roc_receiver*       receiver;
    roc_frame           frame;
    jfloat*             samples;
    jsize               len;

    if (jsamples == NULL) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return;
    }

    samples = env->GetFloatArrayElements(jsamples, 0);
    len = env->GetArrayLength(jsamples);

    receiver = (roc_receiver*) receiverPtr;

    memset(&frame, 0, sizeof(frame));
    frame.samples = samples;
    frame.samples_size = len * sizeof(float);

    if (roc_receiver_read(receiver, &frame) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error with receiver read");
        // no return
    }

    env->ReleaseFloatArrayElements(jsamples, samples, 0);
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Receiver_close(JNIEnv *env, jclass receiverClass, jlong receiverPtr) {

    roc_receiver* receiver = (roc_receiver*) receiverPtr;

    if (roc_receiver_close(receiver) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error closing receiver");
    }
}