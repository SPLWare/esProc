package com.raqsoft.expression.fn.math;

import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

import java.util.ArrayList;

/**
 * 求序列成员的最小公倍数,非数值成员被忽略，数值成员被自动取整，若有小于等于0的成员返回错误值0
 * @author yanjing
 *
 */
public class Lcm extends Function {

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("lcm" +
								  mm.getMessage("function.missingParam"));
		}else{
			int size = param.getSubSize();
			ArrayList<Number> num=new ArrayList<Number>();
			for(int j=0;j<size;j++){
				IParam subj = param.getSub(j);
				if (subj != null) {
					Object result = subj.getLeafExpression().calculate(ctx);
					if (result != null && result instanceof Number) {
						num.add((Number)result);
					}else if(result != null && result instanceof Sequence){
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
			int k=num.size();
			Number[] nums=new Number[k];
			num.toArray(nums);
			return new Long(lcm(nums));
		}
	}
	
	/**最小公倍数算法描述：
	 * 令[a1,a2,..,an] 表示a1,a2,..,an的最小公倍数，(a1,a2,..,an)表示a1,a2,..,an的最大公约数
	 * M为a1,a2,..,an的乘积
	 * 则[a1,a2,..,an]=M/(M/a1,M/a2,..,M/an)
	 * @return
	 */
	private long lcm(Number[] num){
		
		int k=num.length;
		long M=1;
		for(int i=0;i<k;i++){
			M*=Variant.longValue(num[i]);
			if(M<=0) return 0;
		}
		Number[] num1=new Number[k];//为了删除为空的数组成员，顺便造M/a1,M/a2,..,M/an数组
		for(int i=0;i<k;i++){
			num1[i]=new Long(M/Variant.longValue(num[i]));
		}
		
		return M/Gcd.gcd(num1, k);
	}
}
