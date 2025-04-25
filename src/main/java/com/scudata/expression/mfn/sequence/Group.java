package com.scudata.expression.mfn.sequence;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 对序列做分组或者分组汇总
 * A.group(xi,…) A.group(x:F,…;y:G…)
 * @author RunQian
 *
 */
public class Group extends SequenceFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			return srcSequence.group(option);
		} else if (param.isLeaf()) {
			Expression exp = param.getLeafExpression();
			return srcSequence.group(exp, option, ctx);
		} else if (param.getType() == IParam.Comma) { // ,
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

			return srcSequence.group(exps, option, ctx);
		} else if (param.getType() == IParam.Semicolon) { // ;
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("group" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);

			Expression []exps = null;
			String []names = null;
			if (sub0 != null) {
				ParamInfo2 pi0 = ParamInfo2.parse(sub0, "group", true, false);
				exps = pi0.getExpressions1();
				names = pi0.getExpressionStrs2();
			}

			Expression []newExps = null;
			String []newNames = null;
			if (sub1 != null) {
				ParamInfo2 pi1 = ParamInfo2.parse(sub1, "group", true, false);
				newExps = pi1.getExpressions1();
				newNames = pi1.getExpressionStrs2();
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
						gathers.add(newExps[i]);
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
					return srcSequence.group(exps, names, newExps, newNames, option, ctx);
				} else {
					Sequence result = srcSequence.groups(exps, names, tempExps, tempNames, option, ctx);		
					result = result.newTable(senNames, senExps, ctx);
					if (elen > 0 && result instanceof Table) {
						String []pk = new String[elen];
						for (int i = 1; i <= elen; ++i) {
							pk[i - 1] = "#" + i;
						}
						
						((Table)result).setPrimary(pk);
					}
					
					return result;
				}
			} else {
				return srcSequence.group(exps, names, newExps, newNames, option, ctx);
			}
		} else {
			ParamInfo2 pi0 = ParamInfo2.parse(param, "group", true, false);
			Expression []exps0 = pi0.getExpressions1();
			String []names0 = pi0.getExpressionStrs2();
			return srcSequence.group(exps0, names0, null, null, option, ctx);
		}
	}
}
