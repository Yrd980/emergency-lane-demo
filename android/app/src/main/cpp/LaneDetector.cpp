#include "LaneDetector.h"
#include <android/log.h>
#include <android/bitmap.h>
#include <android/asset_manager_jni.h>
#include <cmath>

#define LOG_TAG "LaneDetector"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Sigmoid function
static float sigmoid(float x) {
    return 1.f / (1.f + expf(-x));
}

LaneDetector::LaneDetector() : initialized(false) {
}

LaneDetector::~LaneDetector() {
}

bool LaneDetector::initialize(AAssetManager* mgr) {
    if (initialized) {
        return true;
    }
    
    yolo = std::make_unique<ncnn::Net>();
    
    yolo->opt.use_vulkan_compute = false;
    yolo->opt.num_threads = 2;  // Use 2 threads for better performance
    yolo->opt.use_fp16_packed = false;
    yolo->opt.use_fp16_storage = false;
    yolo->opt.use_int8_storage = false;

    int ret_param = yolo->load_param(mgr, "yolov8n.param");
    int ret_bin = yolo->load_model(mgr, "yolov8n.bin");

    if (ret_param != 0 || ret_bin != 0) {
        LOGE("Failed to load YOLOv8 model: param=%d, bin=%d", ret_param, ret_bin);
        yolo.reset();
        return false;
    }

    initialized = true;
    LOGD("LaneDetector initialized successfully with YOLOv8n");
    return true;
}

std::vector<DetectionResult> LaneDetector::detect(JNIEnv* env, jobject bitmap) {
    std::vector<DetectionResult> results;
    
    if (!initialized || !yolo) {
        LOGE("LaneDetector not initialized");
        return results;
    }

    AndroidBitmapInfo info;
    void* pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != 0) {
        LOGE("Failed to get bitmap info");
        return results;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != 0) {
        LOGE("Failed to lock pixels");
        return results;
    }

    int width = info.width;
    int height = info.height;

    // Create NCNN Mat from bitmap
    ncnn::Mat in = ncnn::Mat::from_pixels_resize(
        (const unsigned char*)pixels,
        ncnn::Mat::PIXEL_RGBA2RGB,
        width, height,
        640, 640
    );

    AndroidBitmap_unlockPixels(env, bitmap);

    // Normalize with standard ImageNet values
    const float mean_vals[3] = {0.f, 0.f, 0.f};
    const float norm_vals[3] = {1/255.f, 1/255.f, 1/255.f};
    in.substract_mean_normalize(mean_vals, norm_vals);

    // Run inference
    ncnn::Extractor ex = yolo->create_extractor();
    ex.set_light_mode(true);
    ex.input("in0", in);

    ncnn::Mat out;
    ex.extract("out0", out);

    if (out.empty()) {
        LOGE("YOLOv8 output is empty");
        return results;
    }

    // YOLOv8 output format: [84, 8400] stored as (w=8400, h=84)
    // out.w = 8400 (number of predictions)
    // out.h = 84 (4 bbox + 80 classes)
    int num_predictions = out.w;
    int num_features = out.h;

    LOGD("Output shape: w=%d, h=%d", out.w, out.h);

    // Iterate through each prediction
    for (int i = 0; i < num_predictions; i++) {
        const float* prediction = (const float*)out.data + i;

        // Find max class score (skip first 4 values which are bbox coords)
        float max_score = 0.f;
        int max_class = -1;
        for (int c = 4; c < num_features; c++) {
            float score = sigmoid(prediction[c * num_predictions]);  // Apply sigmoid
            if (score > max_score) {
                max_score = score;
                max_class = c - 4;
            }
        }

        // Filter by confidence threshold (lowered to 0.25 to catch more candidates, filtering happens in Kotlin)
        if (max_score > 0.25f && max_class >= 0) {
            DetectionResult r;
            
            // YOLOv8 outputs center_x, center_y, width, height (already normalized by model)
            float cx = prediction[0 * num_predictions];
            float cy = prediction[1 * num_predictions];
            float w = prediction[2 * num_predictions];
            float h = prediction[3 * num_predictions];

            // Scale to original image size
            r.x = (cx - w / 2) * width / 640;
            r.y = (cy - h / 2) * height / 640;
            r.w = w * width / 640;
            r.h = h * height / 640;
            r.score = max_score;
            r.cls = max_class;
            
            // Log vehicle classes specifically
            if (max_class == 2 || max_class == 7) {
                LOGD("  VEHICLE: cls=%d, score=%.3f, cx=%.1f, cy=%.1f, w=%.1f, h=%.1f", 
                     max_class, max_score, cx, cy, w, h);
                results.push_back(r);
            }
        }
    }

    LOGD("Detected %zu objects", results.size());
    return results;
}
