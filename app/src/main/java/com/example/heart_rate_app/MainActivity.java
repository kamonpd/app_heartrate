package com.example.heart_rate_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

    private TextView bpmtxt,ibitxt,statustxt;
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
    int bpmQueueCapacity = 20;
    int bpmTotal;
    static boolean Lock;//whether lock the x-axis to 0-5
    static boolean AutoScrollX;//auto scroll to the last x value
    static boolean Stream;//Start or stop streaming
    int old_interval=0;
    int new_interval=0;
    int mean_interval=20;

    Handler handler1 = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
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
                        String strIncom = new String(readBuf, 0, 5);                 // create string from bytes array
                        String bpm = new String(readBuf, 0, 80);
                        String ibi = new String(readBuf, 0, 80);
                        Log.d("strIncom", strIncom);

                        if (strIncom.indexOf('.') == 2 && strIncom.indexOf('s') == 0) {
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

                        int temp1, temp2;
                        temp1 = bpm.indexOf('b');
                        temp2 = bpm.indexOf(',');
                        if (temp1 >= 0 && temp2 >= temp1) {
                            bpm = bpm.substring(temp1 + 1, temp2);
                            TextView BPM = findViewById(R.id.txtBpm);
                            BPM.setText(bpm);
                            if (bpmQueue.size() >= bpmQueueCapacity) {
                                if (BluetoothActivity.ConnectedThread != null) {
                                    BluetoothActivity.ConnectedThread.write("S");
                                }
                                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                                startActivity(intent);
                                return;
                            }
                            bpmQueue.add(Integer.valueOf(bpm));
                            bpmTotal += Integer.valueOf(bpm);
                            HistoryActivity.addNote(bpm, helper);
                            bpmaverage = String.valueOf(bpmTotal / bpmQueue.size());

                            temp1 = ibi.indexOf('i');
                            temp2 = ibi.indexOf('e');
                            if (temp1 >= 0 && temp2 >= temp1) {
                                ibi = ibi.substring(temp1 + 1, temp2);
                                old_interval = new_interval;
                                new_interval = Integer.parseInt(ibi);
                                TextView IBI = findViewById(R.id.txtibi);
                                IBI.setText(ibi);
                            }
                        } else streamChecking = false;
                    }
                    break;
            }
        }
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

        btninit();


    }

    void btninit(){
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
                if(startbtn.isChecked()){
                    //connected?
                    if(true){
                        //write w to bluetooth
                    }
                }
                else {

                }
                break;
            case R.id.btnHistory:
                Intent intentHT = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(intentHT);
                break;
            case R.id.btnConnect:
                Intent intentBT = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(intentBT);
                break;
            case R.id.btnDisconnect:
                break;

        }
    }

    void ClearQueue(){
        bpmQueue.clear();
        bpmTotal = 0;
        HistoryActivity.clearNote(helper);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}


