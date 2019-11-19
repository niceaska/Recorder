package ru.niceaska.recorder;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.niceaska.recorder.RecorderConstants.RECORDER_DIR;

public class FileProvider {

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public List<File> getFileList() {
        if (isExternalStorageReadable()) {
            File listFiles = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + RECORDER_DIR);
            if (listFiles.isDirectory()) {
                File[] files = listFiles.listFiles();
                return files == null ? new ArrayList<File>() : new ArrayList<File>(Arrays.asList(files));
            }
        }
        return new ArrayList<>();
    }

    public List<String> getFilePaths(List<File> fileList) {
        List<String> pathnames = new ArrayList<>();
        for (File file : fileList) {
            pathnames.add(file.getAbsolutePath());
        }
        return pathnames;
    }

}
