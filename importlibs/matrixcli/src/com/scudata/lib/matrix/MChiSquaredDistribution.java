package com.scudata.lib.matrix;

public class MChiSquaredDistribution extends MAbstractRealDistribution{
        public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
        private final MGammaDistribution gamma;
        private final double solverAbsoluteAccuracy;
        public static double inverseCumAccuracy = 0;

        public MChiSquaredDistribution(double degreesOfFreedom) {
            gamma = new MGammaDistribution(degreesOfFreedom / 2, 2);
            solverAbsoluteAccuracy = inverseCumAccuracy;
        }

        public double getDegreesOfFreedom() {
            return gamma.getShape() * 2.0;
        }

        public double density(double x) {
            return gamma.density(x);
        }

        @Override
        public double logDensity(double x) {
            return gamma.logDensity(x);
        }

        public double cumulativeProbability(double x)  {
            return gamma.cumulativeProbability(x);
        }

        @Override
        protected double getSolverAbsoluteAccuracy() {
            return solverAbsoluteAccuracy;
        }

        public double getNumericalMean() {
            return getDegreesOfFreedom();
        }

        public double getNumericalVariance() {
            return 2 * getDegreesOfFreedom();
        }

        public double getSupportLowerBound() {
            return 0;
        }

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

