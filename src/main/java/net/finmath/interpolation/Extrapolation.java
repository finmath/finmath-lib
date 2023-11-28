package net.finmath.interpolation;

public abstract class Extrapolation {
    public abstract double getValue(double[] points, double[] values, double x);
}
