package com.scudata.util;

import java.util.Date;

import com.scudata.common.Escape;
import com.scudata.common.RQException;
import com.scudata.common.Sentence;
import com.scudata.common.StringUtils;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;

import java.util.ArrayList;

public final class JSONUtil {
	private static class JSONImporter {
		private char []chars;
		private int start;
		private int end;
		private DataStruct ds;
		
		private ArrayList <String> nameList = new ArrayList<String>();
		private ArrayList <Object> valueList = new ArrayList<Object>();
		private boolean isPure = true; // 返回是否纯
		
		public JSONImporter(char []chars, int start, int end) {
			this.chars = chars;
			this.start = start;
			this.end = end;
		}
		
		public boolean isPure() {
			return isPure;
		}
		
		public Record next() {
			char []chars = this.chars;
			int i = this.start;
			int next = this.end;

			for (; i <= next; ++i) {
				if (chars[i] == '{') {
					next = indexOf(chars, i + 1, end, '}');
					break;
				} else if (!Character.isWhitespace(chars[i])) {
					throw new RQException("table format error, position: " + i);
				}
			}

			if (i > next) {
				return null;
			}
			
			ArrayList <String> nameList = this.nameList;
			ArrayList <Object> valueList = this.valueList;
			
			i++;
			while (i < next) {
				int index = indexOf(chars, i, next, ':');
				if (index < 0) break;

				String name = new String(chars, i, index - i);
				name = name.trim();
				int len = name.length();
				if (len > 2 && name.charAt(0) == '"' && name.charAt(len - 1) == '"') {
					name = name.substring(1, len - 1);
				}
				
				nameList.add(name);
				i = index + 1;
				index = indexOf(chars, i, next, ',');
				
				if (index < 0) {
					Object value = parseJSON(chars, i, next - 1);
					valueList.add(value);
					break;
				} else {
					Object value = parseJSON(chars, i, index - 1);
					valueList.add(value);
					i = index + 1;
				}
			}
			
			Record r;
			int size = nameList.size();
			if (ds != null) {
				String []names = ds.getFieldNames();
				int fcount = names.length;
				r = new Record(ds);
				for (int f = 0; f < size; ++f) {
					String name = nameList.get(f);
					Object val = valueList.get(f);
					if (f < fcount && names[f].equalsIgnoreCase(name)) {
						r.setNormalFieldValue(f, val);
					} else {
						int index = ds.getFieldIndex(name);
						if (index != -1) {
							r.setNormalFieldValue(index, val);
						} else {
							isPure = false;
							String []newNames = new String[fcount + 1];
							System.arraycopy(names, 0, newNames, 0, fcount);
							newNames[fcount] = name;
							ds = new DataStruct(newNames);
							r = new Record(ds, r.getFieldValues());
							r.setNormalFieldValue(fcount, val);
						}
					}
				}
			} else {
				String []names = new String[size];
				nameList.toArray(names);
				ds = new DataStruct(names);
				r = new Record(ds);
				valueList.toArray(r.getFieldValues());
			}
			
			// 跳过记录分隔符,
			int index = indexOf(chars, next + 1, this.end, ',');
			if (index < 0) {
				this.start = this.end + 1;
			} else {
				this.start = index + 1;
			}
			
			nameList.clear();
			valueList.clear();
			return r;
		}
	}

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

	// v,...
	private static Sequence parseSequence(char []chars, int start, int end) {
		for (; start <= end && Character.isWhitespace(chars[start]); ++start) {
		}
		
		if (start > end) {
			return new Sequence(0);
		} else if (chars[start] == '{') {
			JSONImporter importer = new JSONImporter(chars, start, end);
			Record r = importer.next();
			if (r == null) {
				return null;
			}
			
			Table table = new Table(r.dataStruct());
			ListBase1 mems = table.getMems();
			mems.add(r);
			
			while ((r = importer.next()) != null) {
				mems.add(r);
			}
			
			if (!importer.isPure()) {
				int len = mems.size();
				DataStruct ds = importer.ds;
				Table result = new Table(ds, len);
				
				for (int i = 1; i <= len; ++i) {
					r = (Record)mems.get(i);
					result.newLast(r.getFieldValues());
				}
				
				return result;
			} else {
				return table;
			}
		} else {
			Sequence sequence = new Sequence();
			ListBase1 mems = sequence.getMems();
			
			while (true) {
				int index = indexOf(chars, start, end, ',');
				if (index < 0) {
					Object value = parseJSON(chars, start, end);
					mems.add(value);
					break;
				} else {
					Object value = parseJSON(chars, start, index - 1);
					mems.add(value);
					start = index + 1;
					if (start > end) {
						mems.add(null);
						break;
					}
				}
			}
			
			return sequence;
		}
	}
	
	/**
	 * 解析json格式字符串，如果中括号或花括号不匹配则返回null
	 * @param chars [{F:v,…},…]
	 * @param start
	 * @param end
	 * @return
	 */
	public static Object parseJSON(char []chars, int start, int end) {
		for (; start <= end && Character.isWhitespace(chars[end]); --end){
		}
		
		for (; start <= end; ++start) {
			char c = chars[start];
			if (c == '[') {
				if (chars[end] == ']') {
					return parseSequence(chars, start + 1, end - 1);
				} else {
					return null;
				}
			} else if (c == '{') {
				if (chars[end] == '}') {
					JSONImporter importer = new JSONImporter(chars, start, end);
					return importer.next();
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
	
	public static void toJSON(Object obj, StringBuffer sb) {
		if (obj == null) {
			sb.append("null");
		} else if (obj instanceof Record) {
			Record r = (Record)obj;
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
			ListBase1 mems = ((Sequence)obj).getMems();
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
		ListBase1 mems = ((Sequence)seq).getMems();
		sb.append('[');
		
		for (int i = 1, len = mems.size(); i <= len; ++i) {
			if (i > 1) sb.append(',');
			toJSON(mems.get(i), sb);
		}

		sb.append(']');
		return sb.toString();
	}
	
	public static String toJSON(Record r) {
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