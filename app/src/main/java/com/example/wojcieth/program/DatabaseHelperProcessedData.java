package com.example.wojcieth.program;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.Timestamp;

/**
 * clase que maneja la aceleracion y velocidad en la base de datos SQLite
 * se crea la base de datos y se le envían consultas
 */
public class DatabaseHelperProcessedData extends SQLiteOpenHelper
{
    /**
     * etiqueta
     */
    private static final String TAG = "DatabaseHelperRPY";

    /**
     * nombre de la base de datos
     */
    private static final String TABLE_NAME = "ProcessedData";

    /**
     * columna 1: número de columna
     */
    private static String COL0_NUMBER = "number";

    /**
     * columna 2: identificador
     */
    private static String COL1_ID = "id";

    /**
     * columna 3: nombre
     */
    private static String COL2_EXERCISE = "exercise";

    /**
     * columna 4: tiempo
     */
    private static String COL3_TIME = "time";

    /**
     * columna 5: número de control 1
     */
    private static String COL4_CONTROL_NR = "control_nr_1";

    /**
     * columna 6: aceleración sin gravedad en el eje x
     */
    private static String COL5_ACCX = "accx";

    /**
     * columna 7: aceleración sin gravedad en el eje y
     */
    private static String COL6_ACCY = "accy";

    /**
     * columna 8: aceleración sin gravedad en el eje z
     */
    private static String COL7_ACCZ = "accz";

    /** columna 9: intervalo estatico */
    private static String COL8_STATIC_INTERVAL = "static";

    /**
     * columna 10: velocidad en el eje x
     */
    private static String COL9_VELX = "velx";

    /**
     * columna 11: velocidad en el eje y
     */
    private static String COL10_VELY = "vely";

    /**
     * columna 12: velocidad en el eje z
     */
    private static String COL11_VELZ = "velz";

//    /** columna 6: aceleración sin gravedad en el eje x */
//    private static String COL8_ACCX = "velx";
//
//    /** columna 7: aceleración sin gravedad en el eje y */
//    private static String COL9_ACCY = "vely";
//
//    /** columna 8: aceleración sin gravedad en el eje z */
//    private static String COL10ACCZ = "velz";

    /**
     * contexto
     */
    Context context;

    /**
     * constructor
     *
     * @param context: contexto
     */
    public DatabaseHelperProcessedData(Context context)
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
                COL3_TIME + " DATETIME DEFAULT(STRFTIME('%Y-%m-%d %H:%M:%f')), " +
                COL4_CONTROL_NR + " INTEGER, " +
                COL5_ACCX + " REAL, " +
                COL6_ACCY + " REAL, " +
                COL7_ACCZ + " REAL, " +
                COL8_STATIC_INTERVAL + " INTEGER, " +
                COL9_VELX + " REAL, " +
                COL10_VELY + " REAL, " +
                COL11_VELZ + " REAL)";

        sqLiteDatabase.execSQL(createTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    /**
     * agrega registro a la base de datos y elimina la aceleración debida a la gravedad
     *
     * @param IDofExercise: identificación del ejercicio
     * @param nameOfExercise: nombre del ejercicio
     * @param date: fecha
     * @param controlNumber: número de control
     * @param compensatedAcc: aceleración en el eje x, y, z
     * @return verdadero cuando se agrega correctamente; de lo contrario, falso
     */
    public boolean addData(long IDofExercise, String nameOfExercise, String date, int controlNumber,
                           double[] compensatedAcc, int staticInterval, double[] velocity)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1_ID, IDofExercise);
        contentValues.put(COL2_EXERCISE, nameOfExercise);
        contentValues.put(COL3_TIME, date);
        contentValues.put(COL4_CONTROL_NR, controlNumber);
        contentValues.put(COL5_ACCX, compensatedAcc[0]);
        contentValues.put(COL6_ACCY, compensatedAcc[1]);
        contentValues.put(COL7_ACCZ, compensatedAcc[2]);
        contentValues.put(COL8_STATIC_INTERVAL, staticInterval);
        contentValues.put(COL9_VELX, velocity[0]);
        contentValues.put(COL10_VELY, velocity[1]);
        contentValues.put(COL11_VELZ, velocity[2]);

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

        if (IDinSQL == -1)
        {
            query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COL3_TIME;
        } else
        {
            query = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL1_ID + " = " + Integer.toString(IDinSQL) + " ORDER BY " + COL3_TIME;
        }

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
    }

}
