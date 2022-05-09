package com.scudata.lib.financial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;



/**
 * 基于固定利率和每期等额投资模式,返回一笔投资的未来值/现值。
 * @author yanjing
 * 

 * Fnper@t(rate,pmt,pv,fv)   等额本息时，计算还款期数
 * @param Rate 为每期的利率, 其数值在整个年金期间保持不变。
 * @param pmt  每期的还款额，不能省略。
 * @param Pv 为现值,相当于贷款本金。
 * @param Fv 为未来值，即最后一期还款后剩余的贷款额。

 * @return
 * 
 * 如果type=0:
 *  	Fnper=log(1+rate) (-fv*rate+pmt)/(pmt+pv*rate)
 * 如果type=1:  @t
 * 		Fnper@t=log(1+rate) (-fv*rate+pmt)/(pmt+pv*rate/(1+rate))
 */
public class Nper extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fnper:" +
									  mm.getMessage("function.missingParam"));
		}
		
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if (result[i] != null && !(result[i] instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("The "+i+"th param of Fnper:" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		return pmt(result);
	}
	
	private Object pmt(Object[] result){
		double rate=0;
		double pmt=0;
		double pv=0;
		double fv=0;

		if(result[0]==null || result[1]==null || result[2]==null){
			MessageManager mm = EngineMessage.get();
			throw new RQException("The first three params of Fnper:" + mm.getMessage("function.paramValNull"));
		}
		else{
			rate=Variant.doubleValue(result[0]);
			pmt=Variant.doubleValue(result[1]);
			pv=Variant.doubleValue(result[2]);
		}
		if(result[3]!=null) fv=Variant.doubleValue(result[3]);
		if(option==null || option.indexOf("t")<0){  //type=0 期末还款
			return new Double(Math.log((-fv*rate+pmt)/(pmt+pv*rate))/Math.log(1.0+rate));
		}else{   //type=1  期初还款
			return new Double(Math.log((pmt-fv*rate)/(pmt+pv*rate/(1.0+rate)))/Math.log(1.0+rate));
		}
	}
}
