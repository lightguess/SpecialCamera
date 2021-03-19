# SpecialCamera
    基于OpenGL+OpenCV开发的一款美颜相机，拥有美肤、大眼、贴纸滤镜特效，使用的是Seetaface人脸识别引擎。

1.FaceTrack封装JNI进行人脸识别

    private native long native_create(String model, String seeta);

    private native void native_start(long self);

    private native void native_stop(long self);

    private native Face native_detector(long self, byte[] data, int cameraId, int width,int height);

2.Filter和项目的raw里的GLSL顶点着色器进行滤镜绘制。

3.MyGLSurfaceView和MyGLRenderer 进行绘制和渲染到Activity上。

