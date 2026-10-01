package com.example.wojcieth.program;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivityStandalone extends AppCompatActivity {

    private EditText editTextWithNewName;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer, sensorGyroscope, sensorMagneticField;
    private SensorEventListener sensorEventListener;

    /** contexto */
    private Context context;

    Handler dialogWindowHandler;

    /** asistente de la base de datos */
    private DatabaseHelper mDatabaseHelper;

    /** identificador de la serie de ejercicios */
    private  long IDofExercise;

    ProgressDialog mProgressDialog;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_standalone);

        editTextWithNewName = findViewById(R.id.editTextExerciseName);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if ((sensorAccelerometer == null) || (sensorGyroscope == null) || (sensorMagneticField == null)){
            finish();
        }

        sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListener, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);

        context = this;
        mDatabaseHelper = new DatabaseHelper(context);

/*
        dialogWindowHandler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg){
                mProgressDialog.dismiss();            }

        };
*/
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    public void btnCollectData(View v) {
        String nameOfExercise = editTextWithNewName.getText().toString();
        if (!nameOfExercise.equals("")) {
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(this.getString(R.string.data_collecting));
            alertDialog.setMessage(this.getString(R.string.data_collecting));
            // alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(R.string.stop), new DialogInterface.OnClickListener() {
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(R.string.stop), (dialog, id) -> {
            // mProgressDialog = ProgressDialog.show(context, context.getString(R.string.calculating), context.getString(R.string.please_wait), true);

                sensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent){

                        IDofExercise = System.currentTimeMillis() / 1000; // sensorEvent.timestamp;
                        int controlNr1 = -1;
                        int controlNr2 = -2;
                        double[] RawData = new double[9];
                        switch (sensorEvent.sensor.getType()) {
                            case Sensor.TYPE_ACCELEROMETER:
                                RawData[0] = sensorEvent.values[0];
                                RawData[1] = sensorEvent.values[1];
                                RawData[2] = sensorEvent.values[2];
                                break; // Conciderar eliminar el corte
                            case Sensor.TYPE_GYROSCOPE:
                                RawData[3] = sensorEvent.values[0];
                                RawData[4] = sensorEvent.values[1];
                                RawData[5] = sensorEvent.values[2];
                                break; // Conciderar eliminar el corte
                            case Sensor.TYPE_MAGNETIC_FIELD:
                                RawData[6] = sensorEvent.values[0];
                                RawData[7] = sensorEvent.values[1];
                                RawData[8] = sensorEvent.values[2];
                                break; // Conciderar eliminar el corte
                            default:
                                break;
                        }

                        boolean insertData = mDatabaseHelper.addData(IDofExercise, nameOfExercise, controlNr1, RawData, controlNr2);

                        mDatabaseHelper.close();
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };

                // calcula ángulos de orientacion en otro enhebrado
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DataProcessing dataProcessing = new DataProcessing(getApplicationContext());
                        dataProcessing.processData();
                        dialogWindowHandler.sendEmptyMessage(0);
                    }
                });

                t.start();

            });
            alertDialog.show();
        } else {
            showMessage(this.getString(R.string.you_must_enter_the_name));
        }
    }

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