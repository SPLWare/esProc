package com.scudata.lib.matrix;

import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.FastMath;

public class MGammaDistribution extends MAbstractRealDistribution {
    public static final double DEFAULT_INVERSE_ABSOLUTE_ACCURACY = 1e-9;
    private final double shape;
    private final double scale;
    private final double shiftedShape;
    private final double densityPrefactor1;
    private final double logDensityPrefactor1;
    private final double densityPrefactor2;

    private final double logDensityPrefactor2;

    private final double minY;

    private final double maxLogY;
    private final double solverAbsoluteAccuracy;
    public MGammaDistribution(double shape, double scale)  {
        this(shape, scale, DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
    }
    public MGammaDistribution(double shape, double scale, double inverseCumAccuracy) {
        this.shape = shape;
        this.scale = scale;
        this.solverAbsoluteAccuracy = inverseCumAccuracy;
        this.shiftedShape = shape + Gamma.LANCZOS_G + 0.5;
        final double aux = FastMath.E / (2.0 * FastMath.PI * shiftedShape);
        this.densityPrefactor2 = shape * FastMath.sqrt(aux) / Gamma.lanczos(shape);
        this.logDensityPrefactor2 = FastMath.log(shape) + 0.5 * FastMath.log(aux) -
                FastMath.log(Gamma.lanczos(shape));
        this.densityPrefactor1 = this.densityPrefactor2 / scale *
                FastMath.pow(shiftedShape, -shape) *
                FastMath.exp(shape + Gamma.LANCZOS_G);
        this.logDensityPrefactor1 = this.logDensityPrefactor2 - FastMath.log(scale) -
                FastMath.log(shiftedShape) * shape +
                shape + Gamma.LANCZOS_G;
        this.minY = shape + Gamma.LANCZOS_G - FastMath.log(Double.MAX_VALUE);
        this.maxLogY = FastMath.log(Double.MAX_VALUE) / (shape - 1.0);
    }

    public double getShape() {
        return shape;
    }
    
    public double getScale() {
        return scale;
    }

    public double density(double x) {

        if (x < 0) {
            return 0;
        }
        final double y = x / scale;
        if ((y <= minY) || (FastMath.log(y) >= maxLogY)) {

            final double aux1 = (y - shiftedShape) / shiftedShape;
            final double aux2 = shape * (FastMath.log1p(aux1) - aux1);
            final double aux3 = -y * (Gamma.LANCZOS_G + 0.5) / shiftedShape +
                    Gamma.LANCZOS_G + aux2;
            return densityPrefactor2 / x * FastMath.exp(aux3);
        }
        return densityPrefactor1 * FastMath.exp(-y) * FastMath.pow(y, shape - 1);
    }

    public double logDensity(double x) {

        if (x < 0) {
            return Double.NEGATIVE_INFINITY;
        }
        final double y = x / scale;
        if ((y <= minY) || (FastMath.log(y) >= maxLogY)) {

            final double aux1 = (y - shiftedShape) / shiftedShape;
            final double aux2 = shape * (FastMath.log1p(aux1) - aux1);
            final double aux3 = -y * (Gamma.LANCZOS_G + 0.5) / shiftedShape +
                    Gamma.LANCZOS_G + aux2;
            return logDensityPrefactor2 - FastMath.log(x) + aux3;
        }

        return logDensityPrefactor1 - y + FastMath.log(y) * (shape - 1);
    }

    public double cumulativeProbability(double x) {
        double ret;

        if (x <= 0) {
            ret = 0;
        } else {
            ret = Gamma.regularizedGammaP(shape, x / scale);
        }

        return ret;
    }

    @Override
    protected double getSolverAbsoluteAccuracy() {
        return solverAbsoluteAccuracy;
    }

    public double getNumericalMean() {
        return shape * scale;
    }

    public double getNumericalVariance() {
        return shape * scale * scale;
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

    public double sample()  {
        if (shape < 1) {

            while (true) {

                final double u = random.nextDouble();
                final double bGS = 1 + shape / FastMath.E;
                final double p = bGS * u;

                if (p <= 1) {


                    final double x = FastMath.pow(p, 1 / shape);
                    final double u2 = random.nextDouble();

                    if (u2 > FastMath.exp(-x)) {

                        continue;
                    } else {
                        return scale * x;
                    }
                } else {


                    final double x = -1 * FastMath.log((bGS - p) / shape);
                    final double u2 = random.nextDouble();

                    if (u2 > FastMath.pow(x, shape - 1)) {

                        continue;
                    } else {
                        return scale * x;
                    }
                }
            }
        }

        final double d = shape - 0.333333333333333333;
        final double c = 1 / (3 * FastMath.sqrt(d));

        while (true) {
            final double x = random.nextGaussian();
            final double v = (1 + c * x) * (1 + c * x) * (1 + c * x);

            if (v <= 0) {
                continue;
            }

            final double x2 = x * x;
            final double u = random.nextDouble();


            if (u < 1 - 0.0331 * x2 * x2) {
                return scale * d * v;
            }

            if (FastMath.log(u) < 0.5 * x2 + d * (1 - v + FastMath.log(v))) {
                return scale * d * v;
            }
        }
    }
}
