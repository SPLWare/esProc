package com.scudata.expression.fn.financial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 基于固定利率和等额本息的贷款模式,返回给定期数的偿还额或本金或利息。
 * @author yanjing
 * 
 * Fpmt@t(rate,nper,pv,fv)	pmt	  算本息
	@i(rate,nper:per,pv,fv)	ipmt  算利息
	@p(rate,nper:per,pv,fv)	ppmt  算本金

 * @param Rate 为每期的利率, 其数值在整个年金期间保持不变。
 * @param per	要计算本金或利息的期数
 * @param Nper 为总付款期数。
 * @param Pv 为现值,相当于贷款本金。
 * @param Fv 为未来值，即最后一期还款后剩余的贷款额。
 * @return
 * 
 * 如果type=0:
 * 		pmt=-(pv*(1+rate)^nper+fv)*rate/((1+rate)^nper-1)
 * 		pmt@p=-(pv+fv)*(1+rate)^(per-1)*rate/((1+rate)^nper-1)
 * 		pmt@i=-(pv*((1+rate)^nper-(1+rate)^(per-1))-fv*((1+rate)^(per-1)-1))*rate/((1+rate)^nper-1)

 * 如果type=1:
 * 		pmt=-(pv(1+rate)^(nper-1)+fv)/((1+rate)^(nper-1)+[(1+rate)^(nper-1) - 1]/rate)
 * 		pmt@i=-(A(1+β)^(n-2)-(A(1+β)^(m-1)+fv)*((1+β)^(n-1)-1)/((1+β)^m-1))*β
 * 		pmt@p=-(pv*rate(1+rate)^(m-1)+fv*rate)(1+rate)^(n-1)/((1+rate)^m-1)+pv*rate(1+rate)^(n-2)

 */
public class Pmt extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			throw new RQException("Fpmt:" +
								  mm.getMessage("function.missingParam"));
		}
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if (result[i] != null && !(result[i] instanceof Number)) {
					throw new RQException("The "+i+"th param of Fpmt:" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		return pmt(result);
	}

	private Object pmt(Object[] result){
		double rate=0;
		double pv=0;
		double fv=0;
		long per=1;
		long nper=1;

		
		if (option == null || (option.indexOf('p')==-1 && option.indexOf('i')==-1)) {//pmt
			if(result[0]==null || result[1]==null || result[2]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The first three params of Pmt:" + mm.getMessage("function.paramValNull"));
			}
			else{
				rate=Variant.doubleValue(result[0]);
				nper=Variant.longValue(result[1]);
				pv=Variant.doubleValue(result[2]);
			}
			if(result[3]!=null) fv=Variant.doubleValue(result[3]);
			if(option==null || option.indexOf("t")<0){  //type=0
				return new Double(-(pv*Math.pow(1+rate,nper)+fv)*rate/(Math.pow(1+rate,nper)-1));
			}else if(option.indexOf("t")>=0){	//type=1
				return new Double(-(pv*Math.pow(1+rate,nper-1)+fv)/(Math.pow(1+rate,nper-1)+(Math.pow(1+rate,nper-1) - 1)/rate));
			}
		}
		else if(option.indexOf('p')>=0){//pmt@p
			if(result[0]==null || result[1]==null || result[2]==null || result[3]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The first four params of Pmt@p:" + mm.getMessage("function.paramValNull"));
			}
			else{
				rate=Variant.doubleValue(result[0]);
				nper=Variant.longValue(result[1]);
				per=Variant.longValue(result[2]);
				pv=Variant.doubleValue(result[3]);
			}
			if(result[4]!=null) fv=Variant.doubleValue(result[4]);
			if(option.indexOf("t")<0){  //type=0
				return new Double(-(pv+fv)*Math.pow(1+rate,per-1)*rate/(Math.pow(1+rate,nper)-1));
			}else{  //type=1
				return new Double(-(pv*rate*Math.pow(1+rate, nper-1)+fv*rate)*Math.pow(1+rate, per-1)/(Math.pow(1+rate, nper)-1)+pv*rate*Math.pow(1+rate, per-2));
			}
		}
		else if(option.indexOf('i')>=0){//pmt@i
			if(result[0]==null || result[1]==null || result[2]==null || result[3]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The first four params of Pmt@i:" + mm.getMessage("function.paramValNull"));
			}
			else{
				rate=Variant.doubleValue(result[0]);
				nper=Variant.longValue(result[1]);
				per=Variant.longValue(result[2]);
				
				pv=Variant.doubleValue(result[3]);
			}
			if(result[4]!=null) fv=Variant.doubleValue(result[4]);
			if(option.indexOf("t")<0){  //type=0
				//-(pv*((1+rate)^nper-(1+rate)^(per-1))-fv*((1+rate)^(per-1)-1))*rate/((1+rate)^nper-1)
				return new Double(-(pv*(Math.pow(1+rate,nper)-Math.pow(1+rate,per-1))-fv*(Math.pow(1+rate,per-1)-1))*rate/(Math.pow(1+rate,nper)-1));
			}else{  //type=1
				return new Double(-(pv*Math.pow(1+rate, per-2)-(pv*Math.pow(1+rate, nper-1)+fv)*(Math.pow(1+rate, per-1)-1)/(Math.pow(1+rate, nper)-1))*rate);
			}
		}
		return null;
	}

}
