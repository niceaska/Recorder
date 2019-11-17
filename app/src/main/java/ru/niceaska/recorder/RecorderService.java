package ru.niceaska.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static ru.niceaska.recorder.RecorderConstants.CHANNEL_ID;
import static ru.niceaska.recorder.RecorderConstants.NOTIFICATION_ID;
import static ru.niceaska.recorder.RecorderConstants.PAUSE_ACTION;
import static ru.niceaska.recorder.RecorderConstants.RECORDER_DIR;
import static ru.niceaska.recorder.RecorderConstants.RECORD_ACTION;
import static ru.niceaska.recorder.RecorderConstants.STOP_ACTION;


public class RecorderService extends Service {


    private static final String TAG =  "RecordService";
    private static final long COUNT_PERIOD = 1000;
    private RemoteViews notificationLayout;

    private MediaRecorder recorder;
    private File recorderDir;
    private File currentFile;
    private RecorderService.LocalBinder  localBinder = new LocalBinder();

    private CountDownTimer countDownTimer;
    private OnStopRecordingListener listener;
    private long currentPastedTime = 0;

    class LocalBinder extends Binder {
        RecorderService getService() {
            return RecorderService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        createRecorderDirectory();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction() != null && intent.getAction().equals(RECORD_ACTION)) {
                startRecording();
                notificationLayout.setViewVisibility(R.id.record, INVISIBLE);
                notificationLayout.setViewVisibility(R.id.pause, VISIBLE);
                createChannel();
                startForeground(NOTIFICATION_ID, createNotification(getFormat()));
            } else if (intent.getAction() != null && intent.getAction().equals(STOP_ACTION)) {


                stopRecording();
                boolean isRenamed = renameFile();
                if (!isRenamed) {
                    Log.d(TAG, "onStartCommand: can't rename file");
                }

                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            } else if (intent.getAction() != null && intent.getAction().equals(PAUSE_ACTION)) {
                pauseRecording();
                notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
                notificationLayout.setViewVisibility(R.id.record, VISIBLE);
                notificationLayout.setViewVisibility(R.id.pause, INVISIBLE);
                updateNotification(getFormat());
            }
        }


        return START_NOT_STICKY;
    }

    private Notification createNotification(String time) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingActivityIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        Intent recordIntent = new Intent(this, RecorderService.class);
        recordIntent.setAction(RECORD_ACTION);
        PendingIntent pendingRecord = PendingIntent.getService(this, 0, recordIntent, 0);

        Intent stopIntent = new Intent(this, RecorderService.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingStop = PendingIntent.getService(this, 0, stopIntent, 0);

        Intent pauseIntent = new Intent(this, RecorderService.class);
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent pendingPause = PendingIntent.getService(this, 0, pauseIntent, 0);

        notificationLayout.setOnClickPendingIntent(R.id.record, pendingRecord);
        notificationLayout.setOnClickPendingIntent(R.id.stop, pendingStop);
        notificationLayout.setOnClickPendingIntent(R.id.pause, pendingPause);
        notificationLayout.setTextViewText(R.id.recording_caption, time);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fiber_manual_record_black_24dp)
                .setContentTitle("Audio Recorder")
                .setContentText("Идет запись...")
                .setCustomContentView(notificationLayout)
                .setContentIntent(pendingActivityIntent)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("description");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    private void createRecorderDirectory() {
        if (isExternalStorageReadable()) {
            File dir = Environment.getExternalStorageDirectory();
            recorderDir = new File(dir.getAbsolutePath() + RECORDER_DIR);
            boolean isDone = recorderDir.mkdir();
            if (!isDone) {
            }
        }
    }

    private boolean renameFile() {
        File newFile = new File(
                currentFile.getAbsolutePath().replace(RECORDER_DIR + ".", RECORDER_DIR)
        );
        Log.d(TAG, "renameFile: " + newFile.getAbsolutePath());
        return currentFile.renameTo(newFile);
    }

    private File createFile() {
        if (isExternalStorageReadable()) {
            StringBuilder stringBuilder = new StringBuilder();
            Date c = Calendar.getInstance().getTime();
            SimpleDateFormat df = new SimpleDateFormat("yyyy.dd.MM HH:mm:ssZ", Locale.ENGLISH);
            currentFile = new File(
                    new String(stringBuilder
                            .append(recorderDir.getAbsolutePath())
                            .append("/")
                            .append(".")
                            .append(df.format(c))
                            .append(".aac"))
            );
            return currentFile;
        }
        return null;
    }


    private void startRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            if (recorder != null) {
                recorder.resume();
                createCountDownTimer();
                return;
            }
            File outputFile = createFile();
            if (outputFile == null) {
                Toast.makeText(this, getResources()
                        .getString(R.string.error_output_file), Toast.LENGTH_LONG).show();
                return;
            }
            prepareMediaRecorder(outputFile);
            beginRecording();
        }
    }

    private void prepareMediaRecorder(File outputFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            recorder.setOutputFile(outputFile);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        }
    }

    private void beginRecording() {
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("Error", "prepare() failed");
        }
        createCountDownTimer();
        recorder.start();
    }

    private void stopRecording() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            currentPastedTime = 0;
            countDownTimer = null;
        }
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }

    }

    private void pauseRecording() {
        if (recorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder.pause();
            }
        }
        countDownTimer.cancel();
    }

    private void updateNotification(String time) {
        Notification notification = createNotification(time);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }


    private void createCountDownTimer() {
        countDownTimer = new CountDownTimer(10000000, COUNT_PERIOD) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentPastedTime += COUNT_PERIOD;
                updateNotification(getFormat());
                if (listener != null) {
                    listener.onRecording(getFormat());
                }
            }

            @Override
            public void onFinish() {

            }
        };
        countDownTimer.start();
    }

    public String getFormat() {
        return String.format(Locale.ENGLISH, "%02d:%02d",
                currentPastedTime / COUNT_PERIOD / 60, (currentPastedTime / COUNT_PERIOD) % 60);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    public void setListener(OnStopRecordingListener listener) {
        this.listener = listener;
    }

    public interface OnStopRecordingListener {
        void onRecording(String time);
    }
}
