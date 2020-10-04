package org.rocstreaming.roctoolkit;

class RocLibrary {
    private RocLibrary() {}

    static void loadLibrary() {
        System.loadLibrary("roc_jni");
    }
}
