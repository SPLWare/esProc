package com.scudata.expression;

import com.scudata.array.BoolArray;
import com.scudata.array.ConstArray;
import com.scudata.array.IArray;
import com.scudata.dm.Context;
import com.scudata.dm.DBObject;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.util.Variant;

/**
 * 变量节点
 * @author RunQian
 *
 */
public class VarParam extends Node {
	private Param param;

	public VarParam(Param param) {
		this.param = param;
	}
	
	/**
	 * 对节点做深度优化（包括单元格和参数引用），常数表达式先算成常数
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node deepOptimize(Context ctx) {
		return new Constant(calculate(ctx));
	}

	public Object calculate(Context ctx) {
		if (param.isDeleted()) {
			String name = param.getName();
			param = ctx.getParam(name);
			if (param == null) {
				throw new RuntimeException("Param is deleted");
			}
		}
		
		return param.getValue();
	}

	public Object assign(Object value, Context ctx) {
		if (param.isDeleted()) {
			String name = param.getName();
			param = ctx.getParam(name);
			if (param == null) {
				param = new Param(name, Param.VAR, null);
				ctx.addParam(param);
			}
		}
		
		param.setValue(value);
		return value;
	}
	
	public Object addAssign(Object value, Context ctx) {
		if (param.isDeleted()) {
			param = ctx.getParam(param.getName());
			if (param == null) {
				throw new RuntimeException("Param is deleted");
			}
		}
		
		Object result = Variant.add(param.getValue(), value);
		param.setValue(result);
		return result;
	}

	protected boolean containParam(String name) {
		return name.equals(param.getName());
	}

	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (resultList.get(param.getName()) == null) {
			resultList.addVariable(param.getName(), param.getValue());
		}
	}

	public byte calcExpValueType(Context ctx) {
		Object val = param.getValue();
		if (val instanceof DBObject) {
			return Expression.TYPE_DB;
		} else if (val instanceof FileObject) {
			return Expression.TYPE_FILE;
		} else {
			return Expression.TYPE_OTHER;
		}
	}
	
	/**
	 * 计算出所有行的结果
	 * @param ctx 计算上行文
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx) {
		if (param.isDeleted()) {
			param = ctx.getParam(param.getName());
			if (param == null) {
				throw new RuntimeException("Param is deleted");
			}
		}

		Sequence sequence = ctx.getComputeStack().getTopSequence();
		return new ConstArray(param.getValue(), sequence.length());
	}
	
	/**
	 * 计算signArray中取值为sign的行
	 * @param ctx
	 * @param signArray 行标识数组
	 * @param sign 标识
	 * @return IArray
	 */
	public IArray calculateAll(Context ctx, IArray signArray, boolean sign) {
		return calculateAll(ctx);
	}
	
	/**
	 * 计算逻辑与运算符&&的右侧表达式
	 * @param ctx 计算上行文
	 * @param leftResult &&左侧表达式的计算结果
	 * @return BoolArray
	 */
	public BoolArray calculateAnd(Context ctx, IArray leftResult) {
		if (param.isDeleted()) {
			param = ctx.getParam(param.getName());
			if (param == null) {
				throw new RuntimeException("Param is deleted");
			}
		}
		
		BoolArray result = leftResult.isTrue();
		if (Variant.isFalse(param.getValue())) {
			int size = result.size();
			for (int i = 1; i <= size; ++i) {
				result.set(i, false);
			}
		}
		
		return result;
	}
	
	/**
	 * 返回节点是否单调递增的
	 * @return true：是单调递增的，false：不是
	 */
	public boolean isMonotone() {
		return true;
	}
	
	public Param getParam() {
		return param;
	}
}
