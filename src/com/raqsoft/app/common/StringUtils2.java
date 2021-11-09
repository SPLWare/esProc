package com.raqsoft.app.common;

import java.awt.FontMetrics;
import java.util.ArrayList;

import com.raqsoft.common.ArgumentTokenizer;
import com.raqsoft.common.StringUtils;

/**
 * Tool class for processing String. Solve some functions that are not
 * implemented in StringUtils.
 */
public class StringUtils2 {

	/**
	 * Do the display line break of the expression. Only the width is
	 * considered, and the input characters such as \r\n\t are not processed.
	 * But the carriage return and line feed are handled.
	 * 
	 * @param text
	 *            Text to wrap
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            Line width
	 * @param maxRowCount
	 *            Maximum number of rows
	 * @return
	 */
	public static ArrayList<String> wrapExpString(String text, FontMetrics fm,
			float w, boolean wrapChar, int maxRowCount) {
		ArrayList<String> al = new ArrayList<String>();
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
				int wid = fm.stringWidth(tmp);
				if (wid > w) {
					int cut = cutLine(tmp, c, wrapChar);
					al.add(tmp.substring(0, cut));
					if (maxRowCount > 0 && al.size() > maxRowCount) {
						return al;
					}
					tmp = tmp.substring(cut);
				}
			}
			al.add(tmp);
		}
		return al;
	}

	/**
	 * Wrap the string
	 * 
	 * @param text
	 *            Text to wrap
	 * @param fm
	 *            FontMetrics
	 * @param w
	 *            Line width
	 * @param wrapChar
	 *            Newline symbol
	 * @param maxRowCount
	 *            Maximum number of rows
	 * @return
	 */
	public static ArrayList<String> wrapString(String text, FontMetrics fm,
			float w, boolean wrapChar, int maxRowCount) {
		ArrayList<String> al = new ArrayList<String>();
		text = replace(text, "\\n", "\n");
		text = StringUtils.replace(text, "\\r", "\r");
		text = StringUtils.replace(text, "\r\n", "\n");
		text = StringUtils.replace(text, "\r", "\n");
		/* The new construction method in Argumenttokenizer is used here. */
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
				int wid = fm.stringWidth(tmp);
				if (wid > w) {
					/*
					 * It cannot be judged by the character width alone. Also
					 * need to consider the rules of line breaks such as kinsoku
					 * and other lines.
					 */
					int cut = cutLine(tmp, c, wrapChar);
					al.add(tmp.substring(0, cut));
					if (maxRowCount > 0 && al.size() > maxRowCount) {
						return al;
					}
					tmp = tmp.substring(cut);
				}
			}
			al.add(tmp);
		}
		return al;
	}

	/**
	 * Replace the string
	 * 
	 * @param src
	 *            Source string
	 * @param findString
	 *            The string to replace
	 * @param replaceString
	 *            The string to replace with
	 * @return
	 */
	private static String replace(String src, String findString,
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

	/**
	 * Split line
	 * 
	 * @param s
	 * @param c
	 * @param wrapChar
	 * @return
	 */
	private static int cutLine(String s, char c, boolean wrapChar) {
		/*
		 * If the current wrap length len is known, it will not be calculated.
		 * Otherwise, calculate the number of characters in the first line of
		 * the current line break.
		 */
		int len = s.length() - 1;
		if (wrapChar) {
			return len;
		}

		/*
		 * If the trailing character c is known, there is no need to calculate
		 * it. Otherwise, the last character is calculated.
		 */
		if (c == 0) {
			c = s.charAt(len);
		}
		boolean canBeHead = canBeHead(c);
		boolean isEnglishChar = isEnglishChar(c);
		if (!canBeHead && isEnglishChar) {
			/*
			 * Since consecutive English characters need to be treated as a word
			 * to wrap together. Therefore, it is necessary to determine whether
			 * the line is all continuous English characters.
			 */
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
							/*
							 * If non-avoidance characters appear in this line
							 * (excluding the first word). Then set the first
							 * word to true.
							 */
							hasHead = true;
						}
						seek--;
					} else {
						/*
						 * If non-English characters appear in this line. Then
						 * determine whether the character is a kinsoku
						 * character.
						 */
						if (canBeFoot(seekChar)) {
							/*
							 * If it is a non-kinsoku character, just wrap the
							 * line after this character.
							 */
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
					/*
					 * If an English character is found after the kinsoku
					 * character appears, it is good to disconnect from the
					 * English character.
					 */
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
				/* If there are non-English characters in this line */
				return loc;
			} else {
				/*
				 * If the line is all English characters or consecutive kinsoku
				 * characters, then it is divided normally.
				 */
				return len;
			}
		} else if (!canBeHead) {
			/* If c is to avoid the first character. */
			int seek = len - 1;
			int loc = 0;
			boolean hasHead = false;
			/* Find the first non-avoidance character */
			while (seek >= 0 && loc == 0) {
				char seekChar = s.charAt(seek);
				if (!hasHead) {
					if (canBeHead(seekChar)) {
						/*
						 * If non-avoidance characters appear in this line
						 * (excluding the first word), then set the first word
						 * to true.
						 */
						hasHead = true;
					}
					seek--;
				} else {
					if (isEnglishChar(seekChar)) {
						/*
						 * For alphanumeric characters, search forward to the
						 * first non-consecutive alphanumeric character.
						 */
						int eseek = seek;
						boolean eng = true;
						while (eng && seek > 0) {
							seek--;
							eng = isEnglishChar(s.charAt(seek));
						}
						/*
						 * If there are consecutive English characters from the
						 * current character to the first, then the line breaks
						 * before the last alphanumeric character.
						 */
						if (seek == 0) {
							loc = eseek + 1;
						}
					}
					/*
					 * If there is an initial character, then judge whether to
					 * kinsoku.
					 */
					else if (canBeFoot(seekChar)) {
						/*
						 * If there is no kinsoku, the line breaks after the
						 * character.
						 */
						loc = seek + 1;
					} else {
						/* Need to be kinked */
						seek--;
					}
				}
			}
			if (loc > 0) {
				/* If the line can be broken normally in this line */
				return loc;
			} else {
				/*
				 * If all the characters in this line are slashing or
				 * consecutive slashing characters. Then split normally.
				 */
				return len;
			}
		}
		/* Determine whether c is an English character */
		else if (isEnglishChar) {
			/*
			 * Since consecutive English characters need to be treated as a word
			 * to wrap together. Therefore, it is necessary to determine whether
			 * the line is all continuous English characters.
			 */
			int seek = len - 1;
			int loc = 0;
			boolean hasHead = canBeHead(c);
			boolean letterbreak = false;
			while (seek >= 0 && loc == 0) {
				char seekChar = s.charAt(seek);
				if (!isEnglishChar(seekChar)) {
					/*
					 * When wrapping lines, first judge whether the current
					 * first character is avoiding the first
					 */
					letterbreak = true;
					if (!hasHead) {
						if (canBeHead(seekChar)) {
							hasHead = true;
						}
						seek--;
					}
					/*
					 * If a non-English character appears in this line, then
					 * determine whether the character is a kinsoku character.
					 */
					else if (canBeFoot(seekChar)) {
						/*
						 * If it is a non-kinsoku character, then wrap the line
						 * after this character.
						 */
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
					/*
					 * If an English character is found after the kinsoku
					 * character appears, it will be disconnected from the
					 * English character.
					 */
					loc = seek + 1;
				} else {
					/* If the first character is avoided in English */
					if (canBeHead(seekChar)) {
						hasHead = true;
					} else {
						hasHead = false;
					}
					seek--;
				}
			}
			if (loc > 0) {
				/* If there are non-English characters in this line. */
				return loc;
			} else {
				/*
				 * If the line is all English characters or consecutive kinsoku
				 * characters, then it is divided normally.
				 */
				return len;
			}
		}
		return seekCanBeFoot(s.substring(0, len), len);
	}

	/**
	 * Break the string from the last character that can be placed at the end of
	 * the line. Returns the number of characters in this line after
	 * disconnection.
	 * 
	 * @param s
	 * @param len
	 * @return
	 */
	private static int seekCanBeFoot(String s, int len) {
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
		return len;
	}

	/**
	 * Can a character be used as the end of a line
	 * 
	 * @param c
	 * @return
	 */
	private static boolean canBeFoot(char c) {
		String cannotFoot = "([{¡¤¡®¡°¡´¡¶¡¸¡º¡¾¡²¡¼£¨£®£Û£û¡ê£¤";
		return cannotFoot.indexOf(c) < 0;
	}

	/**
	 * Can a character be the beginning of a line
	 * 
	 * @param c
	 * @return
	 */
	private static boolean canBeHead(char c) {
		String cannotHead = "%£¥!),.:;?]}¡§¡¤¡¦¡¥¨D¡¬¡¯¡±¡­¡Ã¡¢¡£¡¨¡©¡µ¡·¡¹¡»¡¿¡³¡½£¡£¢£§£©£¬£®£º£»£¿£Ý£à£ü£ý¡«¡é";
		return cannotHead.indexOf(c) < 0;
	}

	/**
	 * Whether English character
	 * 
	 * @param c
	 * @return
	 */
	private static boolean isEnglishChar(char c) {
		return (c <= '~' && c > ' ');
	}

	/**
	 * Get the height of the text
	 * 
	 * @param fm
	 * @return
	 */
	public static int getTextRowHeight(FontMetrics fm) {
		int tmpH = (int) Math.ceil(fm.getFont().getSize() * 1.28);
		int textH = fm.getHeight();
		if (tmpH < textH) {
			return textH;
		}
		int dh = tmpH - textH;
		if (dh % 2 == 0) {
			/*
			 * In order to ensure that the height of the text area is always
			 * centered on the line height. Ensure that dh is always an even
			 * number.
			 */
			return tmpH;
		}
		return tmpH + 1;
	}

}
