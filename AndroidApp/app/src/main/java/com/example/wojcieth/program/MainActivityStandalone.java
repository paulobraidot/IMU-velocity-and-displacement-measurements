package com.example.wojcieth.program;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivityStandalone extends AppCompatActivity {

    private static final String TAG = "MainActivityStandalone";

    private EditText editTextWithNewName;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer, sensorGyroscope, sensorMagneticField;
    private SensorEventListener sensorEventListener;

    /** contexto */
    private Context context;

    private Handler dialogWindowHandler;

    /** asistente de la base de datos */
    private DatabaseHelper mDatabaseHelper;

    /** identificador de la serie de ejercicios */
    private long IDofExercise;

    private ProgressDialog mProgressDialog;

    private long startTimeMs;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_standalone);

        editTextWithNewName = findViewById(R.id.editTextExerciseName);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (sensorManager == null || sensorAccelerometer == null || sensorGyroscope == null || sensorMagneticField == null) {
            showMessage(this.getString(R.string.there_is_no_data_to_show));
            finish();
            return;
        }

        context = this;
        mDatabaseHelper = new DatabaseHelper(context);

        dialogWindowHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                showMessage(getString(R.string.data_collecting_stopped));
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorEventListener != null && sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDatabaseHelper != null) {
            mDatabaseHelper.close();
        }
    }

    public void btnCollectData(View v) {
        String nameOfExercise = editTextWithNewName.getText().toString();
        if (!nameOfExercise.equals("")) {
            IDofExercise = System.currentTimeMillis() / 1000;
            startTimeMs = System.currentTimeMillis();

            sensorEventListener = new SensorEventListener() {
                private final float[] accelValues = new float[3];
                private final float[] gyroValues = new float[3];
                private final float[] magValues = new float[3];

                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    switch (sensorEvent.sensor.getType()) {
                        case Sensor.TYPE_ACCELEROMETER:
                            System.arraycopy(sensorEvent.values, 0, accelValues, 0, 3);
                            break;
                        case Sensor.TYPE_GYROSCOPE:
                            System.arraycopy(sensorEvent.values, 0, gyroValues, 0, 3);
                            break;
                        case Sensor.TYPE_MAGNETIC_FIELD:
                            System.arraycopy(sensorEvent.values, 0, magValues, 0, 3);
                            break;
                    }

                    double[] rawData = new double[]{
                            accelValues[0], accelValues[1], accelValues[2],
                            gyroValues[0], gyroValues[1], gyroValues[2],
                            magValues[0], magValues[1], magValues[2]
                    };

                    int controlNr1 = (int) (System.currentTimeMillis() - startTimeMs);
                    int controlNr2 = -2;

                    mDatabaseHelper.addData(IDofExercise, nameOfExercise, controlNr1, rawData, controlNr2);
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {

                }
            };

            sensorManager.registerListener(sensorEventListener, sensorAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(sensorEventListener, sensorGyroscope, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(sensorEventListener, sensorMagneticField, SensorManager.SENSOR_DELAY_GAME);

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(this.getString(R.string.data_collecting));
            alertDialog.setMessage(this.getString(R.string.data_collecting));
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, this.getString(R.string.stop), (dialog, id) -> {
                if (sensorEventListener != null && sensorManager != null) {
                    sensorManager.unregisterListener(sensorEventListener);
                }

                mProgressDialog = ProgressDialog.show(context, context.getString(R.string.calculating), context.getString(R.string.please_wait), true);

                Thread t = new Thread(() -> {
                    try {
                        DataProcessing dataProcessing = new DataProcessing(getApplicationContext());
                        dataProcessing.processData();
                    } catch (Exception e) {
                        Log.e(TAG, "Error during data processing", e);
                    } finally {
                        if (dialogWindowHandler != null) {
                            dialogWindowHandler.sendEmptyMessage(0);
                        }
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
     *
     * @param message: mensaje para mostrar
     */
    private void showMessage(String message) {
        Toast toast = Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT);
        TextView vi = toast.getView().findViewById(android.R.id.message);
        if (vi != null) vi.setGravity(Gravity.CENTER);
        toast.show();
    }
}
