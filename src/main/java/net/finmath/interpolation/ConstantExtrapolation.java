package net.finmath.interpolation;

public class ConstantExtrapolation extends Extrapolation {
    @Override
    public double getValue(double[] points, double[] values, double x) {
        return values[0];
    }
}
