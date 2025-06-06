package com.scudata.expression.fn.string;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.array.StringArray;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * concat(xi,…) 将参数连接成为字符串，且串拼入时不加引号。
 * @author runqian
 *
 */
public class Concat extends Gather {
	private Expression exp;
	private String sep = null; // 分隔符
	private boolean addQuotes = false;
	private boolean addSingleQuotes = false;
	private boolean deleteNull = false;
	
	public void prepare(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("concat" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			exp = param.getLeafExpression();
		} else if (param.getSubSize() == 2) {
			IParam sub = param.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("concat" + mm.getMessage("function.invalidParam"));
			}
			
			exp = sub.getLeafExpression();
			sub = param.getSub(1);
			
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("concat" + mm.getMessage("function.paramTypeError"));
				}

				sep = (String)obj;
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("concat" + mm.getMessage("function.invalidParam"));
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

	private static void concat(Object obj, StringBuffer out) {
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				concat(seq.getMem(i), out);
			}
		} else if (obj != null) {
			out.append(obj.toString());
		}
	}
	
	private void gather(Object obj, StringBuffer out) {
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			for (int i = 1, len = seq.length(); i <= len; ++i) {
				gather(seq.getMem(i), out);
			}
		} else if (obj instanceof StringBuffer) {
			// 多线程二次汇总
			StringBuffer sb = (StringBuffer)obj;
			if (deleteNull) {
				if (sb.length() == 0) {
					return;
				} else if (out.length() == 0) {
					out.append(sb);
					return;
				}
			}
			
			if (sep != null) {
				out.append(sep);
			}
			
			out.append(sb);
		} else if (obj != null) {
			if (deleteNull && obj instanceof String && ((String)obj).length() == 0) {
				return;
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
			if (out.length() > 0 && !deleteNull) {
				out.append(sep);
			}
		}
	}
	
	public Object calculate(Context ctx) {
		StringBuffer sb = new StringBuffer();
		if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			concat(obj, sb);
		} else {
			for (int i = 0, size = param.getSubSize(); i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("concat" + mm.getMessage("function.invalidParam"));
				}
				
				Object obj = sub.getLeafExpression().calculate(ctx);
				concat(obj, sb);
			}
		}

		return sb.toString();
	}
	
	public Object gather(Context ctx) {
		Object obj = exp.calculate(ctx);
		if (obj instanceof StringBuffer) {
			return obj;
		}
		
		StringBuffer sb = new StringBuffer();
		gather(obj, sb);
		return sb;
	}

	public Object gather(Object oldValue, Context ctx) {
		Object obj = exp.calculate(ctx);
		gather(obj, (StringBuffer)oldValue);
		return oldValue;
	}

	public Expression getRegatherExpression(int q) {
		if (sep == null) {
			String str = "concat(#" + q + ")";
			return new Expression(str);
		} else {
			String str = "concat(#" + q + ",\"" + sep + "\")";
			return new Expression(str);
		}
	}
	
	public boolean needFinish() {
		return true;
	}
	
	public Object finish(Object val) {
		return val.toString();
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
				StringBuffer sb = new StringBuffer();
				gather(array.get(i), sb);
				result.add(sb);
			} else {
				StringBuffer sb = (StringBuffer)result.get(resultSeqs[i]);
				gather(array.get(i), sb);
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
				StringBuffer sb1 = (StringBuffer)result.get(seqs[i]);
				StringBuffer sb2 = (StringBuffer)result2.get(i);
				if (sb1 == null) {
					result.set(seqs[i], sb2);
				} else {
					// 多线程二次汇总
					if (sep != null) {
						sb1.append(sep);
					}
					
					sb1.append(sb2.toString());
				}
			}
		}
	}
	
	/**
	 * 对分组结束得到的汇总列进行最终处理
	 * @param array 计算列的值
	 * @return IArray
	 */
	public IArray finish(IArray array) {
		int len = array.size();
		StringArray stringArray = new StringArray(len);
		
		for (int i = 1; i <= len; ++i) {
			Object stringBuffer = array.get(i);
			stringArray.push(stringBuffer.toString());
		}
		
		return stringArray;
	}
}