package com.example.wojcieth.program;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class BluetoothMenu extends AppCompatActivity {
    BluetoothConnection BT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_menu);

        BT = (BluetoothConnection) getApplicationContext();
        BluetoothConnection.enableBluetooth(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BT.setContextAndRegisterReceivers(this);
        BT.setTextViews();
        BT.listPairedDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BT.unregisterBroadcastReceiver();
    }
}
