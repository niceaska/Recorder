package ru.niceaska.recorder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PlayingService extends Service {
    public PlayingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
