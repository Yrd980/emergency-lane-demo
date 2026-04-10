#include <jni.h>
#include <android/asset_manager_jni.h>
#include "LaneDetector.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_emergencylaneguard_LaneDetector_init(JNIEnv* env, jobject, jobject assetManager) {
    LaneDetector* detector = new LaneDetector();
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!detector->initialize(mgr)) {
        delete detector;
        return 0;
    }
    return (jlong)detector;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_emergencylaneguard_LaneDetector_release(JNIEnv* env, jobject, jlong ptr) {
    if (ptr != 0) {
        delete (LaneDetector*)ptr;
    }
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_emergencylaneguard_LaneDetector_detect(JNIEnv* env, jobject, jlong ptr, jobject bitmap) {
    if (ptr == 0) return nullptr;
    LaneDetector* detector = (LaneDetector*)ptr;
    std::vector<DetectionResult> results = detector->detect(env, bitmap);
    
    // Convert to float array [x, y, w, h, score, cls]
    std::vector<float> flat;
    for (const auto& r : results) {
        flat.push_back(r.x);
        flat.push_back(r.y);
        flat.push_back(r.w);
        flat.push_back(r.h);
        flat.push_back(r.score);
        flat.push_back((float)r.cls);
    }
    
    jfloatArray ret = env->NewFloatArray(flat.size());
    env->SetFloatArrayRegion(ret, 0, flat.size(), flat.data());
    return ret;
}
