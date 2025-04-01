package com.scudata.lib.math;

import static java.lang.Math.sqrt;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 正态逆累积分布函数
 * @author bd
 */
public class Norminv extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("norminv" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("norminv" + mm.getMessage("function.invalidParam"));
		} else if (param.getSubSize() == 3) {
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub1 == null || sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("norminv" + mm.getMessage("function.invalidParam"));
			}
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			Object o3 = sub3.getLeafExpression().calculate(ctx);
			if (o1 instanceof Number && o2 instanceof Number && o3 instanceof Number ) {
				double p = ((Number) o1).doubleValue();
				double mu = ((Number) o2).doubleValue();
				double sigma = ((Number) o3).doubleValue();
				return norminv(p, mu, sigma);
			}
			MessageManager mm = EngineMessage.get();
			throw new RQException("ones" + mm.getMessage("function.paramTypeError"));
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException("norminv" + mm.getMessage("function.invalidParam"));
	}

    public static double norminv(double p, double mu, double sigma) {
        if(p < 0 || p > 1)
            throw new RQException("The probality p must be bigger than 0 and smaller than 1");
        if(sigma < 0)
            throw new RQException("The standard deviation sigma must be positive");
        if(p == 0)
            return Double.NEGATIVE_INFINITY;
        if(p == 1)
            return Double.POSITIVE_INFINITY;
        if(sigma == 0)
            return mu;
        double  q, r, val;

        q = p - 0.5;

        /* 0.075 <= p <= 0.925 */
        if(Math.abs(q) <= .425) {
            r = .180625 - q * q;
            val =
                    q * (((((((r * 2509.0809287301226727 +
                            33430.575583588128105) * r + 67265.770927008700853) * r +
                            45921.953931549871457) * r + 13731.693765509461125) * r +
                            1971.5909503065514427) * r + 133.14166789178437745) * r +
                            3.387132872796366608)
                            / (((((((r * 5226.495278852854561 +
                            28729.085735721942674) * r + 39307.89580009271061) * r +
                            21213.794301586595867) * r + 5394.1960214247511077) * r +
                            687.1870074920579083) * r + 42.313330701600911252) * r + 1);
        }
        /* closer than 0.075 from {0,1} boundary */
        else {
            /* r = min(p, 1-p) < 0.075 */
            if (q > 0)
                r = 1 - p;
            else
                r = p;
            r = sqrt(-Math.log(r));
            /* r = sqrt(-log(r))  <==>  min(p, 1-p) = exp( - r^2 ) */

            if (r <= 5) { /* <==> min(p,1-p) >= exp(-25) ~= 1.3888e-11 */
                r += -1.6;
                val = (((((((r * 7.7454501427834140764e-4 +
                        .0227238449892691845833) * r + .24178072517745061177) *
                        r + 1.27045825245236838258) * r +
                        3.64784832476320460504) * r + 5.7694972214606914055) *
                        r + 4.6303378461565452959) * r +
                        1.42343711074968357734)
                        / (((((((r *
                        1.05075007164441684324e-9 + 5.475938084995344946e-4) *
                        r + .0151986665636164571966) * r +
                        .14810397642748007459) * r + .68976733498510000455) *
                        r + 1.6763848301838038494) * r +
                        2.05319162663775882187) * r + 1);
            } else { /* very close to  0 or 1 */
                r += -5;
                val = (((((((r * 2.01033439929228813265e-7 +
                        2.71155556874348757815e-5) * r +
                        .0012426609473880784386) * r + .026532189526576123093) *
                        r + .29656057182850489123) * r +
                        1.7848265399172913358) * r + 5.4637849111641143699) *
                        r + 6.6579046435011037772)
                        / (((((((r *
                        2.04426310338993978564e-15 + 1.4215117583164458887e-7) *
                        r + 1.8463183175100546818e-5) * r +
                        7.868691311456132591e-4) * r + .0148753612908506148525)
                        * r + .13692988092273580531) * r +
                        .59983220655588793769) * r + 1);
            }

            if (q < 0.0) {
                val = -val;
            }
        }

        return mu + sigma * val;
    }
}
