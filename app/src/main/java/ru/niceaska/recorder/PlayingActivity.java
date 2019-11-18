package ru.niceaska.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static ru.niceaska.recorder.PalyerConstants.CURRENT_INDEX;
import static ru.niceaska.recorder.PalyerConstants.NEXT_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PATHNAMES_LIST;
import static ru.niceaska.recorder.PalyerConstants.PAUSE_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PLAY_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PREV_ACTION;
import static ru.niceaska.recorder.PalyerConstants.STOP_ACTION;

public class PlayingActivity extends AppCompatActivity implements View.OnClickListener {

    private List<String> fileList;
    private int currentIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing);

        if (savedInstanceState == null) {
            currentIndex = getIntent().getIntExtra(CURRENT_INDEX, 0);
            fileList = getIntent().getStringArrayListExtra(PATHNAMES_LIST);

            Intent serviceIntent = new Intent(PlayingActivity.this, PlayingService.class);
            serviceIntent.setAction(PLAY_ACTION);
            serviceIntent.putStringArrayListExtra(PATHNAMES_LIST, (ArrayList<String>) fileList);
            serviceIntent.putExtra(CURRENT_INDEX, currentIndex);
            startService(serviceIntent);
        }
        findViewById(R.id.stop_button).setOnClickListener(this);
        findViewById(R.id.pause_button).setOnClickListener(this);
        findViewById(R.id.play_button).setOnClickListener(this);
        findViewById(R.id.skip_next).setOnClickListener(this);
        findViewById(R.id.skip_perv).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent serviceIntent = new Intent(PlayingActivity.this, PlayingService.class);
        switch (v.getId()) {
            case R.id.stop_button:
                serviceIntent.setAction(STOP_ACTION);
                startService(serviceIntent);
                break;
            case R.id.pause_button:
                serviceIntent.setAction(PAUSE_ACTION);
                startService(serviceIntent);
                break;
            case R.id.play_button:
                serviceIntent.setAction(PLAY_ACTION);
                serviceIntent.putStringArrayListExtra(PATHNAMES_LIST, (ArrayList<String>) fileList);
                serviceIntent.putExtra(CURRENT_INDEX, currentIndex);
                startService(serviceIntent);
                break;
            case R.id.skip_next:
                serviceIntent.setAction(NEXT_ACTION);
                startService(serviceIntent);
                break;
            case R.id.skip_perv:
                serviceIntent.setAction(PREV_ACTION);
                startService(serviceIntent);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_INDEX, currentIndex);
        outState.putStringArrayList(PATHNAMES_LIST, (ArrayList<String>) fileList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentIndex = savedInstanceState.getInt(CURRENT_INDEX);
        fileList = savedInstanceState.getStringArrayList(PATHNAMES_LIST);
    }
}
