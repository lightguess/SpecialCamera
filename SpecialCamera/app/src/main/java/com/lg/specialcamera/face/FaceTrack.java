package com.lg.specialcamera.face;

import android.graphics.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.lg.specialcamera.utils.CameraHelper;

public class FaceTrack {
    static {
        System.loadLibrary("native-lib");
    }

    private CameraHelper mCameraHelper;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private long self;
    private Face mFace;

    //1.getCamera facetrack

    public FaceTrack(String model , String seeta, CameraHelper cameraHelper) {
        mCameraHelper = cameraHelper;
        self = native_create(model, seeta);
        mHandlerThread = new HandlerThread("FaceTrack");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                synchronized (FaceTrack.this) {
                    //人脸检测定位 线程中检测
                    mFace = native_detector(self, (byte[]) msg.obj, mCameraHelper.getCameraId(), 800,
                            480);
                    if (mFace != null)
                        Log.e("FaceTrack", mFace.toString());
                }
            }
        };
    }

    public void startTrack() {
        native_start(self);
    }

    public void stopTrack() {
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
            native_stop(self);
            self = 0;
        }
    }


    public void detector(byte[] data) {
        mHandler.removeMessages(11);
        Message message = mHandler.obtainMessage(11);
        message.obj = data;
        mHandler.sendMessage(message);
    }

    public Face getFace() {
        return mFace;
    }



    //1.
    private  native  long native_create(String model,String seeta );

//    //2.
    private native void native_start(long self);

//    //3.
    private native void native_stop(long self);
//
//    //4.
    private native Face native_detector(long self, byte[] data, int cameraId, int width,
                                        int height);
}
