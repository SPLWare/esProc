package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;


/**
 * 求序列成员的乘积或多个参数的乘积,非数值成员被忽略
 * @author yanjing
 *
 */
public class Product extends Function {

	public Object calculate(Context ctx) {
		double result1=1;
		if (param==null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("product" +
								  mm.getMessage("function.missingParam"));
		}else if(param.isLeaf()){
			Object result = param.getLeafExpression().calculate(ctx);
			if (result != null && result instanceof Number) {
				result1*=Variant.doubleValue(result);
			}else if(result != null && result instanceof Sequence){
				int n=((Sequence)result).length();
				for(int i=1;i<=n;i++){
					Object tmp=((Sequence)result).get(i);
					if (tmp!=null && tmp instanceof Number) {
						result1*=Variant.doubleValue(tmp);
					}
				}
			}
		}else{
			int size=param.getSubSize();
			for(int j=0;j<size;j++){
				IParam sub = param.getSub(j);
				if (sub != null) {
					Object result = sub.getLeafExpression().calculate(ctx);
					if (result != null && result instanceof Number) {
						result1*=Variant.doubleValue(result);
					}else if(result != null && result instanceof Sequence){
						int n=((Sequence)result).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result).get(i);
							if (tmp!=null && tmp instanceof Number) {
								result1*=Variant.doubleValue(tmp);
							}
						}
					}
				}
			}
		}
		return new Double(result1);
	}
}
