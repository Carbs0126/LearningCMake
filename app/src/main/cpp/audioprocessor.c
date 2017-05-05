#include <jni.h>
#include <stddef.h>
#include "smbPitchShift.h"

//size代表缓冲区的大小
JNIEXPORT void JNICALL
Java_cn_audioprocess_android_AudioProcessor_process(JNIEnv *env, jclass type, jfloat ratio,
                                                    jbyteArray in_, jbyteArray out_, jint size, jint sampleRate,
                                                    jfloatArray floatInputs_,
                                                    jfloatArray floatOutputs_) {
    jbyte *in = (*env)->GetByteArrayElements(env, in_, NULL);
    jbyte *out = (*env)->GetByteArrayElements(env, out_, NULL);
    jfloat *floatInputs = (*env)->GetFloatArrayElements(env, floatInputs_, NULL);
    jfloat *floatOutputs = (*env)->GetFloatArrayElements(env, floatOutputs_, NULL);

    // 把byte[]输入转换成float[]
    // 两个byte转换成一个float
    int i;
    for(i = 0; i < size; i += 2){
        int lo = in[i] & 0x000000FF;
        int hi = in[i + 1] & 0x000000FF;
        int frame = (hi << 8) + lo;
        //除法运算比位运算慢
        //一定要先强转为signed short，取后面两位
        floatInputs[i >> 1] = (signed short) frame;
    }

    // 调用smbPitchShift，进行声音处理
    smbPitchShift(ratio, 1024, 1024, 4, sampleRate, floatInputs, floatOutputs);
    // 把float[]的输出转换为byte[]
    for (i = 0; i < size; i += 2){
        int frame = (int) floatOutputs[i >> 1];
        out[i] = (jbyte)(frame & 0x000000FF);
        out[i + 1] = (jbyte) (frame >> 8);
    }


    (*env)->ReleaseByteArrayElements(env, in_, in, 0);
    (*env)->ReleaseByteArrayElements(env, out_, out, 0);
    (*env)->ReleaseFloatArrayElements(env, floatInputs_, floatInputs, 0);
    (*env)->ReleaseFloatArrayElements(env, floatOutputs_, floatOutputs, 0);
}
















