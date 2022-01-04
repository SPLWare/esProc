package com.scudata.expression.mfn.channel;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.op.New;
import com.scudata.expression.ChannelFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.Gather;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;

/**
 * 为管道定义采用累计方式进行外存分组汇总的结果集运算
 * ch.groupx(x:F,…;y:G…;n)
 * @author RunQian
 *
 */
public class Groupx extends ChannelFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupx" + mm.getMessage("function.missingParam"));
		}
		
		ArrayList<Object> gathers = new ArrayList<Object>(); // 统计聚合函数
		ArrayList<Integer> poss = new ArrayList<Integer>(); // 分组表达式，对应聚合函数列表的第几个聚合函数

		IParam sub0;
		IParam sub1 = null;
		IParam sub2 = null;
		
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groupx" + mm.getMessage("function.invalidParam"));
			}
			
			sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groupx" + mm.getMessage("function.invalidParam"));
			}
			
			sub1 = param.getSub(1);
			if (size > 2) {
				sub2 = param.getSub(2);
			}
		} else {
			sub0 = param;
		}
		
		Expression []exps;
		String []names = null;
		Expression []newExps = null;
		String []newNames = null;

		ParamInfo2 pi0 = ParamInfo2.parse(sub0, "groupx", true, false);
		exps = pi0.getExpressions1();
		names = pi0.getExpressionStrs2();

		if (sub1 != null) {
			ParamInfo2 pi1 = ParamInfo2.parse(sub1, "groupx", true, false);
			newExps = pi1.getExpressions1();
			newNames = pi1.getExpressionStrs2();
		}

		double n = -1;
		if (sub2 == null) {
		} else if (sub2.isLeaf()) {
			Object obj = sub2.getLeafExpression().calculate(ctx);
			if (obj instanceof Number) {
				n = ((Number)obj).doubleValue();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("groupx" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("groupx" + mm.getMessage("function.invalidParam"));
		}
		
		// 分组表达式和聚合表达式的长度
		int	nlen = null == newExps ? 0 : newExps.length;
		int elen = null == exps ? 0 : exps.length;
		// 解析表达式中的聚合函数
		for (int i = 0; i < nlen; i++) {
			int size = gathers.size();
			gathers.addAll(Expression.getSpecFunc(newExps[i], Gather.class));
			if (size == gathers.size())
				gathers.add(newExps[i]);
			poss.add(gathers.size());
		}
		
		// 生成中间聚合表达式
		Expression[] tempExps = new Expression[gathers.size()];
		String[] tempNames	= newNames;
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
		Expression[] senExps = new Expression[elen+nlen];
		String strExp = null;	// 老表达式字符串。
		int index = 0;	// 老表达式的索引
		New op = null;	// 附加的new操作符
		
		// 根据老表达式，转换为新的new的统计列表达式
		boolean exCal	= false;	// 判断是否拆分聚合表达式
		if (null != newExps) {
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
		
		// @b：结果集去掉分组字段
		boolean bopt = option != null && option.indexOf('b') != -1;
		
		if (exCal) {
			tempNames = null;
			// 填写分组表达式
			for (int i = 1; i <= elen; i++) {
				String funStr = "#" + i;
				senExps[i-1] = new Expression(cs, ctx, funStr);
			}
			
			if (senExps.length > 0)	// 最后一个表达式的生成
				senExps[index+elen] = new Expression(cs, ctx, strExp);
			
			// 生成统一的列名
			String[] senNames	= new String[names.length + newNames.length];
			for (int i = 0; i < names.length; i++) {
				senNames[i] = names[i];
			}
			for (int i =  0; i < newNames.length; i++) {
				if (null == newNames[i]) {
					senNames[i+names.length] = newExps[i].toString();
				} else
					senNames[i+names.length] = newNames[i];
			}
			
			// 生成new操作符
			if (bopt) {
				Expression []alterExps = new Expression[nlen];
				String []alterNames = new String[nlen];
				System.arraycopy(senExps, elen, alterExps, 0, nlen);
				System.arraycopy(senNames, elen, alterNames, 0, nlen);
				op = new New(this, alterExps, alterNames, null);
			} else {
				op = new New(this, senExps, senNames, null);
			}
		} else if (bopt) {
			Expression []alterExps = new Expression[nlen];
			for (int i = 0, q = elen + 1; i < nlen; ++i, ++q) {
				alterExps[i] = new Expression(ctx, "#" + q);
			}
			
			op = new New(this, alterExps, new String[nlen], null);
		}
		
		int capacity;
		if (n > 1) {
			capacity = (int)n;
		} else {
			int fcount = exps == null ? 0 : exps.length;
			if (newExps != null) fcount += newExps.length;
			capacity = EnvUtil.getCapacity(fcount);
			if (n > 0) {
				capacity *= n;
			}
		}
		
		channel.groupx(exps, names, tempExps, tempNames, option, capacity);
		if (op != null) {
			channel.setResultNew(op);
		}
		
		return channel;
	}
}
