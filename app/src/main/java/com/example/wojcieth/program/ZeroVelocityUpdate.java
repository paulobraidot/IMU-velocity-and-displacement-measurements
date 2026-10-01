package com.example.wojcieth.program;

/**
 * clase que detecta el estado inmovil (inactivo) del dispositivo
 */
public class ZeroVelocityUpdate
{
    /* valor umbral de muestras después del cual se considera que el dispositivo está inmovil (en reposo) */
    private final int STATIC_SAMPLES_THRESHOLD = 10;

    /* valor umbral del valor de aceleración después del cual el dispositivo debe considerarse quieto */
    private final double STATIC_ACCELERATION_THRESHOLD = 0.35;

    /* valor umbral del valor de velocidad angular después del cual el dispositivo debe considerarse en reposo */
    private final double STATIC_ANGULAR_VELOCITY_THRESHOLD = 0.15;

    /* contador de las muestras */
    private int samplesCount;

    public ZeroVelocityUpdate()
    {
        samplesCount = 0;
    }

    /**
     * detecta el estado estatico del dispositivo
     *
     * @param acceleration: aceleración
     * @return verdadero si el dispositivo está inactivo, falso en caso contrario
     */
    public boolean zeroVelocityUpdate(double acceleration[], double angularVelocity[])
    {
        if(Math.abs(acceleration[0]) <= STATIC_ACCELERATION_THRESHOLD &&
                Math.abs(acceleration[1]) <= STATIC_ACCELERATION_THRESHOLD &&
                Math.abs(acceleration[2]) <= STATIC_ACCELERATION_THRESHOLD &&
                Math.abs(angularVelocity[0]) <= STATIC_ANGULAR_VELOCITY_THRESHOLD &&
                Math.abs(angularVelocity[1]) <= STATIC_ANGULAR_VELOCITY_THRESHOLD &&
                Math.abs(angularVelocity[2]) <= STATIC_ANGULAR_VELOCITY_THRESHOLD)
        {
            samplesCount++;
        }
        else
        {
            samplesCount = 0;
        }

        return (samplesCount >= STATIC_SAMPLES_THRESHOLD);
    }

}
