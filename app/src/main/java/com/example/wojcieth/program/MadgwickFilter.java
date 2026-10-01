package com.example.wojcieth.program;

import android.util.Log;

/**
 * clase que implementa el filtro madgwick
 * basado en el trabajo:
 * S. O. H. Madgwick, "An efficient orientation filter for inertial and inertial/magnetic sensor arrays", 30/04/2010
 * @source https://www.enib.fr/~kerhoas/rescapt_madgwick.html
 */
public class MadgwickFilter
{

    /** frecuencia de muestreo */
    private double sampleFreq = 10.0f;
    /** ganancia proporcional*/
    private double betaDef = 0.5f;
    /** Kp ~ 2 * ganancia proporcional */
    private double beta = betaDef;
    /** representación del cuaternión del entorno del sensor en relación con el entorno auxiliar */
    private double q0 = 1.0f, q1 = 0.0f, q2 = 0.0f, q3 = 0.0f;

    private double R11;
    private double R21;
    private double R31;
    private double R32;
    private double R33;

    private int count;

    public MadgwickFilter()
    {
        count = 0;
    }


    /**
     * calcula una iteración del filtro
     *
     * @param ax: Medición del acelerómetro en el eje x
     * @param ay: Medición del acelerómetro en el eje y
     * @param az: Medición del acelerómetro en el eje z
     * @param gx: Medición del giroscopio en el eje x
     * @param gy: Medición del giroscopio en el eje y
     * @param gz: Medición del giroscopio en el eje z
     * @param mx: Medición del magnetómetro en el eje x
     * @param my: Medición del magnetómetro en el eje y
     * @param mz: Medición del magnetómetro en el eje z
     */
    public void MadgwickAHRSupdate(double ax, double ay, double az, double gx, double gy, double gz,  double mx, double my, double mz) {
        double recipNorm;
        double s0, s1, s2, s3;
        double qDot1, qDot2, qDot3, qDot4;
        double hx, hy;
        double _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

        // algoritmo de unidad de medición inercial cuando los datos del magnetómetro son cero o incorrectos
        if((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f)) {
            MadgwickAHRSupdateIMU(gx, gy, gz, ax, ay, az);
            return;
        }

        // tasa de cambio del cuaternión desde el giroscopio
        qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
        qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
        qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
        qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

        // calcula la realimentación cuando la medida del acelerómetro es correcta
        if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

            // normalización de las mediciones del acelerómetro
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // normalización de las mediciones del magnetómetro
            recipNorm = invSqrt(mx * mx + my * my + mz * mz);
            mx *= recipNorm;
            my *= recipNorm;
            mz *= recipNorm;

            // variables adicionales para no contarlas dos veces
            _2q0mx = 2.0f * q0 * mx;
            _2q0my = 2.0f * q0 * my;
            _2q0mz = 2.0f * q0 * mz;
            _2q1mx = 2.0f * q1 * mx;
            _2q0 = 2.0f * q0;
            _2q1 = 2.0f * q1;
            _2q2 = 2.0f * q2;
            _2q3 = 2.0f * q3;
            _2q0q2 = 2.0f * q0 * q2;
            _2q2q3 = 2.0f * q2 * q3;
            q0q0 = q0 * q0;
            q0q1 = q0 * q1;
            q0q2 = q0 * q2;
            q0q3 = q0 * q3;
            q1q1 = q1 * q1;
            q1q2 = q1 * q2;
            q1q3 = q1 * q3;
            q2q2 = q2 * q2;
            q2q3 = q2 * q3;
            q3q3 = q3 * q3;

            // dirección de referencia del campo magnético terrestre
            hx = mx * q0q0 - _2q0my * q3 + _2q0mz * q2 + mx * q1q1 + _2q1 * my * q2 + _2q1 * mz * q3 - mx * q2q2 - mx * q3q3;
            hy = _2q0mx * q3 + my * q0q0 - _2q0mz * q1 + _2q1mx * q2 - my * q1q1 + my * q2q2 + _2q2 * mz * q3 - my * q3q3;
            _2bx = Math.sqrt(hx * hx + hy * hy);
            _2bz = -_2q0mx * q2 + _2q0my * q1 + mz * q0q0 + _2q1mx * q3 - mz * q1q1 + _2q2 * my * q3 - mz * q2q2 + mz * q3q3;
            _4bx = 2.0f * _2bx;
            _4bz = 2.0f * _2bz;

            // paso correctivo del algoritmo decente de gradiente
            s0 = -_2q2 * (2.0f * q1q3 - _2q0q2 - ax) + _2q1 * (2.0f * q0q1 + _2q2q3 - ay) - _2bz * q2 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * q3 + _2bz * q1) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * q2 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s1 = _2q3 * (2.0f * q1q3 - _2q0q2 - ax) + _2q0 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * q1 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + _2bz * q3 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * q2 + _2bz * q0) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * q3 - _4bz * q1) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s2 = -_2q0 * (2.0f * q1q3 - _2q0q2 - ax) + _2q3 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * q2 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + (-_4bx * q2 - _2bz * q0) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * q1 + _2bz * q3) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * q0 - _4bz * q2) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            s3 = _2q1 * (2.0f * q1q3 - _2q0q2 - ax) + _2q2 * (2.0f * q0q1 + _2q2q3 - ay) + (-_4bx * q3 + _2bz * q1) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * q0 + _2bz * q2) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * q1 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalizar la magnitud del paso
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // aplica paso de realimentación
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // acumula la tasa de cambio del cuaternión para producir el cuaternión
        q0 += qDot1 * (1.0f / sampleFreq);
        q1 += qDot2 * (1.0f / sampleFreq);
        q2 += qDot3 * (1.0f / sampleFreq);
        q3 += qDot4 * (1.0f / sampleFreq);

        // normalización de cuaterniones
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;


        R11 = 2.*q0*q0 -1 +2.*q1*q1;
        R21 = 2.*(q1*q2 - q0*q3);
        R31 = 2.*(q1*q3 + q0*q2);
        R32 = 2.*(q2*q3 - q0*q1);
        R33 = 2.*q0*q0 -1 +2.*q3*q3;

        if(count < 50)
        {
            count++;
        }
        else
        {
            beta = 0.5;
        }
    }

    /**
     * algoritmo de actualización de unidad de medición inercial
     *
     * @param ax: medición del acelerómetro en el eje y
     * @param ay: medición del acelerómetro en el eje y
     * @param az: medición del acelerómetro en el eje z
     * @param gx: medición del giroscopio en el eje x
     * @param gy: medición del giroscopio en el eje y
     * @param gz: medición del giroscopio en el eje z
     */
    public void MadgwickAHRSupdateIMU(double gx, double gy, double gz, double ax, double ay, double az) {
        double recipNorm;
        double s0, s1, s2, s3;
        double qDot1, qDot2, qDot3, qDot4;
        double _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

        // tasa de cambio del cuaternión desde el giroscopio
        qDot1 = 0.5f * (-q1 * gx - q2 * gy - q3 * gz);
        qDot2 = 0.5f * (q0 * gx + q2 * gz - q3 * gy);
        qDot3 = 0.5f * (q0 * gy - q1 * gz + q3 * gx);
        qDot4 = 0.5f * (q0 * gz + q1 * gy - q2 * gx);

        // calcula la realimentación solo si la medición del acelerómetro es válida (evita "un no numero" en la normalización del acelerómetro)
        if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

            // normaliza la medición del acelerómetro
            recipNorm = invSqrt(ax * ax + ay * ay + az * az);
            ax *= recipNorm;
            ay *= recipNorm;
            az *= recipNorm;

            // variables auxiliares para evitar aritmética repetida
            _2q0 = 2.0f * q0;
            _2q1 = 2.0f * q1;
            _2q2 = 2.0f * q2;
            _2q3 = 2.0f * q3;
            _4q0 = 4.0f * q0;
            _4q1 = 4.0f * q1;
            _4q2 = 4.0f * q2;
            _8q1 = 8.0f * q1;
            _8q2 = 8.0f * q2;
            q0q0 = q0 * q0;
            q1q1 = q1 * q1;
            q2q2 = q2 * q2;
            q3q3 = q3 * q3;

            // paso correctivo del algoritmo decente de gradiente
            s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
            s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * q1 - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
            s2 = 4.0f * q0q0 * q2 + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
            s3 = 4.0f * q1q1 * q3 - _2q1 * ax + 4.0f * q2q2 * q3 - _2q2 * ay;
            recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normaliza la magnitud del paso
            s0 *= recipNorm;
            s1 *= recipNorm;
            s2 *= recipNorm;
            s3 *= recipNorm;

            // aplica paso de realimentación
            qDot1 -= beta * s0;
            qDot2 -= beta * s1;
            qDot3 -= beta * s2;
            qDot4 -= beta * s3;
        }

        // acumula la tasa de cambio del cuaternión para producir el cuaternión
        q0 += qDot1 * (1.0f / sampleFreq);
        q1 += qDot2 * (1.0f / sampleFreq);
        q2 += qDot3 * (1.0f / sampleFreq);
        q3 += qDot4 * (1.0f / sampleFreq);

        // normaliza cuaternión
        recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        q0 *= recipNorm;
        q1 *= recipNorm;
        q2 *= recipNorm;
        q3 *= recipNorm;

        R11 = 2.*q0*q0 - 1 + 2.*q1*q1;
        R21 = 2.*(q1*q2 - q0*q3);
        R31 = 2.*(q1*q3 + q0*q2);
        R32 = 2.*(q2*q3 - q0*q1);
        R33 = 2.*q0*q0 - 1 + 2.*q3*q3;
    }

    /**
     * raíz cuadrada inversa rápida
     *
     * @param x: valor
     * @return x: resultado
     */

    private static double invSqrt(double x) {
        double xhalf = 0.5d * x;
        long i = Double.doubleToLongBits(x);
        i = 0x5fe6ec85e7de30daL - (i >> 1);
        x = Double.longBitsToDouble(i);
        x *= (1.5d - xhalf * x * x);
        return x;
    }

    /**
     * devuelve el ángulo de balanceo
     *
     * @return roll
     */
    public double getRoll()
    {
        double phi = Math.atan2(R32, R33 );

        return phi*180/Math.PI;
    }

    /**
     * devuelve el ángulo de inclinación
     *
     * @return pitch
     */
    public double getPitch()
    {
        double theta = -Math.atan(R31 / Math.sqrt(1-R31*R31) );
        return theta*180/Math.PI;
    }

    /**
     * devuelve de ángulo de deriva
     *
     * @return yaw
     */
    public double getYaw()
    {
        double psi = Math.atan2(R21, R11 );
        return psi*180/Math.PI;
    }

    /**
     * devuelve cuaterniones
     *
     * @return quaternions
     */
    public double[] getQuaternions()
    {
        return new double[]{q0, q1, q2, q3};
    }

