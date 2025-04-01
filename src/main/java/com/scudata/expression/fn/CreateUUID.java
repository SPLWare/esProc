package com.scudata.expression.fn;

import java.util.UUID;

import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.Node;

/**
 * 获得全系统唯一的字串
 * @author runqian
 *
 */
public class CreateUUID extends Function {
	public Node optimize(Context ctx) {
		return this;
	}

	public Object calculate(Context ctx) {
		UUID uuid = UUID.randomUUID();
		long mostSigBits = uuid.getMostSignificantBits();
		long leastSigBits = uuid.getLeastSignificantBits();
        return (digits(mostSigBits >> 32, 8) +
                digits(mostSigBits >> 16, 4) +
                digits(mostSigBits, 4) +
                digits(leastSigBits >> 48, 4) +
                digits(leastSigBits, 12));
		//return uuid.toString();
	}
	
    private static String digits(long val, int digits) {
        long hi = 1L << (digits * 4);
        return Long.toHexString(hi | (val & (hi - 1))).substring(1);
    }
}
