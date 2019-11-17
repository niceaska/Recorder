package ru.niceaska.recorder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RecordsListAdapter extends RecyclerView.Adapter<RecordsListAdapter.RecordsListHolder> {

    private List<File> fileList;
    private OnFileClickedListener listener;

    public RecordsListAdapter(List<File> fileList, OnFileClickedListener listener) {
        this.fileList = hideFiles(fileList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordsListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.records_list_item, parent, false);
        return new RecordsListHolder(v, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordsListHolder holder, int position) {
        holder.bindView(fileList.get(position));
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void updateList(List<File> files) {
        fileList = hideFiles(files);
        notifyDataSetChanged();
    }

    private List<File> hideFiles(List<File> files) {
        List<File> visibleFiles = new ArrayList<>();
        for (File file : files) {
            if (file.getName().charAt(0) != '.') {
                visibleFiles.add(file);
            }
        }
        return visibleFiles;
    }

    static class RecordsListHolder extends RecyclerView.ViewHolder {

        private TextView recordName;
        private OnFileClickedListener listener;

        RecordsListHolder(@NonNull View itemView, OnFileClickedListener listener) {
            super(itemView);
            recordName  = itemView.findViewById(R.id.record_time);
            this.listener = listener;
        }

        void bindView(final File file) {
            recordName.setText(file.getName());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onFileCliced(file);
                }
            });
        }
    }
}
