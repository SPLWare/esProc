package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.IArray;
import com.scudata.array.ObjectArray;
import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Current;
import com.scudata.dm.Sequence;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 成员函数基类，成员函数的实现类需要继承自此类
 * @author WangXiaoJun
 *
 */
public abstract class MemberFunction extends Function {
	private MemberFunction next; // 下一个同名的成员函数类
	protected Node left; // 点操作符的左侧节点
	
	/**
	 * 用于判断点操作符右面的函数是否和左面对象的类型匹配
	 * @param obj 左面对象
	 * @return true：右面的节点跟左面对象的类型匹配，是其成员或成员函数，false：不是
	 */
	abstract public boolean isLeftTypeMatch(Object obj);
	
	/**
	 * 设置点操作符的左侧对象到当前函数
	 * @param obj 左面对象
	 */
	abstract public void setDotLeftObject(Object obj);
	
	/**
	 * 设置节点的左侧节点
	 * @param node 节点
	 */
	public void setLeft(Node node) {
		left = node;
	}

	/**
	 * 取节点的左侧节点，没有返回空
	 * @return Node
	 */
	public Node getLeft() {
		return left;
	}

	/**
	 * 设置函数参数，派生类如果继承了此方法需要调用基类的此方法或者调用next的此方法
	 * @param cs 网格对象
	 * @param ctx 计算上下文
	 * @param param 函数参数字符串
	 */
	public void setParameter(ICellSet cs, Context ctx, String param) {
		super.setParameter(cs, ctx, param);
		if (next != null) {
			next.setParameter(cs, ctx, param);
		}
	}
	
	public void setOption(String opt) {
		super.setOption(opt);
		if (next != null) {
			next.setOption(opt);
		}
	}

	/**
	 * 取下一个同名的成员函数，没有则返回空
	 * @return MemberFunction
	 */
	public MemberFunction getNextFunction() {
		return next;
	}
	
	/**
	 * 设置下一个同名的成员函数
	 * @param fn 成员函数
	 */
	public void setNextFunction(MemberFunction fn) {
		next = fn;
	}
	
	/**
	 * 判断当前节点是否是序列函数
	 * 如果点操作符的右侧节点是序列函数，左侧节点计算出数，则需要把数转成数列
	 * @return
	 */
	public boolean isSequenceFunction() {
		// 如果同名函数有重载了此方法的则调用，否则就返回默认值
		return next == null ? false : next.isSequenceFunction();
	}

	/**
	 * 判断节点是否会修改序列的成员值，此方法为了优化[1,2,3].contain(...)这种表达式，
	 * 如果序列不会被更改则[1,2,3]可以被产生成常数序列，而不是每次计算都产生一个序列
	 * @return true：会修改，false：不会修改
	 */
	public boolean ifModifySequence() {
		// 如果同名函数有重载了此方法的则调用，否则就返回默认值
		return next == null ? true : next.isSequenceFunction();
	}
	
