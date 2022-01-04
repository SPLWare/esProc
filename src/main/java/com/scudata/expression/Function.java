package com.scudata.expression;

import java.util.List;

import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.ParamList;
import com.scudata.resources.EngineMessage;

/**
 * 函数节点基类
 * @author WangXiaoJun
 *
 */
public abstract class Function extends Node {
	protected String functionName; // 函数名
	protected String option; // 选项
	protected String strParam; // 函数参数字符串，用于集群函数把参数字符串传到节点机
	protected IParam param; // 函数参数

	protected ICellSet cs = null; // 网格对象
	protected static final int Default_Size = 4;

	/**
	 * 设置函数参数
	 * @param cs 网格对象
	 * @param ctx 计算上下文
	 * @param param 函数参数字符串
	 */
	public void setParameter(ICellSet cs, Context ctx, String param) {
		strParam = param;
		this.cs = cs;
		this.param = ParamParser.parse(param, cs, ctx);
	}

	/**
	 * 取函数参数字符串
	 * @return
	 */
	public String getParamString() {
		return strParam;
	}
	
	/**
	 * 取函数名
	 * @return String
	 */
	public String getFunctionName() {
		return functionName;
	}
	
	 /**
	  * 设置函数名
	  * @param name 函数名
	  */
	public void setFunctionName(String name) {
		this.functionName = name;
	}
	
	/**
	 * 返回函数参数
	 * @return IParam
	 */
	public IParam getParam() {
		return param;
	}

	/**
	 * 设置函数参数
	 * @param param 参数
	 */
	public void setParam(IParam param) {
		this.param = param;
	}

	/**
	 * 设置函数选项
	 * @param opt 选项
	 */
	public void setOption(String opt) {
		option = opt;
	}

	/**
	 * 取函数选项
	 * @return String
	 */
	public String getOption() {
		return option;
	}

	/**
	 * 返回是否包含指定参数
	 * @param name 参数名
	 * @return boolean true：包含，false：不包含
	 */
	protected boolean containParam(String name) {
		if (param != null) {
			return param.containParam(name);
		} else {
			return false;
		}
	}

	/**
	 * 查找表达式中用到参数
	 * @param ctx 计算上下文
	 * @param resultList 输出值，用到的参数会添加到这里面
	 */
	protected void getUsedParams(Context ctx, ParamList resultList) {
		if (param != null) {
			param.getUsedParams(ctx, resultList);
		}
	}
	
	public void getUsedFields(Context ctx, List<String> resultList) {
		if (param != null) param.getUsedFields(ctx, resultList);
	}

	protected void getUsedCells(List<INormalCell> resultList) {
		if (param != null) param.getUsedCells(resultList);
	}

	/**
	 * 对节点做优化
	 * @param ctx 计算上下文
	 * @param Node 优化后的节点
	 */
	public Node optimize(Context ctx) {
		boolean opt = true;
		if (param != null) {
			// 对参数做优化
			opt = param.optimize(ctx);
		}

		// 如果函数参数是常量则计算出函数值
		// 如果派生函数不想被优化成常量需要重载此方法
		if (opt) {
			return new Constant(calculate(ctx));
		} else {
			return this;
		}
	}
	
	public Expression[] getParamExpressions(String funcName, boolean canNull) {
		IParam param = this.param;
		if (param == null) {
			if (canNull) {
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(funcName + mm.getMessage("function.missingParam"));
			}
		}
		
		Expression []exps;
		if (param.isLeaf()) {
			exps = new Expression[]{param.getLeafExpression()};
		} else {
			int count = param.getSubSize();
			exps = new Expression[count];
			
			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub != null) {
					exps[i] = sub.getLeafExpression();
				} else if (!canNull) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(funcName + mm.getMessage("function.invalidParam"));
				}
			}
		}
		
		return exps;
	}
	
	/**
	 * 取得一个函数和其参数的完整字符串
	 * @return	字符串
	 */
	public String getFunctionString() {
		String strRes = this.getFunctionName();
		
		if (null != option && 0 != option.length())
			strRes += "@" + option;
		
		strRes += "(";
		strRes += this.strParam;
		strRes += ")";
		
		return strRes;
	}

	/**
	 * 判断节点是否是指定函数
	 * @param name 函数名
	 * @return true：是指定函数，false：不是
	 */
	public boolean isFunction(String name) {
		return name.equals(functionName);
	}
}
