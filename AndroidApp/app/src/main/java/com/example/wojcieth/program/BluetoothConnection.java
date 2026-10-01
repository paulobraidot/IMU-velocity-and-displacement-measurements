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

    /** manipulador para la clase manipulador de mensaje entrante
     * @see com.example.wojciech.program.IncomingMessageHandler */
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


    /**
     * crea un receptor de difusion que rastrea los cambios de estado de Bluetooth
     * utilizado por el método enableBluetooth()
     * @see com.example.wojciech.program.BluetoothConnection#enableBluetooth(Context)
     */
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


    /**
     * crea un receptor de difusion que rastrea los cambios en el estado Descubrible (el dispositivo puede ser encontrado por otro)
     * utilizado por el método enableDiscoverableMode()
     * @see com.example.wojciech.program.BluetoothConnection#enableDiscoverableMode()
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    // dispositivo en modo descubrible
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.i("BluetoothConnection", "mBroadcastReceiver2: Discoverability Enabled");
                        break;
                    // dispositivo en modo no descubrible
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


    /**
     * crea un receptor de difusion que rastrea el estado de búsqueda de dispositivos que no están emparejados y los agrega a la lista mBluetoothDevices
     * utilizado por el método discoverDevices()
     * @see com.example.wojciech.program.BluetoothConnection#discoverDevices()
     */
    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_FOUND)) {
                // si encuentra el dispositivo, lo agrega a la lista mBluetoothDevices
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBluetoothDevices.add(device);
                Log.i("BluetoothConnection", "mBroadcastReceiver3:" + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(mContext, R.layout.device_adapter_view, mBluetoothDevices); // Aquí está R.layout.device_adapter_view, que es un archivo xml donde ingresamos el nombre del dispositivo y su dirección mac
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    /**
     * crea un receptor de difusion que detecte cambios de estado de enlace (emparejamiento)
     * utilizado en el constructor
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                {
                    // dispositivo ya vinculado
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_BONDED");
                        showMessage(mContext.getString(R.string.bonded));
                    }
                    // vinculando dispositivo
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_BONDING");
                        showMessage(mContext.getString(R.string.bonding));
                    }
                    // desvinculación
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE)
                    {
                        Log.i("BluetoothConnection", "mBroadcastReceiver4: BOND_NONE");
                        showMessage(mContext.getString(R.string.bond_none));
                    }
                }
            }
        }
    };









    /**
     * receptor de difusion que analiza las transmisiones por Bluetooth, informa cuando se conecta o desconecta del dispositivo
     */
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
                    // conexión
                    if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
                    {
                        Log.i("BluetoothConnection", "Connected");
                        mConnectionStatus = true;
                        showMessage(mContext.getString(R.string.connected));
                    }
                    // desconexión
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








    /**
     * Constructor
     *
     */
    public BluetoothConnection()
    {
        mCollectDataState = false;
        mConnectionStatus = false;
    }


    /**
     * asigna contexto a la variable de contexto mContext y crea mActivity, establece receptores de registros
     * @param context: contexto
     */
    public void setContextAndRegisterReceivers(Context context)
    {
        mContext = context;
        mActivity = getActivity(mContext);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // seguimiento del cambio de estado de Bluetooth a mBroadcastReceiver1
        IntentFilter BluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver1, BluetoothIntent);

        // seguimiento de cambios de estado de origen mBroadcastReceiver2: admite descubrimiento
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver2, intentFilter);

        // difusión desde la búsqueda de dispositivos
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);

        // difusión cuando cambia el estado del vínculo, por ejemplo, emparejamiento
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver4, filter);

        // receptor de difusión que informa sobre cómo conectar el dispositivo
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter1.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter1.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mBroadcastReceiver5, filter1);


        // manipulador para poner en fila los mensajes entrantes
        bluetoothIn = new IncomingMessageHandler(handlerState, mContextMain);
    }


    /**
     * establece TextViews en los que se mostrarán la búsqueda de dispositivos y el dispositivo emparejado
     */
    public void setTextViews()
    {

        lvNewDevices = mActivity.findViewById(R.id.lvNewDevices); // TODO probablemente no sea la lista final de dispositivos
        mBluetoothDevices = new ArrayList<>();
        lvNewDevices.setOnItemClickListener(BluetoothConnection.this);

        lvPairedDevices = mActivity.findViewById(R.id.lvPairedDevices); //TODO probablemente no sea la lista final de dispositivos
        mBluetoothPairedDevices = new ArrayList<>();
        lvPairedDevices.setOnItemClickListener(BluetoothConnection.this);
    }


    /**
     * establece el contexto principal
     */
    public void SetContextMain(Context context)
    {
        mContextMain = context;
    }



    /**
     * imprime dispositivos emparejados
     */
    public void listPairedDevices()
    {
        Set<BluetoothDevice> all_devices = mBluetoothAdapter.getBondedDevices();
        if (all_devices.size() > 0)
        {
            for (BluetoothDevice currentDevice : all_devices)
            {
                Log.i("PairedDevices", "PairedDevices:" + currentDevice.getName() + ": " + currentDevice.getAddress());
                mDevicePairedListAdapter = new DeviceListAdapter(mContext, R.layout.device_adapter_view, mBluetoothPairedDevices);
                mBluetoothPairedDevices.add(currentDevice);
                lvPairedDevices.setAdapter(mDevicePairedListAdapter);
            }
        }
    }






    /**
     * activa bluetooth
     */
    public static void enableBluetooth(Context context)
    {
        BluetoothAdapter mmBluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
        Activity mmActivity = getActivity(context);

        // el dispositivo no tiene bluetooth
        if (mmBluetoothAdapter == null)
        {
            // muesta información sobre bluetooth que falta
            Log.e("Bluetooth", "There is no bluetooth on the device");
        } else if (!mmBluetoothAdapter.isEnabled())
        {
            // pregunta al usuario si desea activar el bluetooth
            Intent turnBluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (mmActivity != null)
                mmActivity.startActivityForResult(turnBluetoothOn, 1);

        }

    }








    /**
     * configura el dispositivo en detectable (visible para otros dispositivos) durante 30 segundos
     */
    public void enableDiscoverableMode()
    {
        // activa detectable durante 30 segundos
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        mContext.startActivity(discoverableIntent);


    }








    /**
     * busca dispositivos bluetooth habilitados
     */
    public void discoverDevices()
    {
        if (mBluetoothAdapter.isDiscovering()) // consulta si esta escaneando
        {
            // deja de escanear
            mBluetoothAdapter.cancelDiscovery();

            // verifica permisos
            checkBluetoothPermissions();

            // empieza a escanear
            mBluetoothAdapter.startDiscovery();

        } else
        {
            // verifica permisos
            checkBluetoothPermissions();

            // empieza a escanear
            mBluetoothAdapter.startDiscovery();

        }
    }








    /**
     * verifica permisos para buscar dispositivos a través de bluetooth, requerida en todos los dispositivos API 23+
     * android debe verificar los permisos mediante programación y agregarlos al manifiesto si no están presentes
     */
    @TargetApi(23)
    private void checkBluetoothPermissions()
    {
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








    /**
     * muestra un mensaje centrado
     *
     * @param message: mensaje para mostrar
     */
    private void showMessage(String message)
    {
        Toast toast = Toast.makeText(mContext.getApplicationContext(), message, Toast.LENGTH_SHORT);
        TextView vi = toast.getView().findViewById(android.R.id.message);
        if (vi != null) vi.setGravity(Gravity.CENTER);
        toast.show();
    }









    /**
     * devuelve actividad desde el contexto
     *
     * @param context: contexto desde el cual se devolverá la actividad
     * @return activity: actividad o nulo en caso de error
     */
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







    /**
     * libera receptor de difusión
     */
    public void unregisterBroadcastReceiver()
    {
        mContext.unregisterReceiver(mBroadcastReceiver1);
        mContext.unregisterReceiver(mBroadcastReceiver2);
        mContext.unregisterReceiver(mBroadcastReceiver3);
        mContext.unregisterReceiver(mBroadcastReceiver4);
        mContext.unregisterReceiver(mBroadcastReceiver5);
    }






    /**
     * cierra el puerto bluetooth
     */
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







    /**
     * empareja con un dispositivo de la lista
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
    {
        if(adapterView.getId() == R.id.lvNewDevices)
        {
            // deja de escanear
            mBluetoothAdapter.cancelDiscovery();

            // obtiene el nombre y la dirección del dispositivo seleccionado
            String deviceName = mBluetoothDevices.get(i).getName();
            String deviceAddress = mBluetoothDevices.get(i).getAddress();

            Log.i("BluetoothConnection", "Device name: " + deviceName);
            Log.i("BluetoothConnection", "Device address: " + deviceAddress);

            // empareja el dispositivo con una forma mas reciente
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                startClient(mBluetoothDevices.get(i));
            }
        }

        // selecciona en la lista de dispositivos emparejados
        else
        {
            // deja de escanear
            mBluetoothAdapter.cancelDiscovery();

            // obtiene el nombre y la dirección del dispositivo seleccionado
            String deviceName = mBluetoothPairedDevices.get(i).getName();
            String deviceAddress = mBluetoothPairedDevices.get(i).getAddress();

            Log.i("BluetoothConnection", "Device name: " + deviceName);
            Log.i("BluetoothConnection", "Device address: " + deviceAddress);

            // empareja el dispositivo con una forma mas reciente
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                startClient(mBluetoothPairedDevices.get(i));
            }
        }
    }







    /**
     * inicia el enhebrado de conexión para establecer una conexión.
     * @see ConnectionThread
     **/
    private void startClient(BluetoothDevice device)
    {
        Log.d("startClient", "startClient: Started.");

        // inicia el diálogo de progreso
        mProgressDialog = ProgressDialog.show(mContext, mContext.getString(R.string.connectingProgress), mContext.getString(R.string.please_wait), true);

        mConnectionThread = new ConnectionThread(device);
        mConnectionThread.start();
    }







    /**
     * establece el enhebrado de la conexión: en la cual se crea un puerto y luego se activa la conexión
     * @see #connected(BluetoothSocket, BluetoothDevice)
     */
    private class ConnectionThread extends Thread
    {
        private BluetoothSocket mmBluetoothSocket;


        /**
         * constructor
         * @param device: dispositivo con el que se va a establecer la conexión
         */
        public ConnectionThread(BluetoothDevice device)
        {
            Log.i("ConnectedThread", "Linked");

            mmDevice = device;
        }


        /**
         * crea una conexión y luego activa las funciones conectadas
         * @see BluetoothConnection#connected(BluetoothSocket, BluetoothDevice)
         */
        public void run()
        {
            BluetoothSocket tmp = null;


            // showMessage(mContext.getString(R.string.connecting));


            // crea un puerto bluetooth para la conexión a un dispositivo específico
            try
            {
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e1)
            {
                Log.e("SetConnection", "Could not create Bluetooth socket");
            }


            mmBluetoothSocket = tmp;


            // deshabilita el descubrimiento porque ralentiza mucho la conexión
            mBluetoothAdapter.cancelDiscovery();

            // crea una conexión
            try
            {
                mmBluetoothSocket.connect();
            } catch (IOException e)
            {
                try
                {
                    // si se produce un error de entrada/salida, intenta cerrar el puerto
                    mmBluetoothSocket.close();
                } catch (IOException e2)
                {
                    Log.e("SetConnection", "ERROR - Could not close Bluetooth socket");
                }
            }


            connected(mmBluetoothSocket, mmDevice);
        }
    }





    /**
     * inicia un enhebrado para gestionar la conexión y enviar datos
     */
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice)
    {
        Log.d("connected", "connected: Starting.");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }






    /**
     * enhebrado responsable de mantener la conexión, enviar y recibir datos.
     **/
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;


        /**
         * constructor
         * @param socket: puerto a manipular
         */
        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d("ConnectedThread", "ConnectedThread: Starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // elimina el cuadro de diálogo de carga cuando se conecta
            try
            {
                mProgressDialog.dismiss();
            } catch (NullPointerException e)
            {
                e.printStackTrace();
            }


            try
            {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        /**
         * soporte de comunicación
         */
        public void run()
        {
            byte[] buffer = new byte[1024];  // vector de flujo

            int bytes; // bytes devueltos por read()

            // lee el flujo de entrada hasta que ocurra una excepción
            while (true)
            {
                // leer el flujo de entrada
                try
                {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d("ConnectedThread", "Reading from input stream: " + incomingMessage);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, incomingMessage).sendToTarget();
                } catch (IOException e)
                {
                    Log.e("ConnectedThread", "Error reading input stream" + e.getMessage());
                    break;
                }
            }
        }


        /**
         * envía datos
         * @param bytes: datos para enviar
         */
        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d("ConnectedThread", "Writing to output stream: " + text);
            try
            {
                mmOutStream.write(bytes);
            } catch (IOException e)
            {
                Log.e("ConnectedThread", "Error writing to output stream" + e.getMessage());
            }
        }

        /**
         * cierra puerto
         */
        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
            }
        }
    }





    /**
     * envía un mensaje, escribiendo en ConnectedThread
     *
     * @param message: datos para enviar
     * @see ConnectedThread#write(byte[])
     */
    public void write(String message)
    {
        Log.d("write", "write: Write Called.");
        //string to byte
        byte[] messageBuffer = message.getBytes();
        //napisz
        if(mConnectedThread != null)
            mConnectedThread.write(messageBuffer);
    }


    /**
     * capta el estado de conexión
     * @return mConnectionStatus: valor verdadero para conectado
     */
    public boolean getConnectionStatus()
    {
        return mConnectionStatus;
    }

    /**
     * cambia el estado de la recopilación de datos: si los datos deben guardarse en la base de datos
     */
    public void collectDataStateChange()
    {
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


    /**
     * capta el estado de la recopilación de datos
     * @return mCollectDataState
     */
    public boolean getCollectDataState()
    {
        return mCollectDataState;
    }



    /**
     * capta de estado de cambio de recopilación de datos: configurado en verdadero recopila cuando hay cambios y se utiliza para asignar una identificación única a los ejercicios
     * @return mCollectDataState
     */
    public boolean getCollectDataStateOnChange()
    {
        return mCollectDataStateOnChange;
    }


    /**
     * establece el estado de cambio de recopilación de datos: configurado en verdadero recopila cuando hay cambios y se utiliza para asignar una identificación única a los ejercicios
     * @param mCollectDataStateOnChange: se establece en falso cuando se asigna un identificador
     */
    public void setCollectDataStateOnChange(boolean mCollectDataStateOnChange)
    {
        this.mCollectDataStateOnChange = mCollectDataStateOnChange;
    }


    /**
     * capta el nombre del ejercicio que se transmite al manejar datos entrantes
     * @return mExerciseName: nombre del ejercicio
     */
    public String getExerciseName()
    {
        return mExerciseName;
    }


    /**
     * establece el nombre del ejercicio
     * @param mExerciseName: nombre del ejercicio
     */
    public void setExerciseName(String mExerciseName)
    {
        this.mExerciseName = mExerciseName;
    }
}


