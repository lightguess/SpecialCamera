// 把顶点坐标给这个变量， 确定要画画的形状
attribute vec4 vPosition;
//接收纹理坐标，接收采样器采样图片的坐标
attribute vec2 vCoord;

//传给片元着色器 像素点
varying vec2 aCoord;

void main() {
    //内置变量 gl_Position ,顶点数据赋值 opengl根据gl_Position进行制作形状
    gl_Position = vPosition;
    aCoord = vCoord;
}