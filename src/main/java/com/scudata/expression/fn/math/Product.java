package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 求序列成员的乘积或多个参数的乘积,非数值成员被忽略
 * @author yanjing
 *
 */
public class Product extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("product" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		double result1=1;
		if(param.isLeaf()){
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
