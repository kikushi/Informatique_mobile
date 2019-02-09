package proj.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button searchBtn;
    private Button connectPairedBtn;
    private Button connectOtherBtn;
    private Spinner pairedSpinner;
    private Spinner otherSpinner;
    private TextView connexionSuccessTextView;
    private TextView pairedDevicesTextView;
    private TextView otherDevicesTextView;
    private EditText editText;
    private Button sendBtn;

    public Set<BluetoothDevice> pairedDevices;
    public Set<BluetoothDevice> otherDevices;
    public ArrayAdapter<String> pairedAdapter = null;
    public ArrayAdapter<String> otherAdapter = null;
    public BluetoothSocket btSocket;
    public BluetoothAdapter myBTAdapter;
    public IntentFilter filter;

    private View.OnClickListener searchBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SearchBtnClicked();
        }
    };

    private View.OnClickListener connectBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean success = false;
            if (v.getId() == R.id.button_connectPaired) {
                success = ConnectBtnClicked(true);
            }
            else if (v.getId() == R.id.button_connectOther) {
                success = ConnectBtnClicked(false);
            }
            if (success) {
                connexionSuccessTextView.setText(getString(R.string.connexion_success) + " with " + btSocket.getRemoteDevice().getName());
                unregisterReceiver(mReceiver);
                editText.setVisibility(View.VISIBLE);
                sendBtn.setVisibility(View.VISIBLE);
            }
        }
    };

    private View.OnClickListener sendBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SendBtnClicked();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pairedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        otherAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

        //Bluetooth
        myBTAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new HashSet<BluetoothDevice>();
        otherDevices = new HashSet<BluetoothDevice>();


        SearchPairedDevices();

        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        //Widgets
        searchBtn = (Button)findViewById(R.id.button_search);
        searchBtn.setOnClickListener(searchBtnListener);

        pairedSpinner = (Spinner)findViewById(R.id.spinner_paired);
        pairedSpinner.setAdapter(pairedAdapter);

        otherSpinner = (Spinner)findViewById(R.id.spinner_other);
        otherSpinner.setAdapter(otherAdapter);
        otherSpinner.setVisibility(View.INVISIBLE);

        connectPairedBtn = (Button)findViewById(R.id.button_connectPaired);
        connectPairedBtn.setOnClickListener(connectBtnListener);

        connectOtherBtn = (Button)findViewById(R.id.button_connectOther);
        connectOtherBtn.setOnClickListener(connectBtnListener);
        connectOtherBtn.setVisibility(View.INVISIBLE);

        editText = (EditText)findViewById(R.id.editText);
        editText.setVisibility(View.INVISIBLE);

        sendBtn = (Button)findViewById(R.id.button_send);
        sendBtn.setOnClickListener(sendBtnListener);
        sendBtn.setVisibility(View.INVISIBLE);

        connexionSuccessTextView = (TextView)findViewById(R.id.textView_connexionSuccess);
        pairedDevicesTextView = (TextView)findViewById(R.id.textView_pairedDevices);
        otherDevicesTextView = (TextView)findViewById(R.id.textView_otherDevices);

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i("TAG", device.getName() + "  " + device.getAddress());
                otherDevices.add(device);
                if (!Contains(otherAdapter, device.getName())) {
                    otherAdapter.add(device.getName());
                }
            }
        }
    };

    private void SearchPairedDevices() {
        if (myBTAdapter == null) {
            // Device doesn't support Bluetooth
            // Message a l'utilisateur pour lui dire d'activer BT
        }
        else {
            pairedDevices = myBTAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                pairedAdapter.clear();
                for (BluetoothDevice device : pairedDevices) {
                    pairedAdapter.add(device.getName());
                }
            }
        }
    }

    private boolean Contains(ArrayAdapter<String> theAdapter, String element) {
        for (int i = 0; i < theAdapter.getCount(); i++) {
            if (theAdapter.getItem(i).equals(element)) {
                return true;
            }
        }
        return false;
    }

    private void SearchBtnClicked() {
        if (myBTAdapter.isDiscovering()) {
            myBTAdapter.cancelDiscovery();
        }
        myBTAdapter.startDiscovery();
        otherSpinner.setVisibility(View.VISIBLE);
        connectOtherBtn.setVisibility(View.VISIBLE);
    }

    private boolean ConnectBtnClicked(boolean boundedPressed) {
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {
            btSocket = GetSelectedSpinnerItem(boundedPressed).createRfcommSocketToServiceRecord(uuid);
            if (!btSocket.isConnected()) {
                btSocket.connect();
                return true;
            }
            else {
                return false;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void SendBtnClicked() {
        try {
            OutputStream btOutputStream = btSocket.getOutputStream();
            btOutputStream.write(editText.getText().toString().getBytes());
            //btSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BluetoothDevice GetSelectedSpinnerItem(boolean bounded) {
        if (bounded) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().equals(pairedSpinner.getSelectedItem().toString())) {
                    return device;
                }
            }
        }
        else {
            for (BluetoothDevice device : otherDevices) {
                if(device.getName().equals(otherSpinner.getSelectedItem().toString())) {
                    return device;
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

}
