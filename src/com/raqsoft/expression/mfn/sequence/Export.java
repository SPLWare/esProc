package com.raqsoft.expression.mfn.sequence;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.ComputeStack;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DataStruct;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.ParamInfo2;
import com.raqsoft.expression.SequenceFunction;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.util.Variant;

/**
 * 将排列格式化成字符串
 * A.export(x:F,…;s)
 * @author RunQian
 *
 */
public class Export extends SequenceFunction {
	public Object calculate(Context ctx) {
		Expression []exps = null;
		String []names = null;
		String s = null;

		IParam param = this.param;
		if (param != null && param.getType() == IParam.Semicolon) { // ;
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}

			IParam param1 = param.getSub(1);
			if (param1 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.invalidParam"));
			}

			Object obj = param1.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("export" + mm.getMessage("function.paramTypeError"));
			}

			s = (String)obj;
			param = param.getSub(0);
		}

		if (param != null) {
			ParamInfo2 pi2 = ParamInfo2.parse(param, "export", true, false);
			exps = pi2.getExpressions1();
			names = pi2.getExpressionStrs2();
		}

		return export(srcSequence, exps, names, s, option, ctx);
	}

	/**
	 * 将排列变成字符串
	 * @param sequence 排列
	 * @param exps 字段表达式，可空
	 * @param names 导出字段名，可空
	 * @param s 列分隔符
	 * @param option 选项
	 * @param ctx
	 * @return
	 */
	public static String export(Sequence sequence, Expression []exps, String []names, 
				String s, String option, Context ctx) {
		boolean isTitle = false, isCsv = false, isQuote = false;
		String LINE_SEPARATOR = new String(FileObject.LINE_SEPARATOR);
		char escapeChar = '\\';
		if (option != null) {
			if (option.indexOf('t') != -1) isTitle = true;
			if (option.indexOf('c') != -1) isCsv = true;
			if (option.indexOf('w') != -1) {
				LINE_SEPARATOR = new String(FileObject.DM_LINE_SEPARATOR);
			}
			
			if (option.indexOf('q') != -1) isQuote = true;
			if (option.indexOf('o') != -1) escapeChar = '"';
		}
				
		StringBuffer sb = new StringBuffer(1024 * 1024);
		if (s == null || s.length() == 0) {
			if (isCsv) {
				s = ",";
			} else {
				s = "\t";
			}
		}

		if (exps == null) {
			int fcount = 1;
			DataStruct ds = sequence.dataStruct();
			if (ds == null) {
				fcount = FileObject.getMaxMemberCount(sequence);
				if (isTitle && fcount < 1) {
					if (isQuote) {
						writeLine(sb, new String[]{FileObject.S_FIELDNAME}, s, LINE_SEPARATOR, escapeChar);
					} else {
						writeLine(sb, new String[]{FileObject.S_FIELDNAME}, s, LINE_SEPARATOR);
					}
				}
			} else {
				fcount = ds.getFieldCount();
				if (isTitle) {
					if (isQuote) {
						writeLine(sb, ds.getFieldNames(), s, LINE_SEPARATOR, escapeChar);
					} else {
						writeLine(sb, ds.getFieldNames(), s, LINE_SEPARATOR);
					}
				}
			}

			if (ds == null) {
				if (fcount < 1) {
					Object []lineObjs = new Object[1];
					for (int i = 1, len = sequence.length(); i <= len; ++i) {
						lineObjs[0] = sequence.getMem(i);
						if (isQuote) {
							writeLine(sb, lineObjs, s, LINE_SEPARATOR, escapeChar);
						} else {
							writeLine(sb, lineObjs, s, LINE_SEPARATOR);
						}
					}
				} else {
					// A是序列的序列时，生成无标题的多列文本
					Object []lineObjs = new Object[fcount];
					for (int i = 1, len = sequence.length(); i <= len; ++i) {
						Sequence seq = (Sequence)sequence.getMem(i);
						if (seq == null) {
							for (int f = 0; f < fcount; ++f) {
								lineObjs[f] = null;
							}
						} else {
							seq.toArray(lineObjs);
							for (int f = seq.length(); f < fcount; ++f) {
								lineObjs[f] = null;
							}
						}
						
						if (isQuote) {
							writeLine(sb, lineObjs, s, LINE_SEPARATOR, escapeChar);
						} else {
							writeLine(sb, lineObjs, s, LINE_SEPARATOR);
						}
					}
				}
			} else {
				for (int i = 1, len = sequence.length(); i <= len; ++i) {
					Record r = (Record)sequence.getMem(i);
					Object []vals = r.getFieldValues();

					if (isQuote) {
						writeLine(sb, vals, s, LINE_SEPARATOR, escapeChar);
					} else {
						writeLine(sb, vals, s, LINE_SEPARATOR);
					}
				}
			}
		} else {
			ComputeStack stack = ctx.getComputeStack();
			Sequence.Current current = sequence.new Current();
			stack.push(current);

			try {
				int fcount = exps.length;
				if (isTitle) {
					if (names == null) names = new String[fcount];
					sequence.getNewFieldNames(exps, names, "export");
					if (isQuote) {
						writeLine(sb, names, s, LINE_SEPARATOR, escapeChar);
					} else {
						writeLine(sb, names, s, LINE_SEPARATOR);
					}
				}

				Object []lineObjs = new Object[fcount];
				for (int i = 1, len = sequence.length(); i <= len; ++i) {
					current.setCurrent(i);
					for (int f = 0; f < fcount; ++f) {
						lineObjs[f] = exps[f].calculate(ctx);
					}

					if (isQuote) {
						writeLine(sb, lineObjs, s, LINE_SEPARATOR, escapeChar);
					} else {
						writeLine(sb, lineObjs, s, LINE_SEPARATOR);
					}
				}
			} finally {
				stack.pop();
			}
		}

		return sb.toString();
	}

	private static void writeLine(StringBuffer sb, Object []items, String s, String ls) {
		String str = Variant.toExportString(items[0]);
		if (str != null) {
			sb.append(str);
		}

		for (int i = 1, len = items.length; i < len; ++i) {
			sb.append(s);
			str = Variant.toExportString(items[i]);
			if (str != null) {
				sb.append(str);
			}
		}

		sb.append(ls);
	}

	private static void writeLine(StringBuffer sb, Object []items, String s, String ls, char escapeChar) {
		String str = Variant.toExportString(items[0], escapeChar);
		if (str != null) {
			sb.append(str);
		}

		for (int i = 1, len = items.length; i < len; ++i) {
			sb.append(s);
			str = Variant.toExportString(items[i], escapeChar);
			if (str != null) {
				sb.append(str);
			}
		}

		sb.append(ls);
	}
}
