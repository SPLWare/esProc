package com.scudata.common;

import java.awt.FontMetrics;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Vector;

import com.scudata.ide.common.GV;
import com.scudata.ide.spl.SPL;

/**
 * 字符串工具类
 * @author RunQian
 *
 */
public class StringUtils {
	private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

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
	 * @param index int,要转换的数字,从1开始,即1列为A
	 * @return String,转换后的列标签,注意Excel标签不是26进制,26进制的Z后应该为BA;但Excel标签Z后为AA
	 */
	public static String toExcelLabel(int index) {
		return toExcel(index - 1);
	}

	/**
	 * 返回字符串是否是空白符
	 * @param s 字符串
	 * @return true：是，false：不是
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
	 * @param l 长整数
	 * @param byteNum 长整数中的低字节数(即从右往左的字节个数)
	 * @return String
	 */
	public final static String toHexString(long l, int byteNum) {
		StringBuffer sb = new StringBuffer(16);
		appendHexString(sb, l, byteNum);
		return sb.toString();
	}

	/**
	 * 将一个长整数中的指定字节数转化成16进制串，并增加到字符串缓冲区中
	 * @param sb 字符串缓冲区
	 * @param l 长整数
	 * @param byteNum 长整数中的低字节数(即从右往左的字节个数)
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
	 * @param s 需要处理的字符串
	 * @param sb 追加处理结果的缓冲区
	 * @param specialChars 需要加前导\的特别字符串
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

	/**
	 * 将字符串中unicode字符转换为&#92;uxxxx形式，并对'\\','\t','\n','\r','\f'进行
	 * 处理，还对specialChars中任何字符加前导\
	 * @param s 需要处理的字符串
	 * @param sb 追加处理结果的缓冲区
	 * @return 若sb!=null则返回sb，否则返回追加了处理结果的新StringBuffer
	 */
	public final static StringBuffer deunicode(String s, StringBuffer sb) {
		return deunicode(s, sb, null);
	}

	/**
	 * 将字符串中unicode字符转换为&#92;uxxxx形式，并对'\\','\t','\n','\r','\f'进行
	 * 处理，还对specialChars中任何字符加前导\
	 * @param s 需要处理的字符串
	 * @param specialChars 需要加前导\的特别字符串
	 * @return String
	 */
	public final static String deunicode(String s, String specialChars) {
		return deunicode(s, null, specialChars).toString();
	}

	/**
	 * 将字符串中unicode字符转换为&#92;uxxxx形式，并对'\\','\t','\n','\r','\f'进行
	 * 处理，还对specialChars中任何字符加前导\
	 * @param s 需要处理的字符串
	 * @return String
	 */
	public final static String deunicode(String s) {
		return deunicode(s, null, null).toString();
	}

