package com.scudata.cellset.graph.draw;

import java.util.*;

import com.scudata.common.ArgumentTokenizer;

import java.awt.FontMetrics;
import java.lang.Character;

/**
 * 为了不引用上层类，该类全盘复制StringUtils2，供DrawBase画数据表使用
 * @author Joancy
 *
 */
public class DrawStringUtils2 {
	private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * 判断s是否为空串
	 * @param s 字符串
	 * @return 如果是返回true，否则返回false
	 */
	public final static boolean isSpaceString(String s) {
		if (s == null) {
			return true;
		}
		for (int i = 0, len = s.length(); i < len; i++) {
			char c = s.charAt(i);
			if (!Character.isWhitespace(c)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 将一个长整数中的指定字节数转化成16进制串
	 * 
	 * @param l
	 *            长整数
	 * @param byteNum
	 *            长整数中的低字节数(即从右往左的字节个数)
	 */
	public final static String toHexString(long l, int byteNum) {
		StringBuffer sb = new StringBuffer(16);
		appendHexString(sb, l, byteNum);
		return sb.toString();
	}

	/**
	 * 将一个长整数中的指定字节数转化成16进制串，并增加到字符串缓冲区中
	 * 
	 * @param sb
	 *            字符串缓冲区
	 * @param l
	 *            长整数
	 * @param byteNum
	 *            长整数中的低字节数(即从右往左的字节个数)
	 */
	public final static void appendHexString(StringBuffer sb, long l,
			int byteNum) {
		for (int i = byteNum * 2 - 1; i >= 0; i--) {
			long x = (l >> (i * 4)) & 0xf;
			sb.append(hexDigits[(int) x]);
		}
	}

	/**
	 * 将字符串中unicode字符转换为&#92;uxxxx形式，并对'\\','\t','\n','\r','\f'进行
	 * 处理，还对specialChars中任何字符加前导\
	 * 
	 * @params s 需要处理的字符串
	 * @params sb 追加处理结果的缓冲区
	 * @params specialChars 需要加前导\的特别字符串
	 * @return 若sb!=null则返回sb，否则返回追加了处理结果的新StringBuffer
	 */
	public final static StringBuffer deunicode(String s, StringBuffer sb,
			String specialChars) {
		int len = s.length();
		if (sb == null) {
			sb = new StringBuffer(len * 2);

		}
		for (int i = 0; i < len; i++) {
			char ch = s.charAt(i);
			switch (ch) {
			case '\\':
				sb.append('\\').append('\\');
				break;
			case '\t':
				sb.append('\\').append('t');
				break;
			case '\n':
				sb.append('\\').append('n');
				break;
			case '\r':
				sb.append('\\').append('r');
				break;
			case '\f':
				sb.append('\\').append('f');
				break;
			default:
				if ((ch < 0x0020) || (ch > 0x007e)) {
					sb.append('\\').append('u');
					sb.append(hexDigits[(ch >> 12) & 0xF]);
					sb.append(hexDigits[(ch >> 8) & 0xF]);
					sb.append(hexDigits[(ch >> 4) & 0xF]);
					sb.append(hexDigits[ch & 0xF]);
				} else {
					if (specialChars != null && specialChars.indexOf(ch) != -1) {
						sb.append('\\');
					}
					sb.append(ch);
				}
			}
		}
		return sb;
	}

	public final static StringBuffer deunicode(String s, StringBuffer sb) {
		return deunicode(s, sb, null);
	}

	public final static String deunicode(String s, String specialChars) {
		return deunicode(s, null, specialChars).toString();
	}

	public final static String deunicode(String s) {
		return deunicode(s, null, null).toString();
	}

	/**
	 * 不用考虑插空格，将给定文本按照宽度w截断为多行文本
	 * @param text 文本串
	 * @param fm 字体信息
	 * @param w 宽度
	 * @return 截断后的多行文本
	 */
	public static ArrayList<String> wrapString2(String text, FontMetrics fm, float w) {
		ArrayList<String> al = wrapString(text, fm, w, false);
		if (al == null) {
			al = wrapString(text, fm, w, true);
		}
		return al;
	}

	/**
	 * 根据指定参数将文本截断为多行文本
	 * @param text 文本
	 * @param fm 字体
	 * @param w 宽度
	 * @param wrapChar 能否截断单词
	 * @param align 对齐方式
	 * @return 截断后的多行文本
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm, float w,
			boolean wrapChar, byte align) {
		ArrayList<String> al = null;
		if (wrapChar == false) {
			al = wrapString(text, fm, w, false);
		}
		if (al == null) {
			al = wrapString(text, fm, w, true);
		}

		return al;
	}

	private static String rightTrim(String str) {
		while (str.lastIndexOf(" ") == str.length() - 1) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}


	/**
	 * 将多行分文按照宽度分散对齐
	 * @param al 多行文本
	 * @param fm 字体信息
	 * @param w 宽度
	 */
	public static void scatter(ArrayList al, FontMetrics fm, float w) {
		String line = rightTrim(String.valueOf((String) al.get(al.size() - 1)));
		String newLine = scatterLine(line,fm,w);
		al.set(al.size() - 1, newLine);
	}
	
	/**
	 * 将一行文本分散对齐
	 * @param src 源文本串
	 * @param fm 字体信息
	 * @param w 宽度
	 * @return 用空格分散对齐两端后的字符串
	 */
	public static String scatterLine(String src, FontMetrics fm, float w) {
		String line = src;
		if (fm.stringWidth(line) >= w) return src;
			String leftSpace = ""; // 记录开头的空格个数
			while (line.indexOf(" ") == 0) {
				line = line.substring(1, line.length());
				leftSpace += " ";
			}

			int numLongspace = 0; // 字符串分几段
			int widthChars = 0; // 字符的总宽度
			StringBuffer sb = new StringBuffer();
			StringBuffer sbchars = new StringBuffer();
			char[] cline = line.toCharArray();
			boolean flag = true;
			for (int i = 0; i < cline.length; i++) {
				char c = cline[i];
				if (flag && Character.isSpaceChar(c)) {
					flag = false;
					numLongspace++;
				}
				if (!Character.isSpaceChar(c)) {
					flag = true;
					sbchars.append(c);
				}
				sb.append(c);
			}
			widthChars = fm.stringWidth(sbchars.toString());
			boolean iss = (widthChars + numLongspace * 2) < (int) (w / 2);

			int index = 0;
			flag = true;
			boolean isInsert = false;
			while (true) { // c
				if (fm.stringWidth(leftSpace + sb.toString() + " ") > w) {
					break;
				}

				char[] nchars = sb.toString().toCharArray();
				char c = nchars[index];
				if (flag && Character.isSpaceChar(c)) {
					flag = false;
					sb.insert(index + 1, ' ');
					index++;
				}
				if (!Character.isSpaceChar(c)) {
					flag = true;
					if (numLongspace == 0 || iss) {
						sb.insert(index + 1, ' ');
						index++;
					}
				}

				index++;
				if (index > sb.length() - 2) {
					index = 0;
				}
			}
			return leftSpace + sb.toString();

	}

/**
 * 往源串中填充空格
 * @param src 源字符串
 * @param fm 字体信息
 * @param w 宽度
 * @return 处理后的字符串
 */
	public static String fitSpaces(String src, FontMetrics fm, float w) {
		String line = rightTrim(src);
		if (fm.stringWidth(line) >= w)
			return src;
		String myLine = String.valueOf(line);
		int scount = 0; // 需要插入的空格数
		while (true) {
			myLine += " ";
			if (fm.stringWidth(myLine) > w) {
				break;
			}
			scount++;
		}
		if (scount <= 0)
			return src;
		char[] cline = line.toCharArray();
		int len = cline.length;
		StringBuffer sb = new StringBuffer();
		for (int c = 0; c < len; c++) {
			sb.append(cline[c]);
		}
		int index = 0;
		boolean flag = false; // 非空格开始的标志
		boolean haspace = false; // 整行中是否存在空格
		while (true) {
			char c = sb.charAt(index);
			if (!Character.isSpaceChar(c)) {
				flag = true;
			}
			if (flag && index < sb.length() - 1) {
				char c1 = sb.charAt(index + 1);
				// 空格和非空格处添加空格
				if (Character.isSpaceChar(c) && !Character.isSpaceChar(c1)) {
					haspace = true;
					sb.insert(index, ' ');
					index++; // 填入空格编号前进一个
					scount--;
					if (scount == 0) {
						break; // 空格填完了退出
					}
				}
			}
			index++;
			if (index > sb.length() - 1) {
				if (!flag) {
					break; // 字符串中不存在非空格退出
				}
				if (!haspace) {
					break; // 字符串中间中不存在空格退出
				}
				index = 0; // 字符到结尾后归零重新计数
			}
		}
		return sb.toString();
	}

	/**
	 * 获得自动换行后所形成的文本行集合 
	 */
	public static ArrayList wrapString(String text, FontMetrics fm, float w) {
		return wrapString(text, fm, w, false);
	}

	/** 获得自动换行后所形成的文本行集合 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm, float w,
			boolean wrapChar) {
		ArrayList<String> al = new ArrayList<String>();
		text = replace(text, "\\n", "\n");
		text = replace(text, "\\r", "\r");
		text = replace(text, "\r\n", "\n");
		text = replace(text, "\r", "\n");
		// 在这里使用Argumenttokenizer中新的构建方法
		ArgumentTokenizer at = new ArgumentTokenizer(text, '\n', true, true,
				true, true);
		while (at.hasNext()) {
			String line = at.next();
			if (at.hasNext()) {
				line += "\n";
			}
			int len = line.length();
			String tmp = "";
			for (int i = 0; i < len; i++) {
				char c = line.charAt(i);
				tmp += String.valueOf(c);
				if (fm.stringWidth(tmp) > w) {
					int cut = cutLine(tmp, c, wrapChar);
					al.add(tmp.substring(0, cut));
					tmp = tmp.substring(cut);
				}
			}
			al.add(tmp);
		}
		return al;
	}

	private static int cutLine(String s, char c, boolean wrapChar) {
		// 如果当前折行长度len已知，那就不去计算，否则计算当前折行时首行字符数
		int len = s.length() - 1;
		if (wrapChar) {
			return len;
		}

		// 如果尾字符c已知，那就不用去计算，否则计算尾字符
		if (c == 0) {
			c = s.charAt(len);
		}
		boolean canBeHead = canBeHead(c);
		boolean isEnglishChar = isEnglishChar(c);
		if (!canBeHead && isEnglishChar) {
			// 由于连续的英文字符需要视为一个单词共同换行，所以需要判断是否本行全部为连续英文字符
			int seek = len - 1;
			int loc = 0;
			boolean hasHead = canBeHead(c);
			boolean letterbreak = false;
			while (seek >= 0 && loc == 0) {
				char seekChar = s.charAt(seek);
				if (!isEnglishChar(seekChar)) {
					letterbreak = true;
					if (!hasHead) {
						if (canBeHead(seekChar)) {
							// 如果本行中出现非避首字符（排除首字），那么设定有首字为true
							hasHead = true;
						}
						seek--;
					} else {
						// 如果本行中出现非英文字符，那么判断该字符是否是避尾字符
						if (canBeFoot(seekChar)) {
							// 如果是非避尾字符，那么从这个字符后折行即可
							loc = seek + 1;
						} else {
							if (canBeHead(seekChar)) {
								hasHead = true;
							} else {
								hasHead = false;
							}
							seek--;
						}
					}
				} else if (letterbreak) {
					// 如果出现避尾字符之后，又找到英文字符，那么从该英文字符后断开就好
					loc = seek + 1;
				} else {
					if (canBeHead(seekChar)) {
						hasHead = true;
					} else {
						hasHead = false;
					}
					seek--;
				}
			}
			if (loc > 0) {
				// 如果本行中有非英文字符
				return loc;
			} else {
				// 如果本行中全都是英文字符或者是连续的避尾字符，那么正常切分
				return len;
			}
		} else if (!canBeHead) {
			// c是避首字符
			int seek = len - 1;
			int loc = 0;
			boolean hasHead = false;
			// 找到第一个非避首字符
			while (seek >= 0 && loc == 0) {
				char seekChar = s.charAt(seek);
				if (!hasHead) {
					if (canBeHead(seekChar)) {
						// 如果本行中出现非避首字符（排除首字），那么设定有首字为true
						hasHead = true;
					}
					seek--;
				} else {
					if (isEnglishChar(seekChar)) {
						// 对于英数字符，向前查询到第一个非连续英数字符为止
						int eseek = seek;
						boolean eng = true;
						while (eng && seek > 0) {
							seek--;
							eng = isEnglishChar(s.charAt(seek));
						}
						// added by bdl, 2011.8.12, 如果从当前字符直到第一个都是连续的英文字符，
						// 那么从最后一个英数字符前断行
						if (seek == 0) {
							loc = eseek + 1;
						}
					}
					// 如果已经有首字，那么判断是否要避尾
					else if (canBeFoot(seekChar)) {
						// 如果不避尾，那么在该字符后断行即可
						loc = seek + 1;
					} else {
						// 需要避尾
						seek--;
					}
				}
			}
			if (loc > 0) {
				// 如果本行中可正常断行
				return loc;
			} else {
				// 如果本行中全都是避首或者是连续的避尾字符，那么正常切分
				return len;
			}
		}
		// 然后再判断c是否是英文字符
		else if (isEnglishChar) {
			// 由于连续的英文字符需要视为一个单词共同换行，所以需要判断是否本行全部为连续英文字符
			int seek = len - 1;
			int loc = 0;
			boolean hasHead = canBeHead(c);
			boolean letterbreak = false;
			while (seek >= 0 && loc == 0) {
				char seekChar = s.charAt(seek);
				if (!isEnglishChar(seekChar)) {
					// edited by bdl, 20111.5.17, 折行时首先判断当前首字符是否避首
					letterbreak = true;
					if (!hasHead) {
						if (canBeHead(seekChar)) {
							hasHead = true;
						}
						seek--;
					}
					// 如果本行中出现非英文字符，那么判断该字符是否是避尾字符
					else if (canBeFoot(seekChar)) {
						// 如果是非避尾字符，那么从这个字符后折行即可
						loc = seek + 1;
					} else {
						if (canBeHead(seekChar)) {
							hasHead = true;
						} else {
							hasHead = false;
						}
						seek--;
					}
				} else if (letterbreak) {
					// 如果出现避尾字符之后，又找到英文字符，那么从该英文字符后断开就好
					loc = seek + 1;
				} else {
					if (canBeHead(seekChar)) {
						hasHead = true;
					} else {
						hasHead = false;
					}
					seek--;
				}
			}
			if (loc > 0) {
				// 如果本行中有非英文字符
				return loc;
			} else {
				// 如果本行中全都是英文字符或者是连续的避尾字符，那么正常切分
				return len;
			}
		}
		return seekCanBeFoot(s.substring(0, len), len);
	}

	//将字符串s从最后面一个可置于本行行末的字符的后面断开，返回断开后本行字符数
	private static int seekCanBeFoot(String s, int len) {
		// 如果当前折行长度len已知，那就不去计算，否则计算当前折行时首行字符数
		if (len == -1) {
			len = s.length();
		}
		if (len <= 1) {
			return len;
		}

		int seek = len - 1;
		int loc = 0;
		while (seek >= 0 && loc == 0) {
			char seekChar = s.charAt(seek);
			if (canBeFoot(seekChar)) {
				loc = seek + 1;
			} else {
				seek--;
			}
		}
		if (loc > 0) {
			return loc;
		}
		// 如果s中所有字符都是避尾字符，那么整行保留
		return len;
	}

	//判断某字符通常情况下是否能作为行尾
	private static boolean canBeFoot(char c) {
		// 断行的时候，应该考虑的避尾字符
		String cannotFoot = "([{·‘“〈《「『【〔〖（．［｛￡￥";
		return cannotFoot.indexOf(c) < 0;
	}

	//判断某字符通常情况下是否能作为行首
	private static boolean canBeHead(char c) {
		// 断行的时候，应该考虑的避首字符, edited by bdl, 2011.5.17，添加百分号避首
		String cannotHead = "%％!),.:;?]}¨·ˇˉ―‖’”…∶、。〃々〉》」』】〕〗！＂＇），．：；？］｀｜｝～￠";
		return cannotHead.indexOf(c) < 0;
	}

	private static boolean isEnglishChar(char c) {
		//在字母数字判定中，添加小数点
		return ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
				|| (c >= '0' && c <= '9') || c == '.' || c == '．' || c == '%' || c == '％');
	}

	/**
	 * 将&#92;uxxxx转换为unicode字符，并对'\\','\t','\n','\r','\f'进行处理
	 * 
	 * @params s 需要处理的字符串
	 * @params sb 追加处理结果的缓冲区
	 * @return 若sb!=null则返回sb，否则返回追加了处理结果的新StringBuffer
	 */
	public final static StringBuffer unicode(String s, StringBuffer sb) {
		int len = s.length();
		if (sb == null) {
			sb = new StringBuffer(len);

		}
		char ch;
		for (int i = 0; i < len;) {
			ch = s.charAt(i++);
			if (ch != '\\') {
				sb.append(ch);
				continue;
			}
			ch = s.charAt(i++);
			if (ch == 'u') {
				// Read the xxxx
				int value = 0;
				for (int j = 0; j < 4; j++) {
					ch = s.charAt(i++);
					switch (ch) {
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						value = (value << 4) + ch - '0';
						break;
					case 'a':
					case 'b':
					case 'c':
					case 'd':
					case 'e':
					case 'f':
						value = (value << 4) + 10 + ch - 'a';
						break;
					case 'A':
					case 'B':
					case 'C':
					case 'D':
					case 'E':
					case 'F':
						value = (value << 4) + 10 + ch - 'A';
						break;
					default:
						throw new IllegalArgumentException("不合法的\\uxxxx编码");
					} // switch(ch)
				} // for(int j)
				sb.append((char) value);
			} else {
				switch (ch) {
				case 't':
					ch = '\t';
					break;
				case 'r':
					ch = '\r';
					break;
				case 'n':
					ch = '\n';
					break;
				case 'f':
					ch = '\f';
					break;
				}
				sb.append(ch);
			}
		} // for(int i)
		return sb;
	}

	/**
	 * 将字符串转换为unicode串
	 * @param s 源串
	 * @return unicode串
	 */
	public final static String unicode(String s) {
		return unicode(s, null).toString();
	}

	/**
	 * 将字符串转换为unicode串
	 * @param theString 源串
	 * @return unicode串
	 */
	public final static String unicode2String(String theString) {
		char aChar;
		int len = theString.length();
		StringBuffer outBuffer = new StringBuffer(len);
		for (int x = 0; x < len;) {
			aChar = theString.charAt(x++);
			if (aChar == '\\') {
				aChar = theString.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						aChar = theString.charAt(x++);
						switch (aChar) {
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							value = (value << 4) + aChar - '0';
							break;
						case 'a':
						case 'b':
						case 'c':
						case 'd':
						case 'e':
						case 'f':
							value = (value << 4) + 10 + aChar - 'a';
							break;
						case 'A':
						case 'B':
						case 'C':
						case 'D':
						case 'E':
						case 'F':
							value = (value << 4) + 10 + aChar - 'A';
							break;
						default:
							throw new IllegalArgumentException(
									"Malformed \\uxxxx encoding.");
						}
					}
					outBuffer.append((char) value);
				} // if(aChar)
			} else {
				outBuffer.append(aChar);
			}
		} // for(int x)
		return outBuffer.toString();
	}

	final static char[] c1Digit = { '零', '壹', '贰', '叁', '肆', '伍', '陆', '柒',
			'捌', '玖' };
	final static char[] c2Digit = { '零', '一', '二', '三', '四', '五', '六', '七',
			'八', '九' };
	final static char[] c1Unit = { '拾', '佰', '仟' };
	final static char[] c2Unit = { '十', '百', '千' };
	final static String[] chinaUnit = { "万", "亿", "亿万" };

	private final static StringBuffer toRMB2(long l, char[] cDigit, char[] cUnit) {
		long ml = l;
		int unit = 0, bit = 0, d;
		boolean hasZero = false, sf = false;
		StringBuffer sb = new StringBuffer(64);
		while (l > 0) {
			if (bit == 4) {
				if (unit > 2) {
					throw new IllegalArgumentException("大写不支持大于一万万亿的数");
				}

				if (sf) {
					if (hasZero || l % 10 == 0) {
						sb.append(cDigit[0]);
						hasZero = false;
					}
				} else {
					int len = sb.length();
					if (len > 0) {
						sb.deleteCharAt(len - 1);
					}
				}
				sb.append(chinaUnit[unit]);
				unit++;
				bit = 0;
				sf = false;
			}

			d = (int) (l % 10);
			if (d > 0) {
				sf = true;
				if (hasZero) {
					sb.append(cDigit[0]);
					hasZero = false;
				}
				if (bit != 0) {
					sb.append(cUnit[bit - 1]);
				}
				//小于100并且cUnit为十cDigit为一时，一不加入
				if (bit == 1 && d == 1 && ml < 100) {
				} else {
					sb.append(cDigit[d]);
				}
			} else {
				if (sf) { // 数据尾部的0忽略
					hasZero = true;
				}
			}

			bit++;
			l /= 10;
		}
		return sb.reverse();
	}

	/**
	 * 将浮点数格式成人民币大写方式
	 * 
	 * @param money
	 *            浮点数
	 * @return 格式化的字符串
	 * @exception IllegalArgumentException
	 *                当money<0或money>=一万万亿时
	 */
	public final static String toRMB(double money) {
		char[] cDigit = c1Digit, cUnit = c1Unit;
		StringBuffer sb = new StringBuffer(64);
		if (money < 0) {
			sb.append("负");
			money = -money;
		}
		long yuan = (long) money; // 元

		if (yuan == 0) {
			sb.append("零");
		} else {
			sb.append(toRMB2(yuan, cDigit, cUnit));
		}
		sb.append('元');

		int jaoFeng = (int) ((money + 0.001 - (long) money) * 100) % 100;
		int jao = jaoFeng / 10;
		int feng = jaoFeng % 10;
		if (jao > 0) {
			sb.append(cDigit[jao]);
			sb.append('角');
		}
		if (feng > 0) {
			if (jao == 0) {
				sb.append('零');
			}
			sb.append(cDigit[feng]);
			sb.append('分');
		} else {
			sb.append('整');
		}
		return sb.toString();
	}

	/**
	 * 将读数转换为中文写法
	 * @param l 数值
	 * @param abbreviate 缩写
	 * @param uppercase 大写
	 * @return
	 */
	public final static String toChinese(long l, boolean abbreviate,
			boolean uppercase) {

		String fu = "";
		if (l == 0) {
			return "零";
		} else if (l < 0) {
			fu = "负";
			l = -l;
		}
		char[] cDigit = uppercase ? c1Digit : c2Digit;
		char[] cUnit = uppercase ? c1Unit : c2Unit;
		if (abbreviate) {
			return fu + toRMB2(l, cDigit, cUnit).toString();
		} else {
			StringBuffer sb = new StringBuffer(64);
			for (; l > 0; l /= 10) {
				int digit = (int) l % 10;
				sb.append(cDigit[digit]);
			}
			sb = sb.reverse();
			return fu + sb.toString();
		} // if ( abbreviate )
	}

	private final static boolean matches(String value, int pos1, String fmt,
			int pos2, boolean ignoreCase) {
		if (value == null || fmt == null) {
			return false;
		}
		int len1 = value.length(), len2 = fmt.length();
		while (pos2 < len2) {
			char ch = fmt.charAt(pos2++);
			if (ch == '*') {
				if (pos1 == len1) {
					while (pos2 < len2) {
						if (fmt.charAt(pos2++) != '*') {
							return false;
						}
					}
					return true;
				}
				do {
					if (matches(value, pos1, fmt, pos2, ignoreCase)) {
						return true;
					}
				} while (pos1++ < len1);

				return false;
			}

			if (ch == '?') {
				if (pos1 == len1) {
					return false;
				}
			} else if (ch == '\\' && pos2 < len2 && fmt.charAt(pos2) == '*') {
				// \* 表示此位置需要字符'*'，而不是通配符*
				if (pos1 == len1 || value.charAt(pos1) != '*') {
					return false;
				}

				pos2++;
			} else {
				if (ignoreCase) {
					if (pos1 == len1
							|| Character.toUpperCase(ch) != Character
									.toUpperCase(value.charAt(pos1))) {
						return false;
					}
				} else {
					if (pos1 == len1 || ch != value.charAt(pos1)) {
						return false;
					}
				}
			}
			pos1++;
		}
		return pos1 == len1;
	}

	/**
	 * 判断字符串是否具有指定的格式
	 * 
	 * @param value
	 *            字符串
	 * @param fmt
	 *            格式串(*表示0个或多个字符，?表示单个字符)
	 * @param ifcase
	 *            是否大小写
	 * @return 若value或fmt为null时返回false，若不匹配时也返回false，否则返回true
	 */
	public final static boolean matches(String value, String fmt, boolean ifcase) {
		return matches(value, 0, fmt, 0, ifcase);
	}

	private final static String[] provinces = { null, null, null, null, null,
			null, null, null, null, null, null, "北京", "天津", "河北", "山西", "内蒙古",
			null, null, null, null, null, "辽宁", "吉林", "黑龙江", null, null, null,
			null, null, null, null, "上海", "江苏", "浙江", "安微", "福建", "江西", "山东",
			null, null, null, "河南", "湖北", "湖南", "广东", "广西", "海南", null, null,
			null, "重庆", "四川", "贵州", "云南", "西藏", null, null, null, null, null,
			null, "陕西", "甘肃", "青海", "宁夏", "新疆", null, null, null, null, null,
			"台湾", null, null, null, null, null, null, null, null, null, "香港",
			"澳门", null, null, null, null, null, null, null, null, "国外" };

	private final static int[] wi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10,
			5, 8, 4, 2, 1 };
	private final static char[] codes = { '1', '0', 'X', '9', '8', '7', '6',
			'5', '4', '3', '2' };

