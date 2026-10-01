package com.example.wojcieth.program;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;


/**
 * manipulador que maneja los datos desde el Bluetooth,
 * comprueba su exactitud e integridad,
 * asigna una identificación única a la serie de mediciones,
 * guarda datos en una base de datos SQL,
 * conectado con enhebrado
 * @see com.example.wojciech.program.BluetoothConnection.ConnectedThread
 */
public class IncomingMessageHandler extends Handler
{
    /** identificador del mensaje */
    private int handlerState;

    /** constructor de cadenas de caracteres para procesar el mensaje */
    private StringBuilder recDataString = new StringBuilder();

    /** asistente de la base de datos */
    private DatabaseHelper mDatabaseHelper;

    /** contexto principal */
    private Context mContextMain;

    /** identificador de la serie de ejercicios */
    private  long IDofExercise;

    BluetoothConnection BT;

    /**
     * constructor
     * @param handlerState: identificador del mensaje
     * @param context: contexto
     */
    public IncomingMessageHandler(int handlerState, Context context)
    {
        this.handlerState = handlerState;
        this.mContextMain = context;
        this.IDofExercise = -1;

        BT = (BluetoothConnection)mContextMain.getApplicationContext();


        mDatabaseHelper = new DatabaseHelper(mContextMain);
    }


    /**
     * admite la recepción de datos desde Bluetooth
     * recupera el nombre del ejercicio ingresado, luego asigna una identificación única que se ingresa en SQL,
     * se utiliza para reconocer claramente un ejercicio determinado, se asigna en función del tiempo actual,
     * reconoce el mensaje basándose en caracteres especiales: comenzando con #, terminando con ~ y separada por +
     * Sobre esta base, las mediciones de sensores específicos (y sus coordenadas) se separan y se ingresan en la base de datos SQL
     *
     * @param msg: mensaje bluetooth
     */
    public void handleMessage(android.os.Message msg)
    {
        boolean insertData;
        String nameOfExercise = BT.getExerciseName(); // obtiene el nombre del ejercicio
        double[] RawData = new double[9];


        if (msg.what == handlerState && BT.getCollectDataState()) // comprueba si el mensaje es el correcto
        {
            // asigna una identificación única
            if(BT.getCollectDataStateOnChange())
            {
                IDofExercise = System.currentTimeMillis() / 1000;
                BT.setCollectDataStateOnChange(false);
            }


            String readMessage = (String) msg.obj; // msg.arg1 = bytes del hilo de conexión
            recDataString.append(readMessage); // copia a la cadena de caracteres
            int endOfLineIndex = recDataString.indexOf("~"); // encuentra el final de la cadena de caracteres
            if (endOfLineIndex > 0)
            {                                           // revisa si hay cadenas de caracteres antes
                String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extrae la cadena de caracetres
                //txtString.setText("Data Received = " + dataInPrint);
                int dataLength = dataInPrint.length(); // toma la longitud
                //txtStringLength.setText("String Length = " + String.valueOf(dataLength));


                if (dataInPrint.charAt(0) == '#' && countOccurrencesOf(dataInPrint, '#') == 1 && countOccurrencesOf(dataInPrint, '+') == 11)                             // encuentra el inicio de la cadena de caracteres
                {
                    // busca índices + para separar las variables entre sí
                    int controlNr1 = -1;
                    int controlNr2 = -2;
                    int indexOfFirstSeparationMark = 0;
                    int indexOfSecondSeparationMark = dataInPrint.indexOf("+");


                    //Log.d("IncomingMessageHandler", "recDataString: " + dataInPrint);


                    // encontrar el número de control
                    String controlNr1String = dataInPrint.substring(indexOfFirstSeparationMark + 1, indexOfSecondSeparationMark);
                    controlNr1 = Integer.valueOf(controlNr1String);


                    indexOfFirstSeparationMark = indexOfSecondSeparationMark;
                    indexOfSecondSeparationMark = dataInPrint.indexOf("+", indexOfFirstSeparationMark+1);



                    // extrae datos de sensores
                    for (int i = 0; i < 9; i++)
                    {
                        String sensor = dataInPrint.substring(indexOfFirstSeparationMark + 1, indexOfSecondSeparationMark);

                        RawData[i] = Double.valueOf(sensor);

                        indexOfFirstSeparationMark = indexOfSecondSeparationMark;
                        indexOfSecondSeparationMark = dataInPrint.indexOf("+", indexOfFirstSeparationMark+1);
                    }

                    // encuentra el número de control
                    String controlNr2String = dataInPrint.substring(indexOfFirstSeparationMark + 1, indexOfSecondSeparationMark);
                    controlNr2 = Integer.valueOf(controlNr2String);


                    insertData = mDatabaseHelper.addData(IDofExercise, nameOfExercise, controlNr1, RawData, controlNr2);

                    mDatabaseHelper.close();

                    // if(insertData)
                    // {
                        //Log.i("Agregado a SQL", "Éxitoso");
                    // }
                    // else
                    // {
                        //Log.e("Agregado a SQL", "Fallido");
                    // }


                }
                recDataString.delete(0, recDataString.length()); // claro
                // strIncom =" ";
                dataInPrint = " ";
            }
        }
    }

    /**
     * cuenta el número de apariciones de símbolos (se utiliza para comprobar si el mensaje no es demasiado grande o está unido)
     *
     * @param string: cadena en la que se van a contar las ocurrencias
     * @param symbol: símbolo
     * @return occurrence: número de apariciones del símbolo
     */
    private int countOccurrencesOf(String string, char symbol)
    {
        int occurrence = 0;

        for(int i = 0; i < string.length(); i++)
        {
            if(string.charAt(i) == symbol)
                occurrence++;
        }

        return occurrence;
    }
}
