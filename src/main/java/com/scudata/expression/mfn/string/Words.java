package com.scudata.expression.mfn.string;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.StringFunction;
import com.scudata.resources.EngineMessage;

/**
 * 将字符串中的英语单词拆出成字符串序列返回
 * s.words()
 * @author RunQian
 *
 */
public class Words extends StringFunction {
	public Object calculate(Context ctx) {
		if (param != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("words" + mm.getMessage("function.invalidParam"));
		}
		
		if (srcStr.length() == 0) {
			return null;
		}
		
		boolean iopt = false;
		if (option != null) {
			if (option.indexOf('i') != -1) {
				iopt = true;
			}
			
			if (option.indexOf('a') != -1) {
				return splitWordAndDigit(srcStr, iopt);
			} else if (option.indexOf('d') != -1) {
				return splitDigit(srcStr);
			} else if (option.indexOf('c') != -1) {
				return splitChinese(srcStr, iopt);
			} else if (option.indexOf('w') != -1) {
				return splitAll(srcStr, iopt, option.indexOf('p') != -1);
			}
		}
		
		return splitWords(srcStr, iopt);
	}
	
	private static boolean isWord(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}
	
	private static boolean isWord(char c, boolean iopt) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (iopt && c == '_');
	}
	
	private static boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private static boolean isChinese(char c) {
		return UCharacter.hasBinaryProperty(c, UProperty.IDEOGRAPHIC);
	}
	
	private static Sequence splitWords(String str, boolean iopt) {
		Sequence series = new Sequence();
		char []chars = str.toCharArray();
		int len = chars.length;

		if (iopt) {
			for (int i = 0; i < len;) {
				if (isWord(chars[i], true)) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end], true) && !isDigit(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end + 1;
				} else {
					++i;
				}
			}
		} else {
			for (int i = 0; i < len;) {
				if (isWord(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end + 1;
				} else {
					++i;
				}
			}
		}

		return series;
	}

	private static Sequence splitDigit(String str) {
		Sequence series = new Sequence();
		char []chars = str.toCharArray();
		int len = chars.length;

		for (int i = 0; i < len;) {
			if (isDigit(chars[i])) {
				int end = i + 1;
				for (; end < len && isDigit(chars[end]); ++end) {
				}
								
				series.add(new String(chars, i, end - i));
				i = end + 1;
			} else {
				++i;
			}
		}

		return series;
	}

	private static Sequence splitWordAndDigit(String str, boolean iopt) {
		Sequence series = new Sequence();
		char []chars = str.toCharArray();
		int len = chars.length;

		if (iopt) {
			for (int i = 0; i < len;) {
				if (isWord(chars[i], true)) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end], true) && !isDigit(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i])) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end]); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					++i;
				}
			}
		} else {
			for (int i = 0; i < len;) {
				if (isWord(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i])) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end]); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					++i;
				}
			}
		}

		return series;
	}
	
	private static Sequence splitChinese(String str, boolean iopt) {
		Sequence series = new Sequence();
		char []chars = str.toCharArray();
		int len = chars.length;

		if (iopt) {
			for (int i = 0; i < len;) {
				if (isWord(chars[i], true)) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (end + 1 < len && Character.isHighSurrogate(chars[end])) {
							++end;
						} else if (!isWord(chars[end], true) && !isDigit(chars[end]) && !isChinese(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i])) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end]); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isChinese(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (end + 1 < len && Character.isHighSurrogate(chars[end])) {
							++end;
						} else if (!isWord(chars[end], true) && !isDigit(chars[end]) && !isChinese(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (i + 1 < len && Character.isHighSurrogate(chars[i])) {
					int end = i + 2;
					for (; end < len; ++end) {
						if (end + 1 < len && Character.isHighSurrogate(chars[end])) {
							++end;
						} else if (!isWord(chars[end], true) && !isDigit(chars[end]) && !isChinese(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					++i;
				}
			}
		} else {
			for (int i = 0; i < len;) {
				if (isWord(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i])) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end]); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isChinese(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if ( end + 1 < len && Character.isHighSurrogate(chars[end])) {
							end++;
						} else if (!isChinese(chars[end])) {
							break;
						}
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (i + 1 < len && Character.isHighSurrogate(chars[i])) {
					int end = i + 2;
					for (; end < len; ++end) {
						if ( end + 1 < len && Character.isHighSurrogate(chars[end])) {
							end++;
						} else if (!isChinese(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					++i;
				}
			}
		}

		return series;
	}

	// 数字、‘.’、‘:’、'-'、'/'
	private static boolean isDigit(char c, boolean popt) {
		if (isDigit(c)) {
			return true;
		} else if (popt) {
			return c == '.' || c == ':' || c == '-' || c == '/';
		} else {
			return false;
		}
	}
	
	private static Sequence splitAll(String str, boolean iopt, boolean popt) {
		Sequence series = new Sequence();
		char []chars = str.toCharArray();
		int len = chars.length;

		if (iopt) {
			for (int i = 0; i < len;) {
				if (isWord(chars[i], true)) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end], true) && !isDigit(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i], popt)) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end], popt); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					if (i + 1< len && Character.isHighSurrogate(chars[i])) {
						series.add(new String(chars, i, 2));
						i += 2;
					} else {
						series.add(new String(chars, i, 1));
						++i;
					}
				}
			}
		} else {
			for (int i = 0; i < len;) {
				if (isWord(chars[i])) {
					int end = i + 1;
					for (; end < len; ++end) {
						if (chars[end] == '\'') {
							if (end + 1 < len && isWord(chars[end + 1])) {
								++end;
							} else {
								break;
							}
						} else if (!isWord(chars[end])) {
							break;
						}
					}
					
					series.add(new String(chars, i, end - i));
					i = end;
				} else if (isDigit(chars[i], popt)) {
					int end = i + 1;
					for (; end < len && isDigit(chars[end], popt); ++end) {
					}
									
					series.add(new String(chars, i, end - i));
					i = end;
				} else {
					if (Character.isHighSurrogate(chars[i]) && i + 1 < len && Character.isLowSurrogate(chars[i + 1])) {
						series.add(new String(chars, i, 2));
						i += 2;
					} else {
						series.add(new String(chars, i, 1));
						++i;
					}
				}
			}
		}

		return series;
	}
}

