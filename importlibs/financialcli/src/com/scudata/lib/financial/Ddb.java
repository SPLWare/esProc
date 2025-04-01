package com.scudata.lib.financial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;



/**
 * @author yanjing
 * 
 * Fddb(cost,salvage,life,period,factor)  使用双倍余额递减法或其他指定方法，计算一笔资产在给定期间内的折旧值
 * 
 * @return
 * 
 * 
 */
public class Ddb extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<4){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fddb:" +
									  mm.getMessage("function.missingParam"));
		}
		
		Object[] result=new Object[5];
		int size=param.getSubSize();
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
			}
		}
		return ddb(result);

	}
	

	
	/**
	 * Fddb(cost,salvage,life,period,factor)  使用双倍余额递减法或其他指定方法，计算一笔资产在给定期间内的折旧值
	 * @param Cost 为资产原值
	 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
	 * @param Life 为折旧期限（有时也称作资产的使用寿命）
	 * @param Period 为需要计算折旧值的期间。Period 必须使用与 life 相同的单位
	 * @param Factor 为余额递减速率。如果 factor 被省略，则缺省为 2（双倍余额递减法）
	 * @return
	 */
	private Object ddb(Object[] result){
		double cost;
		double salvage;
		double life;
		double period;
		double factor=2;
		
		for(int i=0;i<=3;i++){
			if(result[i]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fddb:" + mm.getMessage("function.paramValNull"));
			}
			if (!(result[i] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fddb:" + mm.getMessage("function.paramTypeError"));
			}
		}
		cost=Variant.doubleValue(result[0]);
		salvage=Variant.doubleValue(result[1]);
		life=Variant.doubleValue(result[2]);
		period=Variant.doubleValue(result[3]);
		if(result[4]!=null && result[4] instanceof Number){
			factor=Variant.doubleValue(result[4]);
		}

		double depreciation =0;
		double tmp=0;
		for(int i=1;i<=period;i++){
			tmp=Math.min((cost - depreciation ) * factor/life,cost-salvage-depreciation);
			depreciation+=tmp;
		}
		return new Double(tmp);
	}

}