//    // CONSTANTES
//    /** periodo de muestreo [s] */
//    private final double deltat = 0.005f;
//    /** error de medición del giroscopio [rad/s] */
//    private final double gyroMeasError = 3.14159265358979 * (500.0d / 180.0d); // (mostrado como 5 [deg/s])
//    /** error de medición del giroscopio [rad/s/s] */
//    private final double gyroMeasDrift = 3.14159265358979 * (20d / 180.0d); // (mostrado como 0.2f [deg/s/s])
//    /** calcula beta */
//    private final double beta = Math.sqrt(3.0d / 4.0d) * gyroMeasError;
//    /** calcula zeta */
//    private final double zeta = Math.sqrt(3.0d / 4.0d) * gyroMeasDrift;
//
//    // VARIABLES
//    /** mediciones del acelerómetro */
//    private double a_x, a_y, a_z;
//    /** mediciones de giroscopio */
//    private double w_x, w_y, w_z;
//    /** mediciones del magnetómetro */
//    private double m_x, m_y, m_z;
//    /** orientación estimada del cuaternión con condiciones iniciales */
//    private double SEq_1 = 1, SEq_2 = 0, SEq_3 = 0, SEq_4 = 0;
//    /** dirección del campo de referencia en el sistema terrestre */
//    private double b_x = 1, b_z = 0;
//    /** sesgo estimado del giroscopio */
//    private float w_bx = 0, w_by = 0, w_bz = 0;
//
//
//    double R11;
//    double R21;
//    double R31;
//    double R32;
//    double R33;




    /**
     * calcula una iteración de filtro
     *
     * @param w_x: medición del giroscopio en el eje x
     * @param w_y: medición del giroscopio en el eje y
     * @param w_z: medición del giroscopio en el eje z
     * @param a_x: medición del acelerómetro en el eje x
     * @param a_y: medición del acelerómetro en el eje y
     * @param a_z: medición del acelerómetro en el eje z
     * @param m_x: medición del magnetómetro en el eje x
     * @param m_y: medición del magnetómetro en el eje y
     * @param m_z: medición del magnetómetro en el eje z
     */
