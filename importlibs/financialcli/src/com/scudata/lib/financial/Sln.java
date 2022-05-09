package com.scudata.lib.financial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;



/**
 * 返回某项资产在一个期间中的线性折旧值
 * @author yanjing
 * 
 * sln(cost,salvage,life)   

 * 
 * @param Cost 为资产原值
 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
 * @param Life 为折旧期限（有时也称作资产的使用寿命）
 * @return
 * 
 * 
 */
public class Sln extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fsln:" +
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
		return sln(result);

	}
	
	/** 
	 * @param Cost 为资产原值
	 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
	 * @param Life 为折旧期限（有时也称作资产的使用寿命）
	 * 
	 * @return
	 */
	private Object sln(Object[] result){
		double cost;
		double salvage;
		double life;
		
		for(int i=0;i<=2;i++){
			if(result[i]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fsln:" + mm.getMessage("function.paramValNull"));
			}
			if (!(result[i] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fsln:" + mm.getMessage("function.paramTypeError"));
			}
		}
		cost=Variant.doubleValue(result[0]);
		salvage=Variant.doubleValue(result[1]);
		life=Variant.doubleValue(result[2]);

		return new Double((cost-salvage)/life);
	}

}
