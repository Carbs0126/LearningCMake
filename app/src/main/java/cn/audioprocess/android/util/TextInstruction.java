package cn.audioprocess.android.util;

/**
 * Created by Rick.Wang on 2017/5/5.
 */

public class TextInstruction {

// 1. 改变声音的音色，采用了第三方的smbPitchShift
// 2. 使用cmake进行jni处理
    public static String getInstruction(){
        StringBuilder sb = new StringBuilder();
        sb.append("1. 改变声音的音色，采用了第三方的smbPitchShift；");
        sb.append("\n");
        sb.append("2. 使用cmake进行jni处理；");
        sb.append("\n");
        return sb.toString();
    }
}
