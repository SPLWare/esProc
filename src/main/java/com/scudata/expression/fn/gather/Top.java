package com.scudata.expression.fn.gather;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Env;
import com.scudata.dm.Sequence;
import com.scudata.dm.comparator.ArrayComparator;
import com.scudata.dm.comparator.BaseComparator;
import com.scudata.dm.comparator.DescComparator;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;
import com.scudata.util.MinHeap;

/**
 * 取排名前几的元素
 * top(n,x) top(n;x,…) top(n,y,x)
 * @author RunQian
 *
 */
public class Top extends Gather {
	private int count = 1;
	private Expression exp; // 比较表达式
	private Expression getExp; // 结果集取值表达式
	private boolean isCurrent; // 是否取当前记录
	
	private boolean isPositive = true; // n是否是正数
	private boolean isOne = false; // 是否有@1选项
	private boolean isSame = false; // 是否取所有最大或最小的
	private boolean isRank = false; // 是否用排名方式取前几
	private boolean isDistinct = false; // 是否按去重方式算排名
	
	private Comparator<Object> comparator;
	private int expIndex = -1; // 比较表达式的字段索引

	public void prepare(Context ctx) {
		if (option != null) {
			if (option.indexOf('1') != -1) isOne = true;
			if (option.indexOf('i') != -1) {
				isRank = true;
				isDistinct = true;
			} else if (option.indexOf('r') != -1) {
				isRank = true;
			}
		}
		
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("top" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			}
			
			if (sub1.isLeaf()) {
				exp = sub1.getLeafExpression();
			} else {
				//ValueList valueList = new ValueList();
				//valueList.setParam(sub1);
				//exp = new Expression(valueList);
				
				String []strs = sub1.toStringArray("top", false);
				StringBuffer sb = new StringBuffer(64);
				sb.append('[');
				for (int i = 0; i < strs.length; ++i) {
					if (i > 0) {
						sb.append(',');
					}
					
					sb.append(strs[i]);
				}
				
				sb.append(']');
				exp = new Expression(cs, ctx, sb.toString());
			}
			
			getExp = new Expression(ctx, "~");
			isCurrent = true;
		} else {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				count = ((Number)obj).intValue();
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.paramTypeError"));
			}
			
			IParam sub1 = param.getSub(1);
			if (sub1 != null) exp = sub1.getLeafExpression();
			
