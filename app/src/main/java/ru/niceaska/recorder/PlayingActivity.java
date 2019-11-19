package ru.niceaska.recorder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import static ru.niceaska.recorder.PalyerConstants.CURRENT_INDEX;
import static ru.niceaska.recorder.PalyerConstants.MSG_FINISH;
import static ru.niceaska.recorder.PalyerConstants.MSG_SET_START_VALUES;
import static ru.niceaska.recorder.PalyerConstants.MSG_START;
import static ru.niceaska.recorder.PalyerConstants.MSG_UPDATE;
import static ru.niceaska.recorder.PalyerConstants.NEXT_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PATHNAMES_LIST;
import static ru.niceaska.recorder.PalyerConstants.PAUSE_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PLAYING_NAME;
import static ru.niceaska.recorder.PalyerConstants.PLAYING_TIME;
import static ru.niceaska.recorder.PalyerConstants.PLAY_ACTION;
import static ru.niceaska.recorder.PalyerConstants.PREV_ACTION;
import static ru.niceaska.recorder.PalyerConstants.STOP_ACTION;

public class PlayingActivity extends AppCompatActivity implements View.OnClickListener {

    private List<String> fileList;
    private int currentIndex;
    private int maxProgress;
    private String name;
    private String time;

    private Messenger serviceMessenger;
    private Messenger playingActivityMessenger = new Messenger(new InternalPlayActivityHandler());
    private ProgressBar progressBar;
    private TextView playingTime;
    private TextView recordName;

    private boolean isServiceBound;
    private static final String MAX_PROGRESS = "maxProgress";


    class InternalPlayActivityHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_SET_START_VALUES:
                    maxProgress = msg.arg1;
                    name = msg.getData().getString(PLAYING_NAME);
                    progressBar.setMax(maxProgress);
                    recordName.setText(name);
                    break;
                case MSG_UPDATE:
                    int currentProgress = msg.arg1;
                    time = msg.getData().getString(PLAYING_TIME);
                    playingTime.setText(time);
                    progressBar.setProgress(currentProgress);
                    break;
                case MSG_FINISH:
                    progressBar.setProgress(maxProgress);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            serviceMessenger = new Messenger(iBinder);
            isServiceBound = true;
            Message message = Message.obtain(null, MSG_START);
            message.replyTo = playingActivityMessenger;

            try {
                serviceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBound = false;
        }
    };

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
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
        progressBar = findViewById(R.id.play_progress);
        playingTime = findViewById(R.id.playing_time);
        recordName = findViewById(R.id.player_rec_name);
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
    protected void onStart() {
        super.onStart();
        if (!isServiceBound) {
            Intent serviceIntent = new Intent(PlayingActivity.this, PlayingService.class);
            bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
            isServiceBound = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(PLAYING_TIME, time);
        outState.putString(PLAYING_NAME, name);
        outState.putInt(MAX_PROGRESS, maxProgress);
        outState.putInt(CURRENT_INDEX, currentIndex);
        outState.putStringArrayList(PATHNAMES_LIST, (ArrayList<String>) fileList);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentIndex = savedInstanceState.getInt(CURRENT_INDEX);
        fileList = savedInstanceState.getStringArrayList(PATHNAMES_LIST);
        maxProgress = savedInstanceState.getInt(MAX_PROGRESS);
        time = savedInstanceState.getString(PLAYING_TIME);
        name = savedInstanceState.getString(PLAYING_NAME);
        restoreState();
    }

    private void restoreState() {
        progressBar.setMax(maxProgress);
        recordName.setText(name);
    }
}
