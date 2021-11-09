package com.raqsoft.expression.fn.financial;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;
import com.raqsoft.dm.Context;


/**
 * 返回某项资产按年限总和折旧法计算的指定期间的折旧值
 * @author yanjing
 * 
 * Fsyd(cost,salvage,life,per)   

 * 
 * @param Cost 为资产原值
 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
 * @param Life 为折旧期限（有时也称作资产的使用寿命）
 * @param Per 为指定期间，其单位与 life 相同
 * @return
 * 
 * 
 */
public class Syd extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fsyd:" +
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
		return syd(result);

	}
	
	/** 
	 * @param Cost 为资产原值
	 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
	 * @param Life 为折旧期限（有时也称作资产的使用寿命）
	 * @param Per 为指定期间，其单位与 life 相同
	 * @return
	 */
	private Object syd(Object[] result){
		double cost;
		double salvage;
		double life;
		double per;
		for(int i=0;i<=3;i++){
			if(result[i]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fsyd:" + mm.getMessage("function.paramValNull"));
			}
			if (!(result[i] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fsyd:" + mm.getMessage("function.paramTypeError"));
			}
		}
		cost=Variant.doubleValue(result[0]);
		salvage=Variant.doubleValue(result[1]);
		life=Variant.doubleValue(result[2]);
		per=Variant.doubleValue(result[3]);
		return new Double((cost-salvage)*(life-per+1)*2/(life*(life+1)));
	}

}
