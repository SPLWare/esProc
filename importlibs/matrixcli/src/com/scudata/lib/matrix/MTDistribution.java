package com.scudata.lib.matrix;

import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.FastMath;

public class MTDistribution extends MAbstractRealDistribution{
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    private final double degreesOfFreedom;
    private final double solverAbsoluteAccuracy;
    private final double factor;
    public static double inverseCumAccuracy = 0;

    public MTDistribution(double degreesOfFreedom) {
        this.degreesOfFreedom = degreesOfFreedom;
        solverAbsoluteAccuracy = inverseCumAccuracy;

        final double n = degreesOfFreedom;
        final double nPlus1Over2 = (n + 1) / 2;
        factor = Gamma.logGamma(nPlus1Over2) -
                0.5 * (FastMath.log(FastMath.PI) + FastMath.log(n)) -
                Gamma.logGamma(n / 2);
    }

    public double getDegreesOfFreedom() {
        return degreesOfFreedom;
    }

    public double density(double x) {
        return FastMath.exp(logDensity(x));
    }

    public double logDensity(double x) {
        final double n = degreesOfFreedom;
        final double nPlus1Over2 = (n + 1) / 2;
        return factor - nPlus1Over2 * FastMath.log(1 + x * x / n);
    }

    public double cumulativeProbability(double x) {
        double ret;
        if (x == 0) {
            ret = 0.5;
        } else {
            double t =
                    Beta.regularizedBeta(
                            degreesOfFreedom / (degreesOfFreedom + (x * x)),
                            0.5 * degreesOfFreedom,
                            0.5);
            if (x < 0.0) {
                ret = 0.5 * t;
            } else {
                ret = 1.0 - 0.5 * t;
            }
        }

        return ret;
    }



    protected double getSolverAbsoluteAccuracy() {
        return solverAbsoluteAccuracy;
    }

    public double getNumericalMean() {
        final double df = getDegreesOfFreedom();

        if (df > 1) {
            return 0;
        }

        return Double.NaN;
    }

    public double getNumericalVariance() {
        final double df = getDegreesOfFreedom();

        if (df > 2) {
            return df / (df - 2);
        }

        if (df > 1 && df <= 2) {
            return Double.POSITIVE_INFINITY;
        }

        return Double.NaN;
    }


    public double getSupportLowerBound() {
        return Double.NEGATIVE_INFINITY;
    }


    public double getSupportUpperBound() {
        return Double.POSITIVE_INFINITY;
    }

    public boolean isSupportLowerBoundInclusive() {
        return false;
    }

    public boolean isSupportUpperBoundInclusive() {
        return false;
    }

    public boolean isSupportConnected() {
        return true;
    }
}
