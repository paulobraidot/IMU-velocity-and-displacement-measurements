package com.example.wojcieth.program;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * clase que maneja la velocidad y el desplazamiento en la base de datos SQLite
 * se crea la base de datos y se le envían consultas
 */
public class DatabaseHelperFinalData extends SQLiteOpenHelper
{
    /**
     * nombre de la base de datos
     */
    private static final String TABLE_NAME = "FinalData";

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
     * columna 6: velocidad compensada en el eje x
     */
    private static String COL5_VELX = "velx";

    /**
     * columna 7: velocidad compensada en el eje y
     */
    private static String COL6_VELY = "vely";

    /**
     * columna 8: velocidad compensada en el eje z
     */
    private static String COL7_VELZ = "velz";

    /**
     * columna 9: velocidad resultante
     */
    private static String COL8_VEL_NORM = "velnorm";

    /**
     * columna 10: desplazamiento en el eje x
     */
    private static String COL9_DISPX = "dispx";

    /**
     * columna 11: desplazamiento en el eje y
     */
    private static String COL10_DISPY = "dispy";

    /**
     * columna 12: desplazamiento en el eje z
     */
    private static String COL11_DISPZ = "dispz";

    /**
     * contexto
     */
    Context context;

    /**
     * constructor
     *
     * @param context: contexto
     */
    public DatabaseHelperFinalData(Context context)
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
                COL5_VELX + " REAL, " +
                COL6_VELY + " REAL, " +
                COL7_VELZ + " REAL, " +
                COL8_VEL_NORM + " REAL, " +
                COL9_DISPX + " REAL, " +
                COL10_DISPY + " REAL, " +
                COL11_DISPZ + " REAL)";

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
     * @param controlNumber: número de control
     * @return verdadero cuando se agrega correctamente; de lo contrario, falso
     */
    public boolean addData(long IDofExercise, String nameOfExercise, String date, int controlNumber, double[] velocity, double[] displacement)
    {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COL1_ID, IDofExercise);
        contentValues.put(COL2_EXERCISE, nameOfExercise);
        contentValues.put(COL3_TIME, date);
        contentValues.put(COL4_CONTROL_NR, controlNumber);
        contentValues.put(COL5_VELX, velocity[0]);
        contentValues.put(COL6_VELY, velocity[1]);
        contentValues.put(COL7_VELZ, velocity[2]);
        contentValues.put(COL8_VEL_NORM, Math.sqrt(Math.pow(velocity[0],2) + Math.pow(velocity[1],2) + Math.pow(velocity[2],2)));
        contentValues.put(COL9_DISPX, displacement[0]);
        contentValues.put(COL10_DISPY, displacement[1]);
        contentValues.put(COL11_DISPZ, displacement[2]);

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

        sqLiteDatabase.execSQL(query);
    }
}
