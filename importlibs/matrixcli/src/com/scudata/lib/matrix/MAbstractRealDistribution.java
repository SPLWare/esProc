package com.scudata.lib.matrix;

import org.apache.commons.math3.analysis.solvers.UnivariateSolverUtils;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.analysis.UnivariateFunction;

public abstract class MAbstractRealDistribution {

    public static final double SOLVER_DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

    protected final RandomGenerator random;

    private double solverAbsoluteAccuracy = SOLVER_DEFAULT_ABSOLUTE_ACCURACY;
    
    protected MAbstractRealDistribution() {
        random = null;
    }
    
    public double cumulativeProbability(double x) {
        return x;
    };


    public double probability(double x0, double x1) {
        return cumulativeProbability(x1) - cumulativeProbability(x0);
    }

    public double inverseCumulativeProbability(final double p) {
        double lowerBound = getSupportLowerBound();
        if (p == 0.0) {
            return lowerBound;
        }
        double upperBound = getSupportUpperBound();
        if (p == 1.0) {
            return upperBound;
        }
        final double mu = getNumericalMean();
        final double sig = FastMath.sqrt(getNumericalVariance());
        final boolean chebyshevApplies;
        chebyshevApplies = !(Double.isInfinite(mu) || Double.isNaN(mu) ||
                Double.isInfinite(sig) || Double.isNaN(sig));

        if (lowerBound == Double.NEGATIVE_INFINITY) {
            if (chebyshevApplies) {
                lowerBound = mu - sig * FastMath.sqrt((1. - p) / p);
            } else {
                lowerBound = -1.0;
                while (cumulativeProbability(lowerBound) >= p) {
                    lowerBound *= 2.0;
                }
            }
        }

        if (upperBound == Double.POSITIVE_INFINITY) {
            if (chebyshevApplies) {
                upperBound = mu + sig * FastMath.sqrt(p / (1. - p));
            } else {
                upperBound = 1.0;
                while (cumulativeProbability(upperBound) < p) {
                    upperBound *= 2.0;
                }
            }
        }

        final UnivariateFunction toSolve = new UnivariateFunction()  {
            public double value(final double x) {
                return cumulativeProbability(x) - p;
            }
        };

        double x = UnivariateSolverUtils.solve(toSolve,
                lowerBound,
                upperBound,
                getSolverAbsoluteAccuracy());

        if (!isSupportConnected()) {
            /* Test for plateau. */
            final double dx = getSolverAbsoluteAccuracy();
            if (x - dx >= getSupportLowerBound()) {
                double px = cumulativeProbability(x);
                if (cumulativeProbability(x - dx) == px) {
                    upperBound = x;
                    while (upperBound - lowerBound > dx) {
                        final double midPoint = 0.5 * (lowerBound + upperBound);
                        if (cumulativeProbability(midPoint) < px) {
                            lowerBound = midPoint;
                        } else {
                            upperBound = midPoint;
                        }
                    }
                    return upperBound;
                }
            }
        }
        return x;
    }


    public double probability(double x) {
        return 0d;
    }
    public abstract double density(double x);
    public double logDensity(double x) {
        return FastMath.log(density(x));
    }
    
    protected double getSolverAbsoluteAccuracy() {
    	return solverAbsoluteAccuracy;
    }


     public abstract double getNumericalMean();


     public  abstract double getNumericalVariance();
     
     public abstract double getSupportLowerBound();

     public double getSupportUpperBound() {
         return Double.POSITIVE_INFINITY;
     }

     public boolean isSupportLowerBoundInclusive() {
         return true;
     }

     public boolean isSupportUpperBoundInclusive() {
         return false;
     }


     public boolean isSupportConnected() {
         return true;
     }
}


