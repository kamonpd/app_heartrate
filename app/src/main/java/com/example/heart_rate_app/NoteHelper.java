package com.example.heart_rate_app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import static com.example.heart_rate_app.Constancts.ACTIVITY;
import static com.example.heart_rate_app.Constancts.CONTENT;
import static com.example.heart_rate_app.Constancts.TABLE_NAME;
import static com.example.heart_rate_app.Constancts.TIME;

public class NoteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "history_bpm.db";
    private static final int DATABASE_VERSION = 1;

    public NoteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TIME + " INTEGER, "
                + ACTIVITY + " TEXT, "
                + CONTENT + " TEXT NOT NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
