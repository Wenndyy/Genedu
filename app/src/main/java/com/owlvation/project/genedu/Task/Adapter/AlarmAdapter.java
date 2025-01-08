package com.owlvation.project.genedu.Task.Adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.owlvation.project.genedu.R;
import com.owlvation.project.genedu.Task.Model.AlarmDatabaseHelper;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private Context context;
    private Cursor cursor;
    private AlarmDatabaseHelper dbHelper;

    public AlarmAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
        this.dbHelper = new AlarmDatabaseHelper(context);
    }

    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.alarm_item, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AlarmViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        long id = cursor.getLong(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_ID));
        int hour = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_HOUR));
        int minute = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_MINUTE));
        int day = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_DAY));
        int month = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_MONTH));
        int year = cursor.getInt(cursor.getColumnIndexOrThrow(AlarmDatabaseHelper.COLUMN_YEAR));

        String timeText = String.format("%02d:%02d", hour, minute);
        String dateText = String.format("%02d/%02d/%d", day, month + 1, year);

        holder.timeView.setText(timeText);
        holder.dateView.setText(dateText);
        holder.itemView.setTag(id);

        holder.deleteButton.setOnClickListener(v -> {
            dbHelper.deleteAlarm(id);
            swapCursor(dbHelper.getAllAlarms());
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView timeView;
        TextView dateView;
        ImageButton deleteButton;

        public AlarmViewHolder(View itemView) {
            super(itemView);
            timeView = itemView.findViewById(R.id.text_time);
            dateView = itemView.findViewById(R.id.text_date);
            deleteButton = itemView.findViewById(R.id.button_delete);
        }
    }
}