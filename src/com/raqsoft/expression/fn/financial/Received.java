package com.raqsoft.expression.fn.financial;

import java.util.Date;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.common.DateFactory;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.dm.Context;


/**
 * 返回一次性付息的有价证券到期收回的金额
 * @author yanjing
 * 
 * Freceived(settlement,maturity;investment,discount) 
 *   @e 30/360, 
 * 	 @1 实际天数/年实际天数，
 * 	 @0 实际天数/360， 
 * 	 @5 实际天数/365，
 * 	 缺省为30/360 
 * @param settlement 证券的成交（发行）日
 * @param maturity  证券的到期日
 * @param Investment 为有价证券的投资额
 * @param discount  有价证券的贴现率
 * @return
 * 
 * 
 */
public class Received extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Freceived:" +
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
		return received(result);
	}

	private Object received(Object[] result){
		Date maturity;
		Date settlement;
		double investment;
		double discount;
		long basis=0;
		
		if(result[0]==null || result[1]==null || result[2]==null || result[3]==null){
			MessageManager mm = EngineMessage.get();
			throw new RQException("The params of Freceived:" + mm.getMessage("function.paramValNull"));
		}
		else{
			for(int i=0;i<=1;i++){
				if (!(result[i] instanceof Date)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("The "+i+"th param of Freceived:" + mm.getMessage("function.paramTypeError"));
				}
			}
			settlement=(Date)result[0];
			maturity=(Date)result[1];
			if(Variant.compare(settlement, maturity)==1){
				throw new RQException("The maturity of Freceived should be later than settlement");
			}
			if (!(result[2] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The third param of Freceived:" + mm.getMessage("function.paramTypeError"));
			}
			investment=Variant.doubleValue(result[2]);
			if (!(result[3] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The forth param of Freceived:" + mm.getMessage("function.paramTypeError"));
			}
			discount=Variant.doubleValue(result[3]);
		}
		if(option!=null && option.indexOf("1")>=0) basis=1;
		else if(option!=null && option.indexOf("0")>=0) basis=2;
		else if(option!=null && option.indexOf("5")>=0) basis=3;
		else if(option!=null && option.indexOf("e")>=0) basis=4;
		else basis=0;
		
		long m=Variant.interval(settlement, maturity, "m");
		long DIM;
		if(basis==0 || basis==4){
			DIM=30*m+DateFactory.get().day(maturity)-DateFactory.get().day(settlement);
		}
		else DIM=Variant.interval(settlement, maturity, null);
		
		long B=360;
		if(basis==0 || basis==2|| basis==4){
			B=360;
		}
		else if(basis==3){
			B=365;
		}
		else{
			B=DateFactory.get().daysInYear(maturity);
		}
		return new Double(investment/(1-discount*DIM/B));
	}
}
