package cn.audioprocess.android;

/**
 * Created by Rick.Wang on 2017/5/4.
 */

//参考课程 http://www.imooc.com/learn/778
public class AudioProcessor {

    private final int mBufferSize;//缓冲区大小
    private final byte[] mOutBuffer;//输出缓冲区，需要获取这个变量中的数据
    private final float[] mFloatInput;//临时缓冲区
    private final float[] mFloatOutput;

    public AudioProcessor(int bufferSize){
        mBufferSize = bufferSize;
        mOutBuffer = new byte[mBufferSize];
        //底层实现中，两个byte对应一个float
        mFloatInput = new float[mBufferSize/2];
        mFloatOutput = new float[mBufferSize/2];
    }

    // synchronized 关键字，防止多线程调用导致native层闪退
    public synchronized byte[] process(float ratio, byte[] input, int sampleRate){
        process(ratio, input, mOutBuffer, mBufferSize, sampleRate, mFloatInput, mFloatOutput);
        return mOutBuffer;
    }



    //1. 声明native函数时，可以按alt+enter键，自动为其生成对应的c函数
    //2. 生成的c函数在src/main/jni中，对应的文件名和此类的类名相同，但是此时process函数仍然不能够被找到
    //3. 将jni文件夹中的文件，移到cpp文件夹下，此时部分类需要导入，如NULL找不到定义，因此需要按照as给出的提示，
    //   使用 #include <stddef.h> 导入对应的头文件
    //4. 点击gradle sync

    /**
     *
     * @param ratio         比率控制
     * @param in            输入数据
     * @param out           输出数据
     * @param size          数据长度
     * @param floatInputs   临时输入
     * @param floatOutputs  临时输出
     */
    private static native void process(float ratio, byte[] in, byte[] out,
                                       int size, int sampleRate, float[] floatInputs,
                                       float[] floatOutputs);

    static {
        // 加载so库，libaudio-processor.so
        System.loadLibrary("audio-processor");
//        System.loadLibrary("audio-processor-b");
    }

    //其它说明
    //生成的so文件在 app/build/intermediates/cmake目录下
    //1. 由于smbPitchShift.c适配的是ios平台，因此部分导入的包需要重新改写
    // #import <Accelerate/Accelerate.h>
    /**
     如果没有下载 NDK 和 CMake，需要在Android Studio中打开：Tools -> Android -> SDK Manager

     由于要下载ndk和cmake，因此在 SDK Manager 中点击Android SDK -> SDK Tools
     勾选 CMake LLDB NDK，进行下载







     smbPitchShift FALSE 没有定义

     NSLog(@"init static arrays");

     //去掉了如下函数
     double smbAtan2(double x, double y)

     void printFFTInitSnapshot(long fftFrameSize2,long stepSize,double freqPerBin,double expct,
     long inFifoLatency, long gRover);

     void printFFTSnapshot(long i, long k, long qpd, long index,
     double magn, double phase, double tmp,
     double window, double real, double imag,
     long gRover);

     void smb2PitchShift(float pitchShift, long numSampsToProcess, long fftFrameSize, long osamp,
     float sampleRate, float *indata, float *outdata,
     FFTSetup fftSetup, float *frequency)
     */



}
