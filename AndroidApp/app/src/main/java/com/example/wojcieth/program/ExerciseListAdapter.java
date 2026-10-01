package com.example.wojcieth.program;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ExerciseListAdapter extends ArrayAdapter<String[]>
{
    /** inflación de la disposición */
    private LayoutInflater mLayoutInflater;
    
    /** lista de arrelos con nombres y fechas de las series de datos */
    private ArrayList<String[]> mNames;
    private int mViewResourceId;
    
    /**
     * constructor: lista ejercicios
     *
     * @param context: contexto
     * @param tvResourceId - puntero al archivo xml
     * @param names: lista de ejercicios
     */
    public ExerciseListAdapter(Context context, int tvResourceId, ArrayList<String[]> names)
    {
        super(context, tvResourceId, names);

        this.mNames = names;
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

        String[] exerciseNameAndDate = mNames.get(position);

        if(exerciseNameAndDate != null)
        {
            TextView exerciseName = convertView.findViewById(R.id.tvExerciseName);
            TextView exerciseDate = convertView.findViewById(R.id.tvExerciseDate);

            if (exerciseName != null)
            {
                exerciseName.setText(exerciseNameAndDate[0]);
            }

            if (exerciseDate != null)
            {
                exerciseDate.setText(exerciseNameAndDate[1]);
            }
        }

        return convertView;
    }
}
