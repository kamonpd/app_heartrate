package com.example.heart_rate_app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;

import static android.provider.BaseColumns._ID;
import static com.example.heart_rate_app.Constancts.ACTIVITY;
import static com.example.heart_rate_app.Constancts.CONTENT;
import static com.example.heart_rate_app.Constancts.TABLE_NAME;
import static com.example.heart_rate_app.Constancts.TIME;
import static com.example.heart_rate_app.MainActivity.bpmaverage;

public class HistoryActivity extends AppCompatActivity {
    private TextView bpmavTxt,historyTxt;
    private EditText addEt;
    private Button saveBtn;
    private NoteHelper helper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);

        init();
        bpmavTxt.setText(bpmaverage);

        try{
            Cursor cursor = getActivityNotes();
            Cursor cursor2 = getNonActivityNotes();
            showNotes(cursor, cursor2);
        }finally{
            helper.close();
        }

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    addNote(String.valueOf(bpmaverage), addEt.getText().toString(), helper);
                    Cursor cursor = getActivityNotes();
                    Cursor cursor2 = getNonActivityNotes();
                    showNotes(cursor, cursor2);
                    addEt.setText(null);
                }finally{
                    helper.close();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void init(){
        bpmavTxt = findViewById(R.id.txtBpmAverage);
        historyTxt = findViewById(R.id.txtHistory);
        addEt = findViewById(R.id.edtSave);
        saveBtn = findViewById(R.id.btnSave);
        helper = new NoteHelper(this);
    }

    public static void addNote(String bpm, NoteHelper helper){
        addNote(bpm, "", helper);
    }

    public static void addNote(String bpm, String activity, NoteHelper helper){
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TIME, System.currentTimeMillis());
        values.put(ACTIVITY, activity);
        values.put(CONTENT, bpm);
        db.insertOrThrow(TABLE_NAME, null, values);
    }

    public static void clearNote(NoteHelper helper){
        SQLiteDatabase db = helper.getWritableDatabase();
        String whereClause = ACTIVITY + " = ''";
        db.delete(TABLE_NAME, whereClause, null);
    }

    private static String[] COLUMNS = {_ID, TIME, ACTIVITY, CONTENT};
    private static String ORDER_BY = TIME + " DESC";

    private Cursor getAllNotes(){
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, COLUMNS, null, null, null, null, ORDER_BY);
        return cursor;
    }

    private Cursor getActivityNotes(){
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = "NOT " + ACTIVITY + "= ''";
        Cursor cursor = db.query(TABLE_NAME, COLUMNS, selection, null, null, null, ORDER_BY);
        return cursor;
    }

    private Cursor getNonActivityNotes(){
        SQLiteDatabase db = helper.getReadableDatabase();
        String selection = ACTIVITY + "= ''";
        Cursor cursor = db.query(TABLE_NAME, COLUMNS, null, null, null, null, ORDER_BY);
        return cursor;
    }

    private void showNotes(Cursor activityNote, Cursor nonActivityNote){
        StringBuilder builder = new StringBuilder("ชีพจรที่บันทึกไว้:\n\n");
        while (activityNote.moveToNext()){
            long time = activityNote.getLong(1);
            String activityStatus = activityNote.getString(2);
            String content = activityNote.getString(3);
            String strDate = (String) DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date(time));
            builder.append(strDate).append("\n");
            builder.append("\t").append("กิจกรรมที่ทำ:" + activityStatus + " ชีพจรเฉลี่ย:" + content).append("\n\n");
        }
        while (nonActivityNote.moveToNext()){
            long time = nonActivityNote.getLong(1);
            String activityStatus = nonActivityNote.getString(2);
            String content = nonActivityNote.getString(3);
            String strDate = (String) DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date(time));
            builder.append(strDate).append("\n");
            builder.append("\t").append("ชีพจรเฉลี่ย:" + content).append("\n\n");
        }
        historyTxt.setText(builder);
    }

}
