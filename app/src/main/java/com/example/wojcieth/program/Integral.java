package com.example.wojcieth.program;

/**
 * clase que calcula integral
 */
public class Integral
{
    /**
     * valor actual de la integral
     */
    private double value;

    public Integral()
    {
        value = 0;
    }

    /**
     * integración
     *
     * @param x1: coordenada x del primer punto
     * @param x2: coordenada x del segundo punto
     * @param y1: coordenada y del primer punto
     * @param y2: coordenada y del segundo punto
     * @return value: valor actual de la integral
     */
    public double integrate(double x1, double x2, double y1, double y2)
    {
        value += (y1+y2) * (x2-x1) * 0.5 * 0.001;
        return value;
    }

    public void setToZero()
    {
        value = 0;
    }
}
