package com.example.wojcieth.program;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * clase que maneja los ángulos orientación en la base de datos SQLite
 * se crea la base de datos y se le envían orientación
 */
public class DatabaseHelperRPY extends SQLiteOpenHelper
{

    /** etiqueta */
    private static final String TAG = "DatabaseHelperRPY";

    /** nombre de la base de datos */
    private static final String TABLE_NAME = "RPYDataDatabase";

    /** columna 1: número de columna */
    private static String COL0_NUMBER = "number";

    /** columna 2: identificador */
    private static String COL1_ID = "id";

    /** columna 3: nombre */
    private static String COL2_EXERCISE = "exercise";

    /** columna 4: tiempo*/
    private static String COL3_TIME = "time";

    /** columna 5: número de control 1 */
    private static String COL4_CONTROL_NR = "control_nr_1";

    /** columna 6: ángulo de deriva */
    private static String COL5_YAW = "yaw";

    /** columna 7: ángulo de inclinación */
    private static String COL6_PITCH = "pitch";

    /** columna 8: ángulo de balanceo */
    private static String COL7_ROLL = "roll";

    /** contexto */
    Context context;



    /**
     * constructor
     *
     * @param context: contexto
     */
    public DatabaseHelperRPY(Context context)
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
                COL4_CONTROL_NR + " INTEGER, " +
                COL5_YAW + " REAL, " +
                COL6_PITCH + " REAL, " +
                COL7_ROLL + " REAL)";

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
     * @param controlNumber: número de control
     * @param yawAngle: ángulo de deriva
     * @param pitchAngle: ángulo de inclinación
     * @param rollAngle: ángulo de balanceo
     * @return verdadero cuando se agrega correctamente; de lo contrario, falso
     */
    public boolean addData(long IDofExercise, String nameOfExercise, int controlNumber, double yawAngle, double pitchAngle, double rollAngle)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1_ID, IDofExercise);
        contentValues.put(COL2_EXERCISE, nameOfExercise);
        contentValues.put(COL4_CONTROL_NR, controlNumber);
        contentValues.put(COL5_YAW, yawAngle);
        contentValues.put(COL6_PITCH, pitchAngle);
        contentValues.put(COL7_ROLL, rollAngle);

        long result = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);

        return !(result == -1);
    }


    /**
     * devuelve registro de la base de datos
     *
     * @param IDinSQL: identificador del registro en la base de datos, si es -1 entonces se devuelven todos los datos
     * @return: datos seleccionados
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
