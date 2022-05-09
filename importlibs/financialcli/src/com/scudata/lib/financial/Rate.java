package com.scudata.lib.financial;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 
 * @author yanjing
 * 
 * rate(nper,pmt,pv,fv,guess)  返回年金的各期利率
 *  @t 期初付款,省略为期末付款
 * @param Nper 为总付款期数。
 * @param pmt  为每期的投资额, 其数值在整个年金期间保持不变。如果省略 pmt，则必须包括 pv/fv 参数。
 * @param Pv 为现值,相当于初期的投资。如果省略 PV，则假设其值为零，并且必须包括 pmt 参数。
 * @param Fv 为未来值，即最后一期支付后剩余的投资额，可视为期末必须追加的投资额。如果省略 fv，则假设其值为零且必须包含 pmt 参数。例如，如果需要在 18 年投资期结束时投资￥50,000，则 ￥50,000 就是未来值。
 * @param guess 为估算利率,缺省为10%,迭代20次后如果结果没有收敛则报错
 * @return 无@p返回未来值，@p返回现值
 */
public class Rate extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if(param==null || param.isLeaf() || param.getSubSize()<3){
			throw new RQException("Frate:" +
									  mm.getMessage("function.missingParam"));
		}
		
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if (result[i] != null && !(result[i] instanceof Number)) {
					throw new RQException("The "+i+"th param of Frate" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		return rate(result);
	}

	private Double rate(Object[] result){
		double nper=0;
		double pmt=0;
		double pv=0;
		double fv=0;
		double guess=0.1;
		double type=0;
		if(option!=null && option.indexOf("t")>=0) type=1;
		int size=result.length;
		if(result[0]!=null) nper=Variant.doubleValue(result[0]);
		if(result[1]!=null) pmt=Variant.doubleValue(result[1]);
		if(result[2]!=null) pv=Variant.doubleValue(result[2]);
		if(size>=4 && result[3]!=null) fv=Variant.doubleValue(result[3]);
		if(size>=5 && result[4]!=null) guess=Variant.doubleValue(result[4]);
		
		if(nper<=0) return new Double(0);
		int j;
		double lvalue=0;
		double lguess=guess;
		double step=0.01;
		for(j=1;j<=100;j++){
			double value=pv*Math.pow(1+guess, nper)+pmt*(1+guess*type)*((Math.pow(1+guess, nper)-1)/guess)+fv;
			if(value<0.0000001 && value>-0.0000001) break;
			else if((value>0.0000001 && lvalue<-0.0000001) || (value<-0.0000001 && lvalue>0.0000001)){
				double tmp1=value;
				double tmp2=guess;
				if(value>lvalue){
					double tmp=value;
					value=lvalue;
					lvalue=tmp;
					tmp=guess;
					guess=lguess;
					lguess=tmp;
				}
				guess= lvalue*(guess-lguess)/(lvalue-value)+lguess;
				step=step/10;
				lvalue=tmp1;
				lguess=tmp2;
				continue;
			}
			else if(value>0.0000001){ 
				lguess=guess;
				guess-=step;
			}
			else if(value<-0.0000001){  
				lguess=guess;
				guess+=step;
			}
			if(guess==0 || guess<0){ 
				guess=step/2;
				step=step/10;
			}
			lvalue=value;
		}
		if(j>100){
			throw new RQException("No perfect result for Frate, please change another guess, and try again!");
		}
		return new Double(guess);
	}
}
