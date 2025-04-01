package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * 以分隔符连接序列中的成员成为字符串，处理子序列
 * A.concat(d)
 * @author RunQian
 *
 */
public class Concat extends SequenceFunction {
	public Object calculate(Context ctx) {
		String sep = "";
		if (param != null) {
			Object obj = param.getLeafExpression().calculate(ctx);
			if (obj instanceof String) {
				sep = (String)obj;
			} else if (obj instanceof Sequence) {
				int []lens = ((Sequence)obj).toIntArray();
				return concat(srcSequence, lens, option);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("concat" + mm.getMessage("function.paramTypeError"));
			}
		}

		return srcSequence.toString(sep, option);
	}
	
	public static String concat(Sequence srcSequence, int []lens, String opt) {
		StringBuffer sb = new StringBuffer(128);
		int fcount = lens.length;
		
		if (opt != null && opt.indexOf('n') != -1) {
			for (int r = 1, length = srcSequence.length(); r <= length; ++r) {
				if (r > 1) {
					sb.append('\n');
				}

				Sequence sequence = (Sequence)srcSequence.getMem(r);
				int count = sequence.length();
				for (int f = 0; f < fcount; ++f) {
					Object obj = null;
					if (f < count) {
						obj = sequence.get(f + 1);
					}

					if (obj instanceof String) {
						String str = (String)obj;
						sb.append(str);
						for (int i = str.length(); i < lens[f]; ++i) {
							sb.append(' ');
						}
					} else if (obj instanceof Number) {
						String str = Variant.toString(obj);
						int len = str.length();
						if (len == lens[f]) {
							sb.append(str);
						} else if (len < lens[f]) {
							for (int i = len; i < lens[f]; ++i) {
								sb.append('0');
							}
							
							sb.append(str);
						} else {
							sb.append(str.substring(0, lens[f]));
						}
					} else if (obj != null) {
						String str = Variant.toString(obj);
						sb.append(str);
						for (int i = str.length(); i < lens[f]; ++i) {
							sb.append(' ');
						}
					} else {
						for (int i = 0; i < lens[f]; ++i) {
							sb.append(' ');
						}
					}
				}
			}
		} else {
			int count = srcSequence.length();
			for (int f = 0; f < fcount; ++f) {
				Object obj = null;
				if (f < count) {
					obj = srcSequence.get(f + 1);
				}
				
				if (obj instanceof String) {
					String str = (String)obj;
					sb.append(str);
					for (int i = str.length(); i < lens[f]; ++i) {
						sb.append(' ');
					}
				} else if (obj instanceof Number) {
					String str = Variant.toString(obj);
					int len = str.length();
					if (len == lens[f]) {
						sb.append(str);
					} else if (len < lens[f]) {
						for (int i = len; i < lens[f]; ++i) {
							sb.append('0');
						}
						
						sb.append(str);
					} else {
						sb.append(str.substring(0, lens[f]));
					}
				} else if (obj != null) {
					String str = Variant.toString(obj);
					sb.append(str);
					for (int i = str.length(); i < lens[f]; ++i) {
						sb.append(' ');
					}
				} else {
					for (int i = 0; i < lens[f]; ++i) {
						sb.append(' ');
					}
				}
			}
		}
		
		return sb.toString();
	}
}
