package com.example.heart_rate_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ToggleButton startbtn;
    private Button historybtn,connectbtn,disconnectbtn;
    public static String bpmaverage;
    static LinearLayout GraphView;
    static GraphView graphView;
    static GraphViewSeries Series;
    private static double graph2LastXValue = 0;
    private static int Xview=10;

    private NoteHelper helper;
    boolean streamChecking = false;
    Queue<Integer> bpmQueue = new LinkedList<Integer>();
    int bpmQueueCapacity = 20;          //ใช้เก็บค่า bpm 20 ค่า
    int bpmTotal;
    static boolean Lock;
    static boolean AutoScrollX;
    static boolean Stream;

    Handler handler1 = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            // หาสถานะ bluetooth
            switch (msg.what) {
                case BluetoothActivity.CONNECTING_STATUS:
                    if (msg.arg1 == 1) {
                        if (BluetoothActivity.ConnectedThread != null){
                            BluetoothActivity.ConnectedThread.write("C");
                        }
                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
                    } else {
                        if (BluetoothActivity.ConnectedThread != null){
                            BluetoothActivity.ConnectedThread.write("S");
                        }
                    }
                    break;
                case BluetoothActivity.MESSAGE_READ:
                    if (startbtn.isChecked()) {
                        if (!streamChecking) {
                            ClearQueue();
                            streamChecking = true;
                        }
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, 5);                      //เก็บข้อความจาก bluetooth
                        String bpm = new String(readBuf, 0, 80);                          //เก็บก่า bpm
                        String ibi = new String(readBuf, 0, 80);                          //เก็บค่า ibi
                        Log.d("strIncom", strIncom);

                        if (strIncom.indexOf('s') == 0 && strIncom.indexOf('.') == 2){                //หาข้อความรูปแบบ s600.45. มั้ง
                            strIncom = strIncom.replace("s", "");
                            if (isFloatNumber(strIncom)) {
                                Series.appendData(new GraphViewData(graph2LastXValue, Double.parseDouble(strIncom)), AutoScrollX);
                                System.out.println(strIncom);
                                //X-axis control
                                if (graph2LastXValue >= Xview && Lock == true) {
                                    Series.resetData(new GraphViewData[]{});
                                    graph2LastXValue = 0;
                                } else graph2LastXValue += 0.1;
                                if (Lock == true)graphView.setViewPort(0, Xview);
                                else graphView.setViewPort(graph2LastXValue - Xview, Xview);
                                GraphView.removeView(graphView);
                                GraphView.addView(graphView);
                            }
                        }

                        int temp1, temp2;                                                           //ใช้เก็บ index ของค่า bpm
                        temp1 = bpm.indexOf('b');
                        temp2 = bpm.indexOf(',');
                        if (temp1 >= 0 && temp2 >= temp1) {
                            bpm = bpm.substring(temp1 + 1, temp2);
                            TextView BPM = findViewById(R.id.txtBpm);
                            BPM.setText(bpm);
                            if (bpmQueue.size() >= bpmQueueCapacity) {                              //ถ้าเก็บค่า bpm ครบ 20 ค่า
                                if (BluetoothActivity.ConnectedThread != null) {
                                    BluetoothActivity.ConnectedThread.write("S");
                                }
                                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                                startActivity(intent);
                                return;
                            }
                            bpmQueue.add(Integer.valueOf(bpm));                                     //เก็บจำนวน bpm แต่ละครั้ง
                            bpmTotal += Integer.valueOf(bpm);                                       //เก็บผลรวมของ bpm
                            HistoryActivity.addNote(bpm, helper);                                   //เพิ่ม bpm ลง db
                            bpmaverage = String.valueOf(bpmTotal / bpmQueue.size());                //bpm average

                            temp1 = ibi.indexOf('i');                                               //ใช้เก็บค่า index ของ ibi
                            temp2 = ibi.indexOf('e');
                            if (temp1 >= 0 && temp2 >= temp1) {
                                ibi = ibi.substring(temp1 + 1, temp2);
                                TextView IBI = findViewById(R.id.txtibi);
                                IBI.setText(ibi);
                            }
                        } else streamChecking = false;
                    }
                    break;
            }
        }
        //ใช้เช็คว่าแปลง string เเป็น float ได้ไหม
        public boolean isFloatNumber (String num){
            //Log.d("checkfloatNum", num);
            try {
                Double.parseDouble(num);
            } catch (NumberFormatException nfe) {
                return false;
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        btninit();
    }

    private void init(){
        BluetoothActivity.gethandler(handler1);
        //init graphview
        GraphView = findViewById(R.id.linearLayout);
        Series = new GraphViewSeries("Signal",
                 new GraphViewStyle(Color.YELLOW, 2),
                 new GraphViewData[] {new GraphViewData(0, 0)});
        graphView = new LineGraphView(this , "Heart Rate");
        graphView.setViewPort(0, Xview);
        graphView.setScrollable(true);
        graphView.setScalable(true);
        graphView.setShowLegend(true);
        graphView.setLegendAlign(LegendAlign.BOTTOM);
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(10,0);
        graphView.addSeries(Series); // data
        GraphView.addView(graphView);
    }

    void btninit(){
        //ตั้งค่าปุ่ม
        startbtn = findViewById(R.id.btnStart);
        startbtn.setOnClickListener(this);
        historybtn = findViewById(R.id.btnHistory);
        historybtn.setOnClickListener(this);
        connectbtn = findViewById(R.id.btnConnect);
        connectbtn.setOnClickListener(this);
        disconnectbtn = findViewById(R.id.btnDisconnect);
        disconnectbtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btnStart:
                // startbtn  presss?
                if(startbtn.isChecked()){
                    //bluetooth connect?
                    if(BluetoothActivity.ConnectedThread != null){
                        BluetoothActivity.ConnectedThread.write("W");
                        ClearQueue();
                    }
                }
                else {
                    if(BluetoothActivity.ConnectedThread != null) BluetoothActivity.ConnectedThread.write("S");
                }
                break;
            case R.id.btnHistory:
                //open Historyactivity
                Intent intentHT = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intentHT);
                break;
            case R.id.btnConnect:
                //open BluetoothActivity
                Intent intentBT = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(intentBT);
                break;
            case R.id.btnDisconnect:
                if(BluetoothActivity.ConnectedThread != null){
                    BluetoothActivity.ConnectedThread.write("D");
                }
                BluetoothActivity.disconnect();
                break;
        }
    }

    void ClearQueue(){
        bpmQueue.clear();
        bpmTotal = 0;
        HistoryActivity.clearNote(helper);                                                          //ยังไม่ค่อยเข้าใจ .clearNote
    }

    @Override
    public void onBackPressed() {
        if (BluetoothActivity.ConnectedThread != null) {
            BluetoothActivity.ConnectedThread.write("S");
        }
        super.onBackPressed();
    }

}


