package com.scudata.common;

import java.util.Enumeration;

/**
 * 本类用于拆分字符串，默认的分隔符为逗号
 * 分割时将跳过单双引号与指定种类括号内的分隔符
 * 特别注意：空白串中有一个标记，其值也为空白串
 * @author RunQian
 *
 */
public final class ArgumentTokenizer implements Enumeration<String> {
	private String str; // 源串
	private int len; // 长度
	private int index; // 当前分割的位置
	private char delim = ','; // 分隔符，默认为逗号

	private boolean parentheses = false; // 是否忽略圆括号内的分隔符
	private boolean brackets = false; // 是否忽略中括号内的分隔符
	private boolean braces = false; // 是否忽略花括号内的分隔符
	private boolean singleQuotation = false; // 是否忽略单引号内的分隔符
	
	private boolean count; // 只取数量，不取分割后的子串

	/**
	 * 为指定字符串构造一个参数分割器。缺省分隔符为','。
	 * @param s 指定的字符串
	 */
	public ArgumentTokenizer(String s) {
		this(s, ',', false, false, false);
	}

	/**
	 * 为指定字符串构造一个参数分割器
	 * @param s 指定的字符串
	 * @param delim 指定的分隔符
	 */
	public ArgumentTokenizer(String s, char delim) {
		this(s, delim, false, false, false);
	}

	/**
	 * 为指定字符串构造一个参数分割器
	 * @param s 指定的字符串
	 * @param ignoreParentheses 忽略圆括号内的分隔符
	 * @param ignoreBrackets 忽略中括号内的分隔符
	 * @param ignoreBraces 忽略花括号内的分隔符
	 */
	public ArgumentTokenizer(String s, boolean ignoreParentheses,
			boolean ignoreBrackets, boolean ignoreBraces) {
		this(s, ',', ignoreParentheses, ignoreBrackets, ignoreBraces);
	}

	/**
	 * 为指定字符串构造一个参数分割器
	 * @param s 指定的字符串
	 * @param delim 指定的分隔符
	 * @param ignoreParentheses 忽略圆括号内的分隔符
	 * @param ignoreBrackets 忽略中括号内的分隔符
	 * @param ignoreBraces 忽略花括号内的分隔符
	 */
	public ArgumentTokenizer(String s, char delim, boolean ignoreParentheses,
			boolean ignoreBrackets, boolean ignoreBraces) {
		this(s, delim, ignoreParentheses, ignoreBrackets, ignoreBraces, false);
	}

	/**
	 * 为指定字符串构造一个参数分割器
	 * @param s 指定的字符串
	 * @param delim 指定的分隔符
	 * @param ignoreParentheses 忽略圆括号内的分隔符
	 * @param ignoreBrackets 忽略中括号内的分隔符
	 * @param ignoreBraces 忽略花括号内的分隔符
	 * @param ignoreSingleQuotation 忽略单引号内的分隔符
	 */
	public ArgumentTokenizer(String s, char delim, boolean ignoreParentheses,
			boolean ignoreBrackets, boolean ignoreBraces,
			boolean ignoreSingleQuotation) {
		// str = s.trim();
		str = s;
		this.delim = delim;
		this.parentheses = !ignoreParentheses;
		this.brackets = !ignoreBrackets;
		this.braces = !ignoreBraces;
		this.singleQuotation = !ignoreSingleQuotation;
		len = (str == null || str.length() == 0) ? -1 : str.length();
	}

	/**
	 * 取下一个标记
	 * @return 若字符串为null，则返回null。若hasNext()或hasMoreTokens()为真，则返回
	 *         分割符分割的标记(非空串或空串)，否则返回null。 若不匹配的引号(单/双)则返回引号后的所有字符
	 *         若引号前转义符\，则此引号不起引号作用
	 * 
	 */
	public String next() {
		if (str == null || index > len)
			return null;
		int old = index;
		while (index <= len) {
			if (index == len) {
				index++;
				if (len > 1 && str.charAt(len - 1) == delim)
					return count ? null : "";
				break;
			}
			char ch = str.charAt(index);
			if (ch == '\\') {
				index += 2;
				continue;
			}
			if (ch == '\"' || (singleQuotation && ch == '\'')) {
				int tmp = Sentence.scanQuotation(str, index);
				if (tmp < 0) {
					index = len + 1;
					return count ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (parentheses && ch == '(') {
				int tmp = Sentence.scanParenthesis(str, index);
				if (tmp < 0) {
					index = len + 1;
					return count ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (brackets && ch == '[') {
				int tmp = Sentence.scanBracket(str, index);
				if (tmp < 0) {
					index = len + 1;
					return count ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			if (braces && ch == '{') {
				int tmp = Sentence.scanBrace(str, index);
				if (tmp < 0) {
					index = len + 1;
					return count ? null : str.substring(old);
				}
				index = tmp + 1;
				continue;
			}
			index++;
			if (ch == delim)
				break;
		}
		return count ? null : str.substring(old, index - 1);
	}

	/**
	 * 取下一个标记
	 * @return 若字符串为null，则返回null。若hasMoreTokens()为真，则返回分割符分割
	 *         的标记(非空串或空串)，否则返回null。
	 */
	public String nextToken() {
		return next();
	}

	/**
	 * 取下一个标记
	 * @return 若字符串为null，则返回null。若hasMoreTokens()为真，则返回分割符分割
	 *         的标记(非空串或空串)，否则返回null。
	 */
	public String nextElement() {
		return next();
	}

	/**
	 * 计算指定字符串中所有未访问标记的个数。
	 * @return 字符串中标记数
	 */
	public int countTokens() {
		int j = index;
		count = true;
		int i;
		for (i = 0; index <= len; i++)
			next();

		index = j;
		count = false;
		return i;
	}

	/**
	 * 检查是否还有标记
	 * @return 还有标记返回true，否则返回false
	 */
	public boolean hasNext() {
		return index <= len;
	}

	/**
	 * 检查是否还有标记
	 * @return 还有标记返回true，否则返回false
	 */
	public boolean hasMoreTokens() {
		return hasNext();
	}

	/**
	 * 检查是否还有标记
	 * @return 还有标记返回true，否则返回false
	 */
	public boolean hasMoreElements() {
		return hasNext();
	}
}
