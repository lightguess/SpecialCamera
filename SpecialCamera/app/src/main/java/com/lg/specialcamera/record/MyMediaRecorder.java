package com.lg.specialcamera.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MyMediaRecorder {

    private final int mHeight;
    private final int mWidth;
    private final String mOutputPath;
    private final EGLContext mEglContext;
    private final Context mContext;
    private MediaCodec mMediaCodec;
    private Surface mInputSurface;
    private MediaMuxer mMediaMuxer;
    private Handler mHandler;
    private  MyEGL mEGL;
    private boolean isStart;
    private int index;
    private float mSpeed;

    public MyMediaRecorder(int width, int height, String outputPath , EGLContext eglContext, Context context) {
        mWidth = width;
        mHeight = height;
        mOutputPath = outputPath;
        mEglContext = eglContext;
        mContext = context;

    }

    //1.start record
    public void start(float speed) throws IOException {
        mSpeed = speed;
        //1.create MediaCodec
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);

        //2.videoFormat
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,mWidth,mHeight);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,20);
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //3.create surface
        mInputSurface = mMediaCodec.createInputSurface();

        //4.create MediaMuxer
        mMediaMuxer = new MediaMuxer(mOutputPath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        //5.new EGL
        HandlerThread handlerThread = new HandlerThread("MyMediaRecorder");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEGL = new MyEGL(mEglContext, mInputSurface, mContext, mWidth, mHeight);
                mMediaCodec.start();
                isStart = true;
            }
        });

    }

        //2.stop record
        public void stop() {
            isStart = false;

            mHandler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    getEncodedData(true);
                    if (mMediaCodec != null){
                        mMediaCodec.stop();
                        mMediaCodec.release();
                        mMediaCodec = null;
                    }

                    if (mMediaMuxer != null){
                        try{
                            mMediaMuxer.stop();
                            mMediaMuxer.release();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        mMediaMuxer = null;
                    }

                    if (mInputSurface != null){
                        mInputSurface.release();
                        mInputSurface = null;
                    }
                    mEGL.release();
                    mEGL = null;
                    mHandler.getLooper().quitSafely();
                    mHandler = null;

                }
            });
        }

    //3.encodeFrame
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void encodeFrame(final int textureId, final long timestamp) {
        if (!isStart){
            return;
        }
        if (mHandler != null){
            mHandler.post(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void run() {
                    //把纹理图像画到了虚拟屏上
                    if (mEGL != null){
                        mEGL.draw(textureId, timestamp);
                    }
                    //然后从编码器中的输出缓冲区去获取编码后的数据
                    getEncodedData(false);
                }
            });
        }

    }

    //4.getEncodeData
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getEncodedData(boolean endOfStream) {
        if(endOfStream){
            mMediaCodec.signalEndOfInputStream();
        }
        //输出缓冲区
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (true){
            int status = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);//10ms
            if (status == MediaCodec.INFO_TRY_AGAIN_LATER){

                if (!endOfStream){
                    break;
                }
            }else if(status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                MediaFormat outputFormat = mMediaCodec.getOutputFormat();
                index = mMediaMuxer.addTrack(outputFormat);
                mMediaMuxer.start();//开始封装
            }else if (status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){

            }else {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(status);
                if (outputBuffer == null){
                    throw new RuntimeException("getOutputBuffer fail: " + status);
                }
                //如果取到 outputBuffer 是配置信息
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0){
                    bufferInfo.presentationTimeUs = (long) (bufferInfo.presentationTimeUs / mSpeed);
//                    bufferInfo.presentationTimeUs = (long) (bufferInfo.presentationTimeUs / mSpeed);
                    //偏移位置
                    outputBuffer.position(bufferInfo.offset);
                    //可读写的总长度
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    //写数据（输出）
                    try{
                        mMediaMuxer.writeSampleData(index, outputBuffer, bufferInfo);}
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                //一定要. 使用完输出缓冲区，就可以回收了，让mMediaCodec 能继续使用
                mMediaCodec.releaseOutputBuffer(status, false);

                //结束
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                    break;
                }
            }
        }

    }

}
