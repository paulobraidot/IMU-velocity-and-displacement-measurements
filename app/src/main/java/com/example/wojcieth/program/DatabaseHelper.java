package com.example.wojcieth.program;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * clase que maneja las mediciones de aceleración, angulación y magnetismo en la base de datos SQLite
 * se crea la base de datos y se le envían consultas
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    /** número de columnas */
    private static final Integer NUMBER_OF_COLUMNS = 13;

    /** etiqueta */
    private static final String TAG = "DatabaseHelper";

    /** nombre de la base de datos */
    private static final String TABLE_NAME = "RawDataDatabase4";

    /** columna 1: número */
    private static String COL0_NUMBER = "number";

    /** columna 2: identificador */
    private static String COL1_ID = "id";

    /** columna 3: nombre */
    private static String COL2_EXERCISE = "exercise";

    /** columna 4: tiempo */
    private static String COL3_TIME = "time";

    /** columna 5: número de control 1 */
    private static String COL4_CONTROL_NR_1 = "control_nr_1";

    /** columnas 6 a 14: mediciones de sensores */
    private static String[] COL5_13 = {"acc_x", "acc_y", "acc_z", "gyro_x", "gyro_y", "gyro_z", "mag_x", "mag_y", "mag_z"};

    /** columna 15: número de control 2*/
    private static String COL14_CONTROL_NR_2 = "control_nr_2";

    /** contexto */
    Context context;


    /**
     * constructor
     *
     * @param context: contexto
     */
    public DatabaseHelper(Context context)
    {
        super(context, TABLE_NAME, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" + COL0_NUMBER + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL1_ID + " INTEGER, " +
                COL2_EXERCISE + " TEXT, " +
                COL3_TIME + " DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f', 'NOW')), " +
                COL4_CONTROL_NR_1 + " INTEGER, " +
                COL5_13[0] + " REAL, " +
                COL5_13[1] + " REAL, " +
                COL5_13[2] + " REAL, " +
                COL5_13[3] + " REAL, " +
                COL5_13[4] + " REAL, " +
                COL5_13[5] + " REAL, " +
                COL5_13[6] + " REAL, " +
                COL5_13[7] + " REAL, " +
                COL5_13[8] + " REAL, " +
                COL14_CONTROL_NR_2 + " INTEGER)";

        sqLiteDatabase.execSQL(createTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }


    /**
     * agrega registro a la base de datos
     *
     * @param IDofExercise: identificación del ejercicio
     * @param nameOfExercise: nombre del ejercicio
     * @param rawData: datos con resultados
     * @return booleano: verdadero cuando se agrega correctamente; de lo contrario, falso
     */
    public boolean addData(long IDofExercise, String nameOfExercise, int controlNumber1, double[] rawData, int controlNumber2)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1_ID, IDofExercise);
        contentValues.put(COL2_EXERCISE, nameOfExercise);
        //Log.d(TAG, "addData: Adding " + nameOfExercise + " to " + TABLE_NAME);

        contentValues.put(COL4_CONTROL_NR_1, controlNumber1);

        for (int j = 0; j < 9; j++)
        {
            contentValues.put(COL5_13[j], rawData[j]);
            //Log.d(TAG, "addData: Adding " + String.valueOf(rawData[j]) + " to " + TABLE_NAME);
        }

        contentValues.put(COL14_CONTROL_NR_2, controlNumber2);

        long result = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        return !(result == -1);
    }


    /**
     * devuelve registro de la base de datos
     *
     * @param IDinSQL: identificador del registro en la base de datos, si es -1 se devuelven todos los datos
     * @return datos seleccionados
     */
    public Cursor getData(int IDinSQL)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query;

        if(IDinSQL == -1)
        {
            query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL3_TIME;
        }
        else
        {
            query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL1_ID + " = " + Integer.toString(IDinSQL) + " ORDER BY " + COL3_TIME;
        }

        return sqLiteDatabase.rawQuery(query, null);
    }


    /**
     * devuelve todos los registros sin repeticiones
     *
     * @return datos: identificador, tiempo, ejercicio, nombre de tabla
     */
    public Cursor getIDS()
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query;

        query = "SELECT " + COL1_ID + ", " + "min(" + COL3_TIME + "), " + COL2_EXERCISE +" FROM " + TABLE_NAME + " GROUP BY " + COL1_ID +
                " ORDER BY " + COL1_ID + " DESC";

        return sqLiteDatabase.rawQuery(query, null);
    }


    /**
     * actualiza el nombre del ejercicio
     *
     * @param newName: nombre para actualizar
     * @param id: identificador del registro a actualizar
     */
    public void updateExerciseName(String newName, int id)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL2_EXERCISE + " = '" + newName +
                "' WHERE " + COL1_ID + " = '" + id + "'";
        Log.i(TAG, "updateName " + query);

        sqLiteDatabase.execSQL(query);

        DatabaseHelperRPY databaseHelperRPY = new DatabaseHelperRPY(context);
        databaseHelperRPY.updateExerciseName(newName, id);
    }



    /**
     * elimina un ejercicio con un identificador determinado
     *
     * @param id: identificador del registro a eliminar
     */
    public void deleteExercise(int id)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE " + COL1_ID + " = '" + id + "'";
        Log.i(TAG, "updateName " + query);

        sqLiteDatabase.execSQL(query);

        DatabaseHelperRPY databaseHelperRPY = new DatabaseHelperRPY(context);
        databaseHelperRPY.deleteExercise(id);
    }



    /**
     * devuelve el número de filas de la tabla
     *
     * @return número de filas
     */
    public int getNumberOfRows()
    {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(sqLiteDatabase, TABLE_NAME);
    }


    /**
     * devuelve el número de columnas de la tabla
     *
     * @return número de columnas
     */
    public int getNumberOfColumns()
    {
        return NUMBER_OF_COLUMNS;
    }


    public int getLargestID()
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        String query = "SELECT MAX(" + COL1_ID +") FROM " + TABLE_NAME;

        Cursor data = sqLiteDatabase.rawQuery(query, null);

        int ID = 0;

        boolean first = true;
        while(data.moveToNext())
        {
            if(first)
            {
                ID = data.getInt(0);
                first = false;
            }

        }

        data.close();

        return ID;
    }


}
