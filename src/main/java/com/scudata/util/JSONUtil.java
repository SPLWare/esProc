package com.scudata.util;

import java.util.Date;

import com.scudata.array.IArray;
import com.scudata.common.Escape;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

import java.util.ArrayList;

public final class JSONUtil {
	private static int scanQuotation(char []chars, int start, int end) {
		for (; start <= end; ++start) {
			if (chars[start] == '"') {
				return start;
			} else if (chars[start] == '\\') {
				start++;
			}
		}
		
		return -1;
	}
	
	private static int indexOf(char []chars, int start, int end, char c) {
		for (; start <= end; ++start) {
			if (chars[start] == c) return start;
			
			switch (chars[start]) {
			case '[':
				start = indexOf(chars, start + 1, end, ']');
				if (start < 0) {
					return -1;
				}
				
				break;
			case '{':
				start = indexOf(chars, start + 1, end, '}');
				if (start < 0) {
					return -1;
				}
				
				break;
			case '"':
				start = scanQuotation(chars, start + 1, end);	
				if (start < 0) {
					return -1;
				}
				break;
			case '\\':
				start++;
				break;
			}
		}
		
		return -1;
	}

	/**
	 * v1,v2...
	 * @param chars
	 * @param start 值的起始位置，包含
	 * @param end 值的结束位置，包含
	 * @return Sequence
	 */
	private static Sequence parseSequence(char []chars, int start, int end, String opt) {
		// 跳过前面的空白
		for (; start <= end && Character.isWhitespace(chars[start]); ++start) {
		}
		
		Sequence sequence = new Sequence();
		while (start <= end) {
			int index = indexOf(chars, start, end, ',');
			if (index < 0) {
				Object value = parseJSON(chars, start, end, opt);
				sequence.add(value);
				break;
			} else {
				Object value = parseJSON(chars, start, index - 1, opt);
				sequence.add(value);
				
				// 跳过逗号后的空白
				for (start = index + 1; start <= end && Character.isWhitespace(chars[start]); ++start) {
				}

				if (start > end) {
					sequence.add(null);
					break;
				}
			}
		}
		
		if (!sequence.isPmt()) {
			return sequence;
		}
		
		DataStruct ds = sequence.dataStruct();
		if (ds != null) {
			// 如果是纯排列则转为序表
			int len = sequence.length();
			Table table = new Table(ds, len);
			IArray memes = table.getMems();
			for (int i = 1; i <= len; ++i) {
				Record r = (Record)sequence.getMem(i);
				r.setDataStruct(ds);
				memes.add(r);
			}
			
			return table;
		} else if (opt != null && opt.indexOf('t') != -1) {
			// 不纯的结构进行对齐
			ArrayList<String> nameList = new ArrayList<String>();
			int len = sequence.length();
			for (int i = 1; i <= len; ++i) {
				BaseRecord r = (BaseRecord)sequence.getMem(i);
				if (r != null) {
					String []names = r.getFieldNames();
					for (String name : names) {
						if (!nameList.contains(name)) {
							nameList.add(name);
						}
					}
				}
			}
			
			String []names = new String[nameList.size()];
			nameList.toArray(names);
			ds = new DataStruct(names);
			Table table = new Table(ds, len);
			
			for (int i = 1; i <= len; ++i) {
				BaseRecord nr = table.newLast();
				BaseRecord r = (BaseRecord)sequence.getMem(i);
				if (r != null) {
					int f = 0;
					names = r.getFieldNames();
					for (String name : names) {
						int nf = ds.getFieldIndex(name);
						nr.setNormalFieldValue(nf, r.getNormalFieldValue(f++));
					}
				}
			}
			
			return table;
		} else {
			return sequence;
		}
	}
	
	/**
	 * n1:v1,n2:v2...
	 * @param chars
	 * @param start 值的起始位置，包含
	 * @param end 值的结束位置，包含
	 * @return BaseRecord
	 */
	private static BaseRecord parseRecord(char []chars, int start, int end, String opt) {
		if (start > end) {
			return null;
		}
		
		ArrayList<String> nameList = new ArrayList<String>();
		ArrayList<Object> valueList = new ArrayList<Object>();
		
		while (start <= end) {
			int index = indexOf(chars, start, end, ':');
			if (index < 0) break;

			String name = new String(chars, start, index - start);
			name = name.trim();
			int len = name.length();
			if (len > 2 && name.charAt(0) == '"' && name.charAt(len - 1) == '"') {
				name = name.substring(1, len - 1);
			}
			
			nameList.add(name);
			start = index + 1;
			index = indexOf(chars, start, end, ',');
			
			if (index < 0) {
				Object value = parseJSON(chars, start, end, opt);
				valueList.add(value);
				break;
			} else {
				Object value = parseJSON(chars, start, index - 1, opt);
				valueList.add(value);
				start = index + 1;
			}
		}
		
		int size = nameList.size();
		String []names = new String[size];
		nameList.toArray(names);
		DataStruct ds = new DataStruct(names);
		Record r = new Record(ds);
		valueList.toArray(r.getFieldValues());
		return r;
	}
	
