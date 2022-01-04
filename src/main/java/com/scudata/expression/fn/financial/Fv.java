package com.scudata.expression.fn.financial;

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
 * Fv(rate,nper,pmt,[pv],[type])  计算未来值
 * Fv@p(rate,nper,pmt,[fv],[type])  计算现值
 * @t 期初付款,省略为期末付款
 * @param Rate 为每期的利率, 其数值在整个年金期间保持不变。
 * @param Nper 为总付款期数。
 * @param pmt  为每期的投资额, 其数值在整个年金期间保持不变。如果省略 pmt，则必须包括 pv/fv 参数。
 * @param Pv 为现值,相当于初期的投资。如果省略 PV，则假设其值为零，并且必须包括 pmt 参数。
 * @param Fv 为未来值，即最后一期支付后剩余的投资额，可视为期末必须追加的投资额。如果省略 fv，则假设其值为零且必须包含 pmt 参数。例如，如果需要在 18 年投资期结束时投资￥50,000，则 ￥50,000 就是未来值。

 * @return 无@p返回未来值，@p返回现值
 * 
 * 如果type为0，则value的计算公式如下：=-(pmt*(1+rate)^(nper-1)+ pmt*(1+rate)^(nper-2)+……+pmt*(1+rate)^0+pv*(1+rate)^nper)，
 * 如果type为1，则=-(pmt*(1+rate)^nper+ pmt*(1+rate)^(nper-1)+……+pmt*(1+rate)+pv*(1+rate)^nper)
 * 
 * 如果type为0，value@p的计算公式如下：-(pmt/(1+rate)^nper+pmt/(1+rate)^(nper-1)+...... +pmt/(1+rate)+fv/(1+rate)^nper)，
 * 如果type为1，则-(pmt/(1+rate)^(nper-1)+pmt/(1+rate)^(nper-2)+...... +pmt/(1+rate)^0+fv/(1+rate)^nper)
 */
public class Fv extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			throw new RQException("Fv" +
								  mm.getMessage("function.missingParam"));
		}
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if (result[i] != null && !(result[i] instanceof Number)) {
					throw new RQException("The "+i+"th param of Fv" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		if(result[2]==null) result[2]=new Integer(0);
		if(result[3]==null) result[3]=new Integer(0);
		
		return value(Variant.doubleValue(result[0]),Variant.longValue(result[1]),Variant.doubleValue(result[2]),Variant.doubleValue(result[3]));
	}
	
	private Double value(double rate,long nper,double pmt,double fpv){
		if(rate<=0) return new Double(0);
		if(nper<=0) return new Double(0);
		if (option == null || option.indexOf('p') == -1) {//返回未来值
			if(option==null || option.indexOf('t')==-1){  //type=0
				double value=fpv*Math.pow(1+rate,nper);
				for(long i=nper-1;i>=0;i--){
					value+=pmt*Math.pow(1+rate, i);
				}
				return new Double(-value);
			}else{ // type=1
				double value=fpv*Math.pow(1+rate,nper);
				for(long i=nper;i>=1;i--){
					value+=pmt*Math.pow(1+rate, i);
				}
				return new Double(-value);
			}
		}
		else{//返回现值
			if(option==null || option.indexOf('t')==-1){  //type=0
				double value=fpv/Math.pow(1+rate,nper);
				for (long i=nper;i>=1;i--){
					value+=pmt/Math.pow(1+rate,i);
				}
				return new Double(-value);
			}else {  //type=1
				double value=fpv/Math.pow(1+rate,nper);
				for (long i=nper-1;i>=0;i--){
					value+=pmt/Math.pow(1+rate,i);
				}
				return new Double(-value);
			}
		}
	}
}
