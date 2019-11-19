package ru.niceaska.recorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static ru.niceaska.recorder.PalyerConstants.COUNT_PERIOD;
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
import static ru.niceaska.recorder.RecorderConstants.CHANNEL_ID;
import static ru.niceaska.recorder.RecorderConstants.NOTIFICATION_ID;

public class PlayingService extends Service implements MediaPlayer.OnPreparedListener {


    private final String TAG = "mediaPlayer";

    private MediaPlayer mediaPlayer = null;
    private RemoteViews notificationLayout;
    private CountDownTimer countDownTimer;
    private FileProvider fileProvider = new FileProvider();
    private int currentIndex;
    private List<String> fileList;
    private int duration;
    private String currRecordName;

    private Messenger messenger = new Messenger(new InternalHandler());

    private Messenger playerActivityMessenger;

    class InternalHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_START:
                    playerActivityMessenger = msg.replyTo;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationLayout = new RemoteViews(getPackageName(), R.layout.playing_layout);
    }



    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals(PLAY_ACTION)) {
            int index = intent.getIntExtra(CURRENT_INDEX, 0);
            currentIndex = index == 0 && currentIndex != 0 ? currentIndex : index;
            List<String> pathnamesList = intent.getStringArrayListExtra(PATHNAMES_LIST);
            fileList = pathnamesList  == null ? fileList : pathnamesList;
            if (fileList != null) {
                play(fileList.get(currentIndex));
            }
            notificationLayout.setViewVisibility(R.id.play_record, INVISIBLE);
            notificationLayout.setViewVisibility(R.id.pause_playing, VISIBLE);
            createChannel();
            startForeground(NOTIFICATION_ID, createNotification(getFormat(duration)));
        } else if (intent.getAction() != null && intent.getAction().equals(PAUSE_ACTION)) {
            pausePlaying();
            notificationLayout.setViewVisibility(R.id.play_record, VISIBLE);
            notificationLayout.setViewVisibility(R.id.pause_playing, INVISIBLE);
            updateNotification(getFormat(duration));
        } else if (intent.getAction() != null && intent.getAction().equals(STOP_ACTION)) {
            stopPlaying();
            stopForeground(true);
            stopSelf();
        } else if (intent.getAction() != null && intent.getAction().equals(PREV_ACTION)) {
            stopPlaying();
            if (currentIndex > 0) {
                currentIndex--;
                play(fileList.get(currentIndex));
                notificationLayout.setViewVisibility(R.id.play_record, INVISIBLE);
                notificationLayout.setViewVisibility(R.id.pause_playing, VISIBLE);
            }
        } else if (intent.getAction() != null && intent.getAction().equals(NEXT_ACTION)) {
            stopPlaying();
            playNext();
        }
        return START_NOT_STICKY;
    }

    private void playNext() {
        if (fileList != null) {
            if (currentIndex < fileList.size() - 1) {
                currentIndex++;
                play(fileList.get(currentIndex));
                notificationLayout.setViewVisibility(R.id.play_record, INVISIBLE);
                notificationLayout.setViewVisibility(R.id.pause_playing, VISIBLE);
            }
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            duration = 0;
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void play(String fileName) {
        if (mediaPlayer != null) {

            if (mediaPlayer.isPlaying()) return;
            startMP(mediaPlayer, duration);
            return;
        }

        try (FileInputStream is = new FileInputStream(fileName);) {

            currRecordName = new File(fileName).getName();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(is.getFD());
        } catch (IOException e) {
            Log.d(TAG, "play: " + e.getMessage());
        }
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.prepareAsync();
    }

    private Notification createNotification(String time) {
        Intent activityIntent = new Intent(this, PlayingActivity.class);
        PendingIntent pendingActivityIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        Intent playingIntent = new Intent(this, PlayingService.class);
        playingIntent.setAction(PLAY_ACTION);
        PendingIntent pendingPlay = PendingIntent.getService(this, 0, playingIntent, 0);

        Intent stopIntent = new Intent(this, PlayingService.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingStop = PendingIntent.getService(this, 0, stopIntent, 0);

        Intent pauseIntent = new Intent(this, PlayingService.class);
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent pendingPause = PendingIntent.getService(this, 0, pauseIntent, 0);

        Intent pervIntent = new Intent(this, PlayingService.class);
        pervIntent.setAction(PREV_ACTION);
        PendingIntent pendingPerv = PendingIntent.getService(this, 0, pervIntent, 0);

        Intent nextIntent = new Intent(this, PlayingService.class);
        nextIntent.setAction(NEXT_ACTION);
        PendingIntent pendingNext = PendingIntent.getService(this, 0, nextIntent, 0);

        notificationLayout.setOnClickPendingIntent(R.id.play_record, pendingPlay);
        notificationLayout.setOnClickPendingIntent(R.id.stop_playing, pendingStop);
        notificationLayout.setOnClickPendingIntent(R.id.pause_playing, pendingPause);
        notificationLayout.setOnClickPendingIntent(R.id.perv_playing, pendingPerv);
        notificationLayout.setOnClickPendingIntent(R.id.next_playing, pendingNext);
        notificationLayout.setTextViewText(R.id.playing_caption, time);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
                .setCustomContentView(notificationLayout)
                .setContentIntent(pendingActivityIntent)
                .build();
    }

    private void createCountDownTimer(final long time) {
        countDownTimer = new CountDownTimer(time, COUNT_PERIOD) {
            @Override
            public void onTick(long millisUntilFinished) {
                duration = (int) millisUntilFinished;

                Bundle bundle = new Bundle();

                Message message = Message.obtain(null, MSG_UPDATE);
                message.arg1 = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
                String playingTime = getFormat((int) millisUntilFinished);
                bundle.putString(PLAYING_TIME, playingTime);
                message.setData(bundle);
                sendMessage(message);
                updateNotification(playingTime);
            }

            @Override
            public void onFinish() {
                Message messageFinish = Message.obtain(null, MSG_FINISH);
                sendMessage(messageFinish);
                stopPlaying();
                playNext();

            }
        };
        countDownTimer.start();
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

    private void pausePlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            countDownTimer.cancel();
        }
    }

    private void updateNotification(String time) {
        Notification notification = createNotification(time);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public String getFormat(int ms) {
        return String.format(Locale.ENGLISH, getResources().getString(R.string.format),
                ms / COUNT_PERIOD / 60, (ms / COUNT_PERIOD) % 60);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        duration = mp.getDuration();
        startMP(mp, duration);
        Log.d(TAG, "onPrepared: " + duration);

        Bundle bundle = new Bundle();
        Message message = Message.obtain(null, MSG_SET_START_VALUES);
        bundle.putString(PLAYING_NAME, currRecordName);

        message.setData(bundle);
        message.arg1 = duration;
        sendMessage(message);
        updateNotification(getFormat(duration));
    }

    private void startMP(MediaPlayer mp, int duration) {
        mp.start();
        createCountDownTimer(duration);
    }

    private void sendMessage(Message message) {
        if (playerActivityMessenger != null) {

            try {
                playerActivityMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
