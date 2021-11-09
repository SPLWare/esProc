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
 * digit(xi,…)	sum(2i-1*xi)，xi是字符串时先转成整数
	@h	sum(16i-1*xi)，xi是字符串时先按十六进制数位规则转成数
	@d	sum(10i-1*xi)，xi是字符串时先转成整数
	@n	sum(2i-1*if(xi,1,0))

 * @author yanjing
 *
 */
public class Digit extends Function {

	public Object calculate(Context ctx) {
		if (param == null || param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("digit" + mm.getMessage("function.missingParam"));
		}else{
			int size=param.getSubSize();
		
			Object[] result1=new Object[size];
			for(int j=0;j<size;j++){
				IParam subj = param.getSub(j);
				if (subj != null) {
					result1[j] = subj.getLeafExpression().calculate(ctx);
				}
			}
		
			long result=0;
			int l=0;
			if(option!=null && option.indexOf("h")>=0){//十六进制
				for(int j=0;j<size;j++){
					if(result1[j]!=null && result1[j] instanceof String){
						result+=Math.pow(16, j+l)*Long.valueOf((String)result1[j],16).longValue();
					}else if(result1[j]!=null && result1[j] instanceof Number){
						result+=Math.pow(16, j+l)*((Number)result1[j]).longValue();
					}else if(result1[j] != null && result1[j] instanceof Sequence){
						int n=((Sequence)result1[j]).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result1[j]).get(i);
							if (tmp!=null && tmp instanceof Number) {
								result+=Math.pow(16, j+l+i-1)*((Number)tmp).longValue();
							}else if (tmp!=null && tmp instanceof String) {
								result+=Math.pow(16, j+l+i-1)*Long.valueOf((String)tmp,16).longValue();
							}
						}
						l+=n-1;
					}
				}
			}else if(option!=null && option.indexOf("d")>=0){//10进制
				for(int j=0;j<size;j++){
					if(result1[j]!=null && result1[j] instanceof String){
						result+=Math.pow(10, j+l)*Variant.parseNumber((String)result1[j]).longValue();
					}else if(result1[j]!=null && result1[j] instanceof Number){
						result+=Math.pow(10, j+l)*((Number)result1[j]).longValue();
					}else if(result1[j] != null && result1[j] instanceof Sequence){
						int n=((Sequence)result1[j]).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result1[j]).get(i);
							if (tmp!=null && tmp instanceof Number) {
								result+=Math.pow(10, j+l+i-1)*((Number)tmp).longValue();
							}else if (tmp!=null && tmp instanceof String) {
								result+=Math.pow(10, j+l+i-1)*Variant.parseNumber((String)tmp).longValue();
							}
						}
						l+=n-1;
					}
				}	
			}else if(option!=null && option.indexOf("n")>=0){//空1非空0
				for(int j=0;j<size;j++){
					if(result1[j] != null && result1[j] instanceof Sequence){
						int n=((Sequence)result1[j]).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result1[j]).get(i);
							if (tmp!=null){
								result+=Math.pow(2, j+l+i-1);
							}
						}
						l+=n-1;
					}else if(result1[j]!=null){
						result+=Math.pow(2, j+l);
					}
				}
			}else{//二进制
				for(int j=0;j<size;j++){
					if(result1[j]!=null && result1[j] instanceof String){
						result+=Math.pow(2, j+l)*Variant.parseNumber((String)result1[j]).longValue();
					}else if(result1[j]!=null && result1[j] instanceof Number){
						result+=Math.pow(2, j+l)*((Number)result1[j]).longValue();
					}else if(result1[j] != null && result1[j] instanceof Sequence){
						int n=((Sequence)result1[j]).length();
						for(int i=1;i<=n;i++){
							Object tmp=((Sequence)result1[j]).get(i);
							if (tmp!=null && tmp instanceof Number) {
								result+=Math.pow(2, j+l+i-1)*((Number)tmp).longValue();
							}else if (tmp!=null && tmp instanceof String) {
								result+=Math.pow(2, j+l+i-1)*Variant.parseNumber((String)tmp).longValue();
							}
						}
						l+=n-1;
					}
				}	
			}
			return new Long(result);
		}
	}
}
