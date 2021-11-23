package com.scudata.expression.fn.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


/**
 * ÒÆÎ»ÔËËã£¬s<0×óÒÆ£¬s>0ÓÒÒÆ
 * @author yanjing
 *
 */
public class Shift	extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("shift" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("shift" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("shift" + mm.getMessage("function.invalidParam"));
		}
		
		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (!(result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("The first param of shift" + mm.getMessage("function.paramTypeError"));
		}
		
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result2 == null) {
			return null;
		} else if (!(result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("The second param of shift" + mm.getMessage("function.paramTypeError"));
		}
		
		int n = ((Number)result2).intValue();
		if (n == 0) {
			return result1;
		}
		
		if (result1 instanceof BigDecimal) {
			BigInteger bi = ((BigDecimal)result1).toBigInteger();
			if (n > 0) { // ÓÒÒÆ
				bi = bi.shiftRight(n);
			} else { // ×óÒÆ
				bi = bi.shiftLeft(-n);
			}
			
			return new BigDecimal(bi);
		} else if (result1 instanceof BigInteger) {
			BigInteger bi = (BigInteger)result1;
			if (n > 0) { // ÓÒÒÆ
				bi = bi.shiftRight(n);
			} else { // ×óÒÆ
				bi = bi.shiftLeft(-n);
			}
			
			return new BigDecimal(bi);
		} else if (result1 instanceof Integer) {
			int x = ((Number)result1).intValue();
			if (n > 0) { // ÓÒÒÆ
				if (option == null || option.indexOf('s') == -1) {
					// ·ûºÅÎ»²¹0
					return x >>> n;
				} else {
					return x >> n;
				}
			} else { // ×óÒÆ
				return x << -n;
			}
		} else {
			long x = ((Number)result1).longValue();
			if (n > 0) { // ÓÒÒÆ
				if (option == null || option.indexOf('s') == -1) {
					// ·ûºÅÎ»²¹0
					return x >>> n;
				} else {
					return x >> n;
				}
			} else { // ×óÒÆ
				return x << -n;
			}
		}
	}
}
