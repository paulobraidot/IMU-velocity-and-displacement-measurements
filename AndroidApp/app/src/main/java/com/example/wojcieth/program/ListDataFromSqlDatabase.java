package com.example.wojcieth.program;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
// import android.support.annotation.Nullable;
import androidx.annotation.Nullable;
// import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

/**
 * clase que muestra todas las series de mediciones (un registro por cada una) para seleccionar una de ellas
 * para una mayor transferencia
 */
public class ListDataFromSqlDatabase extends AppCompatActivity
{
    /** etiqueta */
    private static final String TAG = "ListDataActivity";

    /** asistente de base de datos para la gestión */
    DatabaseHelper mDatabaseHelper;

    /** vista en lista que muestra las medidas */
    private ListView mListView;

    /** conexión de bluetooth */
    BluetoothConnection BT;

    /** adaptador para enumerar ejercicios y sus tiempos */
    private ExerciseListAdapter mExerciseListAdapter;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BT = (BluetoothConnection)getApplicationContext();

        setContentView(R.layout.list_data_from_sql_layout);
        mListView = findViewById(R.id.listViewSqlData);

        mDatabaseHelper = new DatabaseHelper(this);
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        BT.setContextAndRegisterReceivers(this);
        listIDS();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        BT.unregisterBroadcastReceiver();
    }

    /**
     * muestra un mensaje centrado
     *
     * @param message: mensaje para mostrar
     */
    private void showMessage(String message)
    {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        TextView vi = toast.getView().findViewById(android.R.id.message);
        if (vi != null) vi.setGravity(Gravity.CENTER);
        toast.show();
    }


    /**
     * Enumera todos los que aparecen desde entonces
     */
    private void listIDS()
    {
        Log.d(TAG, "listing ID's");

        // toma los datos y los une a la lista
        Cursor data = mDatabaseHelper.getIDS();
        final ArrayList<String[]> listData = new ArrayList<>();
        while(data.moveToNext())
        {
            String[] nameAndDate = new String[3];
            // copia y almacena en listData
            nameAndDate[0] = data.getString(2);
            nameAndDate[1] = data.getString(1);
            nameAndDate[2] = data.getString(0); //ID
            listData.add(nameAndDate);
        }


        mExerciseListAdapter = new ExerciseListAdapter(this, R.layout.records_adapter_view, listData);
        mListView.setAdapter(mExerciseListAdapter);



        // cuando se presiona
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                // obteniendo identificación
                String ID = listData.get(i)[2];

                Log.i(TAG, "selected" + ID);

                // iniciar una nueva actividad donde se muestran los datos seleccionados
                Intent listDataScreenIntent = new Intent(ListDataFromSqlDatabase.this, ListDataFromSqlDatabaseBySelectedId.class);
                listDataScreenIntent.putExtra("selectedID", Integer.parseInt(ID));
                startActivity(listDataScreenIntent);
            }
        });
    }


}
