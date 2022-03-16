#include <jni.h>
#include <string>
#include <vector>

#include <android/bitmap.h>
#include <android/log.h>

#define LOG_TAG "C/C++ Logcat"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

typedef struct Pixel_
{
    uint32_t r, g, b;    // red, green, blue
} Pixel;


#define ALPHA(pixel) ((pixel >> 24) & 0xFF)
#define BLUE(pixel) ((pixel >> 16) & 0xFF)
#define GREEN(pixel) ((pixel >> 8) & 0xFF)
#define RED(pixel) (pixel & 0xFF)
#define ABGR(a,b,g,r) (((a & 0xFF) << 24) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | ((r & 0xFF)))

#define RESET_PROCESSING 1
#define GRAY_PROCESSING 2
#define INVERSE_PROCESSING 3

using namespace std;

static uint32_t width;
static uint32_t height;

static vector<Pixel> pixels_origins;

void createNewBitmapPixels(uint32_t **bitmap_org, uint32_t **_bitmap_new) {
    uint32_t *line;
    LOGI("create new bitmap start");
    for (int i = 0; i < height; i++) {
        line = _bitmap_new[i];
        for (int j = 0; j < width; j++) {
            line[j] = bitmap_org[i][j];
        }
    }
    LOGI("create new bitmap success");
}


void extractedPixels(uint32_t **pixels) {
    uint32_t resolution = width * height;
    uint32_t *line, cur = 0;
    auto *p = static_cast<Pixel *>(calloc(resolution, sizeof(Pixel)));

    pixels_origins.clear();

    LOGI("extracted pixels start");
    for (int i = 0; i < height; i++) {
        line = *(pixels + i);
        for (int j = 0; j < width; j++) {
            uint32_t px = line[j];
            p[cur].r = RED(px);
            p[cur].g = GREEN(px);
            p[cur].b = BLUE(px);
            pixels_origins.push_back(p[cur]);
            cur++;
        }
    }
    LOGI("extracted pixels success");
    free(p);
}

void doImageProcess(vector<Pixel> pixels_origin, void *_pixels, uint32_t type) {
    uint32_t *pixels = (uint32_t *) _pixels;
    uint32_t *line, cur = 0, gray;

    LOGI("do process start");
    for (int i = 0; i < height; i++) {
        line = pixels;
        for (int j = 0; j < width; j++) {
            Pixel px = pixels_origin.at(cur);
            switch (type) {
                case RESET_PROCESSING:
                    line[j] = ABGR(255, px.b, px.g, px.r);
                    break;
                case GRAY_PROCESSING:
                    gray = 0.3 * px.r + 0.59 * px.g + 0.11 * px.b;
                    line[j] = ABGR(255, gray, gray, gray);
                    break;
                case INVERSE_PROCESSING:
                    line[j] = ABGR(255, 255 - px.b, 255 - px.g, 255 - px.r);
                    break;

            }
            cur++;

        }
        pixels += width;
    }
    LOGI("do process success");
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_aswlinume_nativebitmapdemo_BitmapProcessingUtils_nativeInit(JNIEnv *env, jclass clazz,
                                                                     jobject bitmap) {
    uint32_t *line;
    auto *info = static_cast<AndroidBitmapInfo *>(malloc(sizeof(AndroidBitmapInfo)));
    AndroidBitmap_getInfo(env, bitmap, info);

    width = info->width;
    height = info->height;

    void *_pixels;

    AndroidBitmap_lockPixels(env, bitmap, &_pixels);

    uint32_t **bitmap_org = (uint32_t **) malloc(sizeof(uint64_t) * height);
    for (int i = 0; i < height; i++) {
        bitmap_org[i] = (uint32_t *) malloc(sizeof(uint32_t) * width);
    }

    uint32_t* pixels = (uint32_t*) _pixels;
    for (int i = 0; i < height; i++) {
        line = pixels;
        for (int j = 0; j < width; j++) {
            bitmap_org[i][j] = line[j];
        }
        pixels += width;
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    free(info);

    extractedPixels(bitmap_org);

    return (jlong) bitmap_org;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_aswlinume_nativebitmapdemo_BitmapProcessingUtils_nativeTransImage
(JNIEnv *env, jclass clz,
jlong ptr, jobject bitmap, jint type, jobject handler) {

    uint32_t **bitmap_new = (uint32_t **) malloc(sizeof(uint64_t) * height);
    for (int i = 0; i < height; i++) {
        bitmap_new[i] = (uint32_t *) malloc(sizeof(uint32_t) * width);
    }
    createNewBitmapPixels((uint32_t **) ptr, bitmap_new);

    void *_pixels;

    AndroidBitmap_lockPixels(env, bitmap, &_pixels);

    doImageProcess(pixels_origins, _pixels, type);

    jmethodID mHandleID = env->GetMethodID(env->GetObjectClass(handler), "sendMessage",
                                           "(Landroid/os/Message;)Z");
    jclass cMessage = env->FindClass("android/os/Message");
    jmethodID mMessageId = env->GetStaticMethodID(cMessage, "obtain", "()Landroid/os/Message;");
    jfieldID fMessageID = env->GetFieldID(cMessage, "obj", "Ljava/lang/Object;");
    jobject oMessage = env->CallStaticObjectMethod(cMessage, mMessageId);
    env->SetObjectField(oMessage, fMessageID, bitmap);
    env->CallBooleanMethod(handler,mHandleID, oMessage);

    AndroidBitmap_unlockPixels(env, bitmap);
    for (int i = 0; i < height; i++) {
        free(bitmap_new[i]);
    }
    free(bitmap_new);
}

