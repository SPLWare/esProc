package com.scudata.lib.math;

import org.apache.commons.math3.special.Beta;
import org.apache.commons.math3.util.FastMath;

public class MFDistribution extends MAbstractRealDistribution {
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    private final double numeratorDegreesOfFreedom;
    private final double denominatorDegreesOfFreedom;
    private final double solverAbsoluteAccuracy;
    private double numericalVariance = Double.NaN;
    private boolean numericalVarianceIsCalculated = false;
    public static double inverseCumAccuracy = 0;

    public MFDistribution(
                         double numeratorDegreesOfFreedom,
                         double denominatorDegreesOfFreedom
                         )
            {
        this.numeratorDegreesOfFreedom = numeratorDegreesOfFreedom;
        this.denominatorDegreesOfFreedom = denominatorDegreesOfFreedom;
        solverAbsoluteAccuracy = inverseCumAccuracy;
    }

    public double density(double x) {
        return FastMath.exp(logDensity(x));
    }

    public double logDensity(double x) {
        final double nhalf = numeratorDegreesOfFreedom / 2;
        final double mhalf = denominatorDegreesOfFreedom / 2;
        final double logx = FastMath.log(x);
        final double logn = FastMath.log(numeratorDegreesOfFreedom);
        final double logm = FastMath.log(denominatorDegreesOfFreedom);
        final double lognxm = FastMath.log(numeratorDegreesOfFreedom * x +
                denominatorDegreesOfFreedom);
        return nhalf * logn + nhalf * logx - logx +
                mhalf * logm - nhalf * lognxm - mhalf * lognxm -
                Beta.logBeta(nhalf, mhalf);
    }


    public double cumulativeProbability(double x)  {
        double ret;
        if (x <= 0) {
            ret = 0;
        } else {
            double n = numeratorDegreesOfFreedom;
            double m = denominatorDegreesOfFreedom;

            ret = Beta.regularizedBeta((n * x) / (m + n * x),
                    0.5 * n,
                    0.5 * m);
        }
        return ret;
    }


    public double getNumeratorDegreesOfFreedom() {
        return numeratorDegreesOfFreedom;
    }


    public double getDenominatorDegreesOfFreedom() {
        return denominatorDegreesOfFreedom;
    }


    protected double getSolverAbsoluteAccuracy() {
        return solverAbsoluteAccuracy;
    }

    public double getNumericalMean() {
        final double denominatorDF = getDenominatorDegreesOfFreedom();

        if (denominatorDF > 2) {
            return denominatorDF / (denominatorDF - 2);
        }

        return Double.NaN;
    }


    public double getNumericalVariance() {
        if (!numericalVarianceIsCalculated) {
            numericalVariance = calculateNumericalVariance();
            numericalVarianceIsCalculated = true;
        }
        return numericalVariance;
    }


    protected double calculateNumericalVariance() {
        final double denominatorDF = getDenominatorDegreesOfFreedom();

        if (denominatorDF > 4) {
            final double numeratorDF = getNumeratorDegreesOfFreedom();
            final double denomDFMinusTwo = denominatorDF - 2;

            return ( 2 * (denominatorDF * denominatorDF) * (numeratorDF + denominatorDF - 2) ) /
                    ( (numeratorDF * (denomDFMinusTwo * denomDFMinusTwo) * (denominatorDF - 4)) );
        }

        return Double.NaN;
    }


    public double getSupportLowerBound() {
        return 0;
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
