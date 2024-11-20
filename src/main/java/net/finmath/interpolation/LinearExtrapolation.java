package net.finmath.interpolation;

public class LinearExtrapolation extends Extrapolation {
    @Override
    public double getValue(double[] points, double[] values, double x) {
        return values[0] + (values[1] - values[0]) / (points[1] - points[0]) * (x - points[0]);
    }
}

