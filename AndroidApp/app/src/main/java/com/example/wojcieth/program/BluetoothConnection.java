package com.example.wojcieth.program;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

/**
 * clase que maneja la conexión bluetooth
 */
public class BluetoothConnection extends Application implements AdapterView.OnItemClickListener {
    /** contexto */
    private Context mContext;

    /** contexto principal */
    private Context mContextMain;

    /** actividad con contexto */
    private Activity mActivity;

    /** adaptador bluetooth */
    private BluetoothAdapter mBluetoothAdapter = null;

    /** puerto bluetooth */
    private BluetoothSocket mBluetoothSocket = null;

    /** flujo de salida */
    private OutputStream mOutputStream = null;

    /** lista que mantiene los dispositivos bluetooth que se han buscado */
    public ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<>();

    /** lista de dispositivos emparejados */
    public ArrayList<BluetoothDevice> mBluetoothPairedDevices;

    /** lista de adaptadores bluetooth */
    public DeviceListAdapter mDeviceListAdapter;

    /** lista de adaptadores bluetooth de dispositivos emparejados */
    public DeviceListAdapter mDevicePairedListAdapter;

    /** vista de lista de dispositivos encontrados */
    private ListView lvNewDevices;

    /** vista de lista de dispositivos emparejados */
    private ListView lvPairedDevices;

    /** identificador necesario para crear una conexión */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /** dispositivo bluetooth utilizado */
    private BluetoothDevice mmDevice;

    /** conexión de manejo de enlace */
    private ConnectionThread mConnectionThread;

    /** enlace que sostiene la conexión */
    private ConnectedThread mConnectedThread = null;

    /** manipulador para la clase manipulador de mensaje entrante */
    Handler bluetoothIn = null;

    /** identificación del manipulador */
    final int handlerState = 0;

    /** cuadro de diálogo en espera */
    private ProgressDialog mProgressDialog;

    /** información sobre si se van a recopilar datos */
    private boolean mCollectDataState;

    /** estado de recopilador datos - verdadero cuando se modifica */
    private boolean mCollectDataStateOnChange;

    /** estado de conexión */
    private boolean mConnectionStatus;

