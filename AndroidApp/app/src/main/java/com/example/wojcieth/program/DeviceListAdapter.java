package com.example.wojcieth.program;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import java.util.ArrayList;

/**
 * clase que enumera los dispositivos encontrados con Bluetooth habilitado
 */
public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice>
{
    /** inflación de la disposición */
    private LayoutInflater mLayoutInflater;

    /** lista de arrelos con dispositivos bluetooth, con el nombre y la dirección MAC */
    private ArrayList<BluetoothDevice> mDevices;
    private int mViewResourceId;

    /**
     * constructor: lista dispositivos necesaria en mBroadcastReceiver3
     *
     * @param context: contexto
     * @param tvResourceId - puntero al archivo xml donde se ingresa el nombre y la dirección del dispositivo
     * @param devices: lista de dispositivos bluetooth
     */
    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices)
    {
        super(context, tvResourceId, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = tvResourceId;
    }

    /**
     * instancia una inflación de vistas de un archivo xml de diseño con sus objetos correspondientes
     *
     * @param position
     * @param convertView
     * @param parent
     * @return convertView
     */
    public View getView(int position, View convertView, ViewGroup parent)
    {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);

        BluetoothDevice device = mDevices.get(position);

        if(device != null)
        {
            TextView deviceName = convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAddress = convertView.findViewById(R.id.tvDeviceAddress);

            if (deviceName != null)
            {
                deviceName.setText(device.getName());
            }

            if (deviceAddress != null)
            {
                deviceAddress.setText(device.getAddress());
            }
        }

        return convertView;
    }
}
