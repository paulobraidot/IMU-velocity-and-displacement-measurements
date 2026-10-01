package com.example.wojcieth.program;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
// import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Clase con la actividad principal
 */
public class MainActivity extends AppCompatActivity
{
    BluetoothConnection BT;
    private EditText editTextWithNewName;
    Context context;
    Handler dialogWindowHandler;
    ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BT = (BluetoothConnection)getApplicationContext();
        BT.SetContextMain(this);


        editTextWithNewName = findViewById(R.id.editTextExerciseName);

        context = this;
        dialogWindowHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
            mProgressDialog.dismiss();            }
        };
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        BT.setContextAndRegisterReceivers(this);

    }

    @Override
    protected void onPause()
    {
        super.onPause();
        BT.unregisterBroadcastReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * soporte para seleccionar un elemento del menú de la aplicación
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // selecciona el menú con conexión bluetooth
        if (item.getItemId() == R.id.bluetoothConnectionMenu)
        {
            Intent mIntentMenuBluetooth  = new Intent(this, BluetoothMenu.class);
            startActivity(mIntentMenuBluetooth);
        }

        // selecciona el menú de la lista de datos SQL
        if (item.getItemId() == R.id.listOfDataFromSqlMenu)
        {
            Intent mIntentMenuBluetooth  = new Intent(this, ListDataFromSqlDatabase.class);
            startActivity(mIntentMenuBluetooth);
        }

        return true;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }



    /**
     * admite un botón que comienza a guardar datos en la base de datos SQL,
     * comprueba si esta conectado el dispositivo mediante Bluetooth y si se ha introducido el nombre del ejercicio
     * cuando no hay ninguna conexión lo lleva a una actividad donde se puede establecer una conexión
     * @see BluetoothMenu
     */
    public void btnCollectData(View v)
    {
        // verifica estado de conexión
        if(BT.getConnectionStatus())
        {
            // revisa si se ha introducido un nombre de ejercicio
            String nameOfExercise = editTextWithNewName.getText().toString();

            if(!nameOfExercise.equals(""))
            {
                BT.setExerciseName(nameOfExercise);
                BT.collectDataStateChange();

                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(this.getString(R.string.data_collecting));
                alertDialog.setMessage(this.getString(R.string.data_collecting));
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(R.string.stop), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        BT.collectDataStateChange();
                        mProgressDialog = ProgressDialog.show(context, context.getString(R.string.calculating), context.getString(R.string.please_wait), true);




                        // calcula ángulos de orientacion en otro enhebrado
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DataProcessing dataProcessing = new DataProcessing(getApplicationContext());
                                dataProcessing.processData();
                                dialogWindowHandler.sendEmptyMessage(0);
                            }});

                        t.start();

                    }
                });
                alertDialog.show();
            }
            else
            {
                showMessage(this.getString(R.string.you_must_enter_the_name));
            }

        }
        else
        {
            showMessage(this.getString(R.string.connect_first));
            Intent mIntentMenuBluetooth  = new Intent(this, BluetoothMenu.class);
            startActivity(mIntentMenuBluetooth);
        }
    }

    // prueba
    /*public void diodeOn(View v)
    {
        EditText et = findViewById(R.id.editText);
        String s = et.getText().toString();
        BT.write(s);
    }*/


    /**
     * muestra un mensaje centrado
     * @param message: mensaje para mostrar
     */
    private void showMessage(String message)
    {
        Toast toast = Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT);
        TextView vi = toast.getView().findViewById(android.R.id.message);
        if (vi != null) vi.setGravity(Gravity.CENTER);
        toast.show();
    }

}
