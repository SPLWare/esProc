package com.scudata.expression.fn.gather;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;


/**
 * 取不重复的元素个数，去除取值为false的元素
 * icount(x1,…)
 * @author RunQian
 *
 */
public class ICount extends Gather {
	private Expression exp; // 表达式
	private boolean isSorted = false; // 数据是否按表达式有序
	
	// 有序icount的中间结果信息
	public static class ICountInfo_o implements Serializable {
		private static final long serialVersionUID = 1L;
		
		private int count;
		private Object startValue;
		private Object endValue;
		
		public ICountInfo_o() {
		}
		
		public ICountInfo_o(Object startValue) {
			if (startValue != null) {
				count = 1;
				this.startValue = startValue;
				this.endValue = startValue;
			}
		}
		
		public void put(Object value) {
			if (value instanceof ICountInfo_o) {
				ICountInfo_o next = (ICountInfo_o)value;
				if (!Variant.isEquals(endValue, next.startValue)) {
					count += next.count;
				} else {
					count += next.count - 1;
				}
				
				endValue = next.endValue;
			} else {
				if (!Variant.isEquals(endValue, value)) {
					count++;
					endValue = value;
				}
			}
		}
	}
	
	public void prepare(Context ctx) {
		if (param == null || !param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.invalidParam"));
		}

		exp = param.getLeafExpression();
		isSorted = option != null && option.indexOf('o') != -1;
	}

	/**
	 * 把一条记录计算出的数据，添加到临时中间数据
	 */
	public Object gather(Context ctx) {
		// 数据按icount字段有序
		if (isSorted) {
			Object val = exp.calculate(ctx);
			if (val instanceof ICountInfo_o){
				return val;
			} else {
				return new ICountInfo_o(val);
			}
		}

		Object val = exp.calculate(ctx);
		if (val instanceof HashSet) {
			return val;
		} else if (val instanceof Sequence){
			Sequence seq = (Sequence)val;
			int len = seq.length();
			HashSet<Object> set = new HashSet<Object>(len + 8);
			for (int i = 1; i <= len; ++i) {
				set.add(seq.getMem(i));
			}
			
			return set;
		} else if (val != null) {
			HashSet<Object> set = new HashSet<Object>();
			if (val != null) {
				set.add(val);
			}
			
			return set;
		} else {
			return null;
		}
	}
	
	/**
	 * 把其它数据整合到临时中间数据
	 * 		
	 * @param	oldValue	老的数据(若不是null，则必为哈希)
	 * @param	ctx			上下文变量
	 */
	public Object gather(Object oldValue, Context ctx) {
		Object val = exp.calculate(ctx);
		if (val == null) {
			return oldValue;
		}
		
		// 数据按icount字段有序
		if (isSorted) {
			((ICountInfo_o)oldValue).put(val);
			return oldValue;
		}
		
		if (oldValue == null) {
			if (val instanceof HashSet) {
				return val;
			} else if (val instanceof Sequence){
				Sequence seq = (Sequence)val;
				int len = seq.length();
				HashSet<Object> set = new HashSet<Object>(len + 8);
				for (int i = 1; i <= len; ++i) {
					set.add(seq.getMem(i));
				}
				
				return set;
			} else {
				HashSet<Object> set = new HashSet<Object>();
				set.add(val);
				return set;
			}
		} else if (oldValue instanceof HashSet){	// 老数据为哈希
			HashSet<Object> set = ((HashSet<Object>)oldValue);
			if (val instanceof HashSet) {
				set.addAll((HashSet<Object>)val);
			} else if (val instanceof Sequence){
				Sequence seq = (Sequence)val;
				int len = seq.length();
				for (int i = 1; i <= len; ++i) {
					set.add(seq.getMem(i));
				}
			} else {
				set.add(val);
			}
			
			return oldValue;
		} else {
			// 老数据为序列(从临时文件加载的)
			Sequence seq = (Sequence)oldValue;
			int len = seq.length();
			HashSet<Object> set = new HashSet<Object>(len + 8);
			for (int i = 1; i <= len; ++i) {
				set.add(seq.getMem(i));
			}
			
			if (val instanceof Sequence) {
				seq = (Sequence)val;
				len = seq.length();
				for (int i = 1; i <= len; ++i) {
					set.add(seq.getMem(i));
				}
			} else {
				set.add(val);
			}
			
			return set;
		} 
	}
	
	/**
	 * 取二次汇总时该聚合字段对应的表达式
	 * @param q	当前汇总字段的序号
	 * @return	汇总表达式
	 */
	public Expression getRegatherExpression(int q) {
		if (isSorted) {
			String str = "icount@o(#" + q + ")";
			return new Expression(str);
		} else {
			String str = "icount(#" + q + ")";
			return new Expression(str);
		}
	}
	
	/**
	 * 是否需要根据中间结果，统计生成最终结果
	 */
	public boolean needFinish() {
		return true;
	}

	/**
	 * 把内存中的中间结果，转换成存盘序列。
	 * 		
	 * @param	val	被转换的数据
	 * @return	返回转换后的结果
	 */
	public Object finish1(Object val) {
		if (isSorted) {
			return val;
		}
		
		if (val instanceof HashSet) {
			HashSet<Object> set = (HashSet<Object>)val;
			Sequence seq = new Sequence(set.size());
			
			Iterator<Object> iter = set.iterator();
			while (iter.hasNext()) {
				seq.add(iter.next());
			}
			
			return seq;
		} else {
			return val;
		}
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("icount" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Sequence) {
				return ((Sequence)obj).icount(option);
			} else {
				if (Variant.isTrue(obj)) {
					return ObjectCache.getInteger(1);
				} else {
					return ObjectCache.getInteger(0);
				}
			}
		}

		int size = param.getSubSize();
		HashSet<Object> set = new HashSet<Object>(size);
		for (int i = 0; i < size; ++i) {
			IParam sub = param.getSub(i);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (Variant.isTrue(obj)) {
					set.add(obj);
				}
			}
		}

		return set.size();
	}
	
	/**
	 * 是否需要返回中间临时数据。
	 */
	public boolean needFinish1() {
		return true;
	}
	
	/**
	 * 统计临时中间数据，生成最终结果。
	 */
	public Object finish(Object val) {
		if (isSorted) {
			return ((ICountInfo_o)val).count;
		}
		
		if (null == val)
			return new Integer(0);
		if (val instanceof Sequence) {
			return ((Sequence)val).length();
		}
		return new Integer(((HashSet<Object>)val).size());
	}
	
	public Expression getExp() {
		return exp;
	}
	
	public boolean isSorted() {
		return isSorted;
	}
}