	/**
	 * 获得自动换行后所形成的文本行集合
	 * @param text 文本
	 * @param fm FontMetrics
	 * @param w 宽度
	 * @return 字符串列表
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			float w) {
		w = (float) Math.ceil(w) - 1.01f;
		ArrayList<String> al = new ArrayList<String>();
		text = StringUtils.replace(text, "\\n", "\n");
		ArgumentTokenizer at = new ArgumentTokenizer(text, '\n', true, true,
				true);

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
					int cut = cutLine(tmp, c);
					al.add(tmp.substring(0, cut));
					tmp = tmp.substring(cut);
				}
			}

			al.add(tmp);
		}

		return al;
	}

	// 处理s的尾字折行，返回折行完成后首行的字符数
	private static int cutLine(String s, char c) {
		// edited by bd, 2018.4.4, 这里的换行规则比较旧了，对于判定为"一个单词"的连续英文字符数字组合，不用考虑是否避尾
		// 如果当前折行长度len已知，那就不去计算，否则计算当前折行时首行字符数
		int len = s.length() - 1;

		// 如果尾字符c已知，那就不用去计算，否则计算尾字符
		if (c == 0) {
			c = s.charAt(len);

			// 在需要在字符c前断行时，首先要考虑c是否是避首字符
			// edited by bdl, 2011.5.17, 当避首字符也是英文字符时（其实就是小数点）
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
					// edited by bdl, 20111.5.17, 如果出现过非英文的避首字符
					// 如果出现避尾字符之后，又找到英文字符，那么从该英文字符后断开就好
					loc = seek + 1;
				} else {
					// edited by bdl, 20111.5.17, 如果英文中出现避首字符
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

	// added by bdl, 2008.5.21，将字符串s从最后面一个可置于本行行末的字符的后面断开，返回断开后本行字符数
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

	// added by bdl, 2008.5.21，判断某字符通常情况下是否能作为行尾
	private static boolean canBeFoot(char c) {
		// 断行的时候，应该考虑的避尾字符
		String cannotFoot = "([{·‘“〈《「『【〔〖（．［｛￡￥";
		return cannotFoot.indexOf(c) < 0;
	}

	// added by bdl, 2008.5.21，判断某字符通常情况下是否能作为行首
	private static boolean canBeHead(char c) {
		// 断行的时候，应该考虑的避首字符
		String cannotHead = "!),.:;?]}¨·ˇˉ―‖’”…∶、。〃々〉》」』】〕〗！＂＇），．：；？］｀｜｝～￠";
		return cannotHead.indexOf(c) < 0;
	}

	private static boolean isEnglishChar(char c) {
		// return ( (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
		// (c >= '0' && c <= '9'));
		// edited by bd, 2018.4.4, 在一串字母数字构成的"超单词"中，word中是允许包含英文符号的
		// 这里的判断仿照这一规则处理，遇到空格、中文字符等才算做英文单词结束
		return (c <= '~' && c > ' ');
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

	public final static String unicode(String s) {
		return unicode(s, null).toString();
	}

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
				sb.append(cDigit[d]);
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
		return toRMB(money, true, true);
	}

	public final static String toRMB(double money, boolean abbreviate,
			boolean uppercase) {
		char[] cDigit = uppercase ? c1Digit : c2Digit;
		char[] cUnit = uppercase ? c1Unit : c2Unit;
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

	/**
	 * 区分大小写的格式匹配
	 * @param src 源串
	 * @param pos1 源串起始位置
	 * @param fmt 格式串
	 * @param pos2 格式串起始位置
	 * @return true：匹配，false：不匹配
	 */
	private final static boolean matches(String src, int pos1, String fmt,
			int pos2) {
		int len1 = src.length(), len2 = fmt.length();
		boolean any = false; // 是否有星号

		while (pos2 < len2) {
			char ch = fmt.charAt(pos2);
			if (ch == '*') {
				pos2++;
				any = true;
			} else if (ch == '?') {
				// 源串需要有任意一个字符与?匹配
				if (++pos1 > len1) {
					return false;
				}

				pos2++;
			} else if (any) {
				// \* 表示此位置需要字符'*'，而不是通配符*
				if (ch == '\\' && pos2 + 1 < len2) {
					char c = fmt.charAt(pos2 + 1);
					if (c == '*' || c == '?') {
						ch = c;
						pos2++;
					}
				}

				while (pos1 < len1) {
					// 找到首个匹配的字符
					if (src.charAt(pos1++) == ch) {
						// 判断剩下的串是否匹配，如果不匹配跳过源串一个字符与格式串重新匹配
						if (matches(src, pos1, fmt, pos2 + 1)) {
							return true;
						}
					}
				}

				return false;
			} else {
				// \* 表示此位置需要字符'*'，而不是通配符*
				if (ch == '\\' && pos2 + 1 < len2) {
					char c = fmt.charAt(pos2 + 1);
					if (c == '*' || c == '?') {
						ch = c;
						pos2++;
					}
				}

				// 源串当前字符需要与格式串当前字符匹配
				if (pos1 == len1 || src.charAt(pos1++) != ch) {
					return false;
				}

				for (++pos2; pos2 < len2;) {
					ch = fmt.charAt(pos2);
					if (ch == '*') {
						any = true;
						pos2++;
						break;
					} else if (ch == '?') {
						if (++pos1 > len1) {
							return false;
						}

						pos2++;
					} else {
						// \* 表示此位置需要字符'*'，而不是通配符*
						if (ch == '\\' && pos2 + 1 < len2) {
							char c = fmt.charAt(pos2 + 1);
							if (c == '*' || c == '?') {
								ch = c;
								pos2++;
							}
						}

						if (pos1 == len1 || src.charAt(pos1++) != ch) {
							return false;
						}

						pos2++;
					}
				}
			}
		}

		return any || pos1 == len1;
	}

