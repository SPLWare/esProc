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
 * Fvdb(cost,salvage,life,start_period,end_period,factor)  使用双倍余额递减法或其他指定的方法，返回指定的任何期间内（包括部分期间）的资产折旧值。函数 db@v 代表可变余额递减法
 * @s  no_switch=true
 * @return
 * 
 * 
 */
public class Vdb extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		if(param==null || param.isLeaf() || param.getSubSize()<4){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fvdb:" +
									  mm.getMessage("function.missingParam"));
		}
		int size=param.getSubSize();
		Object[] result=new Object[7];
		size=Math.min(size,7);
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
			}
		}

		if(option!=null && option.indexOf('s')>=0) result[6]=new Boolean(true);
		return vdb(result);
	}
	
	
	/**
	 * Fvdb(cost,salvage,life,start_period,end_period,factor)
	 * @s  no_switch=true  
	 * 使用双倍余额递减法或其他指定的方法，返回指定的任何期间内（包括部分期间）的资产折旧值。
	 * 函数 Fvdb 代表可变余额递减法
	 * @param Cost 为资产原值
	 * @param Salvage 为资产在折旧期末的价值（有时也称为资产残值）
	 * @param Life 为折旧期限（有时也称作资产的使用寿命）
	 * @param Start_period 为进行折旧计算的起始期间,必须与 life 的单位相同
	 * @param End_period 为进行折旧计算的截止期间,必须与 life 的单位相同
	 * @param Factor 为余额递减速率（折旧因子），如果 factor 被省略，则缺省为 2（双倍余额递减法）
	 * @param No_switch 为一逻辑值，指定当折旧值大于余额递减计算值时，是否转用直线折旧法。
	 * @param 如果 no_switch 为 TRUE，即使折旧值大于余额递减计算值，也不转用直线折旧法。 
	 * @param 如果 no_switch 为 FALSE 或被忽略，且折旧值大于余额递减计算值时，将转用线性折旧法。

	 * @return
	 */
	private Object vdb(Object[] result){
		double cost;
		double salvage;
		double life;
		double start_period;
		double end_period;
		double factor=2;
		boolean no_switch=false;
		
		for(int i=0;i<=3;i++){
			if(result[i]==null){
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fvdb:" + mm.getMessage("function.paramValNull"));
			}
			if (!(result[i] instanceof Number)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("The "+i+"th param of Fvdb:" + mm.getMessage("function.paramTypeError"));
			}
		}
		cost=Variant.doubleValue(result[0]);
		salvage=Variant.doubleValue(result[1]);
		life=Variant.doubleValue(result[2]);
		start_period=Variant.doubleValue(result[3]);
		end_period=start_period;
		if(result[4]!=null && result[4] instanceof Number){
			end_period=Variant.doubleValue(result[4]);
		}
		if(result[5]!=null && result[5] instanceof Number){
			factor=Variant.doubleValue(result[5]);
		}
		if(result[6]!=null && result[6] instanceof Boolean){
			no_switch=((Boolean)result[6]).booleanValue();
		}

		double depreciation =0;
		double value=0;
		for(int i=1;i<=end_period;i++){
			double tmp=(cost - depreciation ) * factor/life;
			double tmp1=cost-salvage-depreciation;
			if(!no_switch && tmp1/(life-i+1)>tmp){
				tmp=tmp1/(life-i+1);
			}else tmp=Math.min(tmp, tmp1);
			depreciation+=tmp;
			if(i>start_period) value+=tmp;
		}
		return new Double(value);
	}
}
