package com.scudata.expression.fn.gather;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.comparator.ArrayComparator;
import com.scudata.dm.comparator.BaseComparator;
import com.scudata.dm.comparator.DescComparator;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.expression.ValueList;
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
	
	private Comparator<Object> comparator;
	private int expIndex = -1; // 比较表达式的字段索引

	public void prepare(Context ctx) {
		if (option != null && option.indexOf('1') != -1) {
			isOne = true;
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
				ValueList valueList = new ValueList();
				valueList.setParam(sub1);
				exp = new Expression(valueList);
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
		
		if (count < 0) {
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
	
	public boolean isPositive() {
		return isPositive;
	}

	public boolean needFinish() {
		return true;
	}
	
	public boolean needFinish1() {
		return true;
	}

	public Object finish1(Object val) {
		return finish(val);
	}
	
	public Object finish(Object val) {
		if (val == null) return null;
		
		if (isSame) {
			if (getExp != null && val instanceof Sequence) {
				Sequence seq = (Sequence)val;
				ListBase1 mems = seq.getMems();
				for (int i = 1, size = mems.size(); i <= size; ++i) {
					Object []tmp = (Object[])mems.get(i);
					mems.set(i, tmp[1]);
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
				Sequence.Current current = tmpSeq.new Current();
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
			} else if (obj instanceof Record) {
				stack.push((Record)obj);
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
				Sequence.Current current = seq.new Current();
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
		} else if (obj instanceof Record) {
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
					stack.push((Record)obj);
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
		Object val = ((Record)obj).getFieldValue(expIndex);
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
			ListBase1 mems = seq.getMems();
			int len = mems.size();
			for (int i = 1; i <= len && size <= count; ++i) {
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
			ListBase1 mems = seq.getMems();
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
		if (count == 0) return null;
		
		if (isSame) {
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
				if (obj instanceof Record) {
					expIndex = ((Record)obj).getFieldIndex(exp.getIdentifierName());
				}
				
				addToHeap(heap, obj, exp, ctx);
			}
			
			return heap;
		} else {
			Object obj;
			if (getExp == null) {
				Object top = ctx.getComputeStack().getTopObject();
				obj = ((Sequence.Current)top).getCurrent();
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
		if (count == 0) return null;
		
		if (isSame) {
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
				obj = ((Sequence.Current)top).getCurrent();
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

	public Expression getRegatherExpression(int q) {
		String str;
		if (isOne) {
			if (isPositive) {
				str = "top@1(";
			} else {
				str = "top@1(-";
			}
		} else {
			if (isPositive) {
				str = "top(";
			} else {
				str = "top(-";
			}
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
}
