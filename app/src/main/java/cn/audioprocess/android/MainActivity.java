package cn.audioprocess.android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.audioprocess.android.util.DateUtil;
import cn.audioprocess.android.util.TextInstruction;

public class MainActivity extends AppCompatActivity {

    public static final String RECORDER_DIR
            = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record/";

    private static final int BUFFER_SIZE = 2048;
    private byte[] mBuffer;

    private TextView seek_bar_value;
    private TextView text_view_instruction;
    private Button button_play;
    private SeekBar seek_bar;
    private AudioProcessor mAudioProcessor;
    private float mRatio = 1;
    private FileOutputStream fileOutputStream;

    //录音状态，volatile保证多线程内存同步，避免出问题
    private volatile boolean isRecording;

    private Button start_record_button;
    private Button stop_record_button;
    private TextView text_view_audio_file;

    private ExecutorService executorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBuffer = new byte[BUFFER_SIZE];
        mAudioProcessor = new AudioProcessor(BUFFER_SIZE);
        executorService = Executors.newSingleThreadExecutor();

        text_view_instruction = (TextView) findViewById(R.id.text_view_instruction);
        text_view_instruction.setText(TextInstruction.getInstruction());
        text_view_audio_file = (TextView) findViewById(R.id.text_view_audio_file);
        start_record_button = (Button) findViewById(R.id.start_record_button);
        stop_record_button = (Button) findViewById(R.id.stop_record_button);
        start_record_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (!mPermissionGrantedGT22){
                        toastForPermissionDeny();
                        return;
                    }
                }

                isRecording = true;
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        startRecord();
                    }
                });
            }
        });
        stop_record_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                isRecording = false;
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        stopRecorder();
                    }
                });
            }
        });

        seek_bar_value = (TextView) findViewById(R.id.seek_bar_value);

        button_play = (Button) findViewById(R.id.button_play);
        button_play.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                if (absoluteFileName != null && !isPlaying){
                    isPlaying = true;

                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            playAudioFile(mRatio);
                        }
                    });
                }
            }
        });
        seek_bar = (SeekBar) findViewById(R.id.seek_bar);
        seek_bar_value.setText(String.valueOf((float)seek_bar.getProgress() / 100));
        seek_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRatio = (float) progress / 100;
                seek_bar_value.setText(String.valueOf(mRatio));
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        requestRecorderPermission();
    }

    private void toastForPermissionDeny(){
        Toast.makeText(getApplicationContext(), "请在\"设置\"中为此应用授予录音权限", Toast.LENGTH_LONG).show();
    }

    private static final int PERMISSION_REQUEST_CODE_RECORD_AUDIO = 99;
    private void requestRecorderPermission(){
        if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP_MR1){
            getWindow().getDecorView().post(new Runnable() {
                @Override
                public void run() {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                                PERMISSION_REQUEST_CODE_RECORD_AUDIO);
                    }else{
                        mPermissionGrantedGT22 = true;
                    }
                }
            });
        }
    }

    private volatile boolean isPlaying;
    private void playAudioFile(float ratio){
        Log.d("wang","playAudioFile() absoluteFileName : " + absoluteFileName + " ratio : " + ratio);
        //扬声器
        int streamType = AudioManager.STREAM_MUSIC;
        //录音时采用的采样频率，所以播放的时候使用同样的采样频率
        int sampleRate = 44100;
        //mono表示单声道，录音输入单声道，播放用输出单声道
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        //录音时使用16bit，所以播放时使用同样的格式
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //流模式
        int mode = AudioTrack.MODE_STREAM;
        //buffer大小
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        //audiotrack
        AudioTrack audioTrack = new AudioTrack(streamType, sampleRate, channelConfig, audioFormat,
                Math.max(minBufferSize, BUFFER_SIZE), mode);
        //从文件流中读取
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(absoluteFileName));
            int read;
            audioTrack.play();
            //只要没读完，就循环写播放
            long x = 0;
            while((read = inputStream.read(mBuffer)) > 0){
                x++;
//                int ret = audioTrack.write(mBuffer, 0, read);
                int ret = audioTrack.write(ratio == 1 ? mBuffer : mAudioProcessor.process(ratio, mBuffer, 44100), 0, read);
                switch (ret){
                    case AudioTrack.ERROR_INVALID_OPERATION:
                    case AudioTrack.ERROR_BAD_VALUE:
                    case AudioManager.ERROR_DEAD_OBJECT:
                        playFail();
                        return;
                    default:
                        break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (inputStream!=null){
                try {
                    inputStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
                inputStream = null;
            }
        }
        try {
            audioTrack.stop();
            audioTrack.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        isPlaying = false;
    }

    private void playFail(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),"播放失败",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean mPermissionGrantedGT22 = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE_RECORD_AUDIO) {
            if (grantResults !=null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                mPermissionGrantedGT22 = true;
            } else {
                // Permission Denied
                mPermissionGrantedGT22 = false;
            }
        }
    }

    private AudioRecord audioRecord;
    private String absoluteFileName;
    public void startRecord() {
        if (!new File(RECORDER_DIR).exists()){
            new File(RECORDER_DIR).mkdirs();
        }

        absoluteFileName = RECORDER_DIR + generateSimpleFileName();
        File f = new File(absoluteFileName);
        if(!f.exists()) {
            try {
                f.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
                return;
            }
        }

        try {

            fileOutputStream = new FileOutputStream(f);

            int audioSource = MediaRecorder.AudioSource.MIC;
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat,
                    Math.max(minBufferSize, BUFFER_SIZE));
            audioRecord.startRecording();
            showFilePath();

            isRecording = true;

            while (isRecording) {
                int read = audioRecord.read(mBuffer, 0, BUFFER_SIZE);
                if (read > 0) {
                    fileOutputStream.write(mBuffer, 0, read);
                } else {

                }
            }
            stopRecorder();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (audioRecord != null){
                audioRecord.release();
            }
        }
    }

    private void showFilePath(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text_view_audio_file.setText(absoluteFileName);
            }
        });
    }

    private void stopRecorder(){
        if(audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        try{
            fileOutputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "文件存储于：" + absoluteFileName,Toast.LENGTH_SHORT).show();
            }
        });


        /*mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;*/
    }

    public static String generateSimpleFileName(){
        Date date = new Date();
        return "recorder_" + DateUtil.dateTime2String(date) + ".pcm";
    }

}




















