package com.scudata.expression.mfn.string;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 用正则表达式对串做匹配，返回拆出项组成的序列，不匹配返回空
 * s.regex(rs)
 * @author RunQian
 *
 */
public class Regex extends StringFunction {
	public Object calculate(Context ctx) {
		int flags = 0;
		if (option != null) {
			if (option.indexOf('c') != -1) flags |= Pattern.CASE_INSENSITIVE;
			if (option.indexOf('u') != -1) flags |= Pattern.UNICODE_CASE;
		}

		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("regex" + mm.getMessage("function.missingParam"));
		} else if (param.isLeaf()) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.paramTypeError"));
			}

			Pattern pattern = Pattern.compile((String)obj, flags);
			Matcher m = pattern.matcher(srcStr);
			int gcount = m.groupCount();

			if (gcount == 0) {
				if (option == null || option.indexOf('w') == -1) {
					if (m.find()) {
						return srcStr;
					} else {
						return null;
					}
				} else {
					if (m.matches()) {
						return srcStr;
					} else {
						return null;
					}
				}
			} else if (gcount == 1) {
				if (!m.find()) {
					return null;
				}
				
				Sequence seq = new Sequence(3);
				if (option == null || option.indexOf('p') == -1) {
					do {
						seq.add(m.group(1));
					} while(m.find());
				} else {
					do {
						String s = m.group(1);
						seq.add(Variant.parse(s));
					} while(m.find());
				}
				
				return seq;
			} else {
				if (!m.find()) {
					return null;
				}
				
				Sequence seq = new Sequence(gcount);
				if (option == null || option.indexOf('p') == -1) {
					do {
						for (int i = 1; i <= gcount; ++i) {
							seq.add(m.group(i));
						}
					} while (m.find());
				} else {
					do {
						for (int i = 1; i <= gcount; ++i) {
							String s = m.group(i);
							seq.add(Variant.parse(s));
						}
					} while (m.find());
				}
				
				return seq;
			}
		} else if (param.getSubSize() == 2) {
			IParam sub0 = param.getSub(0);
			IParam sub1 = param.getSub(1);
			if (sub0 == null || sub1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub0.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.paramTypeError"));
			}
			
			String regex = (String)obj;
			String replacement;
			obj = sub1.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				replacement = (String)obj;
			} else if (obj == null) {
				replacement = "";
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("regex" + mm.getMessage("function.paramTypeError"));
			}
			
			Pattern pattern = Pattern.compile(regex, flags);
			Matcher m = pattern.matcher(srcStr);
			if (option == null || option.indexOf('a') == -1) {
				return m.replaceFirst(replacement);
			} else {
				return m.replaceAll(replacement);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("regex" + mm.getMessage("function.invalidParam"));
		}
	}
}
