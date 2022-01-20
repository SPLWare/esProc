package com.scudata.lib.matrix;

import org.ejml.simple.SimpleMatrix;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ¾ØÕóÇóÎ±Äæ¾ØÕópinv(A)ÇóÎ±Äæ¾ØÕó
 * @author bd
 *
 */
public class PseudoInv extends Function{
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("pinv" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object result1 = param.getLeafExpression().calculate(ctx);
			if (!(result1 instanceof Sequence)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("pinv" + mm.getMessage("function.paramTypeError"));
			}
			double[][] A = toArray((Sequence) result1);
	    	SimpleMatrix sm = new SimpleMatrix(A);
	    	SimpleMatrix pinv = sm.pseudoInverse();
	    	
			return toSequence(pinv);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("inverse" + mm.getMessage("function.invalidParam"));
		}
	}
	
	protected static double[][] toArray(Sequence seq) {
		int len = seq.length();
		double[][] res = new double[len][];
		for (int i = 0; i < len; i++) {
			Object o = seq.get(i+1);
			if (o instanceof Sequence) {
				res[i] = toArray0((Sequence) o);
			}
			else {
				double[] res1 = new double[1];
				res1[0] = getValue(o);
				res[i] = res1;
			}
		}
		return res;
	}
	
	private static double[] toArray0(Sequence seq) {
		int len = seq.length();
		double[] res = new double[len];
		for (int i = 0; i < len; i++) {
			res[i] = getValue(seq.get(i+1));
		}
		return res;
	}
	
	private static double getValue(Object obj) {
		double d = obj instanceof Number ? ((Number) obj).doubleValue() : 0d;
		return d;
	}

	protected Sequence toSequence(SimpleMatrix smatrix) {
		int rows = smatrix.numRows();
		int cols = smatrix.numCols();
		Sequence result = new Sequence(rows);
        for (int r = 0; r < rows ; r++) {
        	Sequence res = new Sequence(cols);
            for (int c = 0; c < cols; c++) {
            	res.add(smatrix.get(r, c));
            }
            result.add(res);
        }
        return result;
	}
}
