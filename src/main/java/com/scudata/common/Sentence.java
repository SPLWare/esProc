package com.scudata.common;

import java.io.*;

/**
 * 此类使用转义字符时请注意不能使用单引号、双引号、圆括号、中括号和大括号。 此类支持转义字符\，即\后的引号与括号不起作用，将被跳过
 */

public final class Sentence {
	/**
	 * 搜索时忽略大小写
	 */
	public static final int IGNORE_CASE = 1;
	/**
	 * 搜索时忽略圆括号,即要扫描括号中内容
	 */
	public static final int IGNORE_PARS = 2;
	/**
	 * 搜索时仅仅查找第一个匹配串
	 */
	public static final int ONLY_FIRST = 4;
	/**
	 * 搜索时仅仅在匹配串是标识符时成功
	 */
	public static final int ONLY_PHRASE = 8;
	/**
	 * 搜索时忽略引号,即要扫描引号中内容
	 */
	public static final int IGNORE_QUOTE = 16;
	/**
	 * 指示删除字符串中空白字符时将引号外的字符大写
	 */
	public static final int UPPER_WHEN_TRIM = 16;
	/**
	 * 指示删除字符串中空白字符时将引号外的字符小写
	 */
	public static final int LOWER_WHEN_TRIM = 32;

	private final static boolean LOG = false;

	public static void log(Object o) {
		if (LOG)
			System.out.println(o);
	}

	/**
	 * 构造函数
	 * 
	 * @param str
	 *            需要进行句法分析的字符串
	 */
	public Sentence(String str) {
		this.str = str;
	}

	/**
	 * 构造函数
	 * 
	 * @param str
	 *            需要进行句法分析的字符串
	 * @param escapeChar
	 *            转义字符
	 */
	public Sentence(String str, char escapeChar) {
		this.str = str;
		this.escapeChar = escapeChar;
	}

	/**
	 * 搜索标识符
	 * 
	 * @param str
	 *            需要搜索标识的原串
	 * @param start
	 *            搜索的起始位置
	 * @return 返回标识符最后一个字符在原串中的位置
	 */
	public static int scanIdentifier(String str, int start) {
		int len = str.length();
		char ch = str.charAt(start);
		if (!Character.isJavaIdentifierStart(ch))
			return -1;
		int i = start + 1;
		while (i < len) {
			ch = str.charAt(i);
			if (ch == (char) 0)
				break;
			if (ch == (char) 1)
				break;
			if (!Character.isJavaIdentifierPart(ch))
				break;
			i++;
		}
		return i - 1;
	}

	public static boolean checkIdentifier(String ident) {
		if (ident == null)
			return false;
		int len = ident.length();
		if (len == 0)
			return false;
		char c = ident.charAt(0);
		if (!Character.isJavaIdentifierStart(c))
			return false;

		for (int i = 1; i < len; i++) {
			c = ident.charAt(i);
			if (c == (char) 0)
				return false;
			if (c == (char) 1)
				return false;
			if (!Character.isJavaIdentifierPart(c))
				return false;
		}
		return true;
	}

	/**
	 * 搜索下一个匹配的引号'或"
	 * 
	 * @param str
	 *            需要搜索引号的原串
	 * @param start
	 *            起始位置,即头一引号在原串中的位置
	 * @param escapeChar
	 *            转义字符
	 * @return 若找到,则返回匹配的引号在原串中的位置,否则返回-1
	 */
	public static int scanQuotation(String str, int start, char escapeChar) {
		char quote = str.charAt(start);
		if (quote != '\"' && quote != '\'')
			return -1;
		int idx = start + 1, len = str.length();
		while (idx < len) {
			/*
			 * idx = str.indexOf(quote, idx); if (idx < 0) break; if (
			 * str.charAt(idx - 1) != escapeChar) return idx; idx++;
			 */
			char ch = str.charAt(idx);
			if (ch == escapeChar)
				idx += 2;
			else if (ch == quote)
				return idx;
			else
				idx++;
		}
		return -1;
	}

	/**
	 * 搜索下一个匹配的引号'或",缺省转义字符为\
	 * 
	 * @param str
	 *            需要搜索引号的原串
	 * @param start
	 *            起始位置,即头一引号在原串中的位置
	 * @return 若找到,则返回匹配的引号在原串中的位置,否则返回-1
	 */
	public static int scanQuotation(String str, int start) {
		return scanQuotation(str, start, '\\');
	}