	/**
	 * 不区分大小写的格式匹配
	 * @param src 源串
	 * @param pos1 源串起始位置
	 * @param fmt 格式串
	 * @param pos2 格式串起始位置
	 * @return true：匹配，false：不匹配
	 */
	private final static boolean matchesIgnoreCase(String src, int pos1,
			String fmt, int pos2) {
		int len1 = src.length(), len2 = fmt.length();
		boolean any = false; // 是否有星号

		while (pos2 < len2) {
			char ch = fmt.charAt(pos2);
			if (ch == '*') {
				pos2++;
				any = true;
			} else if (ch == '?') {
				// 源串需要有任意一个字符与?匹配
				if (++pos1 > len1) {
					return false;
				}

				pos2++;
			} else if (any) {
				// \* 表示此位置需要字符'*'，而不是通配符*
				if (ch == '\\' && pos2 + 1 < len2) {
					char c = fmt.charAt(pos2 + 1);
					if (c == '*' || c == '?') {
						ch = c;
						pos2++;
					}
				}

				while (pos1 < len1) {
					// 找到首个匹配的字符
					if (Character.toUpperCase(src.charAt(pos1++)) == Character
							.toUpperCase(ch)) {
						// 判断剩下的串是否匹配，如果不匹配跳过源串一个字符与格式串重新匹配
						if (matchesIgnoreCase(src, pos1, fmt, pos2 + 1)) {
							return true;
						}
					}
				}

				return false;
			} else {
				// \* 表示此位置需要字符'*'，而不是通配符*
				if (ch == '\\' && pos2 + 1 < len2) {
					char c = fmt.charAt(pos2 + 1);
					if (c == '*' || c == '?') {
						ch = c;
						pos2++;
					}
				}

				// 源串当前字符需要与格式串当前字符匹配
				if (pos1 == len1
						|| Character.toUpperCase(src.charAt(pos1++)) != Character
								.toUpperCase(ch)) {
					return false;
				}

				for (++pos2; pos2 < len2;) {
					ch = fmt.charAt(pos2);
					if (ch == '*') {
						any = true;
						pos2++;
						break;
					} else if (ch == '?') {
						if (++pos1 > len1) {
							return false;
						}

						pos2++;
					} else {
						// \* 表示此位置需要字符'*'，而不是通配符*
						if (ch == '\\' && pos2 + 1 < len2) {
							char c = fmt.charAt(pos2 + 1);
							if (c == '*' || c == '?') {
								ch = c;
								pos2++;
							}
						}

						if (pos1 == len1
								|| Character.toUpperCase(src.charAt(pos1++)) != Character
										.toUpperCase(ch)) {
							return false;
						}

						pos2++;
					}
				}
			}
		}

		return any || pos1 == len1;
	}

