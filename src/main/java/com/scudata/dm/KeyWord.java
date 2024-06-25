package com.scudata.dm;

/**
 * 定义了项目用到的运算符和标识符
 * @author WangXiaoJun
 *
 */
public final class KeyWord {
	public static final String SUBCODEBLOCK = "??"; // 计算以当前格为主格的代码块
	public static final String ARGPREFIX = "?"; // 参数前缀，eval函数的表达式可以用?1、?2这种形式引用参数
	public static final char OPTION = '@'; // 函数选项分隔符

	public static final String CURRENTELEMENT = "~"; // 序列当前元素标识符
	public static final String CURRENTSEQ = "#"; // 序列的当前循环序号
	public static final String ITERATEPARAM = "~~"; // 迭代参数，用于iterate函数中引用迭代值
	public static final String FIELDIDPREFIX = "#"; // 用序号引用字段#1, #2...
	public static final String CURRENTCELL = "@"; // 当前格
	public static final String CURRENTCELLSEQ = "#@"; // 当前格循环序号

	public static final char CELLPREFIX = '#'; // #A1 #C 单元格前缀
	public static final String CONSTSTRINGPREFIX = "'"; // 常量字符串前缀

	/**
	 * 判断指定字符是否是空白或者运算符等符号
	 * @param c 字符
	 * @return true：是，false：不是
	 */
	public static boolean isSymbol(char c) {
		return (Character.isWhitespace(c) ||
			c == '+' || c == '-' || c == '*' || c == '/' || c == '%' ||
			c == '=' || c == '&' || c == '|' || c == '!' || c == '\\' ||
			c == ',' || c == '>' || c == '<' || c == '(' || c == ')' ||
			c == '[' || c == ']' || c == ':' || c == '{' || c == '}' ||
			c == '^' || c == '.' || c == '"' || c == '\'' || c == ';');
	}

	/**
	 * 判断标识符是否是子代码块标识符
	 * @param id 标识符
	 * @return true：是，false：不是
	 */
	public static boolean isSubCodeBlock(String id) {
		return SUBCODEBLOCK.equals(id);
	}
	
	/**
	 * 判断标识符是否是序列的当前元素
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isCurrentElement(String id) {
		return CURRENTELEMENT.equals(id);
	}
	
	/**
	 * 判断标识符是否是序列的当前循环序号
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isCurrentSeq(String id) {
		return CURRENTSEQ.equals(id);
	}
	
	/**
	 * 判断标识符是否是当前格
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isCurrentCell(String id) {
		return CURRENTCELL.equals(id);
	}
	
	/**
	 * 判断标识符是否是当前格循环序号
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isCurrentCellSeq(String id) {
		return CURRENTCELLSEQ.equals(id);
	}
	
	/**
	 * 判断标识符是否是迭代变量
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isIterateParam(String id) {
		return ITERATEPARAM.equals(id);
	}

	/**
	 * 判断标识符是否是字段的序号法引用
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isFieldId(String id) {
		if (id == null || id.length() < 2 || !id.startsWith(FIELDIDPREFIX)) {
			return false;
		}
		
		for (int i = 1, len = id.length(); i < len; ++i) {
			char c = id.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 判断标识符是否是当前序列成员引用（~n）
	 * @param id 标识
	 * @return true：是，false：不是
	 */
	public static boolean isElementId(String id) {
		if (id == null || id.length() < 2 || !id.startsWith(CURRENTELEMENT)) {
			return false;
		}
		
		for (int i = 1, len = id.length(); i < len; ++i) {
			char c = id.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * 取字段的序号，id是形如#1这样的字符串
	 * @param id 字段的#n表示的串
	 * @return 字段序号
	 */
	public static int getFiledId(String id) {
		return Integer.parseInt(id.substring(FIELDIDPREFIX.length()));
	}

	/**
	 * 判断标识符是否是参数，参数可以用?引用，也可以用?1、?2这种指定引用第几个参数
	 * @param id 标识符
	 * @return true：是，false：不是
	 */
	public static boolean isArg(String id) {
		if (id == null || !id.startsWith(ARGPREFIX)) {
			return false;
		}
		
		for (int i = 1, len = id.length(); i < len; ++i) {
			char c = id.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 扫描标识符，返回结束位置
	 * @param expStr 表达式字符串
	 * @param start 起始扫描位置（包含）
	 * @return int 标识符的结束位置（不包含）
	 */
	public static int scanId(String expStr, int start) {
		int len = expStr.length();
		for (; start < len; ++start) {
			if (isSymbol(expStr.charAt(start))) {
				break;
			}
		}

		return start;
	}

	/**
	 * 把字符串中的函数选项分拆出来，源数组存放拆除选项后的串，选项做为返回值返回
	 * @param strs 字符串数组
	 * @return 选项数组
	 */
	public static String[] parseStringOptions(String []strs) {
		int size = strs.length;
		String []opts = new String[size];

		for (int i = 0; i < size; ++i) {
			String tmp = strs[i];
			if (tmp != null) {
				int optIndex = tmp.indexOf(OPTION);
				if (optIndex != -1) {
					strs[i] = tmp.substring(0, optIndex);
					opts[i] = tmp.substring(optIndex + 1);
				}
			}
		}

		return opts;
	}
}