	/**
	 * 搜索下一个匹配的圆括号，但引号内的圆括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左圆括号(在原串中的位置
	 * @param escapeChar
	 *            转义字符
	 * @return 若找到,则返回匹配的右圆括号在原串中的位置,否则返回-1
	 */
	public static int scanParenthesis(String str, int start, char escapeChar) {
		if (str.charAt(start) != '(')
			return -1;

		int len = str.length();
		for (int i = start + 1; i < len;) {
			char ch = str.charAt(i);
			switch (ch) {
			case '(':
				i = scanParenthesis(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				break;
			case '\"':
			case '\'':
				int q = scanQuotation(str, i, escapeChar);
				if (q < 0) {
					i++;
				} else {
					i = q + 1;
				}
				break;
			case '[': // $[str]
				if (i > start && str.charAt(i - 1) == '$') {
					q = scanBracket(str, i, escapeChar);
					if (q < 0) {
						i++;
					} else {
						i = q + 1;
					}
				} else {
					i++;
				}
				break;
			case ')':
				return i;
			default:
				if (ch == escapeChar)
					i++;
				i++;
				break;
			}
		}
		return -1;
	}

	/**
	 * 搜索下一个匹配的圆括号,缺省转义字符为\，且引号内的圆括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左圆括号(在原串中的位置
	 * @return 若找到,则返回匹配的右圆括号在原串中的位置,否则返回-1
	 */
	public static int scanParenthesis(String str, int start) {
		return scanParenthesis(str, start, '\\');
	}

	/**
	 * 搜索下一个匹配的中括号，但引号内的中括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左中括号在原串中的位置
	 * @param escapeChar
	 *            转义字符
	 * @return 若找到,则返回匹配的右中括号在原串中的位置,否则返回-1
	 */
	public static int scanBracket(String str, int start, char escapeChar) {
		if (str.charAt(start) != '[')
			return -1;

		int len = str.length();
		for (int i = start + 1; i < len;) {
			char ch = str.charAt(i);
			switch (ch) {
			case '[':
				i = scanBracket(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				break;
			case '\"':
			case '\'':
				int q = scanQuotation(str, i, escapeChar);
				if (q < 0) {
					i++;
				} else {
					i = q + 1;
				}
				break;
			case ']':
				return i;
			default:
				if (ch == escapeChar)
					i++;
				i++;
				break;
			}
		}
		return -1;
	}

	/**
	 * 搜索下一个匹配的中括号,缺省转义字符为\，且引号内的中括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左中括号在原串中的位置
	 * @return 若找到,则返回匹配的右中括号在原串中的位置,否则返回-1
	 */
	public static int scanBracket(String str, int start) {
		return scanBracket(str, start, '\\');
	}

	/**
	 * 搜索下一个匹配的花括号，但引号内的花括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左花括号在原串中的位置
	 * @param escapeChar
	 *            转义字符
	 * @return 若找到,则返回匹配的右花括号在原串中的位置,否则返回-1
	 */
	public static int scanBrace(String str, int start, char escapeChar) {
		if (str.charAt(start) != '{')
			return -1;

		int len = str.length();
		for (int i = start + 1; i < len;) {
			char ch = str.charAt(i);
			switch (ch) {
			case '{':
				i = scanBrace(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				break;
			case '\"':
			case '\'':
				int q = scanQuotation(str, i, escapeChar);
				if (q < 0) {
					i++;
				} else {
					i = q + 1;
				}
				break;
			case '}':
				return i;
			default:
				if (ch == escapeChar)
					i++;
				i++;
				break;
			}
		}
		return -1;
	}

	/**
	 * 搜索下一个匹配的花括号,缺省转义字符为\，且引号内的花括号被跳过
	 * 
	 * @param str
	 *            需要搜索括号的原串
	 * @param start
	 *            起始位置,即左花括号(在原串中的位置
	 * @return 若找到,则返回匹配的右花括号在原串中的位置,否则返回-1
	 */
	public static int scanBrace(String str, int start) {
		return scanBrace(str, start, '\\');
	}

	/**
	 * 将原串中的空白字符删除,并根据ifcase参数将原串中的非引号内字符大写,小写或不动
	 * 
	 * @param str
	 *            需要删除空白字符的原串
	 * @param ifcase
	 *            可以使用0, UPPER_WHEN_TRIM, LOWER_WHEN_TRIM,
	 *            UPPER_WHEN_TRIM+LOWER_WHEN_TRIM
	 * @param escapeChar
	 *            转义字符
	 * @return 删除空白字符后的串
	 */
	public static String trim(String str, int ifcase, char escapeChar) {
		int idx = 0, len = str.length();
		int flag = 0;
		// 0 - ignore following whitespace
		// 1 - identifier char
		// 2 - whitespace
		StringBuffer dst = new StringBuffer(len);
		while (idx < len) {
			char ch = str.charAt(idx);
			if ((ch == '\"' || ch == '\'')
					&& ((idx > 0 && str.charAt(idx - 1) != escapeChar) || idx == 0)) {
				if (flag == 2)
					dst.append(' ');
				int i = scanQuotation(str, idx, escapeChar);
				if (i < 0)
					throw new RuntimeException("未找到位置" + idx + "处对应的引号");
				i++;
				for (int j = idx; j < i; j++)
					dst.append(str.charAt(j));
				idx = i;
				continue;
			} else if (Character.isWhitespace(ch)) {
				do {
					idx++;
				} while (idx < len && Character.isWhitespace(str.charAt(idx)));
				if (flag > 0)
					flag = 2;
				continue;
			} else if (isWordChar(ch)) {
				if (flag == 2)
					dst.append(' ');
				flag = 1;
			} else {
				flag = 0;
			}
			switch (ifcase) {
			case UPPER_WHEN_TRIM:
				dst.append(Character.toUpperCase(ch));
				break;
			case LOWER_WHEN_TRIM:
				dst.append(Character.toLowerCase(ch));
				break;
			default:
				dst.append(ch);
			}
			idx++;
		}

		return dst.toString();
	}

	/**
	 * 将原串中的空白字符删除,并根据ifcase参数将原串中的非引号内字符大写,小写或不动,缺省转义字符为\
	 * 
	 * @param str
	 *            需要删除空白字符的原串
	 * @param ifcase
	 *            可以为0, UPPER_WHEN_TRIM, LOWER_WHEN_TRIM,
	 *            UPPER_WHEN_TRIM+LOWER_WHEN_TRIM
	 * @return 删除空白字符后的串
	 */
	public static String trim(String str, int ifcase) {
		return trim(str, ifcase, '\\');
	}

	/**
	 * 在原串中搜索短语，不搜索引号中内容
	 * 
	 * @param str
	 *            需要搜索短语的原串
	 * @param phrase
	 *            需要搜索的短语
	 * @param start
	 *            在原串中的起始位置
	 * @param flag
	 *            可以为0,IGNORE_CASE,IGNORE_PARS,IGNORE_CASE+IGNORE_PARS
	 * @param escapeChar
	 *            转义字符
	 * @return 若找到,则返回短语在原串中的位置,否则返回-1
	 */
	public static int phraseAt(String str, String phrase, int start, int flag,
			char escapeChar) {
		int slen = str.length(), plen = phrase.length();
		boolean iswordchar = false;
		for (int i = start; i < slen;) {
			char ch = str.charAt(i);
			if ((ch == '\"' || ch == '\'')
					&& ((i > 0 && str.charAt(i - 1) != '\\') || i == 0)) {
				i = scanQuotation(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				iswordchar = false;
				continue;
			}
			if ((flag & IGNORE_PARS) == 0 && ch == '(') {
				i = scanParenthesis(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				iswordchar = false;
				continue;
			}
			if (!iswordchar) {
				if (phrase.regionMatches((flag & IGNORE_CASE) > 0, 0, str, i,
						plen)) {
					if ((i + plen) >= slen || !isWordChar(str.charAt(i + plen)))
						return i;
				}
			}
			iswordchar = isWordChar(ch);
			i++;
		}
		return -1;
	}

	/**
	 * 在原串中搜索短语,缺省转义字符为\
	 * 
	 * @param str
	 *            需要搜索短语的原串
	 * @param phrase
	 *            需要搜索的短语
	 * @param start
	 *            在原串中的起始位置
	 * @param flag
	 *            可以为0,IGNORE_CASE,IGNORE_PARS及其加法组合
	 * @return 若找到,则返回短语在原串中的位置,否则返回-1
	 */
	public static int phraseAt(String str, String phrase, int start, int flag) {
		return phraseAt(str, phrase, start, flag, '\\');
	}

	/**
	 * 替换原串中的子串
	 * 
	 * @param str
	 *            需要替换的原串
	 * @param start
	 *            对原串开始替换的起始位置
	 * @param sold
	 *            需要替换的子串
	 * @param snew
	 *            替换串
	 * @param flag
	 *            可为0,IGNORE_CASE,IGNORE_PARS,IGNORE_QUOTE,ONLY_FIRST,
	 *            ONLY_PHRASE及其加法组合
	 * @param escapeChar
	 *            转义字符
	 * @return 替换后的串
	 */
	public static String replace(String str, int start, String sold,
			String snew, int flag, char escapeChar) {
		int strlen = str.length(), len = sold.length();
		StringBuffer dst = null;
		char preChar = '*'; // 指示匹配串前一个字符不是有效标识符字符
		int i = start;
		while (i < strlen) {
			char ch = str.charAt(i);
			if ((ch == '\'' || ch == '\"') && (flag & IGNORE_QUOTE) == 0
					&& (i == 0 || (i > 0 && str.charAt(i - 1) != escapeChar))) {
				int idx = scanQuotation(str, i, escapeChar);
				if (idx < 0)
					throw new RuntimeException("未找到位置" + i + "处对应的引号");
				idx++;
				if (dst != null)
					for (int j = i; j < idx; j++)
						dst.append(str.charAt(j));
				i = idx;
				preChar = '*';
				continue;
			}
			if (((flag & IGNORE_PARS) == 0) && (ch == '(')) {
				int idx = scanParenthesis(str, i, escapeChar);
				if (idx < 0)
					throw new RuntimeException("未找到位置" + i + "处对应的圆括号");
				idx++;
				if (dst != null)
					for (int j = i; j < idx; j++)
						dst.append(str.charAt(j));
				i = idx;
				preChar = '*';
				continue;
			}
			boolean lb;
			lb = sold.regionMatches((flag & IGNORE_CASE) > 0, 0, str, i, len);
			if (lb && (flag & ONLY_PHRASE) > 0) {
				// 被匹配串的第一字符及前一字符有一个不是标识符字符时为真
				lb = !isWordChar(sold.charAt(0)) || !isWordChar(preChar);
				// 被匹配串的最后字符及后一字符有一个不是标识符字符时
				if ((i + len) < strlen)
					lb = lb
							&& (!isWordChar(sold.charAt(len - 1)) || !isWordChar(str
									.charAt(i + len)));
			}
			if (lb) {
				if (dst == null) {
					dst = new StringBuffer(strlen << 2);
					for (int j = 0; j < i; j++)
						dst.append(str.charAt(j));
				}
				dst.append(snew);
				i += len;
				preChar = str.charAt(i - 1);
				if ((flag & ONLY_FIRST) > 0) {
					while (i < strlen)
						dst.append(str.charAt(i++));
					break;
				}
			} else {
				if (dst != null)
					dst.append(ch);
				preChar = ch;
				i++;
			}

		} // while(i<strlen)
		return (dst == null) ? str : dst.toString();
	}

	/**
	 * 替换原串中的子串,缺省转义字符为\
	 * 
	 * @param str
	 *            需要替换的原串
	 * @param start
	 *            对原串开始替换的起始位置
	 * @param sold
	 *            需要替换的子串
	 * @param snew
	 *            替换串
	 * @param flag
	 *            可为0,IGNORE_CASE,IGNORE_PARS,ONLY_FIRST,ONLY_PHRASE及其加法组合
	 * @return 替换后的串
	 */
	public static String replace(String str, int start, String sold,
			String snew, int flag) {
		return replace(str, start, sold, snew, flag, '\\');
	}

	/**
	 * 替换原串中的子串,缺省转义字符为\
	 * 
	 * @param str
	 *            需要替换的原串
	 * @param sold
	 *            需要替换的子串
	 * @param snew
	 *            替换串
	 * @param flag
	 *            可为0,IGNORE_CASE,IGNORE_PARS,ONLY_FIRST,ONLY_PHRASE及其加法组合
	 * @return 替换后的串
	 */
	public static String replace(String str, String sold, String snew, int flag) {
		return replace(str, 0, sold, snew, flag, '\\');
	}

	/**
	 * 搜索下一个匹配的引号'或"
	 * 
	 * @param start
	 *            起始位置,即头一引号在原串中的位置
	 * @return 下一个匹配的引号在原串中的位置
	 */
	public int scanQuotation(int start) {
		return scanQuotation(this.str, start, escapeChar);
	}

	/**
	 * 将原串中的空白字符删除,并根据ifcase参数将原串中的非引号内字符大写,小写或不动， 将getSentence()返回的串更改为结果串
	 * 
	 * @param ifcase
	 *            可以为0, UPPER_WHEN_TRIM, LOWER_WHEN_TRIM及其加法组合
	 * @return 删除空白字符后的串
	 */
	public void trim(int ifcase) {
		this.str = trim(this.str, ifcase, escapeChar);
	}

	/**
	 * 在原串中搜索短语
	 * 
	 * @param phrase
	 *            需要搜索的短语
	 * @param start
	 *            在原串中的起始位置
	 * @param flag
	 *            可以为0,IGNORE_CASE,IGNORE_PARS,IGNORE_CASE+IGNORE_PARS
	 * @return 短语在原串中的位置
	 */
	public int phraseAt(String phrase, int start, int flag) {
		return phraseAt(this.str, phrase, start, flag, escapeChar);
	}

	/**
	 * 替换原串中的子串，并将getSentence()返回的字符串更改为结果串
	 * 
	 * @param start
	 *            对原串开始替换的起始位置
	 * @param sold
	 *            需要替换的子串
	 * @param snew
	 *            替换串
	 * @param flag
	 *            可为0,IGNORE_CASE,IGNORE_PARS,ONLY_FIRST,ONLY_PHRASE及其加法组合
	 * @return 被替换子串的个数
	 */
	public int replace(int start, String sold, String snew, int flag) {
		int strlen = str.length(), len = sold.length(), count = 0;
		StringBuffer dst = null;
		char preChar = '*'; // 指示匹配串前一个字符不是有效标识符字符
		int i = start;
		while (i < strlen) {
			char ch = str.charAt(i);
			if ((ch == '\"' || ch == '\'')
					&& ((i > 0 && str.charAt(i - 1) != '\\') || i == 0)) {
				int idx = scanQuotation(str, i, escapeChar);
				if (idx < 0)
					return 0;
				idx++;
				if (dst != null)
					for (int j = i; j < idx; j++)
						dst.append(str.charAt(j));
				i = idx;
				preChar = '*';
				continue;
			}
			if (((flag & IGNORE_PARS) == 0) && (ch == '(')) {
				int idx = scanParenthesis(str, i, escapeChar);
				if (idx < 0)
					return 0;
				idx++;
				if (dst != null)
					for (int j = i; j < idx; j++)
						dst.append(str.charAt(j));
				i = idx;
				preChar = '*';
				continue;
			}

			boolean lb;
			lb = sold.regionMatches((flag & IGNORE_CASE) > 0, 0, str, i, len);
			if (lb && (flag & ONLY_PHRASE) > 0) {
				// 被匹配串的第一字符及前一字符有一个不是标识符字符时为真
				lb = !isWordChar(sold.charAt(0)) || !isWordChar(preChar);
				// 被匹配串的最后字符及后一字符有一个不是标识符字符时
				if ((i + len) < strlen)
					lb = lb
							&& (!isWordChar(sold.charAt(len - 1)) || !isWordChar(str
									.charAt(i + len)));
			}
			if (lb) {
				if (dst == null) {
					dst = new StringBuffer(strlen << 2);
					for (int j = 0; j < i; j++)
						dst.append(str.charAt(j));
				}
				dst.append(snew);
				i += len;
				preChar = str.charAt(i - 1);
				count++;
				if ((flag & ONLY_FIRST) > 0) {
					while (i < strlen)
						dst.append(str.charAt(i++));
					break;
				}
			} else {
				if (dst != null)
					dst.append(ch);
				i++;
			}
		}
		if (dst != null)
			str = dst.toString();
		return count;
	}

	/**
	 * 取分析后的串
	 * 
	 * @return 分析后的串
	 */
	public String toString() {
		return this.str;
	}

	/**
	 * 取分析后的串
	 * 
	 * @return 分析后的串
	 */
	public String getSentence() {
		return this.str;
	}

	/**
	 * 在原串中搜索字符串(可以被包含中短语中)
	 * 
	 * @param phrase
	 *            需要搜索的字符串
	 * @param start
	 *            在原串中的起始位置
	 * @param flag
	 *            可以为0,IGNORE_CASE,IGNORE_PARS,IGNORE_CASE+IGNORE_PARS
	 * @return 字符串在原串中的位置
	 */
	public static int indexOf(String str, String find, int start, int flag,
			char escapeChar) {
		int slen = str.length(), plen = find.length();
		for (int i = start; i < slen;) {
			char ch = str.charAt(i);
			if ((flag & IGNORE_QUOTE) == 0
					&& // xq add,加上支持引号内搜索 2010.8.25
					(ch == '\"' || ch == '\'')
					&& ((i > 0 && str.charAt(i - 1) != '\\') || i == 0)) {
				i = scanQuotation(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				continue;
			}
			if ((flag & IGNORE_PARS) == 0 && ch == '(') {
				i = scanParenthesis(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				continue;
			}
			if (find.regionMatches((flag & IGNORE_CASE) > 0, 0, str, i, plen)) {
				return i;
			}
			i++;
		}
		return -1;
	}

	public int indexOf(String find, int start, int flag) {
		return indexOf(this.str, find, start, flag, escapeChar);
	}

	public static int indexOf(String str, String find, int start, int flag) {
		return indexOf(str, find, start, flag, '\\');
	}

	/**
	 * 集算器查找专用，为了支持ONLY_PHRASE（wunan） 在原串中搜索字符串(可以被包含中短语中)
	 * 
	 * @param phrase
	 *            需要搜索的字符串
	 * @param start
	 *            在原串中的起始位置
	 * @param flag
	 *            可以为0,IGNORE_CASE,IGNORE_PARS,IGNORE_CASE+IGNORE_PARS
	 * @return 字符串在原串中的位置
	 */
	public static int indexOf(String str, int start, String find, int flag) {
		char escapeChar = '\\';
		int slen = str.length(), plen = find.length();
		char preChar = '*';
		for (int i = start; i < slen;) {
			char ch = str.charAt(i);
			if ((flag & IGNORE_QUOTE) == 0
					&& // 加上支持引号内搜索 2018.10.9 wunan
					(ch == '\"' || ch == '\'')
					&& ((i > 0 && str.charAt(i - 1) != '\\') || i == 0)) {
				i = scanQuotation(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				preChar = ch;
				continue;
			}
			if ((flag & IGNORE_PARS) == 0 && (ch == '\"' || ch == '\'')
					&& ((i > 0 && str.charAt(i - 1) != '\\') || i == 0)) {
				i = scanQuotation(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				preChar = ch;
				continue;
			}
			if ((flag & IGNORE_PARS) == 0 && ch == '(') {
				i = scanParenthesis(str, i, escapeChar);
				if (i < 0)
					return -1;
				i++;
				preChar = ch;
				continue;
			}
			if (find.regionMatches((flag & IGNORE_CASE) > 0, 0, str, i, plen)) {
				if ((flag & ONLY_PHRASE) > 0) {
					// 被匹配串的第一字符及前一字符有一个不是标识符字符时为真
					boolean lb = !isWordChar(find.charAt(0))
							|| !isWordChar(preChar);
					// 被匹配串的最后字符及后一字符有一个不是标识符字符时
					if ((i + plen) < slen)
						lb = lb
								&& (!isWordChar(find.charAt(plen - 1)) || !isWordChar(str
										.charAt(i + plen)));
					if (!lb) {
						preChar = ch;
						i++;
						continue;
					}
				}
				return i;
			}
			i++;
			preChar = ch;
		}
		return -1;
	}
	
	/**
	 * 从后往前查找字符串，做引号、括号匹配
	 * @param src 源串
	 * @param find 要查找的串
	 * @return 找不到返回-1
	 */
	public static int lastIndexOf(String src, String find) {
		int end = src.length() - 1;
		int findLen = find.length();
		
		if (findLen == 1) {
			char tc = find.charAt(0);
			while (end >= 0) {
				char c = src.charAt(end);
				if (c == tc) {
					if (end > 0 && src.charAt(end - 1) == '\\') {
						break;
					} else {
						return end;
					}
				} else if (c == '"' || c == '\'' || c == ')' || c == ']' || c == '}' || c == '\\') {
					// 从头开始找匹配
					break;
				} else {
					end--;
				}
			}
		}
		
		int pos = -1;
		int i = 0;
		while (i <= end) {
			char c = src.charAt(i);
			switch (c) {
			case '"':
			case '\'':
				int match = scanQuotation(src, i, '\\');
				if (match == -1) {
					return -1;
				} else {
					i = match + 1;
					continue; // 跳过引号内的内容
				}
			case '(':
				match = Sentence.scanParenthesis(src, i, '\\');
				if (match == -1) {
					return -1;
				} else {
					i = match + 1;
					continue; // 跳过扩号内的内容
				}
			case '[':
				match = Sentence.scanBracket(src, i, '\\');
				if (match == -1) {
					return -1;
				} else {
					i = match + 1;
					continue; // 跳过扩号内的内容
				}
			case '{':
				match = Sentence.scanBrace(src, i, '\\');
				if (match == -1) {
					return -1;
				} else {
					i = match + 1;
					continue; // 跳过扩号内的内容
				}
			case '\\':
				i += 2;
				continue;
			}

			if (src.startsWith(find, i)) {
				pos = i;
				i += findLen;
			} else {
				i++;
			}
		}

		return pos;
	}
	
	public static boolean isWordChar(char ch) {
		return Character.isJavaIdentifierStart(ch)
				|| Character.isJavaIdentifierPart(ch);
	}
	
	//查找下一个\r或\n的位置，未找到返回长度
	private static int scanCRLF(String str, int start) {
		int len = str.length();
		while(start<len){
			char ch = str.charAt(start);
			if(ch=='\r' || ch=='\n') 
				return start;
			start++;
		}
		return len;
	}
	
	//查找*/的位置，未找到返回长度
	private static int scanCommentEnd(String str, int start) {
		int len = str.length();
		while(start<len) {
			char ch = str.charAt(start);
			if(ch=='*' && start<len-1 && str.charAt(start+1)=='/')
				return start;
			start++;
		}
		return len;
	}
	
	/**
	 * 删除串中java风格的行注释和段落注释 
	 */
	public static String removeComment(String str){
		int idx=0, len=str.length();
		StringBuffer buf = new StringBuffer(len);
		while(idx<len){
			char ch = str.charAt(idx);
			if (ch == '\'' || ch == '\"') {
				int tmp = Sentence.scanQuotation(str, idx);
				if(tmp<0) {
					buf.append(str.substring(idx));
					break;
				}else{
					buf.append(str.substring(idx, tmp+1));
					idx=tmp+1;
				}
			}else if(ch == '/') {
				if(idx==len-1){
					buf.append('/');
					break;
				}
				char ch2 = str.charAt(idx+1);
				if(ch2=='/') {
					idx = scanCRLF(str, idx+2);  //位置不加，后续回车换行需输出
				}else if(ch2=='*'){
					idx = scanCommentEnd(str, idx+2)+2;
				}else{
					buf.append('/');
					idx++;
				}
			}else{
				buf.append(ch);
				idx++;
			}
			
		}
		return buf.toString();
	}

	private String str;
	private char escapeChar = '\\';

	public static void main(String[] args) throws Exception {
		StringBuffer buf = new StringBuffer(); 
		BufferedReader br = new BufferedReader(new FileReader("d:\\1.txt"));
		while(true){
			String line = br.readLine();
			if(line==null) break;
			buf.append(line).append("\r\n");
		}
		br.close();
		String s = removeComment(buf.toString());
		System.out.println( s );
		System.out.println( "..." + System.getProperty("line.separator"));
	}
}
