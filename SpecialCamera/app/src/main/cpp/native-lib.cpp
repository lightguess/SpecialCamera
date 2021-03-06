#include  <jni.h>
#include <string>
#include <opencv2/imgproc/types_c.h>
#include "FaceTrack.h"


extern "C"
JNIEXPORT jlong JNICALL
Java_com_lg_specialcamera_face_FaceTrack_native_1create(JNIEnv *env, jobject thiz, jstring model,
                                                        jstring seeta) {
    // TODO: implement native_create()

    const char *myModel = env->GetStringUTFChars(model, 0);
    const char *MySeeta = env->GetStringUTFChars(seeta, 0);
    FaceTrack *faceTrack = new FaceTrack(myModel, MySeeta);
    env->ReleaseStringUTFChars(model, myModel);
    env->ReleaseStringUTFChars(seeta, MySeeta);
    return reinterpret_cast<jlong>(faceTrack);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lg_specialcamera_face_FaceTrack_native_1start(JNIEnv *env, jobject thiz, jlong self) {
    // TODO: implement native_start()
    if (self == 0) {
        return;
    }
    FaceTrack *me = (FaceTrack *) self;
    me->startTracking();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_lg_specialcamera_face_FaceTrack_native_1stop(JNIEnv *env, jobject thiz, jlong self) {
    // TODO: implement native_stop()
    if (self == 0) {
        return;
    }
    FaceTrack *me = (FaceTrack *) self;
    me->stopTracking();
    delete me;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_lg_specialcamera_face_FaceTrack_native_1detector(JNIEnv *env, jobject thiz, jlong self,
                                                          jbyteArray data, jint camera_id,
                                                          jint width, jint height) {
    // TODO: implement native_detector()
    if (self == 0) {
        return NULL;
    }

    jbyte *myData = env->GetByteArrayElements(data, NULL);
    FaceTrack *me = (FaceTrack *) self;
    Mat src(height + height / 2, width, CV_8UC1, myData);
    cvtColor(src, src, CV_YUV2RGBA_NV21);
    if (camera_id == 1) {
        //前置摄像头
        //逆时针90度
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        // y轴 翻转
        flip(src, src, 1);
    } else {
        //后置
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }

    //灰度化
    cvtColor(src, src, COLOR_RGBA2GRAY);
    //直方图均衡化 增强对比效果
    equalizeHist(src, src);

    vector<Rect2f> rects;

    imwrite("/sdcard/src.jpg", src);

    //送去定位
    me->detector(src, rects);
    env->ReleaseByteArrayElements(data, myData, 0);

    int imgWidth = src.cols;
    int imgHeight = src.rows;
    src.release();
    int ret = rects.size();

    if (ret) {
        jclass clazz = env->FindClass("com/lg/specialcamera/face/Face");
        jmethodID construct = env->GetMethodID(clazz, "<init>", "(IIII[F)V");
        int size = ret * 2;
        //创建java 的float 数组
        jfloatArray floatArray = env->NewFloatArray(size);
        for (int i = 0, j = 0; i < size; j++) {
            float f[2] = {rects[j].x, rects[j].y};
            env->SetFloatArrayRegion(floatArray, i, 2, f);
            i += 2;
        }
        Rect2f faceRect = rects[0];
        int faceWidth = faceRect.width;
        int faceHeight = faceRect.height;
        jobject face = env->NewObject(clazz, construct, faceWidth, faceHeight, imgWidth, imgHeight,
                                      floatArray);
        return face;
    }
    return NULL;

}