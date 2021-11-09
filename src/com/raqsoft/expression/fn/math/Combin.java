package com.raqsoft.expression.fn.math;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;


/**
 * 求组合数,从n个对象中选出k个对象的组合数，n<k||n<=0||k<=0均返回0，n和k自动取整
 * @author yanjing
 *
 */
public class Combin	extends Function {

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null || param.isLeaf()) {
			throw new RQException("combin" +
								  mm.getMessage("function.missingParam"));
		}
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			throw new RQException("combin" + mm.getMessage("function.invalidParam"));
		}
		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		}
		if (! (result1 instanceof Number)) {
			throw new RQException("The first param of combin" + mm.getMessage("function.paramTypeError"));
		}
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result2 == null) {
			return null;
		}
		if (! (result2 instanceof Number)) {
			throw new RQException("The second param of combin" + mm.getMessage("function.paramTypeError"));
		}
		return new Long(combin(Variant.longValue(result1),Variant.longValue(result2)));
	}
	
	/**
	 * 求从n个对象中取出k个对象的组合数
	 * @param n
	 * @param k
	 * @return
	 */
	private long combin(long n,long k){
		if(n<k) return 0;
		if(n<=0 || k<=0) return 0;
		long result=1;
		if(k>n/2){
			for(long i=n;i>k;i--){
				result*=i;
			}
			result/=Fact.fact(n-k);
		}else{
			for(long i=n;i>n-k;i--){
				result*=i;
			}
			result/=Fact.fact(k);
		}
		return result;
	}
}
