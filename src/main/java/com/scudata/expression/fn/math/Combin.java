package com.scudata.expression.fn.math;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 求组合数,从n个对象中选出k个对象的组合数，n<k||n<=0||k<=0均返回0，n和k自动取整
 * @author yanjing
 *
 */
public class Combin	extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("combin" + mm.getMessage("function.missingParam"));
		} else if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("combin" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		IParam sub1 = param.getSub(0);
		IParam sub2 = param.getSub(1);
		if (sub1 == null || sub2 == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("combin" + mm.getMessage("function.invalidParam"));
		}
		
		Object result1 = sub1.getLeafExpression().calculate(ctx);
		if (result1 == null) {
			return null;
		} else if (! (result1 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("The first param of combin" + mm.getMessage("function.paramTypeError"));
		}
		
		Object result2 = sub2.getLeafExpression().calculate(ctx);
		if (result2 == null) {
			return null;
		} else if (! (result2 instanceof Number)) {
			MessageManager mm = EngineMessage.get();
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
