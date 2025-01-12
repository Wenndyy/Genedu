package com.owlvation.project.genedu.Task.Model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AlarmDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "AlarmDB";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ALARMS = "alarms";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_HOUR = "hour";
    public static final String COLUMN_MINUTE = "minute";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_ENABLED = "enabled";

    private static final String CREATE_TABLE_ALARMS =
            "CREATE TABLE " + TABLE_ALARMS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_HOUR + " INTEGER, " +
                    COLUMN_MINUTE + " INTEGER, " +
                    COLUMN_DAY + " INTEGER, " +
                    COLUMN_MONTH + " INTEGER, " +
                    COLUMN_YEAR + " INTEGER, " +
                    COLUMN_ENABLED + " INTEGER DEFAULT 1);";

    public AlarmDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ALARMS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
        onCreate(db);
    }

    public long addAlarm(int hour, int minute, int day, int month, int year) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_HOUR, hour);
        values.put(COLUMN_MINUTE, minute);
        values.put(COLUMN_DAY, day);
        values.put(COLUMN_MONTH, month);
        values.put(COLUMN_YEAR, year);
        long id = db.insert(TABLE_ALARMS, null, values);
        Log.d("AlarmDatabaseHelper", "Alarm added with ID: " + id);
        return id;
    }

    public Cursor getAllAlarms() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ALARMS, null, null, null, null, null, null);
    }

    public void deleteAlarm(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_ALARMS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rowsDeleted > 0) {
            Log.d("AlarmDatabaseHelper", "Alarm with ID " + id + " deleted.");
        } else {
            Log.e("AlarmDatabaseHelper", "No alarm found with ID " + id);
        }
    }



    public Cursor getAlarmById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(id)};

        return db.query(TABLE_ALARMS, null, selection, selectionArgs, null, null, null);
    }

}