	/**
	 * 根据GB11643-1999<<公民身份号码>>及GB11643-1989<<社会保障号码>>规定检查身份证号是否符合规范
	 */
	public final static boolean identify(String ident) {
		if (ident == null) {
			return false;
		}

		int len = ident.length();
		if (len != 15 && len != 18) {
			return false;
		}

		for (int i = 0; i < ((len == 15) ? 15 : 17); i++) {
			char ch = ident.charAt(i);
			if (ch < '0' || ch > '9') {
				return false;
			}
		}

		// 检查户口所在县的行政区代码 GB/T2260
		int p = (ident.charAt(0) - '0') * 10 + (ident.charAt(1) - '0');
		if (p >= provinces.length || provinces[p] == null) {
			return false;
		}

		// 检查出生年月日 GB/T7408
		int year = 0, month = 0, day = 0;
		if (len == 15) {
			year = 1900 + (ident.charAt(6) - '0') * 10
					+ (ident.charAt(7) - '0');
			month = (ident.charAt(8) - '0') * 10 + (ident.charAt(9) - '0');
			day = (ident.charAt(10) - '0') * 10 + (ident.charAt(11) - '0');
		} else {
			year = (ident.charAt(6) - '0') * 1000 + (ident.charAt(7) - '0')
					* 100 + (ident.charAt(8) - '0') * 10
					+ (ident.charAt(9) - '0');
			month = (ident.charAt(10) - '0') * 10 + (ident.charAt(11) - '0');
			day = (ident.charAt(12) - '0') * 10 + (ident.charAt(13) - '0');
		}
		if (month == 2) {
			if ((year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0))) {
				// 闰年2月29日
				if (day > 29) {
					return false;
				}
			} else {
				if (day > 28) {
					return false;
				}
			}
		} else if (month == 4 || month == 6 || month == 9 || month == 11) {
			if (day > 30) {
				return false;
			}
		} else if (month <= 12) {
			if (day > 31) {
				return false;
			}
		} else {
			return false;
		}

