package com.raqsoft.expression.fn.financial;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.ListBase1;
import com.raqsoft.dm.Sequence;



/**
 * 
 * @author yanjing
 * Fmirr(values,finance_rate,reinvest_rate)  返回某一连续期间内现金流的修正内部收益率
 * @param values为一个数组。这些数值代表着各期的一系列支出（负值）及收入（正值）
 * @param Finance_rate 为现金流中使用的资金支付的利率。
 * @param Reinvest_rate 为将现金流再投资的收益率
 * 
 */
public class Mirr extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fmirr:" +
									  mm.getMessage("function.missingParam"));
		}
		
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
			}
		}
		return mirr(result);
	}

	private Double mirr(Object[] result){

		if(result[0]==null || result[1]==null || result[2]==null){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fmirr" + mm.getMessage("function.paramValNull"));
		}
		if (!(result[0] instanceof Sequence) ||!(result[1] instanceof Number) || !(result[2] instanceof Number)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fmirr" + mm.getMessage("function.paramTypeError"));
		}
		double frate=Variant.doubleValue(result[1]);
		double rrate=Variant.doubleValue(result[2]);
		ListBase1 mems = ((Sequence)result[0]).getMems();
		int m=mems.size();
		double nvalue=0;
		double pvalue=0;
		for(int i=1;i<=m;i++){
			Object obj = mems.get(i);
			if(obj!=null && obj instanceof Number){
				double tmp=Variant.doubleValue(obj);
				if(tmp>=0){ 
					pvalue+=tmp/Math.pow(1+rrate, i);
				}
				else{
					nvalue+=tmp/Math.pow(1+frate, i);
				}
			}
		}
		return new Double(Math.pow(-pvalue*Math.pow(1+rrate, m)/(nvalue*(1.0+frate)),1.0/(m-1.0))-1.0);
	}

}
