#include "common.h"

#include <limits.h>

int get_boolean_field_value(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, char* error) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, "Z");
    assert(attrId != NULL);
    return env->GetBooleanField(obj, attrId) == JNI_TRUE;
}

int get_int_field_value(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, char* error) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, "I");
    assert(attrId != NULL);
    jint ret = env->GetIntField(obj, attrId);
    if (ret < INT_MIN || ret > INT_MAX) {
        *error = 1;
        return 0;
    }
    return (int) ret;
}

unsigned int get_uint_field_value(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, char* error) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, "I");
    assert(attrId != NULL);
    jint ret = env->GetIntField(obj, attrId);
    if (ret < 0 || ret > UINT_MAX) {
        *error = 1;
        return 0;
    }
    return (unsigned int) ret;
}

long long get_llong_field_value(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, char* error) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, "J");
    assert(attrId != NULL);
    jlong ret = env->GetLongField(obj, attrId);
    return (long long) ret; // always safe (sizeof(long long) == sizeof(jlong))
}
unsigned long long get_ullong_field_value(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, char* error) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, "J");
    assert(attrId != NULL);
    jlong ret = env->GetLongField(obj, attrId);
    if (ret < 0) {
        *error = 1;
        return 0LL;
    }
    return (unsigned long long) ret;
}

int get_enum_value(JNIEnv *env, jclass clazz, jobject enumObj) {
    if (enumObj != NULL) {
        jfieldID attrId = env->GetFieldID(clazz, "value", "I");
        return env->GetIntField(enumObj, attrId);
    }
    return 0;
}

jobject get_object_field(JNIEnv *env, jclass clazz, jobject obj, const char* attr_name, const char* attr_class_name) {
    jfieldID attrId = env->GetFieldID(clazz, attr_name, attr_class_name);
    assert(attrId != NULL);
    return env->GetObjectField(obj, attrId);
}
