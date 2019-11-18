package ru.niceaska.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.niceaska.recorder.RecorderConstants.RECORDER_DIR;
import static ru.niceaska.recorder.RecorderConstants.RECORD_ACTION;
import static ru.niceaska.recorder.RecorderConstants.STOP_ACTION;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_WRITE_CODE =  2;
    private static final int PERMISSION_READ_CODE = 4;
    private static final int PERMISSION_RECORD_CODE = 6;
    private static final String TAG = "activityMain";
    private static final String PATHNAMES_LIST = "pathnames";
    private final String CURRENT_INDEX = "CurrentIndex";

    private boolean permissionToRecordAccepted = false;
    private FileProvider fileProvider;
    private boolean isServiceBound;
    private RecorderService recorderService;
    private RecordsListAdapter recordsListAdapter;
    private RecyclerView recyclerView;
    private TextView recordButton;
    private TextView stopButton;

    private  ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            recorderService = ((RecorderService.LocalBinder) service).getService();
            isServiceBound = true;
            if (recorderService != null) {
                recorderService.setListener(new RecorderService.OnRecordingListener() {
                    @Override
                    public void onRecording(String time) {
                        stopButton.setText(time);
                    }
                });
                recorderService.setOnStopRecordingListener(new RecorderService.OnStopRecordingListener() {
                    @Override
                    public void onRecordingStop() {
                        refreshUIonStop();
                    }
                });
            }
            Log.d(TAG, "onServiceConnected: ");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            recorderService = null;

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_WRITE_CODE);
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_READ_CODE);
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_RECORD_CODE);

        }
        fileProvider = new FileProvider();
        recyclerView = findViewById(R.id.record_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recordsListAdapter = new RecordsListAdapter(fileProvider.getFileList(), new OnFileClickedListener() {
            @Override
            public void onFileCliced(int index) {
                Intent intent = new Intent(MainActivity.this, PlayingActivity.class);
                intent.putExtra(CURRENT_INDEX, index);
                intent.putStringArrayListExtra(PATHNAMES_LIST,
                        (ArrayList<String>) fileProvider.getFilePaths(recordsListAdapter.getFileList()));
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(recordsListAdapter);
        recordButton = findViewById(R.id.new_record);
        stopButton = findViewById(R.id.stop_record);
        setListeners();

    }

    private void setListeners() {
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RecorderService.class);
                intent.setAction(RECORD_ACTION);
                startService(intent);
                if (!isServiceBound) {
                    bindService(intent, serviceConnection, BIND_NOT_FOREGROUND);
                    Log.d(TAG, "onClick: service bound");
                }
                if (recorderService != null) {
                    recorderService.setListener(new RecorderService.OnRecordingListener() {
                        @Override
                        public void onRecording(String time) {
                            stopButton.setText(time);
                        }
                    });
                }
                recordButton.setEnabled(false);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecordService();
                refreshUIonStop();
            }
        });


    }

    private void refreshUIonStop() {
        recordsListAdapter.updateList(fileProvider.getFileList());
        recordButton.setEnabled(true);
        stopButton.setText("");
    }

    private void stopRecordService() {
        Intent intent = new Intent(MainActivity.this, RecorderService.class);
        intent.setAction(STOP_ACTION);
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
            recorderService = null;
        }
        startService(intent);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
            recorderService = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            stopRecordService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_READ_CODE:
            case PERMISSION_RECORD_CODE:
            case PERMISSION_WRITE_CODE:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (!permissionToRecordAccepted ) finish();
                break;
        }

    }

}
