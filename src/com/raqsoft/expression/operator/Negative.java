package com.raqsoft.expression.operator;

import java.util.Date;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Operator;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * ∏∫∫≈‘ÀÀ„∑˚£∫-
 * @author RunQian
 *
 */
public class Negative extends Operator {
	public Negative() {
		priority = PRI_NEGT;
	}

	public Object calculate(Context ctx) {
		if (right == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"-\"" + mm.getMessage("operator.missingRightOperation"));
		}

		Object rightResult = right.calculate(ctx);
		if (rightResult instanceof Number) {
			return Variant.negate(rightResult);
		} else if (rightResult == null) {
			return null;
		} else if (rightResult instanceof Date) {
			Date date = (Date)rightResult;
			Date result = (Date)date.clone();
			result.setTime(-date.getTime());
			return result;
		} else if (rightResult instanceof String) {
			char []chars = ((String)rightResult).toCharArray();
			for (int i = 0, len = chars.length; i < len; ++i) {
				chars[i] = (char)(0xFFFF - chars[i]);
			}

			return new String(chars);
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("\"-\"" +mm.getMessage("operator.numberRightOperation"));
		}
	}
}