    /** el nombre del ejercicio */
    private String mExerciseName;


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("BluetoothConnection", "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i("BluetoothConnection", "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i("BluetoothConnection", "mBroadcastReceiver1: STATE ON");
                        discoverDevices();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i("BluetoothConnection", "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Discoverability Enabled");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Connected");
                        break;
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBluetoothDevices.add(device);
                Log.i("BluetoothConnection", "mBroadcastReceiver3:" + device.getName() + ": " + device.getAddress());
                if (lvNewDevices != null) {
                    mDeviceListAdapter = new DeviceListAdapter(mContext, R.layout.device_adapter_view, mBluetoothDevices);
                    lvNewDevices.setAdapter(mDeviceListAdapter);
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice != null)
                {
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_BONDED");
                        showMessage(mContext.getString(R.string.bonded));
                    }
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_BONDING");
                        showMessage(mContext.getString(R.string.bonding));
                    }
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_NONE");
                        showMessage(mContext.getString(R.string.bond_none));
                    }
                }
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if(action != null)
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                {
                    if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
                    {
                        Log.i("BluetoothConnection", "Connected");
                        mConnectionStatus = true;
                        showMessage(mContext.getString(R.string.connected));
                    }
                    if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
                    {
                        Log.i("BluetoothConnection", "Disconnected");
                        mConnectionStatus = false;
                        showMessage(mContext.getString(R.string.disconnected));
                    }
                }
            }
        }
    };


    public BluetoothConnection()
    {
        mCollectDataState = false;
        mConnectionStatus = false;
    }


    public void setContextAndRegisterReceivers(Context context)
    {
        mContext = context;
        mActivity = getActivity(mContext);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            IntentFilter BluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver1, BluetoothIntent);

            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver2, intentFilter);

            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            mContext.registerReceiver(mBroadcastReceiver4, filter);

            IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter1.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            filter1.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            mContext.registerReceiver(mBroadcastReceiver5, filter1);
        } catch (Exception e) {
            Log.e("BluetoothConnection", "Error registering receivers", e);
        }

        if (mContextMain != null) {
            bluetoothIn = new IncomingMessageHandler(handlerState, mContextMain);
        }
    }


    public void setTextViews()
    {
        if (mActivity == null) return;

        lvNewDevices = mActivity.findViewById(R.id.lvNewDevices);
        mBluetoothDevices = new ArrayList<>();
        if (lvNewDevices != null) {
            lvNewDevices.setOnItemClickListener(BluetoothConnection.this);
        }

        lvPairedDevices = mActivity.findViewById(R.id.lvPairedDevices);
        mBluetoothPairedDevices = new ArrayList<>();
        if (lvPairedDevices != null) {
            lvPairedDevices.setOnItemClickListener(BluetoothConnection.this);
        }
    }


    public void SetContextMain(Context context)
    {
        mContextMain = context;
    }


    public void listPairedDevices()
    {
        if (mBluetoothAdapter == null) return;
        try {
            Set<BluetoothDevice> all_devices = mBluetoothAdapter.getBondedDevices();
            if (all_devices != null && all_devices.size() > 0)
            {
                for (BluetoothDevice currentDevice : all_devices)
                {
                    Log.i("PairedDevices", "PairedDevices:" + currentDevice.getName() + ": " + currentDevice.getAddress());
                    if (lvPairedDevices != null) {
                        mDevicePairedListAdapter = new DeviceListAdapter(mContext, R.layout.device_adapter_view, mBluetoothPairedDevices);
                        mBluetoothPairedDevices.add(currentDevice);
                        lvPairedDevices.setAdapter(mDevicePairedListAdapter);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("BluetoothConnection", "Error listing paired devices", e);
        }
    }


    public static void enableBluetooth(Context context)
    {
        BluetoothAdapter mmBluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
        Activity mmActivity = getActivity(context);

        if (mmBluetoothAdapter == null)
        {
            Log.e("Bluetooth", "There is no bluetooth on the device");
        } else if (!mmBluetoothAdapter.isEnabled())
        {
            Intent turnBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (mmActivity != null)
                mmActivity.startActivityForResult(turnBluetoothOn, 1);
        }

    }


    public void enableDiscoverableMode()
    {
        if (mContext == null) return;
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        mContext.startActivity(discoverableIntent);
    }


    public void discoverDevices()
    {
        if (mBluetoothAdapter == null) return;
        if (mBluetoothAdapter.isDiscovering())
        {
            mBluetoothAdapter.cancelDiscovery();
            checkBluetoothPermissions();
            mBluetoothAdapter.startDiscovery();
        } else
        {
            checkBluetoothPermissions();
            mBluetoothAdapter.startDiscovery();
        }
    }


    @TargetApi(23)
    private void checkBluetoothPermissions()
    {
        if (mContext == null || mActivity == null) return;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
        {
            int permissionCheck = mContext.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += mContext.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0)
            {
                mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
    }


    private void showMessage(String message)
    {
        if (mContext == null) return;
        Toast toast = Toast.makeText(mContext.getApplicationContext(), message, Toast.LENGTH_SHORT);
        TextView vi = toast.getView().findViewById(android.R.id.message);
        if (vi != null) vi.setGravity(Gravity.CENTER);
        toast.show();
    }


    private static Activity getActivity(Context context)
    {
        if (context == null)
        {
            return null;
        } else if (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            } else
            {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }


    public void unregisterBroadcastReceiver()
    {
        if (mContext == null) return;
        try { mContext.unregisterReceiver(mBroadcastReceiver1); } catch (Exception ignored) {}
        try { mContext.unregisterReceiver(mBroadcastReceiver2); } catch (Exception ignored) {}
        try { mContext.unregisterReceiver(mBroadcastReceiver3); } catch (Exception ignored) {}
        try { mContext.unregisterReceiver(mBroadcastReceiver4); } catch (Exception ignored) {}
        try { mContext.unregisterReceiver(mBroadcastReceiver5); } catch (Exception ignored) {}
    }


    public void closeBluetoothSocket()
    {
        try
        {
            if(mBluetoothSocket != null)
                mBluetoothSocket.close();
        } catch (IOException e2)
        {
            Log.e("SetConnection", "ERROR - Failed to close Bluetooth socket");
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if(adapterView.getId() == R.id.lvNewDevices)
        {
            if (i < mBluetoothDevices.size()) {
                String deviceName = mBluetoothDevices.get(i).getName();
                String deviceAddress = mBluetoothDevices.get(i).getAddress();

                Log.i("BluetoothConnection", "Device name: " + deviceName);
                Log.i("BluetoothConnection", "Device address: " + deviceAddress);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                {
                    startClient(mBluetoothDevices.get(i));
                }
            }
        }
        else
        {
            if (mBluetoothPairedDevices != null && i < mBluetoothPairedDevices.size()) {
                String deviceName = mBluetoothPairedDevices.get(i).getName();
                String deviceAddress = mBluetoothPairedDevices.get(i).getAddress();

                Log.i("BluetoothConnection", "Device name: " + deviceName);
                Log.i("BluetoothConnection", "Device address: " + deviceAddress);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
                {
                    startClient(mBluetoothPairedDevices.get(i));
                }
            }
        }
    }


    private void startClient(BluetoothDevice device)
    {
        Log.d("startClient", "startClient: Started.");

        if (mContext != null) {
            mProgressDialog = ProgressDialog.show(mContext, mContext.getString(R.string.connectingProgress), mContext.getString(R.string.please_wait), true);
        }

        mConnectionThread = new ConnectionThread(device);
        mConnectionThread.start();
    }


    private class ConnectionThread extends Thread
    {
        private BluetoothSocket mmBluetoothSocket;

        public ConnectionThread(BluetoothDevice device)
        {
            Log.i("ConnectedThread", "Linked");
            mmDevice = device;
        }

        public void run()
        {
            BluetoothSocket tmp = null;

            try
            {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e1)
            {
                Log.e("SetConnection", "Could not create Bluetooth socket");
            }

            mmBluetoothSocket = tmp;

            if (mBluetoothAdapter != null) {
                mBluetoothAdapter.cancelDiscovery();
            }

            try
            {
                mmBluetoothSocket.connect();
            } catch (IOException e)
            {
                try
                {
                    mmBluetoothSocket.close();
                } catch (IOException e2)
                {
                    Log.e("SetConnection", "ERROR - Could not close Bluetooth socket");
                }
            }

            connected(mmBluetoothSocket, mmDevice);
        }
    }


    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice)
    {
        Log.d("connected", "connected: Starting.");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }


    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d("ConnectedThread", "ConnectedThread: Starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                if (mmSocket != null) {
                    tmpIn = mmSocket.getInputStream();
                    tmpOut = mmSocket.getOutputStream();
                }
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[1024];

            int bytes;

            while (true)
            {
                try
                {
                    if (mmInStream == null) break;
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d("ConnectedThread", "Reading from input stream: " + incomingMessage);
                    if (bluetoothIn != null) {
                        bluetoothIn.obtainMessage(handlerState, bytes, -1, incomingMessage).sendToTarget();
                    }
                } catch (IOException e)
                {
                    Log.e("ConnectedThread", "Error reading input stream" + e.getMessage());
                    break;
                }
            }
        }

        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d("ConnectedThread", "Writing to output stream: " + text);
            try
            {
                if (mmOutStream != null) {
                    mmOutStream.write(bytes);
                }
            } catch (IOException e)
            {
                Log.e("ConnectedThread", "Error writing to output stream" + e.getMessage());
            }
        }

        public void cancel()
        {
            try
            {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            } catch (IOException e)
            {
            }
        }
    }


    public void write(String message)
    {
        Log.d("write", "write: Write Called.");
        byte[] messageBuffer = message.getBytes();
        if(mConnectedThread != null)
            mConnectedThread.write(messageBuffer);
    }


    public boolean getConnectionStatus()
    {
        return mConnectionStatus;
    }


    public void collectDataStateChange()
    {
        if (mContext == null) return;
        if(mCollectDataState)
        {
            mCollectDataState = false;
            showMessage(mContext.getString(R.string.data_collecting_stopped));
        }
        else
        {
            mCollectDataState = true;
            showMessage(mContext.getString(R.string.data_collecting_started));
        }

        mCollectDataStateOnChange = true;
    }


    public boolean getCollectDataState()
    {
        return mCollectDataState;
    }


    public boolean getCollectDataStateOnChange()
    {
        return mCollectDataStateOnChange;
    }


    public void setCollectDataStateOnChange(boolean mCollectDataStateOnChange)
    {
        this.mCollectDataStateOnChange = mCollectDataStateOnChange;
    }


    public String getExerciseName()
    {
        return mExerciseName;
    }


    public void setExerciseName(String mExerciseName)
    {
        this.mExerciseName = mExerciseName;
    }
}
