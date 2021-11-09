package com.raqsoft.expression.mfn.channel;

import java.util.ArrayList;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.op.New;
import com.raqsoft.expression.ChannelFunction;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Gather;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.resources.EngineMessage;

/**
 * 为管道定义采用累计方式进行分组汇总的结果集运算
 * ch.groups(x:F…;y:G…)
 * @author RunQian
 *
 */
public class Groups extends ChannelFunction {
	public Object calculate(Context ctx) {
		// 参数不可以为空
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groups" + mm.getMessage("function.missingParam"));
		}

		ArrayList<Object> gathers = new ArrayList<Object>(); // 统计聚合函数
		ArrayList<Integer> poss = new ArrayList<Integer>(); // 分组表达式，对应聚合函数列表的第几个聚合函数
		Expression[] exps; // 分组表达式数组
		String[] names = null; // 分组表达式的新名字
		Expression[] calcExps = null; // 聚合表达式数组
		String[] calcNames = null; // 聚合字段的名字

		//	把groups的参数分解成 分组表达式、分组表达式的名字、统计表达式、统计表达式的名字
		char type = param.getType();
		if (type == IParam.Normal) { // 只有一个参数
			exps = new Expression[]{param.getLeafExpression()};
		} else if (type == IParam.Colon) { // :
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groups" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groups" + mm.getMessage("function.invalidParam"));
			}

			exps = new Expression[]{sub0.getLeafExpression()};
			names = new String[]{sub1.getLeafExpression().getIdentifierName()};
		} else if (type == IParam.Comma) { // ,
			ParamInfo2 pi = ParamInfo2.parse(param, "groups", true, false);
			exps = pi.getExpressions1();
			names = pi.getExpressionStrs2();
		} else { // ;
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groups" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);

			if (sub0 == null) {
				exps = null;
				names = null;
			} else {
				ParamInfo2 pi = ParamInfo2.parse(sub0, "groups", true, false);
				exps = pi.getExpressions1();
				names = pi.getExpressionStrs2();
			}

			if (sub1 != null) {
				ParamInfo2 pi = ParamInfo2.parse(sub1, "groups", true, false);
				calcExps = pi.getExpressions1();
				calcNames = pi.getExpressionStrs2();
			}
		}
		
		// 分组表达式长度和聚合表达式长度
		int elen = exps == null ? 0 : exps.length;
		int clen = calcExps == null ? 0 : calcExps.length;
		
		// 解析表达式中的聚合函数
		for (int i = 0; i < clen; i++) {
			int size = gathers.size();
			gathers.addAll(Expression.getSpecFunc(calcExps[i], Gather.class));
			if (size == gathers.size())
				gathers.add(calcExps[i]);
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
		if (calcExps != null) {
			strExp = calcExps[index].toString();
		}
		
		for (int i = 0; i < tempExps.length; i++) {
			if (i >= poss.get(index)) {
				senExps[index+elen] = new Expression(cs, ctx, strExp);
				index++;
				strExp = calcExps[index].toString();
			}
			
			String funStr = "#" + (i+elen+1);
			strExp = Expression.replaceFunc(strExp, tempExps[i].toString(), funStr);
			if (!strExp.equals(funStr)) {
				exCal = true;
			}
		}
		
		// @b：结果集去掉分组字段
		boolean bopt = option != null && option.indexOf('b') != -1;
		
		String[] senNames	= null;	// 统一的列名
		String[] tempNames	= calcNames;	// 临时表列名
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
			senNames = new String[elen + calcNames.length];
			for (int i = 0; i < elen; i++) {
				senNames[i] = names[i];
			}
			
			for (int i =  0; i < clen; i++) {
				if (null == calcNames[i]) {
					senNames[i+elen] = calcExps[i].toString();
				} else {
					senNames[i+elen] = calcNames[i];
				}
			}
			
			if (bopt) {
				Expression []alterExps = new Expression[clen];
				String []alterNames = new String[clen];
				System.arraycopy(senExps, elen, alterExps, 0, clen);
				System.arraycopy(senNames, elen, alterNames, 0, clen);
				senExps = alterExps;
				senNames = alterNames;
			}
		} else if (bopt) {
			senNames = new String[clen];
			senExps = new Expression[clen];
			for (int i = 0, q = elen + 1; i < clen; ++i, ++q) {
				senExps[i] = new Expression(ctx, "#" + q);
			}
		}
		
		channel.groups(exps, names, tempExps, tempNames, option);
		
		// 生成new操作符
		if (senNames != null) {
			New op = new New(this, senExps, senNames, option);
			channel.setResultNew(op);
		}
		
		return channel;
	}	
}