			if (size > 2) {
				IParam sub2 = param.getSub(2);
				if (sub2 != null) {
					getExp = sub2.getLeafExpression();
					if (getExp.getHome() instanceof CurrentElement) {
						isCurrent = true;
					}
				}
			}
		}

		if (exp == null) {
			exp = new Expression(ctx, "~");
		} else if (exp.isConstExpression()) {
			exp = null;
		}
		
		if (count == 0) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("top" + mm.getMessage("function.invalidParam"));
		} else if (count < 0) {
			count = -count;
			isPositive = false;
		}
		
		if (count == 1 && !isOne && exp != null) {
			isSame = true;
		}
		
		if (exp != null) {
			if (getExp != null) {
				comparator = new ArrayComparator(1);
			} else {
				comparator = new BaseComparator();
			}
			
			if (!isPositive) {
				comparator = new DescComparator(comparator);
			}
		}
	}
	
	public Expression getRegatherExpression(int q) {
		String str = "top@2";
		if (option != null) {
			str += option;
		}
		
		if (isPositive) {
			str += "(";
		} else {
			str += "(-";
		}
		
		if (exp == null) { // top(n,0) -> top(n,0,#q)
			str += count + ",0,#" + q + ')';
		} else if (getExp == null) { // top(n,x) -> top(n, ~, #q)
			str += count + ",~,#" + q + ')';
		} else { // top(n,x, y) -> top(n, x, #q)
			str += count + "," + exp.toString() + ",#" + q + ')';
		}
		
		return new Expression(str);
	}

	public int getCount() {
		return count;
	}

	public Expression getExp() {
		return exp;
	}

	public Expression getGetExp() {
		return getExp;
	}

	public boolean isCurrent() {
		return isCurrent;
	}

	public boolean isOne() {
		return isOne;
	}

	public boolean isSame() {
		return isSame;
	}

	public Comparator<Object> getComparator() {
		return comparator;
	}

	public int getExpIndex() {
		return expIndex;
	}
	
	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		throw new RQException(mm.getMessage("Expression.unknownFunction") + "top");
	}
	
	public boolean isPositive() {
		return isPositive;
	}

	public boolean needFinish() {
		return !isSame || getExp != null || isRank;
	}
	
	public boolean needFinish1() {
		return !isSame || getExp != null || isRank;
	}

	public Object finish1(Object val) {
		return finish(val);
	}
	
	public IArray finish1(IArray array) {
		return finish(array);
	}
	
	public IArray finish(IArray array) {
		int size = array.size();
		if (isRank) {
			for (int i = 1; i <= size; ++i) {
				RankArray rankArray = (RankArray)array.get(i);
				ObjectArray valueArray = rankArray.getValueArray();
				
				if (getExp != null) {
					for (int v = 1, vcount = valueArray.size(); v <= vcount; ++v) {
						Object []tmp = (Object[])valueArray.get(v);
						valueArray.set(v, tmp[1]);
					}
				}
				
				array.set(i, new Sequence(valueArray));
			}
		} else if (isSame) {
			for (int i = 1; i <= size; ++i) {
				Sequence seq = (Sequence)array.get(i);
				if (seq != null) {
					IArray mems = seq.getMems();
					for (int m = 1, len = mems.size(); m <= len; ++m) {
						Object []tmp = (Object[])seq.getMem(m);
						mems.set(m, tmp[1]);
					}
				}
			}
		} else if (exp != null) {
			boolean ifOne = count == 1 && isOne;
			for (int i = 1; i <= size; ++i) {
				MinHeap heap = (MinHeap)array.get(i);
				int len = heap.size();
				if (len == 0) {
					array.set(i, null);
				} else if (ifOne) {
					Object obj = heap.getTop();
					if (getExp == null) {
						array.set(i, obj);
					} else {
						Object []tmp = (Object[])obj;
						array.set(i, tmp[1]);
					}
				} else {
					Object []objs = heap.toArray();
					Arrays.sort(objs, comparator);
					
					if (getExp == null) {
						array.set(i, new Sequence(objs));
					} else {
						Sequence seq = new Sequence(len);
						for (int m = 0; m < len; ++m) {
							Object []tmp = (Object[])objs[m];
							seq.add(tmp[1]);
						}
						
						array.set(i, seq);
					}
				}
			}
		} else {
			boolean ifOne = count == 1 && isOne;
			for (int i = 1; i <= size; ++i) {
				List<Object> list = (List<Object>)array.get(i);
				if (list.size() == 0) {
					array.set(i, null);
				} else if (ifOne) {
					array.set(i, list.get(0));
				} else {
					array.set(i, new Sequence(list.toArray()));
				}
			}
		}
		
		return array;
	}
	
	public Object finish(Object val) {
		if (val == null) {
			return null;
		} else if (isRank) {
			ObjectArray array = ((RankArray)val).getValueArray();
			
			if (getExp != null) {
				for (int i = 1, size = array.size(); i <= size; ++i) {
					Object []tmp = (Object[])array.get(i);
					array.set(i, tmp[1]);
				}
			}
			
			return new Sequence(array);
		} else if (isSame) {
			if (val instanceof Sequence) {
				Sequence seq = (Sequence)val;
				for (int i = 1, size = seq.length(); i <= size; ++i) {
					Object []tmp = (Object[])seq.getMem(i);
					seq.set(i, tmp[1]);
				}
				
				return seq;
			}

			return val;
		} else if (exp != null) {
			MinHeap heap = (MinHeap)val;
			int size = heap.size();
			if (size == 0) return null;
			
			if (count == 1 && isOne) {
				Object obj = heap.getTop();
				if (getExp == null) {
					return obj;
				} else {
					Object []tmp = (Object[])obj;
					return tmp[1];
				}
			} else {
				Object []objs = heap.toArray();
				Arrays.sort(objs, comparator);
				
				if (getExp == null) {
					return new Sequence(objs);
				} else {
					Sequence seq = new Sequence(size);
					for (int i = 0; i < size; ++i) {
						Object []tmp = (Object[])objs[i];
						seq.add(tmp[1]);
					}
					
					return seq;
				}
			}
		} else {
			List<Object> list = (List<Object>)val;
			if (list.size() == 0) return null;
			
			if (count == 1 && isOne) {
				return list.get(0);
			} else {
				return new Sequence(list.toArray());
			}
		}
	}
	
	// 用于top(1,x)取所有最大或最小的
	private static void addToSequence(Sequence seq, Object obj, Comparator<Object> comparator) {
		if (obj == null) {
			return;
		} else {
			if (seq.length() == 0) {
				seq.add(obj);
			} else {
				int cmp = comparator.compare(seq.getMem(1), obj);
				if (cmp > 0) {
					seq.clear();
					seq.add(obj);
				} else if (cmp == 0) {
					seq.add(obj);
				}
			}
		}
	}
	
	private static void addToSequence(Sequence seq, Object obj, Expression exp, Context ctx, Comparator<Object> comparator) {
		ComputeStack stack = ctx.getComputeStack();
		try {
			if (obj instanceof Sequence) {
				Sequence tmpSeq = (Sequence)obj;
				Current current = new Current(tmpSeq);
				stack.push(current);
				
				for (int i = 1, len = tmpSeq.length(); i <= len; ++i) {
					current.setCurrent(i);
					Object val = exp.calculate(ctx);
					
					if (val != null) {
						Object []vals = new Object[2];
						vals[0] = exp.calculate(ctx);
						vals[1] = current.getCurrent();
						
						if (seq.length() == 0) {
							seq.add(vals);
						} else {
							int cmp = comparator.compare(seq.getMem(1), vals);
							if (cmp > 0) {
								seq.clear();
								seq.add(vals);
							} else if (cmp == 0) {
								seq.add(vals);
							}
						}
					}
				}
			} else if (obj instanceof BaseRecord) {
				stack.push((BaseRecord)obj);
				Object val = exp.calculate(ctx);
				
				if (val != null) {
					Object []vals = new Object[2];
					vals[0] = val;
					vals[1] = obj;
					
					if (seq.length() == 0) {
						seq.add(vals);
					} else {
						int cmp = comparator.compare(seq.getMem(1), vals);
						if (cmp > 0) {
							seq.clear();
							seq.add(vals);
						} else if (cmp == 0) {
							seq.add(vals);
						}
					}
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
		} finally {
			stack.pop();
		}
	}
	
	private static void addToHeap(MinHeap heap, Object obj) {
		if (obj != null) {
			heap.insert(obj);
		}
	}
	
	private void addToHeap(MinHeap heap, Object obj, Expression exp, Context ctx) {
		if (obj instanceof Sequence) {
			ComputeStack stack = ctx.getComputeStack();
			try {
				Sequence seq = (Sequence)obj;
				Current current = new Current(seq);
				stack.push(current);
				
				for (int i = 1, len = seq.length(); i <= len; ++i) {
					current.setCurrent(i);
					Object val = exp.calculate(ctx);
					
					if (val != null) {
						Object []vals = new Object[2];
						vals[0] = exp.calculate(ctx);
						vals[1] = current.getCurrent();
						heap.insert(vals);
					}
				}
			} finally {
				stack.pop();
			}
		} else if (obj instanceof BaseRecord) {
			if (isCurrent) {
				Object val = exp.calculate(ctx);
				
				if (val != null) {
					Object []vals = new Object[2];
					vals[0] = val;
					vals[1] = obj;
					heap.insert(vals);
				}
			} else {
				ComputeStack stack = ctx.getComputeStack();
				try {
					stack.push((BaseRecord)obj);
					Object val = exp.calculate(ctx);
					
					if (val != null) {
						Object []vals = new Object[2];
						vals[0] = val;
						vals[1] = obj;
						heap.insert(vals);
					}
				} finally {
					stack.pop();
				}
			}
		} else if (obj != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("top" + mm.getMessage("function.invalidParam"));
		}
	}
	
	private static void addToHeap(MinHeap heap, Object obj, int expIndex) {
		Object val = ((BaseRecord)obj).getFieldValue(expIndex);
		if (val != null) {
			Object []vals = new Object[2];
			vals[0] = val;
			vals[1] = obj;
			heap.insert(vals);
		}
	}
	
	private static void addToArrayList(ArrayList<Object> list, Object obj, int count) {
		int size = list.size();
		if (size == count || obj == null) return;
		
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			IArray mems = seq.getMems();
			int len = mems.size();
			for (int i = 1; i <= len && size < count; ++i) {
				Object m = mems.get(i);
				if (m != null) {
					list.add(m);
					size++;
				}
			}
		} else {
			list.add(obj);
		}
	}
	
	private static void addToLinkedList(LinkedList<Object> list, Object obj, int count) {
		if (obj == null) return;
		
		int size = list.size();
		if (obj instanceof Sequence) {
			Sequence seq = (Sequence)obj;
			IArray mems = seq.getMems();
			int len = mems.size();
			for (int i = 1; i <= len; ++i) {
				Object m = mems.get(i);
				if (m != null) {
					if (size == count) {
						list.removeFirst();
					} else {
						size++;
					}
					
					list.add(m);
				}
			}
		} else {
			if (size == count) {
				list.removeFirst();
			}
			
			list.add(obj);
		}
	}
	
	public Object gather(Context ctx) {
		if (count == 0) {
			return null;
		} else if (isRank) {
			RankArray array = new RankArray(count, isDistinct);
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToRankArray(array, obj, comparator);
			} else {
				Object obj = getExp.calculate(ctx);
				addToRankArray(array, obj, exp, ctx, comparator);
			}
			
			return array;
		} else if (isSame) {
			Sequence seq = new Sequence();
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToSequence(seq, obj, comparator);
			} else {
				Object obj = getExp.calculate(ctx);
				addToSequence(seq, obj, exp, ctx, comparator);
			}
			
			return seq;
		} else if (exp != null) {
			MinHeap heap = new MinHeap(count, comparator);
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToHeap(heap, obj);
			} else {
				Object obj = getExp.calculate(ctx);
				if (obj instanceof BaseRecord) {
					expIndex = ((BaseRecord)obj).getFieldIndex(exp.getIdentifierName());
				}
				
				addToHeap(heap, obj, exp, ctx);
			}
			
			return heap;
		} else {
			Object obj;
			if (getExp == null) {
				Object top = ctx.getComputeStack().getTopObject();
				obj = ((Current)top).getCurrent();
			} else {
				obj = getExp.calculate(ctx);
			}

			if (isPositive) {
				ArrayList<Object> list = new ArrayList<Object>(count);
				addToArrayList(list, obj, count);
				return list;
			} else {
				LinkedList<Object> list = new LinkedList<Object>();
				addToLinkedList(list, obj, count);
				return list;
			}
		}
	}

	public Object gather(Object oldValue, Context ctx) {
		if (isRank) {
			RankArray array = (RankArray)oldValue;
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToRankArray(array, obj, comparator);
			} else {
				Object obj = getExp.calculate(ctx);
				addToRankArray(array, obj, exp, ctx, comparator);
			}
			
			return array;
		} else if (isSame) {
			Sequence seq;
			if (oldValue != null) {
				seq = (Sequence)oldValue;
			} else {
				seq = new Sequence();
			}
			
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToSequence(seq, obj, comparator);
			} else {
				Object obj = getExp.calculate(ctx);
				addToSequence(seq, obj, exp, ctx, comparator);
			}
			
			return seq;
		} else if (exp != null) {
			if (getExp == null) {
				Object obj = exp.calculate(ctx);
				addToHeap((MinHeap)oldValue, obj);
			} else if (expIndex >= 0) {
				Object obj = getExp.calculate(ctx);
				addToHeap((MinHeap)oldValue, obj, expIndex);
			} else {
				Object obj = getExp.calculate(ctx);
				addToHeap((MinHeap)oldValue, obj, exp, ctx);
			}
		} else {
			Object obj;
			if (getExp == null) {
				Object top = ctx.getComputeStack().getTopObject();
				obj = ((Current)top).getCurrent();
			} else {
				obj = getExp.calculate(ctx);
			}

			if (isPositive) {
				addToArrayList((ArrayList<Object>)oldValue, obj, count);
			} else {
				addToLinkedList((LinkedList<Object>)oldValue, obj, count);
			}
		}
		
		return oldValue;
	}
	
	// 取所有最小值
	private IArray minAll(IArray result, int []resultSeqs, Context ctx) {
		Expression exp = this.exp;
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		IArray array = exp.calculateAll(ctx);
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				if (array.isNull(i)) {
					result.add(null);
				} else {
					IArray resultArray = array.newInstance(8);
					resultArray.push(array, i);
					Sequence seq = new Sequence(resultArray);
					result.add(seq);
				}
			} else if (!array.isNull(i)) {
				Sequence seq = (Sequence)result.get(resultSeqs[i]);
				if (seq == null) {
					IArray resultArray = array.newInstance(8);
					resultArray.push(array, i);
					seq = new Sequence(resultArray);
					result.set(resultSeqs[i], seq);
				} else {
					IArray mems = seq.getMems();
					int cmp = mems.compareTo(1, array, i);
					if (cmp == 0) {
						mems.add(array, i);
					} else if(cmp > 0) {
						mems.clear();
						mems.push(array, i);
					}
				}
			}
		}
		
		return result;
	}
		
	// 取所有使指定表达式最大或最小的记录
	private IArray topAll(IArray result, int []resultSeqs, Context ctx) {
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		Expression exp = this.exp;
		Comparator<Object> comparator = this.comparator;
		IArray array = getExp.calculateAll(ctx);
		
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				Sequence seq = new Sequence();
				addToSequence(seq, array.get(i), exp, ctx, comparator);
				result.add(seq);
			} else {
				Sequence seq = (Sequence)result.get(resultSeqs[i]);
				addToSequence(seq, array.get(i), exp, ctx, comparator);
			}
		}
		
		return result;
	}
	
	// 取最大值
	/*private IArray max(IArray result, int []resultSeqs, Context ctx) {
		Expression exp = this.exp;
		if (getExp == null) {
			IArray array = exp.calculateAll(ctx);
			if (result == null) {
				result = array.newInstance(Env.INITGROUPSIZE);
			}
			
			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					result.add(array, i);
				} else if (!array.isNull(i)) {
					if (result.isNull(resultSeqs[i])) {
						result.set(resultSeqs[i], array, i);
					} else {
						if(result.compareTo(resultSeqs[i], array, i) < 0) {
							result.set(resultSeqs[i], array, i);
						}
					}
				}
			}
		} else {
			if (result == null) {
				result = new ObjectArray(Env.INITGROUPSIZE);
			}
			
			IArray array = getExp.calculateAll(ctx);
			IArray valueArray;
			if (isCurrent) {
				valueArray = exp.calculateAll(ctx);
			} else {
				Sequence seq = new Sequence(array);
				seq = seq.calc(exp, ctx);
				valueArray = seq.getMems();
			}

			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					if (valueArray.isNull(i)) {
						result.add(null);
					} else {
						result.add(array, i);
					}
				} else if (!valueArray.isNull(i)) {
					Object r = result.get(resultSeqs[i]);
					if (r == null) {
						result.set(resultSeqs[i], array, i);
					} else if (r instanceof BaseRecord) {
						Object value = ((BaseRecord)r).calc(exp, ctx);
						int cmp = valueArray.compareTo(i, value);
						if(cmp > 0) {
							result.set(resultSeqs[i], array, i);
						}
					} else if (r instanceof Sequence) {
						Object value = ((Sequence)r).calc(1, exp, ctx);
						int cmp = valueArray.compareTo(i, value);
						if(cmp > 0) {
							result.set(resultSeqs[i], array, i);
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("top" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		}
		
		return result;
	}
	
	// 取最小值
	private IArray min(IArray result, int []resultSeqs, Context ctx) {
		Expression exp = this.exp;
		if (getExp == null) {
			IArray array = exp.calculateAll(ctx);
			if (result == null) {
				result = array.newInstance(Env.INITGROUPSIZE);
			}
			
			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					result.add(array, i);
				} else if (!array.isNull(i)) {
					if (result.isNull(resultSeqs[i])) {
						result.set(resultSeqs[i], array, i);
					} else {
						if(result.compareTo(resultSeqs[i], array, i) > 0) {
							result.set(resultSeqs[i], array, i);
						}
					}
				}
			}
		} else {
			if (result == null) {
				result = new ObjectArray(Env.INITGROUPSIZE);
			}
			
			IArray array = getExp.calculateAll(ctx);
			IArray valueArray;
			if (isCurrent) {
				valueArray = exp.calculateAll(ctx);
			} else {
				Sequence seq = new Sequence(array);
				seq = seq.calc(exp, ctx);
				valueArray = seq.getMems();
			}

			for (int i = 1, len = array.size(); i <= len; ++i) {
				if (result.size() < resultSeqs[i]) {
					if (valueArray.isNull(i)) {
						result.add(null);
					} else {
						result.add(array, i);
					}
				} else if (!valueArray.isNull(i)) {
					Object r = result.get(resultSeqs[i]);
					if (r == null) {
						result.set(resultSeqs[i], array, i);
					} else if (r instanceof BaseRecord) {
						Object value = ((BaseRecord)r).calc(exp, ctx);
						int cmp = valueArray.compareTo(i, value);
						if(cmp < 0) {
							result.set(resultSeqs[i], array, i);
						}
					} else if (r instanceof Sequence) {
						Object value = ((Sequence)r).calc(1, exp, ctx);
						int cmp = valueArray.compareTo(i, value);
						if(cmp < 0) {
							result.set(resultSeqs[i], array, i);
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("top" + mm.getMessage("function.paramTypeError"));
					}
				}
			}
		}
		
		return result;
	}*/
	
	// 取所有最大的
	private IArray maxAll(IArray result, int []resultSeqs, Context ctx) {
		Expression exp = this.exp;
		if (result == null) {
			result = new ObjectArray(Env.INITGROUPSIZE);
		}
		
		IArray array = exp.calculateAll(ctx);
		for (int i = 1, len = array.size(); i <= len; ++i) {
			if (result.size() < resultSeqs[i]) {
				if (array.isNull(i)) {
					result.add(null);
				} else {
					IArray resultArray = array.newInstance(8);
					resultArray.push(array, i);
					Sequence seq = new Sequence(resultArray);
					result.add(seq);
				}
			} else if (!array.isNull(i)) {
				Sequence seq = (Sequence)result.get(resultSeqs[i]);
				if (seq == null) {
					IArray resultArray = array.newInstance(8);
					resultArray.push(array, i);
					seq = new Sequence(resultArray);
					result.set(resultSeqs[i], seq);
				} else {
					IArray mems = seq.getMems();
					int cmp = mems.compareTo(1, array, i);
					if (cmp == 0) {
						mems.add(array, i);
					} else if(cmp < 0) {
						mems.clear();
						mems.push(array, i);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 计算所有记录的值，汇总到结果数组上
	 * @param result 结果数组
	 * @param resultSeqs 每条记录对应的结果数组的序号
	 * @param ctx 计算上下文
	 * @return IArray 结果数组
	 */
	public IArray gather(IArray result, int []resultSeqs, Context ctx) {
		if (isRank) {
			Comparator<Object> comparator = this.comparator;
			Expression exp = this.exp;
			if (result == null) {
				result = new ObjectArray(Env.INITGROUPSIZE);
			}
			
			if (getExp == null) {
				IArray array;
				if (exp != null) {
					array = exp.calculateAll(ctx);
				} else {
					Sequence topSequence = ctx.getComputeStack().getTopSequence();
					array = topSequence.getMems();
				}
				
				for (int i = 1, len = array.size(); i <= len; ++i) {
					if (result.size() < resultSeqs[i]) {
						RankArray rankArray = new RankArray(count, isDistinct);
						addToRankArray(rankArray, array.get(i), comparator);
						result.add(rankArray);
					} else {
						RankArray rankArray = (RankArray)result.get(resultSeqs[i]);
						addToRankArray(rankArray, array.get(i), comparator);
					}
				}
			} else {
				IArray array = getExp.calculateAll(ctx);
				for (int i = 1, len = array.size(); i <= len; ++i) {
					if (result.size() < resultSeqs[i]) {
						RankArray rankArray = new RankArray(count, isDistinct);
						addToRankArray(rankArray, array.get(i), exp, ctx, comparator);
						result.add(rankArray);
					} else {
						RankArray rankArray = (RankArray)result.get(resultSeqs[i]);
						addToRankArray(rankArray, array.get(i), exp, ctx, comparator);
					}
				}
			}
			
			return result;
		} else if (isSame) {
			if (getExp != null) {
				return topAll(result, resultSeqs, ctx);
			} else if (isPositive) {
				return minAll(result, resultSeqs, ctx);
			} else {
				return maxAll(result, resultSeqs, ctx);
			}
		}

		int count = this.count;
		Expression exp = this.exp;
		Comparator<Object> comparator = this.comparator;
		
		if (getExp == null) {
			if (result == null) {
				result = new ObjectArray(Env.INITGROUPSIZE);
			}
			
			if (exp != null) {
				IArray array = exp.calculateAll(ctx);
				for (int i = 1, len = array.size(); i <= len; ++i) {
					if (result.size() < resultSeqs[i]) {
						MinHeap heap = new MinHeap(count, comparator);
						addToHeap(heap, array.get(i));
						result.add(heap);
					} else {
						MinHeap heap = (MinHeap)result.get(resultSeqs[i]);
						addToHeap(heap, array.get(i));
					}
				}
			} else {
				Sequence topSequence = ctx.getComputeStack().getTopSequence();
				IArray array = topSequence.getMems();
				
				if (isPositive) {
					for (int i = 1, len = array.size(); i <= len; ++i) {
						if (result.size() < resultSeqs[i]) {
							ArrayList<Object> list = new ArrayList<Object>(count);
							addToArrayList(list, array.get(i), count);
							result.add(list);
						} else {
							ArrayList<Object> list = (ArrayList<Object>)result.get(resultSeqs[i]);
							addToArrayList(list, array.get(i), count);
						}
					}
				} else {
					for (int i = 1, len = array.size(); i <= len; ++i) {
						if (result.size() < resultSeqs[i]) {
							LinkedList<Object> list = new LinkedList<Object>();
							addToLinkedList(list, array.get(i), count);
							result.add(list);
						} else {
							LinkedList<Object> list = (LinkedList<Object>)result.get(resultSeqs[i]);
							addToLinkedList(list, array.get(i), count);
						}
					}
				}
			}
		} else {
			IArray array = getExp.calculateAll(ctx);
			if (result == null) {
				result = new ObjectArray(Env.INITGROUPSIZE);
				if (exp != null) {
					Object obj = array.get(1);
					if (obj instanceof BaseRecord) {
						expIndex = ((BaseRecord)obj).getFieldIndex(exp.getIdentifierName());
					}
				}
			}
			
			if (expIndex > -1) {
				int expIndex = this.expIndex;
				for (int i = 1, len = array.size(); i <= len; ++i) {
					if (result.size() < resultSeqs[i]) {
						MinHeap heap = new MinHeap(count, comparator);
						addToHeap(heap, array.get(i), expIndex);
						result.add(heap);
					} else {
						MinHeap heap = (MinHeap)result.get(resultSeqs[i]);
						addToHeap(heap, array.get(i), expIndex);
					}
				}
			} else if (exp != null) {
				Current current = ctx.getComputeStack().getTopCurrent();
				for (int i = 1, len = array.size(); i <= len; ++i) {
					current.setCurrent(i);
					if (result.size() < resultSeqs[i]) {
						MinHeap heap = new MinHeap(count, comparator);
						addToHeap(heap, array.get(i), exp, ctx);
						result.add(heap);
					} else {
						MinHeap heap = (MinHeap)result.get(resultSeqs[i]);
						addToHeap(heap, array.get(i), exp, ctx);
					}
				}
			} else {
				if (isPositive) {
					for (int i = 1, len = array.size(); i <= len; ++i) {
						if (result.size() < resultSeqs[i]) {
							ArrayList<Object> list = new ArrayList<Object>(count);
							addToArrayList(list, array.get(i), count);
							result.add(list);
						} else {
							ArrayList<Object> list = (ArrayList<Object>)result.get(resultSeqs[i]);
							addToArrayList(list, array.get(i), count);
						}
					}
				} else {
					for (int i = 1, len = array.size(); i <= len; ++i) {
						if (result.size() < resultSeqs[i]) {
							LinkedList<Object> list = new LinkedList<Object>();
							addToLinkedList(list, array.get(i), count);
							result.add(list);
						} else {
							LinkedList<Object> list = (LinkedList<Object>)result.get(resultSeqs[i]);
							addToLinkedList(list, array.get(i), count);
						}
					}
				}
			}
		}

		return result;
	}
	
	private void minAll(IArray result, IArray result2, int []seqs) {
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0) {
				Sequence sequence1 = (Sequence)result.get(seqs[i]);
				Sequence sequence2 = (Sequence)result2.get(i);
				if (sequence1 == null) {
					result.set(seqs[i], sequence2);
				} else if (sequence2 != null) {
					int cmp = sequence1.getMems().compareTo(1, sequence2.getMems(), 1);
					if (cmp > 0) {
						result.set(seqs[i], sequence2);
					} else if (cmp == 0) {
						sequence1.addAll(sequence2);
					}
				}
			}
		}
	}
	
	private void maxAll(IArray result, IArray result2, int []seqs) {
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0) {
				Sequence sequence1 = (Sequence)result.get(seqs[i]);
				Sequence sequence2 = (Sequence)result2.get(i);
				if (sequence1 == null) {
					result.set(seqs[i], sequence2);
				} else if (sequence2 != null) {
					int cmp = sequence1.getMems().compareTo(1, sequence2.getMems(), 1);
					if (cmp < 0) {
						result.set(seqs[i], sequence2);
					} else if (cmp == 0) {
						sequence1.addAll(sequence2);
					}
				}
			}
		}
	}
	
	// 取所有使指定表达式最大或最小的记录
	private void topAll(IArray result, IArray result2, int []seqs, Context ctx) {
		Expression exp = this.exp;
		Comparator<Object> comparator = this.comparator;
		
		for (int i = 1, len = result2.size(); i <= len; ++i) {
			if (seqs[i] != 0) {
				Sequence sequence1 = (Sequence)result.get(seqs[i]);
				Sequence sequence2 = (Sequence)result2.get(i);
				if (sequence1 == null) {
					result.set(seqs[i], sequence2);
				} else if (sequence2 != null) {
					int cmp = comparator.compare(sequence1.calc(1, exp, ctx), sequence2.calc(1, exp, ctx));
					if (cmp > 0) {
						result.set(seqs[i], sequence2);
					} else if (cmp == 0) {
						sequence1.addAll(sequence2);
					}
				}
			}
		}
	}
	
	private static void addToArrayList(ArrayList<Object> list1, ArrayList<Object> list2, int count) {
		int size1 = list1.size();
		if (size1 == count) {
			return;
		}
		
		int diff = count - size1;
		int size2 = list2.size();
		if (size2 < diff) {
			diff = size2;
		}
		
		for (int i = 0; i < diff; ++i) {
			list1.add(list2.get(i));
		}
	}
	
	private static void addToLinkedList(LinkedList<Object> list1, LinkedList<Object> list2, int count) {
		int size2 = list2.size();
		if (size2 == count) {
			return;
		}
		
		int diff = count - size2;
		int size1 = list1.size();
		if (size1 < diff) {
			diff = size1;
		}
		
		for (int i = 0; i < diff; ++i) {
			list2.addFirst(list1.removeLast());
		}
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
		if (isRank) {
			Comparator<Object> comparator = this.comparator;
			for (int i = 1, len = result2.size(); i <= len; ++i) {
				if (seqs[i] != 0) {
					RankArray rankArray1 = (RankArray)result.get(seqs[i]);
					RankArray rankArray2 = (RankArray)result2.get(i);
					if (rankArray1 == null) {
						result.set(seqs[i], rankArray2);
					} else if (rankArray2 != null) {
						ObjectArray valueArray = rankArray2.getValueArray();
						for (int v = 1, vcount = valueArray.size(); v <= vcount; ++v) {
							rankArray1.add(valueArray.get(v), comparator);
						}
					}
				}
			}
		} else if (isSame) {
			if (getExp != null) {
				topAll(result, result2, seqs, ctx);
			} else if (isPositive) {
				minAll(result, result2, seqs);
			} else {
				maxAll(result, result2, seqs);
			}
		} else if (exp != null) {
			for (int i = 1, len = result2.size(); i <= len; ++i) {
				if (seqs[i] != 0) {
					MinHeap heap1 = (MinHeap)result.get(seqs[i]);
					MinHeap heap2 = (MinHeap)result2.get(i);
					if (heap1 == null) {
						result.set(seqs[i], heap2);
					} else if (heap2 != null) {
						heap1.insertAll(heap2);
					}
				}
			}
		} else {
			int count = this.count;
			if (isPositive) {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						ArrayList<Object> list1 = (ArrayList<Object>)result.get(seqs[i]);
						ArrayList<Object> list2 = (ArrayList<Object>)result2.get(i);
						if (list1 == null) {
							result.set(seqs[i], list2);
						} else if (list2 != null) {
							addToArrayList(list1, list2, count);
						}
					}
				}
			} else {
				for (int i = 1, len = result2.size(); i <= len; ++i) {
					if (seqs[i] != 0) {
						LinkedList<Object> list1 = (LinkedList<Object>)result.get(seqs[i]);
						LinkedList<Object> list2 = (LinkedList<Object>)result2.get(i);
						if (list1 == null) {
							result.set(seqs[i], list2);
						} else if (list2 != null) {
							addToLinkedList(list1, list2, count);
							result.set(seqs[i], list2);
						}
					}
				}
			}
		}
	}
	private static void addToRankArray(RankArray array, Object obj, Comparator<Object> comparator) {
		if (obj != null) {
			array.add(obj, comparator);
		}
	}
	
	private static void addToRankArray(RankArray array, Object obj, 
			Expression exp, Context ctx, Comparator<Object> comparator) {
		ComputeStack stack = ctx.getComputeStack();
		try {
			if (obj instanceof Sequence) {
				Sequence tmpSeq = (Sequence)obj;
				Current current = new Current(tmpSeq);
				stack.push(current);
				
				for (int i = 1, len = tmpSeq.length(); i <= len; ++i) {
					current.setCurrent(i);
					Object val = exp.calculate(ctx);
					
					if (val != null) {
						Object []vals = new Object[2];
						vals[0] = exp.calculate(ctx);
						vals[1] = current.getCurrent();
						array.add(vals, comparator);
					}
				}
			} else if (obj instanceof BaseRecord) {
				stack.push((BaseRecord)obj);
				Object val = exp.calculate(ctx);
				
				if (val != null) {
					Object []vals = new Object[2];
					vals[0] = val;
					vals[1] = obj;
					array.add(vals, comparator);
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("top" + mm.getMessage("function.invalidParam"));
			}
		} finally {
			stack.pop();
		}
	}
}