		// 检查校验码
		if (len == 18) {
			int[] w = wi;
			int mod = 0;
			for (int i = 0; i < 17; i++) {
				mod += (ident.charAt(i) - '0') * w[i];
			}
			mod = mod % 11;
			if (ident.charAt(17) != codes[mod]) {
				return false;
			}
		}
		return true;
	}
/**
 * 全替换字符串
 * @param src 源串
 * @param findString 查找串
 * @param replaceString 替换串
 * @return 替换完成后的串
 */
	public static String replace(String src, String findString,
			String replaceString) {
		if (src == null) {
			return src;
		}
		int len = src.length();
		if (len == 0) {
			return src;
		}
		if (findString == null) {
			return src;
		}
		int len1 = findString.length();
		if (len1 == 0) {
			return src;
		}
		if (replaceString == null) {
			return src;
		}

		int start = 0;
		StringBuffer sb = null;
		while (true) {
			int pos = src.indexOf(findString, start);
			if (pos >= 0) {
				if (sb == null) {
					sb = new StringBuffer(len + 100);
				}
				for (int i = start; i < pos; i++) {
					sb.append(src.charAt(i));
				}
				sb.append(replaceString);
				start = pos + len1;
			} else {
				if (sb != null) {
					for (int i = start; i < len; i++) {
						sb.append(src.charAt(i));
					}
				}
				break;
			}
		}
		if (sb != null) {
			return sb.toString();
		}
		return src;
	}

	private static String[] excelLabels = { "A", "B", "C", "D", "E", "F", "G",
			"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z" };

	private static String toExcel(int index) {
		if (index < 26) {
			return excelLabels[index];
		}

		int shang = index / 26;
		int yu = index % 26;

		return toExcel(shang - 1) + excelLabels[yu];
	}

	/**
	 * 将数字转换为Excel的列标签
	 * 
	 * @param index
	 *            int,要转换的数字,从1开始,即1列为A
	 * @return String,转换后的列标签,注意Excel标签不是26进制,26进制的Z后应该为BA;但Excel标签Z后为AA
	 */
	public static String toExcelLabel(int index) {
		return toExcel(index - 1);
	}


	/**
	 * 去掉字符串中的空白字符
	 * @param s 源串
	 * @return 去掉空格的串
	 */
	public static String trimWhitespace(String s) {
		if (s == null) {
			return null;
		}
		int st = 0, len = s.length();
		while (st < len && Character.isWhitespace(s.charAt(st))) {
			st++;
		}
		while (st < len && Character.isWhitespace(s.charAt(len - 1))) {
			len--;
		}
		return ((st > 0) || (len < s.length())) ? s.substring(st, len) : s;

	}

	/**
	 * 获取当前字体的文本高度
	 * @param fm 字体信息
	 * @return 文本高度
	 */
	public static int getTextRowHeight(FontMetrics fm) {
		int tmpH = (int) Math.ceil(fm.getFont().getSize() * 1.28); // 文字的行高估算高度
		int textH = fm.getHeight(); // 文字区域高度
		if (tmpH < textH) {
			return textH;
		}
		int dh = tmpH - textH;
		if (dh % 2 == 0) { // 为了保证文字区域高度总是居中于行高，保证dh总是为偶数
			return tmpH;
		}
		return tmpH + 1;
	}
	
	
}
