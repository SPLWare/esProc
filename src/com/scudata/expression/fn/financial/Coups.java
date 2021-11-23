package com.scudata.expression.fn.financial;

import java.util.Date;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

import java.util.Calendar;


/**
 * 返回结算日所在付息期的计息天数。
 * @author yanjing
 * Fcoups(settlement,maturity)返回在结算日和到期日之间的付息次数，向上舍入到最近的整数
 * @d  从上一个付息日到下一个付息日的计息天数
 * @b  从上一个付息日到结算日的计息天数
 * @n  从结算日到下一个付息日的计息天数
 * 
 * @param settlement 证券的结算日
 * @param maturity  证券的到期日
 *   无，按年支付，frequency = 1；
 *   @2 按半年期支付，frequency = 2；
 *   @4 按季支付，frequency = 4
 *   
 * 	 @e 30/360, 
 * 	 @1 实际天数/年实际天数，
 * 	 @0 实际天数/360， 
 * 	 @5 实际天数/365，
 * 	 缺省为30/360*  
 * @return
 * 
 * 
 */
public class Coups extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if(param==null || param.isLeaf()){
			throw new RQException("FCoups:" +
									  mm.getMessage("function.missingParam"));
		}
		
		Object[] result=new Object[2];
		for(int i=0;i<2;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if(result[i]==null){
					throw new RQException("The "+i+"th param of Fcoups:" + mm.getMessage("function.paramValNull"));
				}
				if (!(result[i] instanceof Date)) {
					throw new RQException("The "+i+"th param of Fcoups:" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		Date settlement=(Date)result[0];
		Date maturity=(Date)result[1];
		if(Variant.compare(settlement, maturity)==1){
			throw new RQException("The maturity of FCoups should be later than settlement");
		}
		double frequency=1;
		int basis=0;
		
		if(option!=null && option.indexOf("2")>=0) frequency=2;
		else if(option!=null && option.indexOf("4")>=0) frequency=4;
		else frequency=1;
		
		if(option!=null && option.indexOf("1")>=0) basis=1;
		else if(option!=null && option.indexOf("0")>=0) basis=2;
		else if(option!=null && option.indexOf("5")>=0) basis=3;
		else if(option!=null && option.indexOf("e")>=0) basis=4;
		else basis=0;
		
		if(option==null || (option.indexOf("d")<0 && option.indexOf("b")<0 && option.indexOf("n")<0) ){
			return coupnum(maturity, settlement, frequency, basis);
		}
		return coupdays(maturity, settlement, frequency, basis);

	}
	private Object coupnum(Date maturity,Date settlement,double frequency,int basis){	
		double d=Variant.realInterval(settlement, maturity, "d");
		double m;
		if(basis==0 || basis==4) m=d/30;
		else{
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(settlement);
			int m1=calendar.get(Calendar.MONTH)+1;
			int y1=calendar.get(Calendar.YEAR);
			int d1=calendar.get(Calendar.DAY_OF_MONTH);
			calendar.setTime(maturity);
			int m2=calendar.get(Calendar.MONTH)+1;
			int y2=calendar.get(Calendar.YEAR);
			int d2=calendar.get(Calendar.DAY_OF_MONTH);
			m=(y2-y1)*12+m2-m1;
			if(d1>d2) m-=1;
		}
		double z=12/frequency;
		long n=new Double(Math.ceil(m/z)).longValue();
		return new Long(n);
	}
	private Object coupdays(Date maturity,Date settlement,double frequency,int basis){
		
		long m=Variant.interval(settlement, maturity, "m");
		int z=new Double(12/frequency).intValue();
		int n=new Long(m/z).intValue();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(maturity);
		calendar.add(Calendar.MONTH, -n*z);
		Date end=new Date();
		end.setTime(calendar.getTimeInMillis());
		Date start=new Date();
		if(Variant.compare(settlement, end)==1){
			start=end;
			calendar.add(Calendar.MONTH, z);
			end.setTime(calendar.getTimeInMillis());
		}
		else{
			calendar.add(Calendar.MONTH, -z);
			start.setTime(calendar.getTimeInMillis());
		}
		double days=360;
		if(option.indexOf('d')>=0){
			if(basis==0 || basis==2|| basis==4){
				days=360/frequency;
			}
			else if(basis==3){
				days=365/frequency;
			}
			else{
				
				days=Variant.interval(start,end,null);
			}
		}
		else if(option.indexOf('b')>=0){
			if(basis==0 || basis==4){
				days=Variant.interval(start, settlement, "m")*30;
				days=days-(DateFactory.get().day(start)-DateFactory.get().day(settlement));
			}
			else days=Variant.interval(start, settlement, null);
		}
		else{//n
			if(basis==0 || basis==4){
				days=Variant.interval(settlement, end, "m")*30;
				days=days-(DateFactory.get().day(settlement)-DateFactory.get().day(end));
			}
			else days=Variant.interval(settlement, end, null);
		}
		
		return new Double(days);
	}
}