	/**
	 * 解析json格式字符串，如果中括号或花括号不匹配则返回null
	 * @param chars [{F:v,…},…]
	 * @param start
	 * @param end
	 * @param opt t：排列结构不纯时将对齐成序表
	 * @return
	 */
	public static Object parseJSON(char []chars, int start, int end, String opt) {
		for (; start <= end && Character.isWhitespace(chars[end]); --end){
		}
		
		for (; start <= end; ++start) {
			char c = chars[start];
			if (c == '[') {
				if (chars[end] == ']') {
					return parseSequence(chars, start + 1, end - 1, opt);
				} else {
					//return null;
					return new String(chars, start, end - start + 1).trim();
				}
			} else if (c == '{') {
				if (chars[end] == '}') {
					return parseRecord(chars, start + 1, end - 1, opt);
				} else {
					return null;
				}
			} else if (!Character.isWhitespace(c)) {
				String str = new String(chars, start, end - start + 1);
				return parse(str);
			}
		}
		
		return null;
	}
	
	/**
	 * 解析json格式字符串，如果中括号或花括号不匹配则返回null
	 * @param chars [{F:v,…},…]
	 * @param start
	 * @param end
	 * @return
	 */
	public static Object parseJSON(char []chars, int start, int end) {
		return parseJSON(chars, start, end, null);
	}
	
	public static void toJSON(Object obj, StringBuffer sb) {
		if (obj == null) {
			sb.append("null");
		} else if (obj instanceof BaseRecord) {
			BaseRecord r = (BaseRecord)obj;
			String []names = r.getFieldNames();
			Object []vals = r.getFieldValues();
			sb.append('{');
			for (int f = 0, fcount = vals.length; f < fcount; ++f) {
				if (f > 0) sb.append(',');
				
				// 中文名不加引号的话网页报错
				sb.append(Escape.addEscAndQuote(names[f]));
				sb.append(':');
				toJSON(vals[f], sb);
			}

			sb.append('}');
		} else if (obj instanceof Sequence) {
			IArray mems = ((Sequence)obj).getMems();
			sb.append('[');
			for (int i = 1, len = mems.size(); i <= len; ++i) {
				if (i > 1) sb.append(',');
				toJSON(mems.get(i), sb);
			}

			sb.append(']');
		} else if (obj instanceof String) {
			sb.append(Escape.addEscAndQuote((String)obj));
		} else if (obj instanceof Date) {
			String str = Variant.toString(obj);
			sb.append(Escape.addEscAndQuote(str));
		} else {
			sb.append(Variant.toString(obj));
		}
	}
	
	public static String toJSON(Sequence seq) {
		StringBuffer sb = new StringBuffer(1024);
		IArray mems = ((Sequence)seq).getMems();
		sb.append('[');
		
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			if (i > 1) sb.append(',');
			toJSON(mems.get(i), sb);
		}

		sb.append(']');
		return sb.toString();
	}
	
	public static String toJSON(BaseRecord r) {
		StringBuffer sb = new StringBuffer(1024);
		String []names = r.getFieldNames();
		Object []vals = r.getFieldValues();
		sb.append('{');
		
		for (int f = 0, fcount = vals.length; f < fcount; ++f) {
			if (f > 0) sb.append(',');
			
			// 中文名不加引号的话网页报错
			sb.append(Escape.addEscAndQuote(names[f]));
			sb.append(':');
			toJSON(vals[f], sb);
		}

		sb.append('}');
		return sb.toString();
	}
	
	private static Object parse(String s) {
		char ch0 = s.charAt(0);
		if (ch0 == '"'|| ch0 == '\'') {
			int match = Sentence.scanQuotation(s, 0);
			if (match == s.length() -1) {
				s = s.substring(1, match);
			}
			
			return StringUtils.unicode(s);
		}
		
		return Variant.parse(s, true);
	}
}