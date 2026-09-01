package com.scudata.expression.fn.gather;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.array.StringArray;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.HashLinkSet;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;


/**
 * 连接不重复的元素
 * iconcat(x;d)
 * @author RunQian
 *
 */
public class IConcat extends Gather {
	private Expression exp; // 表达式
	private String sep = null; // 分隔符
	private boolean addQuotes = false;
	private boolean addSingleQuotes = false;
	private boolean deleteNull = false;
	
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownFunction") + "iconcat");
	}
	
	public void prepare(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("iconcat" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			exp = param.getLeafExpression();
		} else if (param.getSubSize() == 2) {
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("iconcat" + mm.getMessage("function.invalidParam"));
			}
			
			exp = sub.getLeafExpression();
			sub = param.getSub(1);
			
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("iconcat" + mm.getMessage("function.paramTypeError"));
				}

				sep = (String)obj;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("iconcat" + mm.getMessage("function.invalidParam"));
		}
		
		if (option != null) {
			if (option.indexOf('c') != -1) sep = ",";
			if (option.indexOf('q') != -1) addQuotes = true;
			if (option.indexOf('i') != -1) addSingleQuotes = true;
			if (option.indexOf('0') != -1) deleteNull = true;
		}
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("concat" + mm.getMessage("function.missingParam"));
		}
	}

	/**
	 * 把一条记录计算出的数据，添加到临时中间数据
	 */
	public Object gather(Context ctx) {
		Object val = exp.calculate(ctx);
		if (val instanceof HashLinkSet) {
			return val;
		} else  {
			HashLinkSet set = new HashLinkSet();
			set.put(val);
			return set;
		}
	}
	
	/**
	 * 把其它数据整合到临时中间数据
	 * @param oldValue 前面数据的汇总值
	 * @param ctx 上下文变量
	 */
	public Object gather(Object oldValue, Context ctx) {
		HashLinkSet set = (HashLinkSet)oldValue;
		Object val = exp.calculate(ctx);
		
		if (val instanceof HashLinkSet) {
			set.putAll((HashLinkSet)val);
		} else {
			set.put(val);
		}
		
		return set;
	}
	
	/**
	 * 取二次汇总时该聚合字段对应的表达式
	 * @param q	当前汇总字段的序号
	 * @return	汇总表达式
	 */
	public Expression getRegatherExpression(int q) {
		String exp = "iconcat";
		if (option != null) {
			exp += "@" + option;
		}
		
		if (sep == null) {
			exp += "(#" + q + ")";
		} else {
			exp += "(#" + q + ",\"" + sep + "\")";
		}
		
		return new Expression(exp);
	}
	
	/**
	 * 是否需要根据中间结果，统计生成最终结果
	 */
	public boolean needFinish() {
		return true;
	}
	
	/**
	 * 对分组结束得到的汇总列进行最终处理
	 * @param array 计算列的值
	 * @return IArray
	 */
	public IArray finish(IArray array) {
		int size = array.size();
		StringArray result = new StringArray(size);
		
		for (int i = 1; i <= size; ++i) {
			Object val = array.get(i);
			String str = (String)finish(val);
			result.push(str);
		}
		
		return result;
	}
	
	/**
	 * 统计临时中间数据，生成最终结果。
	 */
	public Object finish(Object val) {
		HashLinkSet set = (HashLinkSet)val;
		IArray array = set.getElementArray();
		StringBuffer out = new StringBuffer();
		
		for (int i = 1, size = array.size(); i <= size; ++i) {
			Object obj = array.get(i);
			if (obj != null) {
				if (deleteNull && obj instanceof String && ((String)obj).length() == 0) {
					continue;
				}
				
				if (sep != null && out.length() > 0) {
					out.append(sep);
				}
				
				if (addQuotes) {
					if (obj instanceof String) {
						out.append(Escape.addEscAndQuote((String)obj));
					} else {
						out.append(obj.toString());
					}
				} else if (addSingleQuotes) {
					if (obj instanceof String) {
						out.append('\'');
						out.append((String)obj);
						out.append('\'');
					} else {
						out.append(obj.toString());
					}				
				} else {
					out.append(obj.toString());
				}
			} else {
				if (i > 1 && !deleteNull) {
					out.append(sep);
				}
			}
		}
		
		return out.toString();
	}
	
	public Expression getExp() {
		return exp;
	}

	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		IArray array = exp.calculateAll(ctx);
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				HashLinkSet set = new HashLinkSet();
				set.put(array.get(i));
				result.add(set);
			} else {
				HashLinkSet set = (HashLinkSet)result.get(resultSeqs[i]);
				set.put(array.get(i));
			}
		}
		
		return result;
	}
	
	/**
	 * 多线程分组的二次汇总运算
	 * @param result 一个线程的分组结果
	 * @param result2 另一个线程的分组结果
	 * @param seqs 另一个线程的分组跟第一个线程分组的对应关系
	 * @param ctx 计算上下文
	 * @return
	 */
	public void gather2(IArray result, IArray result2, int []seqs, Context ctx) {
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0) {
				HashLinkSet value1 = (HashLinkSet) result.get(seqs[i]);
				value1.putAll((HashLinkSet)result2.get(i));
			}
		}
	}
}
