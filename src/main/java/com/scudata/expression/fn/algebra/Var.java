package com.scudata.expression.fn.algebra;


import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.resources.EngineMessage;

/**
 * 向量V的总体方差, 使用@s选项时返回样本方差
 * @author bd
 */
public class Var extends Gather{
	private Expression exp;
	private boolean sta;
	private boolean root;
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.invalidParam"));
		}
	}

	public Object calculate(Context ctx) {
		Object result1 = param.getLeafExpression().calculate(ctx);
		if (!(result1 instanceof Sequence)) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.paramTypeError"));
		}
		Sequence ser = (Sequence) result1;
		if (this.option != null) {
			this.sta = this.option.indexOf('s') > -1;
			this.root = this.option.indexOf('r') > -1;
		}
		return var(ser, this.sta, this.root);
	}
	
	protected static double var(Sequence ser, boolean sta, boolean root) {
		Object avg = ser.average();
		if (avg instanceof Number) {
			double avgValue = ((Number) avg).doubleValue();
			int n = ser.length();
			double result = 0;
			for(int i = 1; i <= n; i++){
				Number tmp = (Number) ser.get(i);
				double v = tmp == null ? 0 : tmp.doubleValue();
				if (tmp!=null){
					result+=Math.pow(v-avgValue, 2);
				}
			} 
			if (sta) {
				result = result / (n - 1);
			} else {
				result = result / n;
			}
			if (root) {
				return Math.sqrt(result);
			}
			return result;
		}
		return 0d;		
	}
	
	protected static double var(double[] vs, boolean sta) {
		double sum = 0d;
		int n = vs.length;
		if (n < 1) return 0;
		for (int i = 0; i < n; i++) {
			sum += vs[i];
		}
		double avg = sum/n;
		double result = 0;
		for(int i = 0; i < n; i++){
			result+=Math.pow(vs[i]-avg, 2);
		}
		if (sta) {
			return result / (n - 1);
		} else {
			return result / n;
		}
	}
	
	protected static double std(Sequence ser, boolean sta) {
		return var(ser, sta, true);
	}
	
	protected static double std(double[] vs, boolean sta) {
		double var = var(vs, sta);
		return Math.sqrt(var);
	}

	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("var" + mm.getMessage("function.invalidParam"));
		}
		exp = param.getLeafExpression();
		if (this.option != null) {
			this.sta = this.option.indexOf('s') > -1;
			this.root = this.option.indexOf('r') > -1;
		}
	}

	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (val instanceof VarValue) {
			return val;
		} else {
			return new VarValue(val);
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (oldValue == null) {
			if (val instanceof VarValue) {
				return val;
			} else {
				return new VarValue(val);
			}
		} else {
			((VarValue)oldValue).add(val);
			return oldValue;
		}
	}

	public Expression getRegatherExpression(int q) {
		String str = "var";
		if (this.option != null)
			str += "@"+this.option;
		str += "(#" + q + ")";
		return new Expression(str);
	}
	
	public boolean needFinish() {
		return true;
	}

	public Object finish(Object val) {
		if (val instanceof VarValue) {
			return ((VarValue)val).getVar(this.sta, this.root);
		} else {
			return val;
		}
	}
	
	public Expression getExp() {
		return exp;
	}
	
	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组，ObjectArray，成员都是VarValue
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		IArray array = exp.calculateAll(ctx);
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				VarValue vv = new VarValue(array.get(i));
				result.add(vv);
			} else {
				VarValue vv = (VarValue)result.get(resultSeqs[i]);
				vv.add(array.get(i));
			}
		}
		
		return result;
	}
	
	/**
	 * 多程程分组的二次汇总运算
	 * @param result 一个线程的分组结果
	 * @param result2 另一个线程的分组结果
	 * @param seqs 另一个线程的分组跟第一个线程分组的对应关系
	 * @param ctx 计算上下文
	 * @return
	 */
	public void gather2(IArray result, IArray result2, int []seqs, Context ctx) {
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0) {
				VarValue vv1 = (VarValue)result.get(seqs[i]);
				VarValue vv2 = (VarValue)result2.get(i);
				if (vv1 == null) {
					result.set(seqs[i], vv2);
				} else if (vv2 != null) {
					vv1.add(vv2);
				}
			}
		}
	}
	
	public IArray finish(IArray array) {
		int size = array.size();
		for (int i = 1; i <= size; ++i) {
			VarValue vv = (VarValue)array.get(i);
			Object var = vv.getVar(this.sta, this.root);
			array.set(i, var);
		}
		return array;
	}

}
