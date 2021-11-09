package com.raqsoft.expression.fn.financial;

import java.util.Date;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.DateFactory;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.dm.Context;


/**
 * 返回有价证券的应计利息。
 * @author yanjing
 * Faccrint@24105e(first_interest , settlement, issue; rate, par)
 * Faccrint(issue,first_interest,settlement,rate,par,frequency,basis,calc_method)  定期付息证券
 * 
 * @param issue 有价证券的发行日
 * @param first_interest	证券的首次计息日
 * @param settlement 为有价证券的到期日/证券的结算日
 * @param rate  有价证券的年息票利率
 * @param par 为有价证券的票面价值，如果省略 par，视 par 为 ￥1,000。
 *   无，按年支付，frequency = 1；
 *   @2 按半年期支付，frequency = 2；
 *   @4 按季支付，frequency = 4
 *   
 * 	 @e 30/360, 
 * 	 @1 实际天数/年实际天数，
 * 	 @0 实际天数/360， 
 * 	 @5 实际天数/365，
 * 	 缺省为30/360
 * @param calc_method 逻辑值，指定当结算日期晚于首次计息日期时用于计算总应计利息的方法。
 *                    如果值为 TRUE (1)，则返回从发行日到结算日的总应计利息。
 *                    如果值为 FALSE (0)，则返回从首次计息日到结算日的应计利息。缺省为 TRUE。
 * @return
 * 
 * 
 */
public class Accrint extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		
		if(param==null || param.isLeaf() || param.getSubSize()<4){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Faccrint:" +
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
		return accrint(result);
	}
	private Object accrint(Object[] result){
		Date issue;
		Date first_interest;
		Date settlement;
		double rate;
		double par=1000;
		long frequency=1;
		long basis=0;
		MessageManager mm = EngineMessage.get();
		if(result[0]==null || result[1]==null || result[2]==null || result[3]==null){
			throw new RQException("The first four params of Faccrint:" + mm.getMessage("function.paramValNull"));
		}
		else{
			for(int i=0;i<=2;i++){
				if (!(result[i] instanceof Date)) {
					throw new RQException("The "+i+"th param of Faccrint:" + mm.getMessage("function.paramTypeError"));
				}
			}
			
			first_interest=(Date)result[0];
			settlement=(Date)result[1];
			issue=(Date)result[2];
			if (!(result[3] instanceof Number)) {
				throw new RQException("The 4th param of Faccrint:" + mm.getMessage("function.paramTypeError"));
			}
			rate=Variant.doubleValue(result[3]);
		}
		int size=result.length;
		if(size>4 && result[4]!=null) par=Variant.doubleValue(result[4]);

		if(option!=null && option.indexOf("2")>=0) frequency=2;
		else if(option!=null && option.indexOf("4")>=0) frequency=4;
		else frequency=1;
		
		if(option!=null && option.indexOf("1")>=0) basis=1;
		else if(option!=null && option.indexOf("0")>=0) basis=2;
		else if(option!=null && option.indexOf("5")>=0) basis=3;
		else if(option!=null && option.indexOf("e")>=0) basis=4;
		else basis=0;

		long realdays=0;

		if(basis==0 || basis==4){
			realdays=Variant.interval(issue, settlement, "m")*30;
			realdays=realdays-(DateFactory.get().day(issue)-DateFactory.get().day(settlement));
		}
		else realdays=Variant.interval(issue, settlement, "d");

		double ydays=360;
		if(basis==0 || basis==2 || basis==4){
			ydays=360/frequency;
		}
		else if(basis==3) ydays=365/frequency;
		else ydays=Variant.interval(issue, first_interest, "d")+1;
		return new Double(par*rate/frequency*realdays/ydays);
	}
	
}
