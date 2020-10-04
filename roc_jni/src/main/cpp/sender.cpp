#include "org_rocstreaming_roctoolkit_Sender.h"
#include "common.h"
#include "channel_set.h"
#include "frame_encoding.h"
#include "packet_encoding.h"
#include "resampler_profile.h"
#include "fec_code.h"
#include "address.h"

#include <roc/sender.h>
#include <roc/frame.h>

#define SENDER_CLASS                PACKAGE_BASE_NAME "/Sender"
#define SENDER_CONFIG_CLASS         PACKAGE_BASE_NAME "/SenderConfig"

char sender_config_unmarshall(JNIEnv *env, roc_sender_config* config, jobject jconfig) {
    jobject tempObject;
    jclass senderConfigClass;
    char err = 0;

    senderConfigClass = env->FindClass(SENDER_CONFIG_CLASS);
    assert(senderConfigClass != NULL);

    config->frame_sample_rate = get_uint_field_value(env, senderConfigClass, jconfig, "frameSampleRate", &err);
    if (err) return err;

    tempObject = get_object_field(env, senderConfigClass, jconfig, "frameChannels", "L" CHANNEL_SET_CLASS ";");
    if (tempObject == NULL) return 1;
    config->frame_channels = (roc_channel_set) get_channel_set(env, tempObject);

    tempObject = get_object_field(env, senderConfigClass, jconfig, "frameEncoding", "L" FRAME_ENCODING_CLASS ";");
    if (tempObject == NULL) return 1;
    config->frame_encoding = (roc_frame_encoding) get_frame_encoding(env, tempObject);

    config->packet_sample_rate = get_uint_field_value(env, senderConfigClass, jconfig, "packetSampleRate", &err);
    if (err) return err;

    tempObject = get_object_field(env, senderConfigClass, jconfig, "packetChannels", "L" CHANNEL_SET_CLASS ";");
    config->packet_channels = (roc_channel_set) get_channel_set(env, tempObject);

    tempObject = get_object_field(env, senderConfigClass, jconfig, "packetEncoding", "L" PACKET_ENCODING_CLASS ";");
    config->packet_encoding = (roc_packet_encoding) get_packet_encoding(env, tempObject);

    config->packet_length = get_ullong_field_value(env, senderConfigClass, jconfig, "packetLength", &err);
    if (err) return err;
    config->packet_interleaving = get_uint_field_value(env, senderConfigClass, jconfig, "packetInterleaving", &err);
    if (err) return err;
    config->automatic_timing = get_boolean_field_value(env, senderConfigClass, jconfig, "automaticTiming", &err);
    if (err) return err;

    tempObject = get_object_field(env, senderConfigClass, jconfig, "resamplerProfile", "L" RESAMPLER_PROFILE_CLASS ";");
    config->resampler_profile = (roc_resampler_profile) get_resampler_profile(env, tempObject);

    tempObject = get_object_field(env, senderConfigClass, jconfig, "fecCode", "L" FEC_CODE_CLASS ";");
    config->fec_code = (roc_fec_code) get_fec_code(env, tempObject);

    config->fec_block_source_packets = get_uint_field_value(env, senderConfigClass, jconfig, "fecBlockSourcePackets", &err);
    if (err) return err;
    config->fec_block_repair_packets = get_uint_field_value(env, senderConfigClass, jconfig, "fecBlockRepairPackets", &err);
    return err;
}

JNIEXPORT jlong JNICALL Java_org_rocstreaming_roctoolkit_Sender_open(JNIEnv *env, jclass senderClass, jlong contextPtr, jobject jconfig) {
    roc_context*            context;
    roc_sender_config       config;
    roc_sender*             sender;

    context = (roc_context*) contextPtr;

    if (sender_config_unmarshall(env, &config, jconfig) != 0) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return (jlong) NULL;
    }

    if ((sender = roc_sender_open(context, &config)) == NULL) {
        jclass exceptionClass = env->FindClass(EXCEPTION);
        env->ThrowNew(exceptionClass, "Error opening sender");
        return (jlong) NULL;
    }

    return (jlong) sender;
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Sender_close(JNIEnv *env, jclass senderClass, jlong senderPtr) {

    roc_sender* sender = (roc_sender*) senderPtr;

    if (roc_sender_close(sender) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error closing sender");
    }
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Sender_bind(JNIEnv *env, jobject thisObj, jlong senderPtr, jobject jaddress) {
    roc_address     address;
    roc_sender*     sender;
    int             port;

    if (jaddress == NULL) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return;
    }

    sender = (roc_sender*) senderPtr;

    address_unmarshall(env, &address, jaddress);

    if ((port = roc_address_port(&address)) < 0) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error binding sender");
        return;
    }

    if (roc_sender_bind(sender, &address) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error binding sender");
        return;
    }

    // ephemeral port
    if (!port) {
        port = roc_address_port(&address);
        address_set_port(env, jaddress, port);
    }
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Sender_connect(JNIEnv *env, jobject thisObj, jlong senderPtr, jint portType, jint protocol, jobject jaddress) {
    roc_address     address;
    roc_sender*     sender;

    if (jaddress == NULL) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return;
    }

    sender = (roc_sender*) senderPtr;
    address_unmarshall(env, &address, jaddress);

    if (roc_sender_connect(sender, (roc_port_type) portType, (roc_protocol) protocol, &address) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error with sender connect");
    }
}

JNIEXPORT void JNICALL Java_org_rocstreaming_roctoolkit_Sender_writeFloats(JNIEnv *env, jobject thisObj, jlong senderPtr, jfloatArray jsamples) {
    jfloat*     samples;
    jsize       len;
    roc_frame   frame;

    if (jsamples == NULL) {
        jclass exceptionClass = env->FindClass(ILLEGAL_ARGUMENTS_EXCEPTION);
        env->ThrowNew(exceptionClass, "Bad arguments");
        return;
    }

    samples = env->GetFloatArrayElements(jsamples, 0);
    assert(samples != NULL);

    len = env->GetArrayLength(jsamples);

    memset(&frame, 0, sizeof(frame));

    frame.samples = samples;
    frame.samples_size = len * sizeof(float);

    roc_sender* sender = (roc_sender*) senderPtr;
    if (roc_sender_write(sender, &frame) != 0) {
        jclass exceptionClass = env->FindClass(IO_EXCEPTION);
        env->ThrowNew(exceptionClass, "Error sending frame");
    }

    env->ReleaseFloatArrayElements(jsamples, samples, 0);
}
