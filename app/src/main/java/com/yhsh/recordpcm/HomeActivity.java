package com.yhsh.recordpcm;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @author Zheng Cong
 * @date 2022/1/19 15:32
 */
public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
    }


    public void kSongWriteRead(View view) {
        startActivity(new Intent(this, KSongWriteReadMainActivity.class));
    }

    public void kSong(View view) {
        startActivity(new Intent(this, KSongMainActivity.class));
    }

    public void recordSong(View view) {
        startActivity(new Intent(this, Pcm16MainActivity.class));
    }
}