	/**
	 * 判断字符串是否具有指定的格式
	 * @param value  字符串
	 * @param fmt 格式串(*表示0个或多个字符，?表示单个字符)
	 * @param ignoreCase true：忽略大小写，false：大小写敏感
	 * @return 若value或fmt为null时返回false，若不匹配时也返回false，否则返回true
	 */
	public final static boolean matches(String value, String fmt,
			boolean ignoreCase) {
		if (value == null || fmt == null) {
			return false;
		}

		if (ignoreCase) {
			return matchesIgnoreCase(value, 0, fmt, 0);
		} else {
			return matches(value, 0, fmt, 0);
		}
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

	public static String replace(String src, String findString,
			String replaceString) {
		if (src == null || findString == null) {
			return src;
		}

		int len = src.length();
		if (len == 0) {
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
		} else {
			return src;
		}
	}

	/**
	 * 是否有效的可见字符串
	 *
	 * @param str
	 *            Object，要判断的字符串对象
	 * @return boolean，有效返回true，否则返回false
	 */
	public static boolean isValidString(Object str) {
		if (!(str instanceof String)) {
			return false;
		}
		return !isSpaceString((String) str);
	}

	/**
	 * 判断str是否一个符合编辑规范的表达式，只检查了需要配对的引号，括号。这些配对后，不会造成保存的表达式串出现配对混乱
	 * 暂且没有做计算规范检查。 比如  123)  编辑规范合法，但是计算非法。
	 * @param str
	 * @return
	 */
	public static boolean isValidExpression(String str) {
		if (!isValidString(str)) {
			return true;
		}

		int len = str.length();
		int index = 0;
		while (index < len) {
			char ch = str.charAt(index);
			if (ch == '\\') {
				index += 2;
			} else if (ch == '\"' || ch == '\'') {
				int tmp = Sentence.scanQuotation(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '(') {
				int tmp = Sentence.scanParenthesis(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '[') {
				int tmp = Sentence.scanBracket(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else if (ch == '{') {
				int tmp = Sentence.scanBrace(str, index);
				if (tmp < 0) {
					return false;
				} else {
					index = tmp + 1;
				}
			} else {
				index++;
			}
		}
		return true;
	}

	/**
	 * 找出compare在keys中的位置
	 *
	 * @param keys
	 *            String[]
	 * @param compare
	 *            String
	 * @return int
	 */
	public static int indexOf(String[] keys, String compare) {
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].equals(compare)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 将逗号分开的每一个元素添加到Vector,空串设置为null对象
	 *
	 * @param str
	 *            String,要转换的字符串
	 * @return Vector,返回相应的Vector,Vector可能Size为0,但该对象不可能为null
	 */
	public static Vector string2Vector(String str) {
		return string2Vector(str, ',', false);
	}

	/**
	 * 将delim分开的每一个元素添加到Vector,空串设置为null对象
	 *
	 * @param str
	 *            String,要转换的字符串
	 * @param delim
	 *            char,分割符号
	 * @return Vector,返回相应的Vector,Vector可能Size为0,但该对象不可能为null
	 */
	public static Vector string2Vector(String str, char delim) {
		return string2Vector(str, delim, false);
	}

	/**
	 * 将delim分开的每一个元素添加到Vector,空串设置为null对象
	 *
	 * @param str
	 *            String,要转换的字符串
	 * @param delim
	 *            char,分割符号
	 * @param removeEsc
	 *            boolean,是否替换每一节的转意
	 * @return Vector,返回相应的Vector,Vector可能Size为0,但该对象不可能为null
	 */
	public static Vector string2Vector(String str, char delim, boolean removeEsc) {
		Vector v = new Vector();
		if (str == null) {
			return v;
		}
		ArgumentTokenizer st = new ArgumentTokenizer(str, delim);
		String s;
		while (st.hasMoreTokens()) {
			s = st.nextToken();
			if (removeEsc) {
				s = Escape.removeEscAndQuote(s);
			}
			if (!isValidString(s)) {
				s = null;
			}
			v.add(s);
		}
		return v;
	}

	/**
	 * 将对象列表转为字符串数组
	 *
	 * @param list List
	 * @return String[]
	 */
	// 引用到util目录,没有人引用先注释掉,wunan
	// 改用简单toString
	public static String[] toStringArray(ArrayList list) {
		int c = list.size();
		if (c == 0)
			return null;
		String[] array = new String[c];
		for (int i = 0; i < c; i++) {
			Object o = list.get(i);
			if (o != null) {
				array[i] = o.toString();
			}
		}
		return array;
	}

	/**
	 * 检查当前机器是否在指定的hosts范围
	 * @param hosts，指定多个机器名时，用英文逗号分开。hosts为空时，不检查，直接返回true
	 * @return
	 */
	public static boolean checkHosts(String hosts) {
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}

		if (!isValidString(hosts)) {
			return true;
		}
		String thisIp = host;
		boolean found = false;
		ArgumentTokenizer at = new ArgumentTokenizer(hosts, ',');
		while (at.hasMoreTokens()) {
			String tmpIp = at.nextToken().trim();
			if (thisIp.equals(tmpIp)) {
				found = true;
				break;
			}
		}
		return found;
	}

	/**
	 * 针对字号的比例放大，背景图用到水印文字时，也需要放大相应字体
	 * @param fontSize
	 * @param scale
	 * @return
	 */
	public static int getScaledFontSize(int fontSize, float scale) {
		int size = fontSize;
		// 由于字体放大后，只取整数部分，会造成部分接近Ceiling的一些字号会偏小，所以加上0.3以后，再取整
		return (int) (size * scale + 0.3f);
	}

	/**
	 * 查找子串的位置，忽略大小写
	 * @param source 源串
	 * @param target 目标子串
	 * @param fromIndex 源串的起始查找位置
	 * @return 位置，找不到返回-1
	 */
	public static int indexOfIgnoreCase(String source, String target,
			int fromIndex) {
		int sourceCount = source.length();
		int targetCount = target.length();
		if (fromIndex >= sourceCount) {
			return (targetCount == 0 ? sourceCount : -1);
		}

		if (fromIndex < 0) {
			fromIndex = 0;
		}

		if (targetCount == 0) {
			return fromIndex;
		}

		char first = target.charAt(0);
		char upFirst = Character.toUpperCase(first);
		int max = sourceCount - targetCount;

		Next: for (int i = fromIndex; i <= max; ++i) {
			// Look for first character.
			while (source.charAt(i) != first
					&& Character.toUpperCase(source.charAt(i)) != upFirst) {
				if (++i > max) {
					return -1;
				}
			}

			// Found first character, now look at the rest of v2
			for (int j = i + 1, k = 1; k < targetCount; ++j, ++k) {
				if (source.charAt(j) != target.charAt(k)
						&& Character.toUpperCase(source.charAt(j)) != Character
								.toUpperCase(target.charAt(k))) {
					continue Next;
				}
			}

			// Found whole string.
			return i;
		}

		return -1;
	}

	/**
	 *   用名字前缀pre产生一个在已知名字范围existsNames内的唯一新名字
	 * @param pre 名字前缀
	 * @param existsNames 已知的名字范围
	 * @return 已知范围内唯一的新名字
	 */
	public static String getNewName(String pre,String[] existsNames) {
		ArrayList<String> names = new ArrayList<String>();
		if(existsNames!=null) {
			int size = existsNames.length;
			for(int i=0;i<size; i++) {
				names.add( existsNames[i]);
			}
		}
		return getNewName( pre, names);
	}
	/**
	 * 用前缀pre，根据已有的names，产生一个新的不重复的名字
	 * @param pre 前缀
	 * @param names 已有的名称列表
	 * @return
	 */
	public static String getNewName(String pre, ArrayList<String> names) {
		if (names == null) {
			names = new ArrayList<String>();
		}
		if (!names.contains(pre)) {
			return pre;
		}
		int index = 1;
		while (names.contains(pre + "_" + index)) {
			index++;
		}
		return pre + "_" + index;
	}
}
