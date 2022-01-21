package com.yhsh.recordpcm;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author DELL
 */
public class KSongMainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "KSongMainActivity";
    private TextView tvAudioSuccess;
    private ScrollView mScrollView;
    private Button startAudio;
    private Button stopAudio;
    private Button playAudio;
    ThreadPoolExecutor mExecutorService = new ThreadPoolExecutor(
            3, 5,
            1, TimeUnit.MINUTES,
            new LinkedBlockingDeque<>(10),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
    private boolean playStatue = true;
    private Button stopVoice;
    private SongPathReceiver songPathReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ksong);
        PlayByteManagerUtils.getInstance().setContext(new WeakReference<>(getApplicationContext()));
        songPathReceiver = new SongPathReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.K_SONG_DEMO");
        registerReceiver(songPathReceiver, intentFilter);
        startAudio = findViewById(R.id.startAudio);
        startAudio.setOnClickListener(this);
        stopAudio = findViewById(R.id.stopAudio);
        stopAudio.setOnClickListener(this);
        playAudio = findViewById(R.id.playAudio);
        playAudio.setOnClickListener(this);
        stopVoice = findViewById(R.id.stopVoice);
        stopVoice.setOnClickListener(this);
        Button deleteAudio = findViewById(R.id.deleteAudio);
        deleteAudio.setOnClickListener(this);
        tvAudioSuccess = findViewById(R.id.tv_audio_succeess);
        mScrollView = findViewById(R.id.mScrollView);
        Button btSendAction = findViewById(R.id.bt_send_action);
        //模拟系统发送广播
        btSendAction.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("filepath", "/sd/song/彩虹的天堂.mp3");
            intent.setAction("android.intent.action.K_SONG_DEMO");
            sendBroadcast(intent);
        });
    }

    public class SongPathReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null != intent) {
                String songFilePath = intent.getStringExtra("filepath");
                Log.e(TAG, "打印歌曲伴奏路径:" + songFilePath);
                Toast.makeText(context.getApplicationContext(), "歌曲路径：" + songFilePath, Toast.LENGTH_SHORT).show();
                File file = new File(songFilePath);
                if (!file.exists()) {
                    Log.e(TAG, "打印歌曲伴奏路径不存在！");
                    Toast.makeText(context.getApplicationContext(), "打印歌曲伴奏路径不存在！" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    return;
                }
                PlayByteManagerUtils.getInstance().playPcm(file);
                //同步开启开始录音
                mExecutorService.execute(() -> {
                    PlayByteManagerUtils.getInstance().startRecord();
                });
                printLog("开始录音");
                buttonEnabled(false, true, false);
                Toast.makeText(context.getApplicationContext(), "打印歌曲伴奏路径已存在！" + file, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "打印歌曲伴奏路径已存在！");
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //全网搜 VR 引擎 用到GUX协议 仙豆广播
            case R.id.startAudio:
                mExecutorService.execute(() -> {
                    PlayByteManagerUtils.getInstance().startRecord();
                });
                printLog("开始录音");
                buttonEnabled(false, true, false);
                break;
            case R.id.stopAudio:
                PlayByteManagerUtils.getInstance().setRecord(false);
                buttonEnabled(true, false, true);
                printLog("停止录音");
                break;
            case R.id.playAudio:
                //两个一起播放
                PlayByteManagerUtils.getInstance().playPcm();
                buttonEnabled(true, false, false);
                printLog("播放录音");
                receiverServerData();
                break;
            case R.id.stopVoice:
                playStatue = !playStatue;
                Toast.makeText(this, playStatue ? "点击了播放" : "点击了暂停", Toast.LENGTH_LONG).show();
                stopVoice.setText(playStatue ? "暂停原声" : "继续原声");
                //暂停原声播放
                PlayByteManagerUtils.getInstance().setPlayStatus(playStatue);
                break;
            case R.id.deleteAudio:
                deleteFile();
                break;
            default:
                break;
        }
    }

    /**
     * 打印log
     *
     * @param resultString 返回数据
     */
    private void printLog(final String resultString) {
        tvAudioSuccess.post(() -> {
            tvAudioSuccess.append(resultString + "\n");
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    /**
     * 获取/失去焦点
     *
     * @param start 是否可点击
     * @param stop  是否可点击
     * @param play  是否可点击
     */
    private void buttonEnabled(boolean start, boolean stop, boolean play) {
        startAudio.setEnabled(start);
        stopAudio.setEnabled(stop);
        playAudio.setEnabled(play);
    }

    /**
     * 删除文件
     */
    private void deleteFile() {
        File recordFile = PlayByteManagerUtils.getInstance().getRecordFile();
        if (recordFile == null) {
            return;
        }
        recordFile.delete();
        printLog("文件删除成功");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != songPathReceiver) {
            //解绑广播
            unregisterReceiver(songPathReceiver);
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());

    private void receiverServerData() {
        System.setProperty("https.protocols", "TLSv1.2");
        mExecutorService.submit(() -> {
            try {
                String ip = "10.53.137.24";
                Socket socket = new Socket(ip, 9999);
                InetAddress inetAddress = socket.getInetAddress();
                String hostName = inetAddress.getHostName();
                String hostAddress = inetAddress.getHostAddress();
                Log.e("打印主机地址：", hostAddress + "--------主机名字：" + hostName);
                while (true) {
                    if (socket.isConnected()) {
                        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                        byte[] bys = new byte[1024];
                        int len;
                        while ((len = inputStream.read(bys)) != -1) {
                            String score = new String(bys, 0, len);
                            Log.e("客户端接收到的数据为：", "receiver：" + score);
                        }
                        inputStream.close();
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}