package com.scudata.lib.financial;

import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 返回有价证券的贴现率
 * @author yanjing
 * 
 * Fdisc(settlement,maturity;pr,redemption) 
 * 	 @e 30/360, 
 * 	 @1 实际天数/年实际天数，
 * 	 @0 实际天数/360， 
 * 	 @5 实际天数/365，
 * 	 缺省为30/360* * 
 * @param settlement 证券的结算日
 * @param maturity  证券的到期日
 * @param pr  面值 ￥100 的有价证券的价格
 * @param redemption  面值 ￥100 的有价证券的清偿价值
 * @return
 * 
 * 
 */
public class Disc extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<4){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fdisc:" +
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
		return disc(result);

	}

	private Object disc(Object[] result){
		Date maturity;
		Date settlement;
		double pr;
		double redemption;
		long basis=0;
		
		if(result[0]==null || result[1]==null || result[2]==null || result[3]==null){
			MessageManager mm = EngineMessage.get();
			throw new RQException("The params of Fdisc:" + mm.getMessage("function.paramValNull"));
		}
		else{
			for(int i=0;i<=1;i++){
				if (!(result[i] instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("The "+i+"th param of Fdisc:" + mm.getMessage("function.paramTypeError"));
				}
			}
			settlement=(Date)result[0];
			maturity=(Date)result[1];
			if(Variant.compare(settlement, maturity)==1){
				throw new RQException("The maturity of Fdisc should be later than settlement");
			}
			if (!(result[2] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The third param of Fdisc:" + mm.getMessage("function.paramTypeError"));
			}
			pr=Variant.doubleValue(result[2]);
			if (!(result[3] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The forth param of Fdisc:" + mm.getMessage("function.paramTypeError"));
			}
			redemption=Variant.doubleValue(result[3]);
		}
		if(option!=null && option.indexOf("1")>=0) basis=1;
		else if(option!=null && option.indexOf("0")>=0) basis=2;
		else if(option!=null && option.indexOf("5")>=0) basis=3;
		else if(option!=null && option.indexOf("e")>=0) basis=4;
		else basis=0;
		
		double m=Variant.interval(settlement, maturity, "m");
		double DSM;
		if(basis==0 || basis==4){
			DSM=30*m+DateFactory.get().day(maturity)-DateFactory.get().day(settlement);
		}
		else DSM=Variant.interval(settlement, maturity, null);
		
		double B=360;
		if(basis==0 || basis==2|| basis==4){
			B=360;
		}
		else if(basis==3){
			B=365;
		}
		else{
			B=DateFactory.get().daysInYear(maturity);
		}
		return new Double((redemption-pr)*B/redemption/DSM);
	}
	

}