	/**
	 * 对节点做优化
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		if (param != null) {
			// 对参数做优化
			param.optimize(ctx);
			if (next != null) {
				next.optimize(ctx);
			}
		}
		
		return this;
	}
	
	// x:…,A:y:…,z:F,…
	protected static void parseJoinParam(IParam param, int index, Expression[][] exps,
								   Object[] codes, Expression[][] dataExps,
								   Expression[][] newExps, String[][] newNames, Context ctx) {
		int size = param.getSubSize();
		if (size < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("join" + mm.getMessage("function.invalidParam"));
		}

		IParam sub = param.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("join" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			exps[index] = new Expression[]{sub.getLeafExpression()};
		} else {
			int expCount = sub.getSubSize();
			Expression []tmps = new Expression[expCount];
			exps[index] = tmps;

			for (int i = 0; i < expCount; ++i) {
				IParam p = sub.getSub(i);
				if (p == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				tmps[i] = p.getLeafExpression();
			}
		}

		sub = param.getSub(1);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("join" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			codes[index] = sub.getLeafExpression().calculate(ctx);
		} else {
			IParam p = sub.getSub(0);
			if (p == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("join" + mm.getMessage("function.invalidParam"));
			}

			codes[index] = p.getLeafExpression().calculate(ctx);
			int expCount = sub.getSubSize() - 1;
			Expression []tmps = new Expression[expCount];
			dataExps[index] = tmps;

			for (int i = 0; i < expCount; ++i) {
				p = sub.getSub(i + 1);
				if (p == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				tmps[i] = p.getLeafExpression();
			}
		}
		
		int expCount = size - 2;
		Expression []tmpExps = new Expression[expCount];
		String []tmpNames = new String[expCount];
		newExps[index] = tmpExps;
		newNames[index] = tmpNames;

		for (int i = 0; i < expCount; ++i) {
			IParam p = param.getSub(i + 2);
			if (p == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("join" + mm.getMessage("function.invalidParam"));
			}

			if (p.isLeaf()) {
				tmpExps[i] = p.getLeafExpression();
			} else {
				if (p.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = p.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				tmpExps[i] = sub0.getLeafExpression();
				IParam sub1 = p.getSub(1);
				if (sub1 != null) {
					tmpNames[i] = sub1.getLeafExpression().getIdentifierName();
				}
			}
		}
	}

	// x:…,A:y:…,z:F,…
	protected static void parseJoinxParam(IParam param, int index, Expression[][] exps,  Object[] codes, 
			Expression[][] dataExps, Expression[][] newExps, String[][] newNames, Context ctx) {
		int size = param.getSubSize();
		if (size < 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		}

		IParam sub = param.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			exps[index] = new Expression[]{sub.getLeafExpression()};
		} else {
			int expCount = sub.getSubSize();
			Expression []tmps = new Expression[expCount];
			exps[index] = tmps;

			for (int i = 0; i < expCount; ++i) {
				IParam p = sub.getSub(i);
				if (p == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				tmps[i] = p.getLeafExpression();
			}
		}

		sub = param.getSub(1);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			codes[index] = sub.getLeafExpression().calculate(ctx);
		} else {
			IParam p = sub.getSub(0);
			if (p == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}

			codes[index] = p.getLeafExpression().calculate(ctx);
			int expCount = sub.getSubSize() - 1;
			Expression []tmps = new Expression[expCount];
			dataExps[index] = tmps;

			for (int i = 0; i < expCount; ++i) {
				p = sub.getSub(i + 1);
				if (p == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				tmps[i] = p.getLeafExpression();
			}
		}
		
		int expCount = size - 2;
		Expression []tmpExps = new Expression[expCount];
		String []tmpNames = new String[expCount];
		newExps[index] = tmpExps;
		newNames[index] = tmpNames;

		for (int i = 0; i < expCount; ++i) {
			IParam p = param.getSub(i + 2);
			if (p == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
			}

			if (p.isLeaf()) {
				tmpExps[i] = p.getLeafExpression();
			} else {
				if (p.getSubSize() != 2) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				IParam sub0 = p.getSub(0);
				if (sub0 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("joinx" + mm.getMessage("function.invalidParam"));
				}

				tmpExps[i] = sub0.getLeafExpression();
				IParam sub1 = p.getSub(1);
				if (sub1 != null) {
					tmpNames[i] = sub1.getLeafExpression().getIdentifierName();
				}
			}
		}
	}

	protected static void parseSwitchParam(IParam param, int i, 
			String []fkNames, Object []codes, Expression []exps, Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			fkNames[i] = param.getLeafExpression().getIdentifierName();
			return;
		}
		
		int size = param.getSubSize();
		if (size > 3) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub = param.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		}
		
		fkNames[i] = sub.getLeafExpression().getIdentifierName();
		sub = param.getSub(1);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			codes[i] = sub.getLeafExpression().calculate(ctx);
			if (codes[i] == null) {
				codes[i] = new Sequence();
			}
			
			if (size > 2) {
				sub = param.getSub(2);
				if (sub != null) {
					exps[i] = sub.getLeafExpression();
				}
			}
		} else {
			if (sub.getSubSize() != 2 || size > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = sub.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.invalidParam"));
			}
			
			codes[i] = sub0.getLeafExpression().calculate(ctx);
			if (codes[i] == null) {
				codes[i] = new Sequence();
			}

			IParam sub1 = sub.getSub(1);
			if (sub1 != null) {
				exps[i] = sub1.getLeafExpression();
			}
		}
	}
	
	// F:FT,A:x:xt
	protected static void parseSwitchParam(IParam param, int i,  String []fkNames, String []timeFkNames, 
			Object []codes, Expression []exps, Expression []timeExps, Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			fkNames[i] = param.getLeafExpression().getIdentifierName();
			return;
		} else if (param.getSubSize() != 2 || param.getType() != IParam.Comma) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		}
		
		IParam sub = param.getSub(0);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			fkNames[i] = sub.getLeafExpression().getIdentifierName();
		} else if (sub.getSubSize() == 2) {
			IParam sub0 = sub.getSub(0);
			IParam sub1 = sub.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.invalidParam"));
			}
			
			fkNames[i] = sub0.getLeafExpression().getIdentifierName();
			timeFkNames[i] = sub1.getLeafExpression().getIdentifierName();
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		}
		
		sub = param.getSub(1);
		if (sub == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("switch" + mm.getMessage("function.invalidParam"));
		} else if (sub.isLeaf()) {
			codes[i] = sub.getLeafExpression().calculate(ctx);
			if (codes[i] == null) {
				codes[i] = new Sequence();
			}
		} else {
			int size = sub.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = sub.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("switch" + mm.getMessage("function.invalidParam"));
			}
			
			codes[i] = sub0.getLeafExpression().calculate(ctx);
			if (codes[i] == null) {
				codes[i] = new Sequence();
			}

			IParam sub1 = sub.getSub(1);
			if (sub1 != null) {
				exps[i] = sub1.getLeafExpression();
			}
			
			if (size > 2) {
				IParam sub2 = sub.getSub(2);
				if (sub2 != null) {
					timeExps[i] = sub2.getLeafExpression();
				}
			}
		}
	}
	
	protected IArray calculateAll(IArray leftArray, Context ctx) {
		Current current = ctx.getComputeStack().getTopCurrent();
		int len = current.length();
		ObjectArray array = new ObjectArray(len);
		array.setTemporary(true);
		
		Next:
		for (int i = 1; i <= len; ++i) {
			current.setCurrent(i);
			Object leftValue = leftArray.get(i);
			
			if (leftValue == null) {
				array.push(null);
			} else {
				if (leftValue instanceof Number && isSequenceFunction()) {
					int n = ((Number)leftValue).intValue();
					if (n > 0) {
						leftValue = new Sequence(1, n);
					} else {
						leftValue = new Sequence(0);
					}
				}
				
				for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
					if (right.isLeftTypeMatch(leftValue)) {
						right.setDotLeftObject(leftValue);
						Object value = right.calculate(ctx);
						array.push(value);
						continue Next;
					}
				}
				
				String fnName = getFunctionName();
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
			}
		}
		
		return array;
	}
	
	protected BoolArray calculateAnd(IArray leftArray, Context ctx, IArray leftResult) {
		Current current = ctx.getComputeStack().getTopCurrent();
		int len = current.length();
		BoolArray result = leftResult.isTrue();
		
		Next:
		for (int i = 1; i <= len; ++i) {
			if (leftResult.isTrue(i)) {
				current.setCurrent(i);
				Object leftValue = leftArray.get(i);
				if (leftValue == null) {
					result.set(i, false);
				} else {
					if (leftValue instanceof Number && isSequenceFunction()) {
						int n = ((Number)leftValue).intValue();
						if (n > 0) {
							leftValue = new Sequence(1, n);
						} else {
							leftValue = new Sequence(0);
						}
					}
					
					for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
						if (right.isLeftTypeMatch(leftValue)) {
							right.setDotLeftObject(leftValue);
							Object value = right.calculate(ctx);
							if (Variant.isFalse(value)) {
								result.set(i, false);
							}

							continue Next;
						}
					}
					
					String fnName = getFunctionName();
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
				}
			}
		}
		
		return result;
	}
	
	protected IArray calculateAll(IArray leftArray, Context ctx, IArray signArray, boolean sign) {
		int size = signArray.size();
		ObjectArray result = new ObjectArray(size);
		result.setTemporary(true);
		Current current = ctx.getComputeStack().getTopCurrent();
		
		Next:
		for (int i = 1; i <= size; ++i) {
			if (signArray.isTrue(i) == sign) {
				current.setCurrent(i);
				Object leftValue = leftArray.get(i);
				
				if (leftValue == null) {
					result.push(null);
				} else {
					if (leftValue instanceof Number && isSequenceFunction()) {
						int n = ((Number)leftValue).intValue();
						if (n > 0) {
							leftValue = new Sequence(1, n);
						} else {
							leftValue = new Sequence(0);
						}
					}
					
					for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
						if (right.isLeftTypeMatch(leftValue)) {
							right.setDotLeftObject(leftValue);
							Object value = right.calculate(ctx);
							result.push(value);
							continue Next;
						}
					}
					
					String fnName = getFunctionName();
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
				}
			} else {
				result.push(null);
			}
		}
		
		return result;
	}
	
	/**
	 * 计算出所有行的结果
	 * @param left 点运算符的左侧节点
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		return calculateAll(left.calculateAll(ctx), ctx);
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		Node left = this.left;
		int size = signArray.size();
		ObjectArray result = new ObjectArray(size);
		result.setTemporary(true);
		Current current = ctx.getComputeStack().getTopCurrent();
		
		Next:
		for (int i = 1; i <= size; ++i) {
			if (signArray.isTrue(i) == sign) {
				current.setCurrent(i);
				Object leftValue = left.calculate(ctx);
				
				if (leftValue == null) {
					result.push(null);
				} else {
					if (leftValue instanceof Number && isSequenceFunction()) {
						int n = ((Number)leftValue).intValue();
						if (n > 0) {
							leftValue = new Sequence(1, n);
						} else {
							leftValue = new Sequence(0);
						}
					}
					
					for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
						if (right.isLeftTypeMatch(leftValue)) {
							right.setDotLeftObject(leftValue);
							Object value = right.calculate(ctx);
							result.push(value);
							continue Next;
						}
					}
					
					String fnName = getFunctionName();
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
				}
			} else {
				result.push(null);
			}
		}
		
		return result;
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		Node left = this.left;
		BoolArray result = leftResult.isTrue();
		int size = result.size();
		Current current = ctx.getComputeStack().getTopCurrent();
		
		Next:
		for (int i = 1; i <= size; ++i) {
			if (result.isTrue(i)) {
				current.setCurrent(i);
				Object leftValue = left.calculate(ctx);
				
				if (leftValue == null) {
					result.set(i, false);
				} else {
					if (leftValue instanceof Number && isSequenceFunction()) {
						int n = ((Number)leftValue).intValue();
						if (n > 0) {
							leftValue = new Sequence(1, n);
						} else {
							leftValue = new Sequence(0);
						}
					}
					
					for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
						if (right.isLeftTypeMatch(leftValue)) {
							right.setDotLeftObject(leftValue);
							Object value = right.calculate(ctx);
							if (Variant.isFalse(value)) {
								result.set(i, false);
							}

							continue Next;
						}
					}
					
					String fnName = getFunctionName();
					MessageManager mm = EngineMessage.get();
					throw new RQException(mm.getMessage("dot.leftTypeError", Variant.getDataType(leftValue), fnName));
				}
			}
		}
		
		return result;
	}
		
	/**
	 * 判断给定的值域范围是否满足当前条件表达式
	 * @param ctx 计算上行文
	 * @return 取值参照Relation. -1：值域范围内没有满足条件的值，0：值域范围内有满足条件的值，1：值域范围的值都满足条件
	 */
	/*public int isValueRangeMatch(Context ctx) {
		IArray array = left.calculateRange(ctx);
		if (!(array instanceof ConstArray)) {
			return Relation.PARTICALMATCH;
		}
		
		Object leftValue = array.get(1);
		if (leftValue instanceof Number && isSequenceFunction()) {
			int n = ((Number)leftValue).intValue();
			if (n > 0) {
				leftValue = new Sequence(1, n);
			} else {
				leftValue = new Sequence(0);
			}
		}
		
		for (MemberFunction right = this; right != null; right = right.getNextFunction()) {
			if (right.isLeftTypeMatch(leftValue)) {
				right.setDotLeftObject(leftValue);
				return right.isValueRangeMatch(ctx);
			}
		}
		
		return Relation.PARTICALMATCH;
	}*/
}
