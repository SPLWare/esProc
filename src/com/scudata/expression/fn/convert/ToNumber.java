package com.scudata.expression.fn.convert;

import java.math.BigDecimal;
import com.ibm.icu.text.DecimalFormat;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 将字符串转换成相应的实数
 * number( x )或number( x, y )
 * number( x, "format" )如果第二个参数是串则当做格式
 * 如果x能转成数就返回数，如果不能，没y或y不是数的时候返回null，否则返回y
 * @author runqian
 *
 */
public class ToNumber extends Function {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("number" + mm.getMessage("function.missingParam"));
		}
		
		if (param.isLeaf()) {
			Object result = param.getLeafExpression().calculate(ctx);
			if (result instanceof String) {
				return Variant.parseNumber((String)result);
			} else if (result instanceof Number) {
				return result;
			} else {
				return null;
			}
		} else {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("number" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			if (sub1 == null || sub2 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("number" + mm.getMessage("function.invalidParam"));
			}
			
			Object result1 = sub1.getLeafExpression().calculate(ctx);
			Object result2 = sub2.getLeafExpression().calculate(ctx);
			
			if (result1 instanceof String) {
				if (result2 instanceof String) {
					try {
						DecimalFormat nf = new DecimalFormat((String)result2);
						nf.setRoundingMode(BigDecimal.ROUND_HALF_UP);
						return nf.parse((String)result1);
					} catch (Exception e) {
					}
				}
				
				Object result = Variant.parse((String)result1);
				if (result instanceof Number) {
					return result;
				}
			} else if (result1 instanceof Number) {
				return result1;
			}
			
			if (result2 instanceof Number) {
				return result2;
			} else {
				return null;
			}
		}
	}
}
