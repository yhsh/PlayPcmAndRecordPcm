package com.yhsh.recordpcm;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Zheng Cong
 * 一边录音一边播放
 * @date 2021/12/17 18:10
 */
public class PlayByteManagerUtil {
    private static final String TAG = "PlayManagerUtils";
    private WeakReference<Context> weakReference;
    private File recordFile;
    private boolean isRecording;
    /**
     * 是否播放,默认播放
     */
    private boolean isPlay = true;
    /**
     * 最多只能存2条记录
     */
    private final List<File> filePathList = new ArrayList<>(2);

    /**
     * 16K采集率
     */
//    int sampleRateInHz = 16000;
    int sampleRateInHz = 44100;
    /**
     * 格式
     */
    int channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
    /**
     * 16Bit
     */
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;


    private PlayByteManagerUtil() {
    }

    private static final PlayByteManagerUtil PLAY_MANAGER_UTILS = new PlayByteManagerUtil();

    public static PlayByteManagerUtil getInstance() {
        return PLAY_MANAGER_UTILS;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(
            3, 5,
            1, TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(10),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    public void startRecord() {
        Log.e(TAG, "开始录音");
        try {
            int bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO, audioEncoding, bufferSize);

//            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfiguration, audioEncoding);
            //下面的bufferSize必须得和读pcm流的带下一致，否则一边录音一边播放声音很刺耳，一句话形容也就是说读写的bufferSize必须保持一样大小
            AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfiguration, audioEncoding, bufferSize, AudioTrack.MODE_STREAM);
            //开始播放
            player.play();
            byte[] buffer = new byte[bufferSize];
            audioRecord.startRecording();
            Log.e(TAG, "开始录音");
            long startTime = System.currentTimeMillis();
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                player.write(buffer, 0, bufferReadResult);
            }
            audioRecord.stop();
            long endTime = System.currentTimeMillis();
            float recordTime = (endTime - startTime) / 1000f;
            Log.e(TAG, "录音总时长：" + recordTime + "s");
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
            showToast("录音失败");
        }
    }

    public void playPcm(boolean isChecked) {
        mExecutorService.execute(this::playBanZou);
    }

    public void playPcm(File file) {
        //两首一起播放
        mExecutorService.execute(() -> playPcmData(file));
    }

    /**
     * 播放Pcm流,边读取边播
     */
    private void playPcmData(File recordFiles) {
        Log.e(TAG, "线程名字" + Thread.currentThread().getName());
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordFiles)));
            //最小缓存区
            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfiguration, audioEncoding);
            AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfiguration, audioEncoding, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            byte[] data = new byte[bufferSizeInBytes];
            long startTime = System.currentTimeMillis();
            //开始播放
            player.play();
            while (true) {
                int i = 0;
                while (dis.available() > 0 && i < data.length) {
                    if (isPlay) {
                        data[i] = dis.readByte();
                        i++;
                    } else {
                        Log.e(TAG, "暂停播放：");
                    }
                }
                player.write(data, 0, data.length);
                //表示读取完了
                if (i != bufferSizeInBytes) {
                    player.stop();//停止播放
                    player.release();//释放资源
                    dis.close();
                    long endTime = System.currentTimeMillis();
                    float time = (endTime - startTime) / 1000f;
                    Log.e(TAG, "播放原声完成总计：" + time + "s");
                    showToast("播放原声完成总时长为：" + time + "s");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "播放异常: " + e.getMessage());
            showToast("播放异常: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 播放Pcm流,边读取边播
     */
    private void playBanZou() {
        try {
            InputStream inputStream = weakReference.get().getResources().openRawResource(R.raw.xiayiye5);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(inputStream));
            //最小缓存区
            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfiguration, audioEncoding);
            AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfiguration, audioEncoding, bufferSizeInBytes, AudioTrack.MODE_STREAM);
            byte[] data = new byte[bufferSizeInBytes];
            long startTime = System.currentTimeMillis();
            //开始播放
            player.play();
            while (true) {
                int i = 0;
                while (dis.available() > 0 && i < data.length) {
                    data[i] = dis.readByte();
                    i++;
                }
                player.write(data, 0, data.length);
                //表示读取完了
                if (i != bufferSizeInBytes) {
                    player.stop();//停止播放
                    player.release();//释放资源
                    dis.close();
                    long endTime = System.currentTimeMillis();
                    float time = (endTime - startTime) / 1000f;
                    Log.e(TAG, "播放伴奏完成总计：" + time + "s");
                    showToast("播放伴奏完成总时长为：" + time + "s");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "播放异常: " + e.getMessage());
            showToast("播放异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public File getRecordFile() {
        return recordFile;
    }

    public void setRecord(boolean isRecording) {
        this.isRecording = isRecording;
    }

    private void showToast(String msg) {
        handler.post(() -> Toast.makeText(weakReference.get(), msg, Toast.LENGTH_LONG).show());
    }

    /**
     * 设置播放状态
     *
     * @param isPlay 是否播放原声
     */
    public void setPlayStatus(boolean isPlay) {
        this.isPlay = isPlay;
    }

    public void setContext(WeakReference<Context> weakReference) {
        this.weakReference = weakReference;

    }
}