//    public void MadgwickAHRSupdate (double a_x, double a_y, double a_z, double w_x, double w_y, double w_z, double m_x, double m_y, double m_z)
//    {
//        double norm; // valor normalizador
//        double SEqDot_omega_1, SEqDot_omega_2, SEqDot_omega_3, SEqDot_omega_4; // variación de cuaternión desde giroscopio
//        double f_1, f_2, f_3, f_4, f_5, f_6; // funciones objetivo (propósito)
//        double J_11or24, J_12or23, J_13or22, J_14or21, J_32, J_33, // jacobianos objetivo (propósito)
//                J_41, J_42, J_43, J_44, J_51, J_52, J_53, J_54, J_61, J_62, J_63, J_64;
//        double SEqHatDot_1, SEqHatDot_2, SEqHatDot_3, SEqHatDot_4; // dirección estimada del error del giroscopio
//        double w_err_x, w_err_y, w_err_z; // dirección estimada del error del giroscopio (angular)
//        double h_x, h_y, h_z; // flujo calculado en coordenadas terrestres
//
//        // variables auxiliares para evitar cálculos múltiples
//        double halfSEq_1 = 0.5f * SEq_1;
//        double halfSEq_2 = 0.5f * SEq_2;
//        double halfSEq_3 = 0.5f * SEq_3;
//        double halfSEq_4 = 0.5f * SEq_4;
//        double twoSEq_1 = 2.0f * SEq_1;
//        double twoSEq_2 = 2.0f * SEq_2;
//        double twoSEq_3 = 2.0f * SEq_3;
//        double twoSEq_4 = 2.0f * SEq_4;
//        double twob_x = 2.0f * b_x;
//        double twob_z = 2.0f * b_z;
//        double twob_xSEq_1 = 2.0f * b_x * SEq_1;
//        double twob_xSEq_2 = 2.0f * b_x * SEq_2;
//        double twob_xSEq_3 = 2.0f * b_x * SEq_3;
//        double twob_xSEq_4 = 2.0f * b_x * SEq_4;
//        double twob_zSEq_1 = 2.0f * b_z * SEq_1;
//        double twob_zSEq_2 = 2.0f * b_z * SEq_2;
//        double twob_zSEq_3 = 2.0f * b_z * SEq_3;
//        double twob_zSEq_4 = 2.0f * b_z * SEq_4;
//        double SEq_1SEq_2;
//        double SEq_1SEq_3 = SEq_1 * SEq_3;
//        double SEq_1SEq_4;
//        double SEq_2SEq_3;
//        double SEq_2SEq_4 = SEq_2 * SEq_4;
//        double SEq_3SEq_4;
//        double twom_x = 2.0f * m_x;
//        double twom_y = 2.0f * m_y;
//        double twom_z = 2.0f * m_z;
//
//
//        // normaliza las mediciones del acelerómetro
//        norm = Math.sqrt(a_x * a_x + a_y * a_y + a_z * a_z);
//        a_x /= norm;
//        a_y /= norm;
//        a_z /= norm;
//
//        // normaliza las mediciones del magnetómetro
//        norm = Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
//        m_x /= norm;
//        m_y /= norm;
//        m_z /= norm;
//
//        // calcula funciones objetivas y jacobianas
//        f_1 = twoSEq_2 * SEq_4 - twoSEq_1 * SEq_3 - a_x;
//        f_2 = twoSEq_1 * SEq_2 + twoSEq_3 * SEq_4 - a_y;
//        f_3 = 1.0f - twoSEq_2 * SEq_2 - twoSEq_3 * SEq_3 - a_z;
//        f_4 = twob_x * (0.5f - SEq_3 * SEq_3 - SEq_4 * SEq_4) + twob_z * (SEq_2SEq_4 - SEq_1SEq_3) - m_x;
//        f_5 = twob_x * (SEq_2 * SEq_3 - SEq_1 * SEq_4) + twob_z * (SEq_1 * SEq_2 + SEq_3 * SEq_4) - m_y;
//        f_6 = twob_x * (SEq_1SEq_3 + SEq_2SEq_4) + twob_z * (0.5f - SEq_2 * SEq_2 - SEq_3 * SEq_3) - m_z;
//        J_11or24 = twoSEq_3; // negado en la multiplicación de matrices
//        J_12or23 = 2.0f * SEq_4;
//        J_13or22 = twoSEq_1; // negado en la multiplicación de matrices
//        J_14or21 = twoSEq_2;
//        J_32 = 2.0f * J_14or21; // negado en la multiplicación de matrices
//        J_33 = 2.0f * J_11or24; // negado en la multiplicación de matrices
//        J_41 = twob_zSEq_3; // negado en la multiplicación de matrices
//        J_42 = twob_zSEq_4;
//        J_43 = 2.0f * twob_xSEq_3 + twob_zSEq_1; // negado en la multiplicación de matrices
//        J_44 = 2.0f * twob_xSEq_4 - twob_zSEq_2; // negado en la multiplicación de matrices
//        J_51 = twob_xSEq_4 - twob_zSEq_2; // negado en la multiplicación de matrices
//        J_52 = twob_xSEq_3 + twob_zSEq_1;
//        J_53 = twob_xSEq_2 + twob_zSEq_4;
//        J_54 = twob_xSEq_1 - twob_zSEq_3; // negado en la multiplicación de matrices
//        J_61 = twob_xSEq_3;
//        J_62 = twob_xSEq_4 - 2.0f * twob_zSEq_2;
//        J_63 = twob_xSEq_1 - 2.0f * twob_zSEq_3;
//        J_64 = twob_xSEq_2;
//
//        // calcula gradiente
//        SEqHatDot_1 = J_14or21 * f_2 - J_11or24 * f_1 - J_41 * f_4 - J_51 * f_5 + J_61 * f_6;
//        SEqHatDot_2 = J_12or23 * f_1 + J_13or22 * f_2 - J_32 * f_3 + J_42 * f_4 + J_52 * f_5 + J_62 * f_6;
//        SEqHatDot_3 = J_12or23 * f_2 - J_33 * f_3 - J_13or22 * f_1 - J_43 * f_4 + J_53 * f_5 + J_63 * f_6;
//        SEqHatDot_4 = J_14or21 * f_1 + J_11or24 * f_2 - J_44 * f_4 - J_54 * f_5 + J_64 * f_6;
//
//        // normaliza gradiente para estimar la dirección del error del giroscopio
//        norm = Math.sqrt(SEqHatDot_1 * SEqHatDot_1 + SEqHatDot_2 * SEqHatDot_2 + SEqHatDot_3 * SEqHatDot_3 + SEqHatDot_4 * SEqHatDot_4);
//        SEqHatDot_1 = SEqHatDot_1 / norm;
//        SEqHatDot_2 = SEqHatDot_2 / norm;
//        SEqHatDot_3 = SEqHatDot_3 / norm;
//        SEqHatDot_4 = SEqHatDot_4 / norm;
//
//        // estima la dirección angular del error del giroscopio
//        w_err_x = twoSEq_1 * SEqHatDot_2 - twoSEq_2 * SEqHatDot_1 - twoSEq_3 * SEqHatDot_4 + twoSEq_4 * SEqHatDot_3;
//        w_err_y = twoSEq_1 * SEqHatDot_3 + twoSEq_2 * SEqHatDot_4 - twoSEq_3 * SEqHatDot_1 - twoSEq_4 * SEqHatDot_2;
//        w_err_z = twoSEq_1 * SEqHatDot_4 - twoSEq_2 * SEqHatDot_3 + twoSEq_3 * SEqHatDot_2 - twoSEq_4 * SEqHatDot_1;
//
//        // calcula y elimina el sesgo del giroscopio
//        w_bx += w_err_x * deltat * zeta;
//        w_by += w_err_y * deltat * zeta;
//        w_bz += w_err_z * deltat * zeta;
//        w_x -= w_bx;
//        w_y -= w_by;
//        w_z -= w_bz;
//
//        // calcula la variación en el cuaternión medido por el giroscopio
//        SEqDot_omega_1 = -halfSEq_2 * w_x - halfSEq_3 * w_y - halfSEq_4 * w_z;
//        SEqDot_omega_2 = halfSEq_1 * w_x + halfSEq_3 * w_z - halfSEq_4 * w_y;
//        SEqDot_omega_3 = halfSEq_1 * w_y - halfSEq_2 * w_z + halfSEq_4 * w_x;
//        SEqDot_omega_4 = halfSEq_1 * w_z + halfSEq_2 * w_y - halfSEq_3 * w_x;
//
//        // acumula la variación del cuaternión para producir el cuaternión
//        SEq_1 += (SEqDot_omega_1 - (beta * SEqHatDot_1)) * deltat;
//        SEq_2 += (SEqDot_omega_2 - (beta * SEqHatDot_2)) * deltat;
//        SEq_3 += (SEqDot_omega_3 - (beta * SEqHatDot_3)) * deltat;
//        SEq_4 += (SEqDot_omega_4 - (beta * SEqHatDot_4)) * deltat;
//
//        // normaliza cuaterniones
//        norm = Math.sqrt(SEq_1 * SEq_1 + SEq_2 * SEq_2 + SEq_3 * SEq_3 + SEq_4 * SEq_4);
//        SEq_1 /= norm;
//        SEq_2 /= norm;
//        SEq_3 /= norm;
//        SEq_4 /= norm;
//
//        // obtiene campo en el sistema terrestre
//        SEq_1SEq_2 = SEq_1 * SEq_2; // recalcula datos adicionales
//        SEq_1SEq_3 = SEq_1 * SEq_3;
//        SEq_1SEq_4 = SEq_1 * SEq_4;
//        SEq_3SEq_4 = SEq_3 * SEq_4;
//        SEq_2SEq_3 = SEq_2 * SEq_3;
//        SEq_2SEq_4 = SEq_2 * SEq_4;
//        h_x = twom_x * (0.5f - SEq_3 * SEq_3 - SEq_4 * SEq_4) + twom_y * (SEq_2SEq_3 - SEq_1SEq_4) + twom_z * (SEq_2SEq_4 + SEq_1SEq_3);
//        h_y = twom_x * (SEq_2SEq_3 + SEq_1SEq_4) + twom_y * (0.5f - SEq_2 * SEq_2 - SEq_4 * SEq_4) + twom_z * (SEq_3SEq_4 - SEq_1SEq_2);
//        h_z = twom_x * (SEq_2SEq_4 - SEq_1SEq_3) + twom_y * (SEq_3SEq_4 + SEq_1SEq_2) + twom_z * (0.5f - SEq_2 * SEq_2 - SEq_3 * SEq_3);
//
//        // normaliza campo para tener componentes solo en x y z
//        b_x = Math.sqrt((h_x * h_x) + (h_y * h_y));
//        b_z = h_z;

        // variables del sistema local
  /*     double norm; // valor normalizador
        double SEqDot_omega_1, SEqDot_omega_2, SEqDot_omega_3, SEqDot_omega_4; // Tasa de cuaterniones de elementos de giroscopios.
        double f_1, f_2, f_3, f_4, f_5, f_6; // elementos de la función objetivo
        double J_11or24, J_12or23, J_13or22, J_14or21, J_32, J_33, // elementos jacobianos de la función objetivo 
                J_41, J_42, J_43, J_44, J_51, J_52, J_53, J_54, J_61, J_62, J_63, J_64; //
        double SEqHatDot_1, SEqHatDot_2, SEqHatDot_3, SEqHatDot_4; // dirección estimada del error del giroscopio
        double w_err_x, w_err_y, w_err_z; // dirección estimada del error del giroscopio (angular)
        double h_x, h_y, h_z; // flujo calculado en el marco de la tierra
        // variables auxiliares para evitar cálculos repetidos
        double halfSEq_1 = 0.5 * SEq_1;
        double halfSEq_2 = 0.5 * SEq_2;
        double halfSEq_3 = 0.5 * SEq_3;
        double halfSEq_4 = 0.5 * SEq_4;
        double twoSEq_1 = 2.0 * SEq_1;
        double twoSEq_2 = 2.0 * SEq_2;
        double twoSEq_3 = 2.0 * SEq_3;
        double twoSEq_4 = 2.0 * SEq_4;
        double twob_x = 2.0 * b_x;
        double twob_z = 2.0 * b_z;
        double twob_xSEq_1 = 2.0 * b_x * SEq_1;
        double twob_xSEq_2 = 2.0 * b_x * SEq_2;
        double twob_xSEq_3 = 2.0 * b_x * SEq_3;
        double twob_xSEq_4 = 2.0 * b_x * SEq_4;
        double twob_zSEq_1 = 2.0 * b_z * SEq_1;
        double twob_zSEq_2 = 2.0* b_z * SEq_2;
        double twob_zSEq_3 = 2.0 * b_z * SEq_3;
        double twob_zSEq_4 = 2.0 * b_z * SEq_4;
        double SEq_1SEq_2;
        double SEq_1SEq_3 = SEq_1 * SEq_3;
        double SEq_1SEq_4;
        double SEq_2SEq_3;
        double SEq_2SEq_4 = SEq_2 * SEq_4;
        double SEq_3SEq_4;
        // normaliza la medición del acelerómetro
        norm = Math.sqrt(a_x * a_x + a_y * a_y + a_z * a_z);
        a_x /= norm;
        a_y /= norm;
        a_z /= norm;
        // normaliza la medición del magnetómetro
        norm = Math.sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
        m_x /= norm;
        m_y /= norm;
        m_z /= norm;
        double twom_x = 2.0 * m_x;
        double twom_y = 2.0 * m_y;
        double twom_z = 2.0 * m_z;
        // calcula la función objetivo y jacobianos
        f_1 = twoSEq_2 * SEq_4 - twoSEq_1 * SEq_3 - a_x;
        f_2 = twoSEq_1 * SEq_2 + twoSEq_3 * SEq_4 - a_y;
        f_3 = 1.0 - twoSEq_2 * SEq_2 - twoSEq_3 * SEq_3 - a_z;
        f_4 = twob_x * (0.5 - SEq_3 * SEq_3 - SEq_4 * SEq_4) + twob_z * (SEq_2SEq_4 - SEq_1SEq_3) - m_x;
        f_5 = twob_x * (SEq_2 * SEq_3 - SEq_1 * SEq_4) + twob_z * (SEq_1 * SEq_2 + SEq_3 * SEq_4) - m_y;
        f_6 = twob_x * (SEq_1SEq_3 + SEq_2SEq_4) + twob_z * (0.5 - SEq_2 * SEq_2 - SEq_3 * SEq_3) - m_z;
        J_11or24 = twoSEq_3; // negado en la multiplicación de matrices
        J_12or23 = 2.0 * SEq_4;
        J_13or22 = twoSEq_1; // negado en la multiplicación de matrices
        J_14or21 = twoSEq_2;
        J_32 = 2.0 * J_14or21; // negado en la multiplicación de matrices
        J_33 = 2.0 * J_11or24; // negado en la multiplicación de matrices
        J_41 = twob_zSEq_3; // negado en la multiplicación de matrices
        J_42 = twob_zSEq_4;
        J_43 = 2.0 * twob_xSEq_3 + twob_zSEq_1; // negado en la multiplicación de matrices
        J_44 = 2.0 * twob_xSEq_4 - twob_zSEq_2; // negado en la multiplicación de matrices
        J_51 = twob_xSEq_4 - twob_zSEq_2; // negado en la multiplicación de matrices
        J_52 = twob_xSEq_3 + twob_zSEq_1;
        J_53 = twob_xSEq_2 + twob_zSEq_4;
        J_54 = twob_xSEq_1 - twob_zSEq_3; // negado en la multiplicación de matrices
        J_61 = twob_xSEq_3;
        J_62 = twob_xSEq_4 - 2.0 * twob_zSEq_2;
        J_63 = twob_xSEq_1 - 2.0 * twob_zSEq_3;
        J_64 = twob_xSEq_2;
        // calcula el gradiente (multiplicación de matrices)
        SEqHatDot_1 = J_14or21 * f_2 - J_11or24 * f_1 - J_41 * f_4 - J_51 * f_5 + J_61 * f_6;
        SEqHatDot_2 = J_12or23 * f_1 + J_13or22 * f_2 - J_32 * f_3 + J_42 * f_4 + J_52 * f_5 + J_62 * f_6;
        SEqHatDot_3 = J_12or23 * f_2 - J_33 * f_3 - J_13or22 * f_1 - J_43 * f_4 + J_53 * f_5 + J_63 * f_6;
        SEqHatDot_4 = J_14or21 * f_1 + J_11or24 * f_2 - J_44 * f_4 - J_54 * f_5 + J_64 * f_6;
        // normaliza el gradiente para estimar la dirección del error del giroscopio
        norm = Math.sqrt(SEqHatDot_1 * SEqHatDot_1 + SEqHatDot_2 * SEqHatDot_2 + SEqHatDot_3 * SEqHatDot_3 + SEqHatDot_4 * SEqHatDot_4);
        SEqHatDot_1 = SEqHatDot_1 / norm;
        SEqHatDot_2 = SEqHatDot_2 / norm;
        SEqHatDot_3 = SEqHatDot_3 / norm;
        SEqHatDot_4 = SEqHatDot_4 / norm;
        // calcula la dirección angular estimada del error del giroscopio
        w_err_x = twoSEq_1 * SEqHatDot_2 - twoSEq_2 * SEqHatDot_1 - twoSEq_3 * SEqHatDot_4 + twoSEq_4 * SEqHatDot_3;
        w_err_y = twoSEq_1 * SEqHatDot_3 + twoSEq_2 * SEqHatDot_4 - twoSEq_3 * SEqHatDot_1 - twoSEq_4 * SEqHatDot_2;
        w_err_z = twoSEq_1 * SEqHatDot_4 - twoSEq_2 * SEqHatDot_3 + twoSEq_3 * SEqHatDot_2 - twoSEq_4 * SEqHatDot_1;
        // calcula y elimina el descentramiento del giroscopio
        w_bx += w_err_x * deltat * zeta;
        w_by += w_err_y * deltat * zeta;
        w_bz += w_err_z * deltat * zeta;
        w_x -= w_bx;
        w_y -= w_by;
        w_z -= w_bz;
        // calcula la tasa de cambio de cuaterniones medida por giroscopios
        SEqDot_omega_1 = -halfSEq_2 * w_x - halfSEq_3 * w_y - halfSEq_4 * w_z;
        SEqDot_omega_2 = halfSEq_1 * w_x + halfSEq_3 * w_z - halfSEq_4 * w_y;
        SEqDot_omega_3 = halfSEq_1 * w_y - halfSEq_2 * w_z + halfSEq_4 * w_x;
        SEqDot_omega_4 = halfSEq_1 * w_z + halfSEq_2 * w_y - halfSEq_3 * w_x;
        // calcula y acumula la variacion estimada de cuaterniones
        SEq_1 += (SEqDot_omega_1 - (beta * SEqHatDot_1)) * deltat;
        SEq_2 += (SEqDot_omega_2 - (beta * SEqHatDot_2)) * deltat;
        SEq_3 += (SEqDot_omega_3 - (beta * SEqHatDot_3)) * deltat;
        SEq_4 += (SEqDot_omega_4 - (beta * SEqHatDot_4)) * deltat;
        // normaliza el cuaternión
        norm = Math.sqrt(SEq_1 * SEq_1 + SEq_2 * SEq_2 + SEq_3 * SEq_3 + SEq_4 * SEq_4);
        SEq_1 /= norm;
        SEq_2 /= norm;
        SEq_3 /= norm;
        SEq_4 /= norm;
        // calcula el flujo en el marco de la tierra
        SEq_1SEq_2 = SEq_1 * SEq_2; // recalcula variables axuliares
        SEq_1SEq_3 = SEq_1 * SEq_3;
        SEq_1SEq_4 = SEq_1 * SEq_4;
        SEq_3SEq_4 = SEq_3 * SEq_4;
        SEq_2SEq_3 = SEq_2 * SEq_3;
        SEq_2SEq_4 = SEq_2 * SEq_4;
        h_x = twom_x * (0.5 - SEq_3 * SEq_3 - SEq_4 * SEq_4) + twom_y * (SEq_2SEq_3 - SEq_1SEq_4) + twom_z * (SEq_2SEq_4 + SEq_1SEq_3);
        h_y = twom_x * (SEq_2SEq_3 + SEq_1SEq_4) + twom_y * (0.5 - SEq_2 * SEq_2 - SEq_4 * SEq_4) + twom_z * (SEq_3SEq_4 - SEq_1SEq_2);
        h_z = twom_x * (SEq_2SEq_4 - SEq_1SEq_3) + twom_y * (SEq_3SEq_4 + SEq_1SEq_2) + twom_z * (0.5 - SEq_2 * SEq_2 - SEq_3 * SEq_3);
        // normaliza el flujo para que tenga solo componentes en x y z
        b_x = Math.sqrt((h_x * h_x) + (h_y * h_y));
        b_z = h_z;



        R11 = 2.*SEq_1*SEq_1 -1 +2.*SEq_2*SEq_2;
        R21 = 2.*(SEq_2*SEq_3 - SEq_1*SEq_3);
        R31 = 2.*(SEq_2*SEq_4 + SEq_1*SEq_3);
        R32 = 2.*(SEq_3*SEq_4 - SEq_1*SEq_2);
        R33 = 2.*SEq_1*SEq_1 -1 +2.*SEq_4*SEq_4;
    }



    public double getRoll()
    {
        double phi = Math.atan2(R32, R33 );
        return phi*180/Math.PI;
    }

    public double getPitch()
    {
        double theta = -Math.atan(R31 / Math.sqrt(1-R31*R31) );
        return theta*180/Math.PI;
    }

    public double getYaw()
    {
        double psi = Math.atan2(R21, R11 );
        return psi*180/Math.PI;
    }*/

}
