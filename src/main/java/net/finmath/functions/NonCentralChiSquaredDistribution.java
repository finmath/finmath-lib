/*
 * Refactored from https://github.com/charles-cooper/idylfin/blob/master/src/com/opengamma/analytics/math/statistics/distribution/NonCentralChiSquaredDistribution.java
 * OpenGamma licensing has changed since this file was created, please check implications.
 *
 */

package net.finmath.functions;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.special.Gamma;


public class NonCentralChiSquaredDistribution {
    private final double _lambdaOverTwo;
    private final int _k;
    private final double _dofOverTwo;
    private final double _pStart;
    private final double _eps = 1e-16;

    /**
     * @param degrees       The number of degrees of freedom, positive
     * @param nonCentrality The non-centrality parameter, not negative
     */
    public NonCentralChiSquaredDistribution(final double degrees, final double nonCentrality) {
        Validate.isTrue(degrees > 0, "degrees of freedom must be > 0, have " + degrees);
        Validate.isTrue(nonCentrality >= 0, "non-centrality must be >= 0, have " + nonCentrality);
        _dofOverTwo = degrees / 2.0;
        _lambdaOverTwo = nonCentrality / 2.0;
        _k = (int) Math.round(_lambdaOverTwo);

        if (_lambdaOverTwo == 0) {
            _pStart = 0.0;
        } else {
            final double logP = -_lambdaOverTwo + _k * Math.log(_lambdaOverTwo) - Gamma.logGamma(_k + 1);
            _pStart = Math.exp(logP);
        }
    }

    private double getFraserApproxCDF(final double x) {
        final double s = Math.sqrt(_lambdaOverTwo * 2.0);
        final double mu = Math.sqrt(x);
        double z;
        if (Double.doubleToLongBits(mu) == Double.doubleToLongBits(s)) {
            z = (1 - _dofOverTwo * 2.0) / 2 / s;
        } else {
            z = mu - s - (_dofOverTwo * 2.0 - 1) / 2 * (Math.log(mu) - Math.log(s)) / (mu - s);
        }
        return NormalDistribution.cumulativeDistribution(z);
    }


    public double getCDF(final Double x) {
        Validate.notNull(x, "x");
        if (x < 0) {
            return 0.0;
        }

        if ((_dofOverTwo + _lambdaOverTwo) > 1000) {
            return getFraserApproxCDF(x);
        }

        double regGammaStart = 0;
        final double halfX = x / 2.0;
        final double logX = Math.log(halfX);
        try {
            regGammaStart = Gamma.regularizedGammaP(_dofOverTwo + _k, halfX);
        } catch (final org.apache.commons.math3.exception.MaxCountExceededException ex) {
            throw ex;
        }

        double sum = _pStart * regGammaStart;
        double oldSum = Double.NEGATIVE_INFINITY;
        double p = _pStart;
        double regGamma = regGammaStart;
        double temp;
        int i = _k;

        // first add terms below _k
        while (i > 0 && Math.abs(sum - oldSum) / sum > _eps) {
            i--;
            p *= (i + 1) / _lambdaOverTwo;
            temp = (_dofOverTwo + i) * logX - halfX - Gamma.logGamma(_dofOverTwo + i + 1);
            regGamma += Math.exp(temp);
            oldSum = sum;
            sum += p * regGamma;
        }

        p = _pStart;
        regGamma = regGammaStart;
        oldSum = Double.NEGATIVE_INFINITY;
        i = _k;
        while (Math.abs(sum - oldSum) / sum > _eps) {
            i++;
            p *= _lambdaOverTwo / i;
            temp = (_dofOverTwo + i - 1) * logX - halfX - Gamma.logGamma(_dofOverTwo + i);
            regGamma -= Math.exp(temp);
            oldSum = sum;
            sum += p * regGamma;
        }

        return sum;
    }

    /**
     * {@inheritDoc}
     *
     * @return Not supported
     * @throws NotImplementedException
     */
    public double getInverseCDF(final Double p) {
        throw new NotImplementedException("NCX2: Inverse CDF not implemented.");
    }

    /**
     * {@inheritDoc}
     *
     * @return Not supported
     * @throws NotImplementedException
     */
    public double getPDF(final Double x) {
        throw new NotImplementedException("NCX2: PDF not implemented.");
    }

    /**
     * {@inheritDoc}
     *
     * @return Not supported
     * @throws NotImplementedException
     */
    public double nextRandom() {
        throw new NotImplementedException("NCX2: Random number generation not implemented.");
    }

    /**
     * @return The number of degrees of freedom
     */
    public double getDegrees() {
        return _dofOverTwo * 2.0;
    }

    /**
     * @return The non-centrality parameter
     */
    public double getNonCentrality() {
        return _lambdaOverTwo * 2.0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(_dofOverTwo);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(_lambdaOverTwo);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NonCentralChiSquaredDistribution other = (NonCentralChiSquaredDistribution) obj;
        if (Double.doubleToLongBits(_dofOverTwo) != Double.doubleToLongBits(other._dofOverTwo)) {
            return false;
        }
        return Double.doubleToLongBits(_lambdaOverTwo) == Double.doubleToLongBits(other._lambdaOverTwo);
    }


}
