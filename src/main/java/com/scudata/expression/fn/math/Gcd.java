package com.scudata.expression.fn.math;

import java.util.ArrayList;
import java.util.Arrays;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 求序列成员或多个参数的最大公约数,非数值成员被忽略，数值成员被自动取整,若有小于0的成员返回错误值0
 * @author yanjing
 *
 */
public class Gcd extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("gcd" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		ArrayList<Number> num=new ArrayList<Number>();
		if(param.isLeaf()) {
			Object result = param.getLeafExpression().calculate(ctx);
			if (result != null && result instanceof Number) {
				num.add((Number)result);
			} else if (result != null && result instanceof Sequence) {
				int n=((Sequence)result).length();
				for (int i=1;i<=n;i++) {
					Object tmp=((Sequence)result).get(i);
					if (tmp!=null && tmp instanceof Number) {
						num.add((Number)tmp);
					}
				}
			}
		} else {
			int size = param.getSubSize();
			
			for (int j=0; j<size; j++) {
				IParam subj = param.getSub(j);
				if (subj != null) {
					Object result = subj.getLeafExpression().calculate(ctx);
					if (result != null && result instanceof Number) {
						num.add((Number)result);
					} else if(result != null && result instanceof Sequence) {
						int n=((Sequence)result).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result).get(i);
							if (tmp!=null && tmp instanceof Number) {
								num.add((Number)tmp);
							}
						}
					}
				}
			}
		}
		
		int k=num.size();
		Number[] nums=new Number[k];
		num.toArray(nums);
		return new Long(gcd(nums,k));
	}
	
	/**
	 * 求数组的最大公约数，因为最小公倍数函数要用到本函数，所以单独拿出来
	 * @param num
	 * @param k
	 * @return
	 */
	public static long gcd(Number[] num,int k){
		Arrays.sort(num);//从小到大排序
		
		//从最小的数开始，挨个两两求最大公约数
		long d = Variant.longValue(num[0]);
		if (d<0) {
			return 0;
		}
		
		for (int i = 1; i < k; i++) {
			long d1=Variant.longValue(num[i]);
			if(d1<0) return 0;
			d=gcd(d1,d);
		}
		
		return d;
	}
	
	/**
	 * 求两个数的最大公约数，采取欧几里德定理，f(a,b)=f(b,a%b)
	 * @param max
	 * @param min
	 * @return
	 */
	private static long gcd(long max,long min){
		if (max == min) {
			return max;
		}
		
		if (min > max) {
			long tmp=min;
			min=max;
			max=tmp;
		}
		
		if(min==0) {
			return max;//因为0能被任何数整除，依据百度百科
		} else {
			return gcd(min,max%min);
		}
	}
}
