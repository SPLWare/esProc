package com.scudata.dm.op;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 游标或管道附加的模式匹配运算处理类
 * op.regex(rs,F) op是游标或管道
 * @author RunQian
 *
 */
public class Regex extends Operation {
	private Pattern pattern; // 模式
	private String []names; // 字段名数组
	private Expression exp; // 需要做匹配的字段，缺省用~
	private DataStruct ds; // 结果集数据结构，等于空时返回源记录
	private String opt;
	private boolean doParse;

	public Regex(Pattern pattern, String []names, Expression exp, String opt) {
		this(null, pattern, names, exp, opt);
	}
	
	public Regex(Function function, Pattern pattern, String []names, Expression exp, String opt) {
		super(function);
		this.pattern = pattern;
		this.names = names;
		this.exp = exp;
		this.opt = opt;
		this.doParse = opt != null && opt.indexOf('p') != -1;
		if (names != null) {
			ds = new DataStruct(names);
		}
	}
	
	/**
	 * 复制运算用于多线程计算，因为表达式不能多线程计算
	 * @param ctx 计算上下文
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression dupExp = dupExpression(exp, ctx);
		return new Regex(function, pattern, names, dupExp, opt);
	}

	/**
	 * 处理游标或管道当前推送的数据
	 * @param seq 数据
	 * @param ctx 计算上下文
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		Pattern pattern = this.pattern;
		DataStruct ds = this.ds;
		int gcount = ds == null ? 0 : ds.getFieldCount();

		int len = seq.length();
		Sequence data = new Sequence(len);
		Sequence strs;
		if (exp == null) {
			strs = seq; // .fieldValues(0)
		} else {
			strs = seq.calc(exp, ctx);
		}
		
		IArray strMems = strs.getMems();
		IArray srcMems = seq.getMems();
		
		for (int i = 1; i <= len; ++i) {
			Object obj = strMems.get(i);
			if (obj instanceof String) {
				Matcher m = pattern.matcher((String)obj);
				if (m.find()) {
					if (ds == null) {
						data.add(srcMems.get(i));
					} else {
						Record r = new Record(ds);
						data.add(r);
						
						if (doParse) {
							for (int g = 1; g <= gcount; ++g) {
								String s = m.group(g);
								r.setNormalFieldValue(g - 1, Variant.parse(s));
							}
						} else {
							for (int g = 1; g <= gcount; ++g) {
								r.setNormalFieldValue(g - 1, m.group(g));
							}
						}
					}
				}
			} else if (obj != null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needStringExp"));
			}
		}
					
		if (data.length() != 0) {
			return data;
		} else {
			return null;
		}
	}
}
