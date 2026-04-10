#pragma once

#include "net.h"
#include <android/asset_manager.h>
#include <vector>
#include <jni.h>
#include <memory>

struct DetectionResult {
    float x, y, w, h;
    float score;
    int cls;
};

class LaneDetector {
public:
    std::unique_ptr<ncnn::Net> yolo;
    bool initialized;

    LaneDetector();
    ~LaneDetector();
    bool initialize(AAssetManager* mgr);
    std::vector<DetectionResult> detect(JNIEnv* env, jobject bitmap);
};
