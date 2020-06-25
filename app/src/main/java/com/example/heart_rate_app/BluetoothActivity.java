package com.example.heart_rate_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    private TextView txtstatus;
    private Button onBtn,offBtn,scanBtn,piredBtn;
    private ListView list;
    private BluetoothAdapter BTAdapter;
    private Set<BluetoothDevice> PairedDevices;
    private ArrayAdapter<String> BTArrayAdapter;                                                    //ใช้เก็บรายชื่อบูลทูล

    public static void gethandler(Handler handler){                                                 //Bluetooth handler
        Handler = handler;
    }
    static Handler Handler = new Handler();
    private Handler Handler1;                                               // Our main handler that will receive callback notifications
    static ConnectedThread ConnectedThread;                                 // bluetooth background worker thread to send and receive data
    private BluetoothSocket BTSocket = null;                                // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 2;                         // used to identify adding bluetooth names
    protected static final int MESSAGE_READ = 1;                            // used in bluetooth handler to identify message update
    protected static final int CONNECTING_STATUS = 0;                       // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);

        init();
        // handler check status bluetooth
        Handler1 = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        txtstatus.setText("Connected to Device: " + (String)(msg.obj));
                    else txtstatus.setText("Connection Failed");
                }
            }
        };
        //มีรายชื่อบูลทูธ?
        if (BTArrayAdapter == null) {
            // Device does not support Bluetooth
            txtstatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            onBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            offBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            scanBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });

            piredBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });
        }
    }

    void init(){
        //ตั้งค่าปุ่มกด
        onBtn = findViewById(R.id.btnOn);
        offBtn = findViewById(R.id.btnOff);
        scanBtn = findViewById(R.id.btnScan);
        piredBtn = findViewById(R.id.btnPired);

        BTAdapter = BluetoothAdapter.getDefaultAdapter();                                           //get a handle on the bluetooth radio
        BTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        list = findViewById(R.id.listbluetooth);
        list.setAdapter(BTArrayAdapter);                                                            // assign model to view
        list.setOnItemClickListener(mDeviceClickListener);
    }

    private void bluetoothOn(View view){
        if (!BTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                              //เปิดหน้าต่าง yes/no
            txtstatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT){
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                txtstatus.setText("Enabled");
            } else txtstatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        BTAdapter.disable(); // turn off bt
        txtstatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(BTAdapter.isDiscovering()){
            BTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(BTAdapter.isEnabled()) {
                BTArrayAdapter.clear(); // clear items
                BTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                BTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        PairedDevices = BTAdapter.getBondedDevices();
        if(BTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : PairedDevices) {
                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            Toast.makeText(getApplicationContext(),"Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else Toast.makeText(getApplicationContext(),"Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            if(!BTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(),"Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }
            txtstatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);
            // Spawn a new thread to avoid blocking the GUI one
            new Thread(){
                public void run() {
                    boolean fail = false;
                    BluetoothDevice device = BTAdapter.getRemoteDevice(address);
                    try {
                        BTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        BTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            BTSocket.close();
                            Handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                            Handler1.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        ConnectedThread = new ConnectedThread(BTSocket);
                        ConnectedThread.start();
                        Handler.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                        Handler1.obtainMessage(CONNECTING_STATUS, 1, -1, name).sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;                                                                          // buffer store for the stream
            int bytes;                                                                              // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);                                                // Read from the InputStream
                    Handler.obtainMessage(MESSAGE_READ, bytes,-1, buffer).sendToTarget();     // Send the obtained bytes to the UI activity
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
                for(int i=0;i<input.getBytes().length;i++)
                    Log.v("outStream"+Integer.toString(i),Character.toString((char)(Integer.parseInt(Byte.toString(input.getBytes()[i])))));
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public static void disconnect(){
        if (ConnectedThread != null) {
            ConnectedThread.cancel();
            ConnectedThread = null;
        }
    }

}
