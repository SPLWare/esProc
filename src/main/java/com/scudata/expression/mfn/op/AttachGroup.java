package com.scudata.expression.mfn.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.op.Operable;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.OperableFunction;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.fn.algebra.Var;
import com.scudata.resources.EngineMessage;

/**
 * 对游标或管道附加有序分组运算
 * op.group(x,…) op.group(x:F,…;y:G,…) op是游标或管道
 * @author RunQian
 *
 */
public class AttachGroup extends OperableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("group" + mm.getMessage("function.missingParam"));
		}

		if (option != null && option.indexOf('q') != -1) {
			if (param.getType() != IParam.Semicolon) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			if (size == 2) {
				IParam sub0 = param.getSub(0);
				IParam sub1 = param.getSub(1);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
				
				Expression []exps = sub0.toArray("group", false);
				Expression []sortExps = sub1.toArray("group", false);
				return operable.group(this, exps, sortExps, option, ctx);
			} else if (size == 3) {
				IParam sub0 = param.getSub(0);
				IParam sub1 = param.getSub(1);
				IParam sub2 = param.getSub(2);
				if (sub0 == null || sub1 == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}

				ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
				Expression []exps = pi0.getExpressions1();
				String []names = pi0.getExpressionStrs2();
				
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				Expression []sortExps = pi1.getExpressions1();
				String []sortNames = pi1.getExpressionStrs2();
				
				Expression []newExps = null;
				String []newNames = null;
				if (sub2 != null) {
					ParamInfo2 pi2 = ParamInfo2.parse(sub2, "group", true, false);
					newExps = pi2.getExpressions1();
					newNames = pi2.getExpressionStrs2();
				}
				
				return operable.group(this, exps, names, sortExps, sortNames, newExps, newNames, option, ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			Expression []exps = new Expression[] {exp};
			return operable.group(this, exps, option, ctx);
		} else if (param.getType() == IParam.Comma) {
			if (option != null && option.indexOf('i') != -1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			int size = param.getSubSize();
			Expression []exps = new Expression[size];
			for (int i = 0; i < size; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("group" + mm.getMessage("function.invalidParam"));
				}
				exps[i] = sub.getLeafExpression();
			}
			
			return operable.group(this, exps, option, ctx);
		} else if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
			Expression []exps = pi0.getExpressions1();
			String []names = pi0.getExpressionStrs2();
			
			Expression []newExps = null;
			String []newNames = null;
			IParam sub1 = param.getSub(1);
			if (sub1 != null) {
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				newExps = pi1.getExpressions1();
				newNames = pi1.getExpressionStrs2();
			}
			
			if (option != null && option.indexOf('i') != -1 && exps.length != 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			if (option != null && option.indexOf('s') != -1) {
				// 分组表达式长度和聚合表达式长度
				int elen = exps == null ? 0 : exps.length;
				int clen = newExps == null ? 0 : newExps.length;
				ArrayList<Object> gathers = new ArrayList<Object>(); // 统计聚合函数
				ArrayList<Integer> poss = new ArrayList<Integer>(); // 分组表达式，对应聚合函数列表的第几个聚合函数
				
				// 解析表达式中的聚合函数
				for (int i = 0; i < clen; i++) {
					int size = gathers.size();
					gathers.addAll(Expression.getSpecFunc(newExps[i], Gather.class));
					
					if (size == gathers.size()) {
						Node home = newExps[i].getHome();
						if (home instanceof Var) {
							Var var = (Var)home;
							String param = var.getParamString();
							String opt = var.getOption();
							String sumStr = "sum(" + param + ")";
							Expression exp = new Expression(cs, ctx, sumStr);
							gathers.add(exp.getHome());
							
							String countStr = "count(" + param + ")";
							exp = new Expression(cs, ctx, countStr);
							gathers.add(exp.getHome());
							
							String sum2Str = "sum(power(" + param + "))";
							exp = new Expression(cs, ctx, sum2Str);
							gathers.add(exp.getHome());
							
							// sum2+count*power(sum/count) - 2*sum*sum/count
							String expStr = sum2Str + "+" + countStr + "*power(" + sumStr + "/" + countStr + 
									")-2*" + sumStr + "*" + sumStr + "/" + countStr;
							if (opt == null || opt.indexOf('s') == -1) {
								expStr = "(" + expStr + ")/" + countStr;
							} else {
								expStr = "(" + expStr + ")/(" + countStr + "-1)";
							}
							
							newExps[i] = new Expression(cs, ctx, expStr);
							if (newNames[i] == null || newNames[i].length() == 0) {
								newNames[i] = "var";
							}
						} else {
							gathers.add(newExps[i]);
						}
					}
					
					poss.add(gathers.size());
				}
				
				// 生成中间聚合表达式
				Expression[] tempExps = new Expression[gathers.size()];
				for (int i = 0; i < tempExps.length; i++) {
					Object obj = gathers.get(i);
					if (obj instanceof Gather) {
						Gather gather = (Gather)gathers.get(i);
						tempExps[i] = new Expression(cs, ctx, gather.getFunctionString());
					} else {
						tempExps[i] = (Expression)gathers.get(i);
					}
				}
				
				// new 游标的表达式
				Expression[] senExps = new Expression[elen+clen];
				String strExp = null;	// 老表达式字符串。
				int index = 0;	// 老表达式的索引
				
				// 根据老表达式，转换为新的new的统计列表达式
				boolean exCal	= false;	// 判断是否拆分聚合表达式
				if (newExps != null) {
					strExp = newExps[index].toString();
				}
				
				for (int i = 0; i < tempExps.length; i++) {
					if (i >= poss.get(index)) {
						senExps[index+elen] = new Expression(cs, ctx, strExp);
						index++;
						strExp = newExps[index].toString();
					}
					
					String funStr = "#" + (i+elen+1);
					strExp = Expression.replaceFunc(strExp, tempExps[i].toString(), funStr);
					if (!strExp.equals(funStr)) {
						exCal = true;
					}
				}
				
				String[] senNames	= null;	// 统一的列名
				String[] tempNames	= newNames;	// 临时表列名
				if (exCal) {
					tempNames = null;	// 若需要用new二次整理，临时表列名为空
					// 填写分组表达式
					for (int i = 1; i <= elen; i++) {
						String funStr = "#" + i;
						senExps[i-1] = new Expression(cs, ctx, funStr);
					}
					
					if (senExps.length > 0)	{// 最后一个表达式的生成
						senExps[index+elen] = new Expression(cs, ctx, strExp);
					}
					
					// 生成统一的列名
					senNames = new String[elen + newNames.length];
					for (int i = 0; i < elen; i++) {
						senNames[i] = names[i];
					}
					
					for (int i =  0; i < clen; i++) {
						if (null == newNames[i]) {
							senNames[i+elen] = newExps[i].toString();
						} else {
							senNames[i+elen] = newNames[i];
						}
					}
				}
				
				if (senNames == null) {
					return operable.group(this, exps, names, newExps, newNames, option, ctx);
				} else {
					Operable result = operable.group(this, exps, names, tempExps, tempNames, option, ctx);
					return result.newTable(null, senExps, senNames, null, ctx);
				}
			} else {
				return operable.group(this, exps, names, newExps, newNames, option, ctx);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("group" + mm.getMessage("function.invalidParam"));
		}
	}
}
