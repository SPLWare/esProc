package com.scudata.lib.financial;

import java.util.Date;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;



/**
 * 
 * @author yanjing
 * 
 * Fnpv(rate,A)		通过使用贴现率，返回一组投资的净现值，这组投资支出(负值)和收入(正值)定期发生
 * Fnpv(rate,A,B)	通过使用贴现率,返回一组投资的净现值，这组投资支出(负值)和收入(正值)不一定定期发生
 * @param rate 贴现率
 * @param A		一组投资，支出(负值)和收入(正值)
 * @param B		与现金流相对应的支付日期序列
 * @return Double
 * 
 * Fnpv(rate,A)=A(1)/(1+rate)^1+A(2)/(1+rate)^2+......
 * Fnpv(rate,A,B)=A(1)/(1+rate)^((B(1)-B(1))/365)+A(2)/(1+rate)^((B(2)-B(1))/365)+......
 * 
 * 
 */
public class Npv extends Function {
                                                                                                                            
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if(param==null || param.isLeaf() || param.getSubSize()<2){
			throw new RQException("Fnpv:" +
								  mm.getMessage("function.missingParam"));
		}
		int size=param.getSubSize();
		Object[] result=new Object[size];
		for(int i=0;i<size;i++){
			IParam sub = param.getSub(i);
			if (sub != null) {
				result[i] = sub.getLeafExpression().calculate(ctx);
				if(i<=1 && result[i]==null){
					throw new RQException("The first two param of Fnpv:" +
										  mm.getMessage("function.paramValNull"));
				}
				else if (i==0 && !(result[i] instanceof Number)) {
					throw new RQException("The first param of Fnpv:" + mm.getMessage("function.paramTypeError"));
				}
				else if (i>0 && !(result[i] instanceof Sequence)) {
					throw new RQException("The "+i+"th param of Fnpv:" + mm.getMessage("function.paramTypeError"));
				}
			}
		}
		if(size>2){
			return xnpv(Variant.doubleValue(result[0]),(Sequence)result[1],(Sequence)result[2]);
		}else{
			return npv(Variant.doubleValue(result[0]),(Sequence)result[1]);
		}
	}
	
	private Double npv(double rate,Sequence A){
		ListBase1 mems = A.getMems();
		int n=mems.size();
		double value;
		
		value=0;
		for(int i=1;i<=n;i++){
			Object obj1 = mems.get(i);
			if (obj1 instanceof Number) {
				value+=Variant.doubleValue(obj1)/Math.pow(1+rate, i);
			}
		}
		return new Double(value);
	}
	
	private Double xnpv(double rate,Sequence A, Sequence B){
		ListBase1 mems = A.getMems();
		int n=mems.size();
		if(n!=B.length()){
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fnpv:The size of second and third param is not equal" +
								  mm.getMessage("function.invalidParam"));
		}
		Object o=B.get(1);
		if(o instanceof String){
			o = Variant.parseDate((String)o);
		}
		if (!(o instanceof Date)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("Fnpv:The member of third param should be Date." + mm.getMessage("function.paramTypeError"));
		}
		Date B1=(Date)o;
		double value=0;
		for(int i=1;i<=n;i++){
			Object obj1 = mems.get(i);
			Object obj2 = B.get(i);
			if(obj2 instanceof String){
				obj2 = Variant.parseDate((String)obj2);
			}
			if (!(obj2 instanceof Date)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("Fnpv:The member of third param should be Date." + mm.getMessage("function.paramTypeError"));
			}
			Date Bi=(Date)obj2;
			if (obj1 instanceof Number) {
				value+=Variant.doubleValue(obj1)/Math.pow(1+rate, Variant.realInterval(B1, Bi,null)/365.0);
			}
		}
		return new Double(value);
		
	}
}
