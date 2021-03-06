package com.yhsh.recordpcm;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
public class Pcm16MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
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
    private boolean isChecked;
    private boolean playStatue = true;
    private Button stopVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.K_SONG_DEMO");
        startAudio = findViewById(R.id.startAudio);
        startAudio.setOnClickListener(this);
        stopAudio = findViewById(R.id.stopAudio);
        stopAudio.setOnClickListener(this);
        CheckBox cbTogetherPlay = findViewById(R.id.cb_together_play);
        cbTogetherPlay.setOnCheckedChangeListener((buttonView, isChecked) -> this.isChecked = isChecked);
        playAudio = findViewById(R.id.playAudio);
        playAudio.setOnClickListener(this);
        stopVoice = findViewById(R.id.stopVoice);
        stopVoice.setOnClickListener(this);
        Button deleteAudio = findViewById(R.id.deleteAudio);
        deleteAudio.setOnClickListener(this);
        tvAudioSuccess = findViewById(R.id.tv_audio_succeess);
        mScrollView = findViewById(R.id.mScrollView);
        Button btSendAction = findViewById(R.id.bt_send_action);
        //????????????????????????
        btSendAction.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("filepath", "/sd/song/???????????????.mp3");
            intent.setAction("android.intent.action.K_SONG_DEMO");
            sendBroadcast(intent);
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //????????? VR ?????? ??????GUX?????? ????????????
            case R.id.startAudio:
                mExecutorService.execute(() -> {
                    PlayManagerUtils.getInstance().startRecord(new WeakReference<>(getApplicationContext()));
                });
                printLog("????????????");
                buttonEnabled(false, true, false);
                break;
            case R.id.stopAudio:
                PlayManagerUtils.getInstance().setRecord(false);
                buttonEnabled(true, false, true);
                printLog("????????????");
                break;
            case R.id.playAudio:
                //??????????????????
                PlayManagerUtils.getInstance().playPcm(isChecked);
                buttonEnabled(true, false, false);
                printLog("????????????");
                receiverServerData();
                break;
            case R.id.stopVoice:
                playStatue = !playStatue;
                Toast.makeText(this, playStatue ? "???????????????" : "???????????????", Toast.LENGTH_LONG).show();
                stopVoice.setText(playStatue ? "????????????" : "????????????");
                //??????????????????
                PlayManagerUtils.getInstance().setPlayStatus(playStatue);
                break;
            case R.id.deleteAudio:
                deleteFile();
                break;
            default:
                break;
        }
    }

    /**
     * ??????log
     *
     * @param resultString ????????????
     */
    private void printLog(final String resultString) {
        tvAudioSuccess.post(new Runnable() {
            @Override
            public void run() {
                tvAudioSuccess.append(resultString + "\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * ??????/????????????
     *
     * @param start ???????????????
     * @param stop  ???????????????
     * @param play  ???????????????
     */
    private void buttonEnabled(boolean start, boolean stop, boolean play) {
        startAudio.setEnabled(start);
        stopAudio.setEnabled(stop);
        playAudio.setEnabled(play);
    }

    /**
     * ????????????
     */
    private void deleteFile() {
        File recordFile = PlayManagerUtils.getInstance().getRecordFile();
        if (recordFile == null) {
            return;
        }
        recordFile.delete();
        printLog("??????????????????");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                Log.e("?????????????????????", hostAddress + "--------???????????????" + hostName);
                while (true) {
                    if (socket.isConnected()) {
                        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                        byte[] bys = new byte[1024];
                        int len;
                        while ((len = inputStream.read(bys)) != -1) {
                            String score = new String(bys, 0, len);
                            Log.e("?????????????????????????????????", "receiver???" + score);
                        }
                        inputStream.close();

//                        String score = inputStream.readUTF();
//                        Log.e("?????????????????????????????????", "receiver???" + score);
//                        handler.post(() -> Toast.makeText(getApplicationContext(), "?????????????????????????????????" + score, Toast.LENGTH_SHORT).show());
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