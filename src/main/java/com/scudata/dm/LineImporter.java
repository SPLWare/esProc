package com.scudata.dm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import com.scudata.common.DateFormatFactory;
import com.scudata.common.DateFormatX;
import com.scudata.common.Escape;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.Types;
import com.scudata.resources.EngineMessage;
import com.scudata.util.FloatingDecimal;
import com.scudata.util.Variant;

/**
 * 用于把文本文件读成序表
 * @author RunQian
 *
 */
public final class LineImporter implements ILineInput {
	private static final int BOM_SIZE = 4; // BOM头最大的大小
	private static final int PARSEMODE_DEFAULT = 0; // 对列进行类型转换，如果转不到指定的类型则尝试转成其它类型
	private static final int PARSEMODE_DELETE = 1; // 对列进行类型转换，如果转不到指定的类型则删除行
	private static final int PARSEMODE_EXCEPTION = 2; // 对列进行类型转换，如果转不到指定的类型则抛异常
	private static final int PARSEMODE_MULTI_STRING = 3; // 不做类型解析，返回多字段字符串
	private static final int PARSEMODE_SINGLE_STRING = 4; // 不做列拆分，每行返回成字符串
	
	private static final byte CR = (byte)'\r';
	private static final byte LF = (byte)'\n';
	private static final byte CONTINUECHAR = '\\'; // 续行符
	
	private InputStream is; // 输入流
	private byte[] buffer; // 每次读入的字节缓存
	private int index; // 下一行在buffer中的索引
	private int count; // 读入buffer的实际字节数目
	private long position; // 读入光标在流中的位置
	private boolean isEof = false; // 是否已经文件结束

	private String charset; // 字符集
	private byte colSeparator; // 列间隔
	private byte []colSeparators; // 多字符列间隔，如果不为空则忽略colSeparator
	
	private int []colLens; // 字段的大小，用于固定长度的文件，列间没有分隔符
	private byte []colTypes; // 列类型
	private DateFormatX []fmts; // 日期时间的格式
	private int []serialByteLens; // 排号字段的长度
	private int []selIndex; // 列是否选出，小于0不选出

	private int parseMode = PARSEMODE_DEFAULT; // 解析值的模式
	private char escapeChar = '\\'; // 转义符，@o选项时使用excel标准，转义符为"，并且找行尾时忽略引号内的换行符
	private boolean isQuote = false; // 剥离数据项两端引号，包括标题，处理转义
	private boolean isSingleQuote = false; // 剥离数据项两端单引号，包括标题，处理转义
	private boolean doQuoteMatch; // 是否做引号匹配
	private boolean doSingleQuoteMatch; // 是否做单引号匹配
	private boolean doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
	private boolean isTrim = true; // 是否取出两边的空白
	private boolean isContinueLine = false; // 是否有续行
	private boolean checkColCount = false; // 用于列数超过的行删除选项，以第一行为准
	private boolean checkValueType = false; // 数据类型和格式是否匹配
	
	private boolean isStringMode = false; // 是否先把行读成String再分列，防止有的编码的汉字的第二个字节的值等于列分隔符
	
	/**
	 * 用于表示每行数据对应的字节数组
	 * @author RunQian
	 *
	 */
	private class LineBytes {
		private byte[] buffer; // 缓冲区
		private int i; // 行开始位置，包含
		private int count; // 行字节数
		
		public LineBytes(byte[] buffer, int i, int count) {
			this.buffer = buffer;
			this.i = i;
			this.count = count;
		}
	}
	
	/**
	 * 构建把文本文件读成行列数据对象
	 * @param is 输入流
	 * @param charset 字符集
	 * @param colSeparator 列分隔符
	 * @param opt 选项
	 * 		@s	不拆分字段，读成单字段串构成的序表，忽略参数
	 * 		@q	剥离数据项两端引号，包括标题，处理转义；中间的引号不管
	 * 		@a	把单引号也作为引号处理，缺省不处理
	 * 		@o	使用Excel标准转义，串中双个引号表示一个引号，其它字符不转义
	 * 		@p	解析时处理括号和引号匹配，括号内分隔符不算，引号外转义也处理
	 * 		@f	不做任何解析，简单用分隔符拆成串
	 * 		@l	允许续行（行尾是转义字符\）
	 * 		@k	保留数据项两端的空白符，缺省将自动做trim
	 * 		@e	Fi在串中不存在时将生成null，缺省将报错
	 * 		@d	行内有数据不匹配类型和格式时删除该行，包括引号匹配和括号匹配
	 * 		@n	列数不匹配算作错误，忽略此行
	 * 		@v	@d@n时出错时抛出违例，中断程序，输出出错行的内容
	 */
	public LineImporter(InputStream is, String charset, byte[] colSeparator, String opt) {
		this(is, charset, colSeparator, opt, Env.FILE_BUFSIZE);
	}

	/**
	 * 构建把文本文件读成行列数据对象
	 * @param is 输入流
	 * @param charset 字符集
	 * @param colSeparator 列分隔符
	 * @param opt 选项
	 * 		@s	不拆分字段，读成单字段串构成的序表，忽略参数
	 * 		@q	剥离数据项两端引号，包括标题，处理转义；中间的引号不管
	 * 		@a	把单引号也作为引号处理，缺省不处理
	 * 		@o	使用Excel标准转义，串中双个引号表示一个引号，其它字符不转义
	 * 		@p	解析时处理括号和引号匹配，括号内分隔符不算，引号外转义也处理
	 * 		@f	不做任何解析，简单用分隔符拆成串
	 * 		@l	允许续行（行尾是转义字符\）
	 * 		@k	保留数据项两端的空白符，缺省将自动做trim
	 * 		@e	Fi在串中不存在时将生成null，缺省将报错
	 * 		@d	行内有数据不匹配类型和格式时删除该行，包括引号匹配和括号匹配
	 * 		@n	列数不匹配算作错误，忽略此行
	 * 		@v	@d@n时出错时抛出违例，中断程序，输出出错行的内容
	 * @param bufSize 读缓冲区大小
	 */
	public LineImporter(InputStream is, String charset, byte[] colSeparator, String opt, int bufSize) {
		if (colSeparator.length == 1) {
			this.colSeparator = colSeparator[0];
		} else {
			this.colSeparators = colSeparator;
		}

		this.is = is;
		this.charset = charset;
		buffer = new byte[bufSize];

		if (opt != null) {
			if (opt.indexOf('s') != -1) {
				// 不拆分字段，读成单字段串构成的序表
				parseMode = PARSEMODE_SINGLE_STRING;
			} else if (opt.indexOf('f') != -1) {
				// 不做类型解析，简单用分隔符拆成串
				parseMode = PARSEMODE_MULTI_STRING;
			} else {
				if (opt.indexOf('d') != -1) {
					checkValueType = true;
					parseMode = LineImporter.PARSEMODE_DELETE;
				}
				
				if (opt.indexOf('n') != -1) {
					checkColCount = true;
					parseMode = LineImporter.PARSEMODE_DELETE;
				}
				
				if (opt.indexOf('v') != -1) {
					parseMode = LineImporter.PARSEMODE_EXCEPTION;
				}
			}
			
			if (opt.indexOf('q') != -1) {
				// 剥离数据项两端引号，包括标题，处理转义，处理括号匹配
				isQuote = true;
				doQuoteMatch = true;
			}
			
			if (opt.indexOf('a') != -1) {
				// 剥离数据项两端单引号，包括标题，处理转义，处理括号匹配
				isSingleQuote = true;
				doSingleQuoteMatch = true;
			}
			
			if (opt.indexOf('o') != -1) {
				// 使用Excel标准转义，串中双个引号表示一个引号，其它字符不转义，处理括号匹配
				escapeChar = '"';
				doQuoteMatch = true;
			}
			
			// 解析时处理括号和引号（含单引号）匹配
			if (opt.indexOf('p') != -1) {
				doQuoteMatch = true;
				//doSingleQuoteMatch = true;
				doBracketsMatch = true;
			}
			
			// 允许续行（行尾是转义符\）
			if (opt.indexOf('l') != -1) isContinueLine = true;
			
			// 保留数据项两端的空白符，缺省将自动做trim
			if (opt.indexOf('k') != -1) isTrim = false;
			
			if (colSeparators == null && opt.indexOf('r') != -1) isStringMode = true;
		}
		
		// 跳过BOM头
		init();
	}

	/**
	 * 复制指定LineImporter的属性
	 * @param other
	 */
	public void copyProperty(LineImporter other) {
		this.charset = other.charset;
		this.colSeparator = other.colSeparator;
		this.colSeparators = other.colSeparators;
		this.colTypes = other.colTypes;
		this.fmts = other.fmts;
		this.serialByteLens = other.serialByteLens;
		this.selIndex = other.selIndex;
		
		this.parseMode = other.parseMode;
		this.escapeChar = other.escapeChar;
		this.isQuote = other.isQuote;
		this.isSingleQuote = other.isSingleQuote;
		this.doQuoteMatch = other.doQuoteMatch;
		this.doSingleQuoteMatch = other.doSingleQuoteMatch;
		this.doBracketsMatch = other.doBracketsMatch;

		this.isTrim = other.isTrim;
		this.isContinueLine = other.isContinueLine;
		this.checkColCount = other.checkColCount;
		this.checkValueType = other.checkValueType;
	}
	
	private void init() {
		// 检查是否有BOM头
		try {
			count = is.read(buffer);
			position = count;
			index = 0;
			
			if (count < BOM_SIZE) {
				return;
			} else if (buffer[0] == (byte)0xEF && buffer[1] == (byte)0xBB && buffer[2] == (byte)0xBF) {
				charset = "UTF-8";
				index = 3;
			} /*else if (buffer[0] == (byte)0xFF && buffer[1] == (byte)0xFE && buffer[2] == (byte)0x00 && buffer[3] == (byte)0x00) {
				charset = "UTF-32LE";
				index = 4;
			} else if (buffer[0] == (byte)0x00 && buffer[1] == (byte)0x00 && buffer[2] == (byte)0xFE && buffer[3] == (byte)0xFF) {
				charset = "UTF-32BE";
				index = 4;
			} else if (buffer[0] == (byte)0xFF && buffer[1] == (byte)0xFE) {
				charset = "UTF-16LE";
				index = 2;
			} else if (buffer[0] == (byte)0xFE && buffer[1] == (byte)0xFF) {
				charset = "UTF-16BE";
				index = 2;				
			} */else {
				return;
			}
			
			// UTF-16和UTF-32列分割父和回车会占用多个字节，目前没有处理
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	/**
	 * 取转义符
	 * @return char
	 */
	public char getEscapeChar() {
		return escapeChar;
	}
	
	// 读入文件内容到缓冲区
	private int readBuffer() throws IOException {
		if (!isEof) {
			do {
				count = is.read(buffer);
			} while (count == 0);

			if (count > 0) {
				position += count;
			} else {
				isEof = true;
			}

			index = 0;
			return count;
		} else {
			return -1;
		}
	}

	/**
	 * 取当前的读入位置
	 * @return
	 */
	public long getCurrentPosition() {
		return position - count + index;
	}

	// 跳过输入流指定字节
	static private long skip(InputStream is, long count) throws IOException {
		long old = count;
		while (count > 0) {
			long num = is.skip(count);
			if (num <= 0) break;

			count -= num;
		}

		return old - count;
	}

	/**
	 * 跳到指定位置，用于多线程读取数据，会做掐头去尾处理
	 * @param pos 位置
	 * @throws IOException
	 */
	public void seek(long pos) throws IOException {
		if (pos <= 0) {
		} else if (pos < position) {
			long dif = position - pos;
			if (dif < count) {
				index = count - (int)dif;
				skipLine();
			} else { // 只能往前seek
				throw new RuntimeException();
			}
		} else {
			long skipCount = skip(is, pos - position);
			position += skipCount;
			readBuffer();
			skipLine();
		}
	}

	/**
	 * 设置字段类型
	 * @param types 类型数组
	 * @param strFmts 格式数组，用于日期时间
	 */
	public void setColTypes(byte []types, String []strFmts) {
		int count = types.length;
		this.colTypes = types;
		this.fmts = new DateFormatX[count];
		this.serialByteLens = new int[count];
		
		for (int i = 0; i < count; ++i) {
			if (types[i] == Types.DT_DATE) {
				if (strFmts == null || strFmts[i] == null) {
					fmts[i] = DateFormatFactory.newDateFormatX();
				} else {
					fmts[i] = DateFormatFactory.newFormatX(strFmts[i]);
				}
			} else if (types[i] == Types.DT_DATETIME) {
				if (strFmts == null || strFmts[i] == null) {
					fmts[i] = DateFormatFactory.newDateTimeFormatX();
				} else {
					fmts[i] = DateFormatFactory.newFormatX(strFmts[i]);
				}
			} else if (types[i] == Types.DT_TIME) {
				if (strFmts == null || strFmts[i] == null) {
					fmts[i] = DateFormatFactory.newTimeFormatX();
				} else {
					fmts[i] = DateFormatFactory.newFormatX(strFmts[i]);
				}
			} else if (types[i] == Types.DT_SERIALBYTES) {
				serialByteLens[i] = Integer.parseInt(strFmts[i]);
			}
		}
	}

	/**
	 * 设置字段的大小，用于固定长度的文件，列间没有分隔符
	 * @param fieldLens
	 */
	public void setColLens(int[] colLens) {
		this.colLens = colLens;
	}

	/**
	 * 取字段类型
	 * @return 类型数组
	 */
	public byte[] getColTypes() {
		return colTypes;
	}

	/**
	 * 设置选取的列
	 * @param index 列索引组成的数组，从0开始计数
	 */
	public void setColSelectIndex(int []index) {
		this.selIndex = index;
	}

	/**
	 * 取选出的列序号组成的数组，从0开始计数
	 * @return
	 */
	public int[] getColSelectIndex() {
		return selIndex;
	}
	
	// 读出下一行数据所占的字节
	private LineBytes readLineBytes() throws IOException {
		// 是否跳过引号内的回车
		boolean skipQuoteEnter = escapeChar == '"';
		byte[] buffer = this.buffer;
		byte []prevBuffer = null; // 上次剩余的字节
		LineBytes line = null;
		
		int count = this.count;
		int index = this.index;
		int start = index;
		
		Next:
		while (true) {
			if (index >= count) {
				// 当前缓存的数据已经遍历完，保存当前数据到prevBuffer中
				int curCount = count - start;
				if (curCount > 0) {
					if (prevBuffer == null) {
						prevBuffer = new byte[curCount];
						System.arraycopy(buffer, start, prevBuffer, 0, curCount);
					} else {
						int prevLen = prevBuffer.length;
						byte[] temp = new byte[curCount + prevLen];
						System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
						System.arraycopy(buffer, start, temp, prevLen, curCount);
						prevBuffer = temp;
					}
				}

				// 读入后面的字节
				if (readBuffer() <= 0) {
					if (prevBuffer != null) { // 最后一行
						return new LineBytes(prevBuffer, 0, prevBuffer.length);
					} else {
						return null;
					}
				} else {
					count = this.count;
					start = 0;
					index = 0;
				}
			}
			
			if (buffer[index] == LF) {
				// 找到行结束符，索引跳到换行后
				this.index = index + 1;
				
				if (index > start) {
					// 检查LF前是否是CR
					if (buffer[index - 1] == CR) {
						index--;
					}
					
					int curLen = index - start;
					if (prevBuffer == null) {
						line = new LineBytes(buffer, start, curLen);
					} else if (curLen > 0) {
						int prevLen = prevBuffer.length;
						byte []temp = new byte[prevLen + curLen];
						System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
						System.arraycopy(buffer, start, temp, prevLen, curLen);
						line = new LineBytes(temp, 0, temp.length);
					} else {
						return new LineBytes(prevBuffer, 0, prevBuffer.length);
					}
				} else {
					if (prevBuffer != null) {
						// 检查会车前是否是换行符
						int prevLen = prevBuffer.length;
						if (prevBuffer[prevLen - 1] == CR) { // \r在上一次读出的缓冲区中，index等于0
							line = new LineBytes(prevBuffer, 0, prevLen -1);
						} else {
							line = new LineBytes(prevBuffer, 0, prevLen);
						}
					} else {
						// 此行内容为空，只有回车
						line = new LineBytes(buffer, start, 0);
					}
				}
				
				return line;
			} else if (skipQuoteEnter && buffer[index] == '"') {
				// 找引号匹配，跳到右引号的下一个字符
				++index;
				while (true) {
					if (index == count) {
						// 保存当前数据到prevBuffer中
						int curCount = count - start;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}

						// 读入后面的字节
						if (readBuffer() <= 0) {
							return new LineBytes(prevBuffer, 0, prevBuffer.length);
						} else {
							count = this.count;
							start = 0;
							index = 0;
						}
					}
					
					if (buffer[index] == '"') {
						++index;
						if (index < count) {
							if (buffer[index] != '"') {
								// 找到引号匹配
								continue Next;
							} else {
								// 连续两个双引号是对引号转义
								++index;
							}
						} else {
							// 保存当前数据到prevBuffer中
							int curCount = count - start;
							if (prevBuffer == null) {
								prevBuffer = new byte[curCount];
								System.arraycopy(buffer, start, prevBuffer, 0, curCount);
							} else {
								int prevLen = prevBuffer.length;
								byte[] temp = new byte[curCount + prevLen];
								System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
								System.arraycopy(buffer, start, temp, prevLen, curCount);
								prevBuffer = temp;
							}
	
							// 读入后面的字节
							if (readBuffer() <= 0) {
								return new LineBytes(prevBuffer, 0, prevBuffer.length);
							} else {
								count = this.count;
								start = 0;
								if (buffer[0] != '"') {
									// 找到引号匹配
									index = 0;
									continue Next;
								} else {
									// 连续两个双引号是对引号转义
									index = 1;
								}
							}
						}
					} else {
						++index;
					}
				}
			} else if (isContinueLine && buffer[index] == CONTINUECHAR) {
				// 如果允许续行，检查当前字符是否是‘\’
				++index;
				if (index < count) {
					if (buffer[index] == LF) { // \n
						// 保存当前数据到prevBuffer中
						int curCount = index - start - 1;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
						
						start = ++index;
					} else if (buffer[index] == CR) { // \r\n
						// 保存当前数据到prevBuffer中
						int curCount = index - start - 1;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
						
						// CR后面跟着LF，跳到LF
						++index;
						if (index == count) {
							// \n在下一个缓冲区
							if (readBuffer() <= 0) {
								return new LineBytes(prevBuffer, 0, prevBuffer.length);
							} else {
								count = this.count;
								index = 1;
							}
						} else {
							++index;
						}
						
						start = index;
					}
				} else {
					// 保存当前数据到prevBuffer中
					int curCount = index - start - 1;
					if (curCount > 0) {
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
					}
					
					if (readBuffer() <= 0) {
						if (prevBuffer != null) {
							return new LineBytes(prevBuffer, 0, prevBuffer.length);
						} else {
							return null;
						}
					} else {
						count = this.count;
						if (buffer[0] == LF) { // \n
							index = 1;
							start = 1;
						} else if (buffer[0] == CR) { // \r\n
							index = 2;
							start = 2;
						} else {
							index = 0;
							start = 0;
							
							// 把续行符加入到之前的缓冲区
							if (prevBuffer == null) {
								prevBuffer = new byte[]{CONTINUECHAR};
							} else {
								int prevLen = prevBuffer.length;
								byte[] temp = new byte[prevLen + 1];
								System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
								temp[prevLen] = CONTINUECHAR;
								prevBuffer = temp;
							}
						}
					}
				}
			} else {
				++index;
			}
		}
	}
	
	// 把下一行数据读成字符串
	private String readLineString() throws IOException {
		// 是否跳过引号内的回车
		boolean skipQuoteEnter = escapeChar == '"';
		byte[] buffer = this.buffer;
		byte []prevBuffer = null; // 上次剩余的字节
		int count = this.count;
		int index = this.index;
		int start = index;
		
		Next:
		while (true) {
			if (index >= count) {
				// 当前缓存的数据已经遍历完，保存当前数据到prevBuffer中
				int curCount = count - start;
				if (curCount > 0) {
					if (prevBuffer == null) {
						prevBuffer = new byte[curCount];
						System.arraycopy(buffer, start, prevBuffer, 0, curCount);
					} else {
						int prevLen = prevBuffer.length;
						byte[] temp = new byte[curCount + prevLen];
						System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
						System.arraycopy(buffer, start, temp, prevLen, curCount);
						prevBuffer = temp;
					}
				}

				// 读入后面的字节
				if (readBuffer() <= 0) {
					if (prevBuffer != null) { // 最后一行
						return new String(prevBuffer, 0, prevBuffer.length, charset);
					} else {
						return null;
					}
				} else {
					count = this.count;
					start = 0;
					index = 0;
				}
			}
			
			if (buffer[index] == LF) {
				// 找到行结束符，索引跳到换行后
				this.index = index + 1;
				
				if (index > start) {
					// 检查LF前是否是CR
					if (buffer[index - 1] == CR) {
						index--;
					}
					
					int curLen = index - start;
					if (prevBuffer == null) {
						return new String(buffer, start, curLen, charset);
					} else if (curLen > 0) {
						int prevLen = prevBuffer.length;
						byte []temp = new byte[prevLen + curLen];
						System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
						System.arraycopy(buffer, start, temp, prevLen, curLen);
						return new String(temp, 0, temp.length, charset);
					} else {
						return new String(prevBuffer, 0, prevBuffer.length, charset);
					}
				} else {
					if (prevBuffer != null) {
						// 检查会车前是否是换行符
						int prevLen = prevBuffer.length;
						if (prevBuffer[prevLen - 1] == CR) { // \r在上一次读出的缓冲区中，index等于0
							return new String(prevBuffer, 0, prevLen -1, charset);
						} else {
							return new String(prevBuffer, 0, prevLen, charset);
						}
					} else {
						// 此行内容为空，只有回车
						return new String(buffer, start, 0, charset);
					}
				}
			} else if (skipQuoteEnter && buffer[index] == '"') {
				// 找引号匹配，跳到右引号的下一个字符
				++index;
				while (true) {
					if (index == count) {
						// 保存当前数据到prevBuffer中
						int curCount = count - start;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}

						// 读入后面的字节
						if (readBuffer() <= 0) {
							return new String(prevBuffer, 0, prevBuffer.length, charset);
						} else {
							count = this.count;
							start = 0;
							index = 0;
						}
					}
					
					if (buffer[index] == '"') {
						++index;
						if (index < count) {
							if (buffer[index] != '"') {
								// 找到引号匹配
								continue Next;
							} else {
								// 连续两个双引号是对引号转义
								++index;
							}
						} else {
							// 保存当前数据到prevBuffer中
							int curCount = count - start;
							if (prevBuffer == null) {
								prevBuffer = new byte[curCount];
								System.arraycopy(buffer, start, prevBuffer, 0, curCount);
							} else {
								int prevLen = prevBuffer.length;
								byte[] temp = new byte[curCount + prevLen];
								System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
								System.arraycopy(buffer, start, temp, prevLen, curCount);
								prevBuffer = temp;
							}
	
							// 读入后面的字节
							if (readBuffer() <= 0) {
								return new String(prevBuffer, 0, prevBuffer.length, charset);
							} else {
								count = this.count;
								start = 0;
								if (buffer[0] != '"') {
									// 找到引号匹配
									index = 0;
									continue Next;
								} else {
									// 连续两个双引号是对引号转义
									index = 1;
								}
							}
						}
					} else {
						++index;
					}
				}
			} else if (isContinueLine && buffer[index] == CONTINUECHAR) {
				// 如果允许续行，检查当前字符是否是‘\’
				++index;
				if (index < count) {
					if (buffer[index] == LF) { // \n
						// 保存当前数据到prevBuffer中
						int curCount = index - start - 1;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
						
						start = ++index;
					} else if (buffer[index] == CR) { // \r\n
						// 保存当前数据到prevBuffer中
						int curCount = index - start - 1;
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
						
						// CR后面跟着LF，跳到LF
						++index;
						if (index == count) {
							// \n在下一个缓冲区
							if (readBuffer() <= 0) {
								return new String(prevBuffer, 0, prevBuffer.length, charset);
							} else {
								count = this.count;
								index = 1;
							}
						} else {
							++index;
						}
						
						start = index;
					}
				} else {
					// 保存当前数据到prevBuffer中
					int curCount = index - start - 1;
					if (curCount > 0) {
						if (prevBuffer == null) {
							prevBuffer = new byte[curCount];
							System.arraycopy(buffer, start, prevBuffer, 0, curCount);
						} else {
							int prevLen = prevBuffer.length;
							byte[] temp = new byte[curCount + prevLen];
							System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
							System.arraycopy(buffer, start, temp, prevLen, curCount);
							prevBuffer = temp;
						}
					}
					
					if (readBuffer() <= 0) {
						if (prevBuffer != null) {
							return new String(prevBuffer, 0, prevBuffer.length, charset);
						} else {
							return null;
						}
					} else {
						count = this.count;
						if (buffer[0] == LF) { // \n
							index = 1;
							start = 1;
						} else if (buffer[0] == CR) { // \r\n
							index = 2;
							start = 2;
						} else {
							index = 0;
							start = 0;
							
							// 把续行符加入到之前的缓冲区
							if (prevBuffer == null) {
								prevBuffer = new byte[]{CONTINUECHAR};
							} else {
								int prevLen = prevBuffer.length;
								byte[] temp = new byte[prevLen + 1];
								System.arraycopy(prevBuffer, 0, temp, 0, prevLen);
								temp[prevLen] = CONTINUECHAR;
								prevBuffer = temp;
							}
						}
					}
				}
			} else {
				++index;
			}
		}
	}

	// 每行数据读成一个字符串
	private String readLineString(LineBytes line) throws IOException {
		byte[] buffer = line.buffer;
		int count = line.count;
		if (count < 1) {
			return "";
		} else if (count < 2) {
			return new String(buffer, line.i, count, charset);
		}
		
		int i = line.i;
		if (isQuote && buffer[i] == '"' && buffer[i + count - 1] == '"') {
			String str = new String(buffer, i + 1, count - 2, charset);
			return Escape.remove(str, escapeChar);
		} else if (isSingleQuote && buffer[i] == '\'' && buffer[i + count - 1] == '\'') {
			String str = new String(buffer, i + 1, count - 2, charset);
			return Escape.remove(str, '\\');
		} else {
			return new String(buffer, i, count, charset);
		}
	}
	
	// 根据字段类型把拆分行的列，并转成对象
	private Object[] readLine(LineBytes line, byte []colTypes) throws IOException {
		if (colSeparators != null) {
			return readLine2(line, colTypes);
		}
		
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		int count = line.count;
		if (count < 1) {
			return values;
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		
		byte colSeparator = this.colSeparator;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		int colIndex = 0;
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		while (index < end && colIndex < colCount) {
			byte c = buffer[index];
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					values[colIndex] = parse(buffer, start, index, colIndex);
				}
				
				colIndex++;
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							break;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						break;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (colIndex < colCount && (selIndex == null || selIndex[colIndex] != -1)) {
			values[colIndex] = parse(buffer, start, end, colIndex);
		}

		return values;
	}
	
	// 多字符列分隔符
	// 根据字段类型把拆分行的列，并转成对象
	private Object[] readLine2(LineBytes line, byte []colTypes) throws IOException {
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		int count = line.count;
		if (count < 1) {
			return values;
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		
		byte []colSeparators = this.colSeparators;
		int sepLen = colSeparators.length;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		int colIndex = 0;
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		while (index < end && colIndex < colCount) {
			if (BracketsLevel == 0 && isColSeparators(buffer, index, end, colSeparators, sepLen)) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					values[colIndex] = parse(buffer, start, index, colIndex);
				}
				
				colIndex++;
				index += sepLen;
				start = index;
				continue;
			}

			byte c = buffer[index];
			if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							break;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						break;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (colIndex < colCount && (selIndex == null || selIndex[colIndex] != -1)) {
			values[colIndex] = parse(buffer, start, end, colIndex);
		}

		return values;
	}
	
	// 读数据时会做数据类型、列数匹配、括号匹配等检查
	// 根据字段类型把拆分行的列，并转成对象
	private Object[] readLineOnCheck(LineBytes line, byte []colTypes) throws IOException {
		if (colSeparators != null) {
			return readLineOnCheck2(line, colTypes);
		}

		int count = line.count;
		if (count < 1) {
			return null;
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		
		byte colSeparator = this.colSeparator;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		boolean checkValueType = this.checkValueType;
		
		int colIndex = 0;
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		Next:
		while (index < end && colIndex < colCount) {
			byte c = buffer[index];
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					if (checkValueType) {
						if (!parse(buffer, start, index, colIndex, values)) {
							return null;
						}
					} else {
						values[colIndex] = parse(buffer, start, index, colIndex);
					}
				}
				
				colIndex++;
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							continue Next;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的引号返回空
				return null;
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						continue Next;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的单引号返回空
				return null;
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (BracketsLevel != 0) {
			// 有不匹配的括号
			return null;
		}
		
		if (colIndex < colCount) {
			if (checkColCount && colIndex + 1 < colCount) {
				return null;
			}
			
			if (selIndex == null || selIndex[colIndex] != -1) {
				if (checkValueType) {
					if (!parse(buffer, start, end, colIndex, values)) {
						return null;
					}
				} else {
					values[colIndex] = parse(buffer, start, end, colIndex);
				}
			}
		}

		return values;
	}

	// 多字符列分隔符
	// 根据字段类型把拆分行的列，并转成对象
	private Object[] readLineOnCheck2(LineBytes line, byte []colTypes) throws IOException {
		int count = line.count;
		if (count < 1) {
			return null;
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		
		byte []colSeparators = this.colSeparators;
		int sepLen = colSeparators.length;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		boolean checkValueType = this.checkValueType;
		
		int colIndex = 0;
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		Next:
		while (index < end && colIndex < colCount) {
			if (BracketsLevel == 0 && isColSeparators(buffer, index, end, colSeparators, sepLen)) { // 列结束
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					if (checkValueType) {
						if (!parse(buffer, start, index, colIndex, values)) {
							return null;
						}
					} else {
						values[colIndex] = parse(buffer, start, index, colIndex);
					}
				}
				
				colIndex++;
				index += sepLen;
				start = index;
				continue;
			}
			
			byte c = buffer[index];
			if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							continue Next;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的引号返回空
				return null;
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						continue Next;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的单引号返回空
				return null;
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (BracketsLevel != 0) {
			// 有不匹配的括号
			return null;
		}
		
		if (colIndex < colCount) {
			if (checkColCount && colIndex + 1 < colCount) {
				return null;
			}
			
			if (selIndex == null || selIndex[colIndex] != -1) {
				if (checkValueType) {
					if (!parse(buffer, start, end, colIndex, values)) {
						return null;
					}
				} else {
					values[colIndex] = parse(buffer, start, end, colIndex);
				}
			}
		}

		return values;
	}

	private Object[] readLine(LineBytes line) throws IOException {
		if (colSeparators != null) {
			return readLine2(line);
		}
		
		int count = line.count;
		if (count < 1) {
			return new Object[0];
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		
		String charset = this.charset;
		byte colSeparator = this.colSeparator;
		char escapeChar = this.escapeChar;
		boolean isTrim = this.isTrim;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		ArrayList<Object> list = new ArrayList<Object>();
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		int start = index; // 列的起始位置
		
		while (index < end) {
			byte c = buffer[index];
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				int len = index - start;
				if (len > 0) {
					String str = new String(buffer, start, len, charset);
					if (isTrim) {
						str = str.trim();
					}
					
					list.add(parse(str));
				} else {
					list.add(null);
				}
				
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							break;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						break;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		int len = end - start;
		if (len > 0) {
			String str = new String(buffer, start, len, charset);
			if (isTrim) {
				str = str.trim();
			}
			
			list.add(parse(str));
		} else {
			list.add(null);
		}
		
		return list.toArray();
	}
	
	private static boolean isColSeparators(byte []buffer, int index, int end, byte []colSeparators, int len) {
		if (end - index < len) {
			return false;
		}
		
		for (int i = 0; i < len; ++i, ++index) {
			if (buffer[index] != colSeparators[i]) {
				return false;
			}
		}
		
		return true;
	}
	
	// 多字符列分隔符
	private Object[] readLine2(LineBytes line) throws IOException {
		int count = line.count;
		if (count < 1) {
			return new Object[0];
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		
		String charset = this.charset;
		byte []colSeparators = this.colSeparators;
		int sepLen = colSeparators.length;
		char escapeChar = this.escapeChar;
		boolean isTrim = this.isTrim;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		ArrayList<Object> list = new ArrayList<Object>();
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		while (index < end) {
			if (BracketsLevel == 0 && isColSeparators(buffer, index, end, colSeparators, sepLen)) {
				// 列结束
				int len = index - start;
				if (len > 0) {
					String str = new String(buffer, start, len, charset);
					if (isTrim) {
						str = str.trim();
					}
					
					list.add(parse(str));
				} else {
					list.add(null);
				}
				
				index += sepLen;
				start = index;
				continue;
			}

			byte c = buffer[index];
			if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							break;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						break;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		int len = end - start;
		if (len > 0) {
			String str = new String(buffer, start, len, charset);
			if (isTrim) {
				str = str.trim();
			}
			
			list.add(parse(str));
		} else {
			list.add(null);
		}
		
		return list.toArray();
	}
	
	/**
	 * 读入首行
	 * @return 列值组成的数组
	 * @throws IOException
	 */
	public Object[] readFirstLine() throws IOException {
		if (isStringMode) {
			String line = readLineString();
			if (line == null) {
				return null;
			} else if (parseMode == PARSEMODE_SINGLE_STRING) {
				if (isQuote || isSingleQuote) {
					line = Escape.removeEscAndQuote(line, escapeChar);
				}
				
				return new Object[] {line};
			} else if (colTypes != null) {
				return readLine(line, colTypes);
			} else {
				return readLine(line);
			}
		}
		
		LineBytes line = readLineBytes();
		if (line == null) {
			return null;
		} else if (parseMode == PARSEMODE_SINGLE_STRING) {
			return new Object[] {readLineString(line)};
		} else if (colTypes != null) {
			return readLine(line, colTypes);
		} else {
			return readLine(line);
		}
	}
	
	/**
	 * 返回下一行，如果结束了则返回null
	 * @return Object[]
	 * @throws IOException
	 */
	public Object[] readLine() throws IOException {
		if (colLens != null) {
			return readFixedLengthLine();
		} else if (isStringMode) {
			if (parseMode == PARSEMODE_DELETE) {
				while (true) {
					String line = readLineString();
					if (line == null) {
						return null;
					}
					
					Object []vals = readLineOnCheck(line, colTypes);
					if (vals != null) {
						return vals;
					}
				}
			} else if (parseMode == PARSEMODE_EXCEPTION) {
				String line = readLineString();
				if (line == null) {
					return null;
				}
				
				Object []vals = readLineOnCheck(line, colTypes);
				if (vals != null) {
					return vals;
				} else {
					if (line.length() > 0) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(line + mm.getMessage("file.rowDataError"));
					} else {
						return null;
					}
				}
			} else {
				String line = readLineString();
				if (line == null) {
					return null;
				} else if (parseMode == PARSEMODE_SINGLE_STRING) {
					if (isQuote || isSingleQuote) {
						line = Escape.removeEscAndQuote(line, escapeChar);
					}
					
					return new Object[] {line};
				} else if (colTypes != null) {
					return readLine(line, colTypes);
				} else {
					return readLine(line);
				}
			}
		} else if (parseMode == PARSEMODE_DELETE) {
			while (true) {
				LineBytes line = readLineBytes();
				if (line == null) {
					return null;
				}
				
				Object []vals = readLineOnCheck(line, colTypes);
				if (vals != null) {
					return vals;
				}
			}
		} else if (parseMode == PARSEMODE_EXCEPTION) {
			LineBytes line = readLineBytes();
			if (line == null) {
				return null;
			}
			
			Object []vals = readLineOnCheck(line, colTypes);
			if (vals != null) {
				return vals;
			} else {
				if (line.count > 0) {
					String strLine = new String(line.buffer, line.i, line.count);
					MessageManager mm = EngineMessage.get();
					throw new RQException(strLine + mm.getMessage("file.rowDataError"));
				} else {
					return null;
				}
			}
		} else {
			// PARSEMODE_DEFAULT、PARSEMODE_MULTI_STRING、PARSEMODE_SINGLE_STRING
			LineBytes line = readLineBytes();
			if (line == null) {
				return null;
			} else if (parseMode == PARSEMODE_SINGLE_STRING) {
				return new Object[] {readLineString(line)};
			} else if (colTypes != null) {
				return readLine(line, colTypes);
			} else {
				return readLine(line);
			}
		}
	}

	/**
	 * 跳过下一行，如果结束了则返回false，否则返回true
	 * @return boolean
	 * @throws IOException
	 */
	public boolean skipLine() throws IOException {
		// 是否跳过引号内的回车
		boolean skipQuoteEnter = escapeChar == '"';
		byte[] buffer = this.buffer;
		int count = this.count;
		int index = this.index;
		boolean sign = index < count;
		
		Next:
		while (true) {
			if (index >= count) {
				// 读入后面的字节
				if (readBuffer() <= 0) {
					return sign;
				} else {
					count = this.count;
					index = 0;
					sign = true;
				}
			}
			
			if (buffer[index] == LF) {
				this.index = index + 1;
				return true;
			} else if (skipQuoteEnter && buffer[index] == '"') {
				// 找引号匹配，跳到右引号的下一个字符
				++index;
				while (true) {
					if (index == count) {
						// 读入后面的字节
						if (readBuffer() <= 0) {
							return true;
						} else {
							count = this.count;
							index = 0;
						}
					}
					
					if (buffer[index] == '"') {
						++index;
						if (index < count) {
							if (buffer[index] != '"') {
								// 找到引号匹配
								continue Next;
							} else {
								// 连续两个双引号是对引号转义
								++index;
							}
						} else {
							// 读入后面的字节
							if (readBuffer() <= 0) {
								return true;
							} else {
								count = this.count;
								if (buffer[0] != '"') {
									// 找到引号匹配
									index = 0;
									continue Next;
								} else {
									// 连续两个双引号是对引号转义
									index = 1;
								}
							}
						}
					} else {
						++index;
					}
				}
			} else if (isContinueLine && buffer[index] == CONTINUECHAR) {
				// 如果允许续行，检查当前字符是否是‘\’
				++index;
				if (index < count) {
					if (buffer[index] == LF) { // \n
						++index;
					} else if (buffer[index] == CR) { // \r\n
						// CR后面跟着LF，跳到LF
						++index;
						if (index == count) {
							// \n在下一个缓冲区
							if (readBuffer() <= 0) {
								return true;
							} else {
								count = this.count;
								index = 1;
							}
						} else {
							++index;
						}
					}
				} else {
					if (readBuffer() <= 0) {
						return true;
					} else {
						count = this.count;
						if (buffer[0] == LF) { // \n
							index = 1;
						} else if (buffer[0] == CR) { // \r\n
							index = 2;
						} else {
							index = 0;
						}
					}
				}
			} else {
				index++;
			}
		}
	}

	// 返回下一行的起始位置，结束返回-1
	public static int readLine(char []buffer, int index, char colSeparator, ArrayList<String> line) {
		int start = index;
		int count = buffer.length;
		while (index < count) {
			if (buffer[index] == colSeparator) { // 列结束
				line.add(new String(buffer, start, index - start));
				index++;
				start = index;
			} else if (buffer[index] == LineImporter.LF) { // \n
				// \r\n 或 \n两种换行符都要兼容
				if (index > start) {
					if (buffer[index - 1] == LineImporter.CR) {
						line.add(new String(buffer, start, index - start - 1));
					} else {
						line.add(new String(buffer, start, index - start));
					}
				} else {
					line.add(null);
				}

				index++;
				start = index;
				return index;
			} else {
				index++;
			}
		}

		if (count > start) {
			line.add(new String(buffer, start, count - start));
		} else if (line.size() > 0) { // 最后一个字段空
			line.add(null);
		} // 空行

		return -1;
	}

	/**
	 * 关闭输入
	 * @throws IOException
	 */
	public void close() throws IOException {
		is.close();
	}
	
	// 判断字符串是否是表示null的标识符
	private static boolean isNull(String str) {
		String []nulls = Env.getNullStrings();
		if (nulls != null) {
			for (String nullStr : nulls) {
				if (str.equalsIgnoreCase(nullStr)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	// 把字符串解析成对象
	private Object parse(String str) {
		int len = str.length();
		if (len > 1) {
			char c = str.charAt(0);
			if ((isQuote && c == '"') || (isSingleQuote && c == '\'')) {
				// 在结尾是否有引号
				if (str.charAt(len - 1) == c) {
					return Escape.remove(str.substring(1, len - 1), escapeChar);
				} else {
					return str;
				}
			}
		}
		
		if (parseMode == PARSEMODE_MULTI_STRING) {
			if (isNull(str)) {
				return null;
			} else {
				return str;
			}
		} else {
			return Variant.parseDirect(str);
		}
	}
	
	// 把字节数组解析成对象
	private Object parse(byte []bytes, int start, int end, int col) throws UnsupportedEncodingException {
		if (isTrim) {
			while (start < end && Character.isWhitespace(bytes[start])) {
				start++;
			}
			
			while (end > start && Character.isWhitespace(bytes[end - 1])) {
				end--;
			}
		}
		
		if (start >= end) {
			return null;
		}

		byte []types = this.colTypes;
		byte c = bytes[start];
		if ((isQuote && c == '"') || (isSingleQuote && c == '\'')) {
			// 在结尾是否有引号
			if (bytes[end - 1] == c) {
				start++;
				end--;
				if (start < end) {
					if (types[col] == Types.DT_DEFAULT || types[col] == Types.DT_STRING) {
						String str = new String(bytes, start, end - start, charset);
						return Escape.remove(str, escapeChar);
					}
				} else if (start == end) {
					return "";
				} else {
					return String.valueOf(c);
				}
			} else {
				return new String(bytes, start, end - start, charset);
			}
		} else if (parseMode == PARSEMODE_MULTI_STRING) {
			// 直接返回串
			String str = new String(bytes, start, end - start, charset);
			if (isNull(str)) {
				return null;
			} else {
				return str;
			}
		}

		switch (types[col]) {
		case Types.DT_STRING:
			String str = new String(bytes, start, end - start, charset);
			if (isNull(str)) {
				return null;
			} else {
				return str;
			}
		case Types.DT_INT:
			Number num = parseInt(bytes, start, end);
			if (num != null) return num;

			num = parseLong(bytes, start, end);
			if (num != null) {
				types[col] = Types.DT_LONG;
				return num;
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					return new Double(fd.doubleValue());
				}
			} catch (RuntimeException e) {
			}

			break;
		case Types.DT_DOUBLE:
			if (bytes[end - 1] == '%') { // 5%
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end - 1);
					if (fd != null) return new Double(fd.doubleValue() / 100);
				} catch (RuntimeException e) {
				}
			} else {
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
					if (fd != null) return new Double(fd.doubleValue());
				} catch (RuntimeException e) {
				}
			}

			break;
		case Types.DT_DATE:
			String text = new String(bytes, start, end - start, charset);
			Date date = fmts[col].parse(text);
			if (date != null) return new java.sql.Date(date.getTime());

			break;
		case Types.DT_DECIMAL:
			try {
				return new BigDecimal(new String(bytes, start, end - start, charset));
			} catch (NumberFormatException e) {
			}

			break;
		case Types.DT_LONG:
			if (end - start > 2 && bytes[start] == '0' &&
				(bytes[start + 1] == 'X' || bytes[start + 1] == 'x')) {
				num = parseLong(bytes, start + 2, end, 16);
			} else {
				num = parseLong(bytes, start, end);
			}

			if (num != null) {
				return num;
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					return new Double(fd.doubleValue());
				}
			} catch (RuntimeException e) {
			}

			break;
		case Types.DT_DATETIME:
			text = new String(bytes, start, end - start, charset);
			date = fmts[col].parse(text);
			if (date != null) return new java.sql.Timestamp(date.getTime());

			break;
		case Types.DT_TIME:
			text = new String(bytes, start, end - start, charset);
			date = fmts[col].parse(text);
			if (date != null) return new java.sql.Time(date.getTime());

			break;
		case Types.DT_BOOLEAN:
			Boolean b = parseBoolean(bytes, start, end);
			if (b != null) return b;

			break;
		case Types.DT_SERIALBYTES:
			num = parseLong(bytes, start, end);
			if (num != null) {
				return new SerialBytes(num.longValue(), serialByteLens[col]);
			}
			
			break;
		default:
			str = new String(bytes, start, end - start, charset);
			if (isNull(str)) {
				return null;
			}
			
			Object val = Variant.parseDirect(str);
			types[col] = Variant.getObjectType(val);
			
			if (types[col] == Types.DT_DATE) {
				fmts[col] = DateFormatFactory.newDateFormatX();
			} else if (types[col] == Types.DT_DATETIME) {
				fmts[col] = DateFormatFactory.newDateTimeFormatX();
			} else if (types[col] == Types.DT_TIME) {
				fmts[col] = DateFormatFactory.newTimeFormatX();
			}
			
			return val;
		}
		
		//types[col] = getObjectType(val);
		String str = new String(bytes, start, end - start, charset);
		if (isNull(str)) {
			return null;
		} else {
			return Variant.parseDirect(str);
		}
	}
	
	private Object parse(String str, int col) throws UnsupportedEncodingException {
		int start = 0, end = str.length();
		
		if (isTrim) {
			while (start < end && Character.isWhitespace(str.charAt(start))) {
				start++;
			}
			
			while (end > start && Character.isWhitespace(str.charAt(end - 1))) {
				end--;
			}
		}
		
		if (start >= end) {
			return null;
		}

		byte []types = this.colTypes;
		char c = str.charAt(start);
		if ((isQuote && c == '"') || (isSingleQuote && c == '\'')) {
			// 在结尾是否有引号
			if (str.charAt(end - 1) == c) {
				start++;
				end--;
				if (start < end) {
					if (types[col] == Types.DT_DEFAULT || types[col] == Types.DT_STRING) {
						return Escape.removeEscAndQuote(str, escapeChar);
					}
				} else if (start == end) {
					return "";
				} else {
					return String.valueOf(c);
				}
			} else {
				return str;
			}
		} else if (parseMode == PARSEMODE_MULTI_STRING) {
			// 直接返回串
			if (isNull(str)) {
				return null;
			} else {
				return str;
			}
		}

		switch (types[col]) {
		case Types.DT_STRING:
			if (isNull(str)) {
				return null;
			} else {
				return str;
			}
		case Types.DT_INT:
			Number num = Variant.parseNumber(str);
			if (num != null) {
				return num;
			} else {
				break;
			}
		case Types.DT_DOUBLE:
			num = Variant.parseDouble(str);
			if (num != null) {
				return num;
			} else {
				break;
			}
		case Types.DT_DATE:
			Date date = fmts[col].parse(str);
			if (date != null) return new java.sql.Date(date.getTime());

			break;
		case Types.DT_DECIMAL:
			try {
				return new BigDecimal(str);
			} catch (NumberFormatException e) {
			}

			break;
		case Types.DT_LONG:
			num = parseLong(str);
			if (num != null) {
				return num;
			}

			break;
		case Types.DT_DATETIME:
			date = fmts[col].parse(str);
			if (date != null) return new java.sql.Timestamp(date.getTime());

			break;
		case Types.DT_TIME:
			date = fmts[col].parse(str);
			if (date != null) return new java.sql.Time(date.getTime());

			break;
		case Types.DT_BOOLEAN:
			if (str.equals("true")) return Boolean.TRUE;
			if (str.equals("false")) return Boolean.FALSE;

			break;
		case Types.DT_SERIALBYTES:
			num = Variant.parseLong(str);
			if (num != null) {
				return new SerialBytes(num.longValue(), serialByteLens[col]);
			}
			
			break;
		default:
			if (isNull(str)) {
				return null;
			}
			
			Object val = Variant.parseDirect(str);
			types[col] = Variant.getObjectType(val);
			
			if (types[col] == Types.DT_DATE) {
				fmts[col] = DateFormatFactory.newDateFormatX();
			} else if (types[col] == Types.DT_DATETIME) {
				fmts[col] = DateFormatFactory.newDateTimeFormatX();
			} else if (types[col] == Types.DT_TIME) {
				fmts[col] = DateFormatFactory.newTimeFormatX();
			}
			
			return val;
		}
		
		if (isNull(str)) {
			return null;
		} else {
			return Variant.parseDirect(str);
		}
	}
	
	// 把字节数组解析成对象，如果类型不符则返回false
	private boolean parse(String str, int col, Object []outValue) throws UnsupportedEncodingException {
		int start = 0, end = str.length();
		
		if (isTrim) {
			while (start < end && Character.isWhitespace(str.charAt(start))) {
				start++;
			}
			
			while (end > start && Character.isWhitespace(str.charAt(end - 1))) {
				end--;
			}
		}
		
		if (start >= end) {
			return true;
		}

		byte []types = this.colTypes;
		char c = str.charAt(start);
		if ((isQuote && c == '"') || (isSingleQuote && c == '\'')) {
			// 在结尾是否有引号
			if (str.charAt(end - 1) == c) {
				start++;
				end--;
				if (start < end) {
					if (types[col] == Types.DT_DEFAULT || types[col] == Types.DT_STRING) {
						outValue[col] = Escape.remove(str, escapeChar);
						return true;
					}
				} else if (start == end) {
					outValue[col] = "";
					return true;
				} else {
					outValue[col] = String.valueOf(c);
					return true;
				}
			} else {
				outValue[col] = str;
				return true;
			}
		} else if (parseMode == PARSEMODE_MULTI_STRING) {
			// 直接返回串
			if (isNull(str)) {
				outValue[col] = null;
				return true;
			} else {
				outValue[col] = str;
				return true;
			}
		}

		switch (types[col]) {
		case Types.DT_STRING:
			if (!isNull(str)) {
				outValue[col] = str;
			}
			
			return true;
		case Types.DT_INT:
			Number num = Variant.parseNumber(str);
			if (num != null) {
				outValue[col] = num;
				return true;
			}

			break;
		case Types.DT_DOUBLE:
			num = Variant.parseDouble(str);
			if (num != null) {
				outValue[col] = num;
				return true;
			}

			break;
		case Types.DT_DATE:
			Date date = fmts[col].parse(str);
			if (date != null) {
				outValue[col] = new java.sql.Date(date.getTime());
				return true;
			}

			break;
		case Types.DT_DECIMAL:
			try {
				outValue[col] = new BigDecimal(str);
				return true;
			} catch (NumberFormatException e) {
			}

			break;
		case Types.DT_LONG:
			num = Variant.parseLong(str);
			if (num != null) {
				outValue[col] = num;
				return true;
			}

			break;
		case Types.DT_DATETIME:
			date = fmts[col].parse(str);
			if (date != null) {
				outValue[col] = new java.sql.Timestamp(date.getTime());
				return true;
			}

			break;
		case Types.DT_TIME:
			date = fmts[col].parse(str);
			if (date != null) {
				outValue[col] = new java.sql.Time(date.getTime());
				return true;
			}

			break;
		case Types.DT_BOOLEAN:
			if (str.equals("true")) {
				outValue[col] = Boolean.TRUE;
				return true;
			} else if (str.equals("false")) {
				outValue[col] = Boolean.FALSE;
				return true;
			}

			break;
		case Types.DT_SERIALBYTES:
			num = Variant.parseLong(str);
			if (num != null) {
				outValue[col] = new SerialBytes(num.longValue(), serialByteLens[col]);
				return true;
			}
			
			break;
		default:
			if (isNull(str)) {
				return true;
			}
			
			outValue[col] = Variant.parseDirect(str);
			types[col] = Variant.getObjectType(outValue[col]);
			
			if (types[col] == Types.DT_DATE) {
				fmts[col] = DateFormatFactory.newDateFormatX();
			} else if (types[col] == Types.DT_DATETIME) {
				fmts[col] = DateFormatFactory.newDateTimeFormatX();
			} else if (types[col] == Types.DT_TIME) {
				fmts[col] = DateFormatFactory.newTimeFormatX();
			}
			
			return true;
		}

		if (isNull(str)) {
			return true;
		} else {
			return false;
		}
	}
	
	// 把字节数组解析成对象，如果类型不符则返回false
	private boolean parse(byte []bytes, int start, int end, int col, Object []outValue) throws UnsupportedEncodingException {
		if (isTrim) {
			while (start < end && bytes[start] == ' ') {
				start++;
			}
			
			while (end > start && bytes[end - 1] == ' ') {
				end--;
			}
		}
		
		if (start >= end) {
			return true;
		}

		byte []types = this.colTypes;
		byte c = bytes[start];
		if ((isQuote && c == '"') || (isSingleQuote && c == '\'')) {
			// 在结尾是否有引号
			if (bytes[end - 1] == c) {
				start++;
				end--;
				if (start < end) {
					if (types[col] == Types.DT_DEFAULT || types[col] == Types.DT_STRING) {
						String str = new String(bytes, start, end - start, charset);
						outValue[col] = Escape.remove(str, escapeChar);
						return true;
					}
				} else if (start == end) {
					outValue[col] = "";
					return true;
				} else {
					outValue[col] = String.valueOf(c);
					return true;
				}
			} else {
				outValue[col] = new String(bytes, start, end - start, charset);
				return true;
			}
		} else if (parseMode == PARSEMODE_MULTI_STRING) {
			// 直接返回串
			String str = new String(bytes, start, end - start, charset);
			if (isNull(str)) {
				outValue[col] = null;
				return true;
			} else {
				outValue[col] = str;
				return true;
			}
		}

		switch (types[col]) {
		case Types.DT_STRING:
			String str = new String(bytes, start, end - start, charset);
			if (!isNull(str)) {
				outValue[col] = str;
			}
			
			return true;
		case Types.DT_INT:
			Number num = parseInt(bytes, start, end);
			if (num != null) {
				outValue[col] = num;
				return true;
			}

			num = parseLong(bytes, start, end);
			if (num != null) {
				types[col] = Types.DT_LONG;
				outValue[col] = num;
				return true;
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					outValue[col] = new Double(fd.doubleValue());
					return true;
				}
			} catch (RuntimeException e) {
			}

			break;
		case Types.DT_DOUBLE:
			if (bytes[end - 1] == '%') { // 5%
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end - 1);
					outValue[col] = new Double(fd.doubleValue() / 100);
					return true;
				} catch (RuntimeException e) {
				}
			} else {
				try {
					FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
					outValue[col] = new Double(fd.doubleValue());
					return true;
				} catch (RuntimeException e) {
				}
			}

			break;
		case Types.DT_DATE:
			String text = new String(bytes, start, end - start, charset);
			Date date = fmts[col].parse(text);
			if (date != null) {
				outValue[col] = new java.sql.Date(date.getTime());
				return true;
			}

			break;
		case Types.DT_DECIMAL:
			try {
				outValue[col] = new BigDecimal(new String(bytes, start, end - start, charset));
				return true;
			} catch (NumberFormatException e) {
			}

			break;
		case Types.DT_LONG:
			if (end - start > 2 && bytes[start] == '0' &&
				(bytes[start + 1] == 'X' || bytes[start + 1] == 'x')) {
				num = parseLong(bytes, start + 2, end, 16);
			} else {
				num = parseLong(bytes, start, end);
			}

			if (num != null) {
				outValue[col] = num;
				return true;
			}

			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(bytes, start, end);
				if (fd != null) {
					types[col] = Types.DT_DOUBLE;
					outValue[col] = new Double(fd.doubleValue());
					return true;
				}
			} catch (RuntimeException e) {
			}

			break;
		case Types.DT_DATETIME:
			text = new String(bytes, start, end - start, charset);
			date = fmts[col].parse(text);
			if (date != null) {
				outValue[col] = new java.sql.Timestamp(date.getTime());
				return true;
			}

			break;
		case Types.DT_TIME:
			text = new String(bytes, start, end - start, charset);
			date = fmts[col].parse(text);
			if (date != null) {
				outValue[col] = new java.sql.Time(date.getTime());
				return true;
			}

			break;
		case Types.DT_BOOLEAN:
			Boolean b = parseBoolean(bytes, start, end);
			if (b != null) {
				outValue[col] = b;
				return true;
			}

			break;
		case Types.DT_SERIALBYTES:
			num = parseLong(bytes, start, end);
			if (num != null) {
				outValue[col] = new SerialBytes(num.longValue(), serialByteLens[col]);
				return true;
			}
			
			break;
		default:
			str = new String(bytes, start, end - start, charset);
			if (isNull(str)) {
				return true;
			}
			
			outValue[col] = Variant.parseDirect(str);
			types[col] = Variant.getObjectType(outValue[col]);
			
			if (types[col] == Types.DT_DATE) {
				fmts[col] = DateFormatFactory.newDateFormatX();
			} else if (types[col] == Types.DT_DATETIME) {
				fmts[col] = DateFormatFactory.newDateTimeFormatX();
			} else if (types[col] == Types.DT_TIME) {
				fmts[col] = DateFormatFactory.newTimeFormatX();
			}
			
			return true;
		}

		String str = new String(bytes, start, end - start, charset);
		if (isNull(str)) {
			return true;
		} else {
			return false;
		}
	}
	
	// 解析整数值，不是整数值则返回空
	private static Integer parseInt(byte []bytes, int i, int e) {
		int result = 0;
		boolean negative = false;
		int limit;
		int multmin;
		int digit;

		if (bytes[i] == '-') {
			negative = true;
			limit = Integer.MIN_VALUE;
			i++;
		} else {
			limit = -Integer.MAX_VALUE;
		}
		multmin = limit / 10;

		if (i < e) {
			digit = Character.digit((char)bytes[i++], 10);
			if (digit < 0) {
				return null;
			} else {
				result = -digit;
			}
		}

		while (i < e) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			digit = Character.digit((char)bytes[i++], 10);
			if (digit < 0) {
				return null;
			}
			if (result < multmin) {
				return null;
			}
			result *= 10;
			if (result < limit + digit) {
				return null;
			}
			result -= digit;
		}

		if (negative) {
			if (i > 1) {
				return new Integer(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Integer( -result);
		}
	}
	
	// 解析Long值，不是Long值则返回空
	private static Long parseLong(byte []bytes, int i, int e) {
		// 1L
		if (e - i > 1 && bytes[e - 1] == 'L') e--;
		
		long result = 0;
		boolean negative = false;
		long limit;
		long multmin;
		int digit;

		if (bytes[i] == '-') {
			negative = true;
			limit = Long.MIN_VALUE;
			i++;
		} else {
			limit = -Long.MAX_VALUE;
		}

		multmin = limit / 10;
		if (i < e) {
			digit = Character.digit((char)bytes[i++], 10);
			if (digit < 0) {
				return null;
			} else {
				result = -digit;
			}
		}

		while (i < e) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			digit = Character.digit((char)bytes[i++], 10);
			if (digit < 0) {
				return null;
			}
			if (result < multmin) {
				return null;
			}
			result *= 10;
			if (result < limit + digit) {
				return null;
			}
			result -= digit;
		}

		if (negative) {
			if (i > 1) {
				return new Long(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Long( -result);
		}
	}

	// 解析Long值，不是Long值则返回空
	private static Long parseLong(byte []bytes, int i, int e, int radix) {
		long result = 0;
		boolean negative = false;
		//int i = 0, max = s.length();
		long limit;
		long multmin;
		int digit;

		if (bytes[i] == '-') {
			negative = true;
			limit = Long.MIN_VALUE;
			i++;
		} else {
			limit = -Long.MAX_VALUE;
		}
		multmin = limit / radix;
		if (i < e) {
			digit = Character.digit((char)bytes[i++], radix);
			if (digit < 0) {
				return null;
			} else {
				result = -digit;
			}
		}
		while (i < e) {
			// Accumulating negatively avoids surprises near MAX_VALUE
			digit = Character.digit((char)bytes[i++], radix);
			if (digit < 0) {
				return null;
			}
			if (result < multmin) {
				return null;
			}
			result *= radix;
			if (result < limit + digit) {
				return null;
			}
			result -= digit;
		}

		if (negative) {
			if (i > 1) {
				return new Long(result);
			} else { /* Only got "-" */
				return null;
			}
		} else {
			return new Long(-result);
		}
	}
	
	private static Number parseLong(String s) {
		s = s.trim();
		int len = s.length();
		if (len == 0) return null;


		Long numObj = Variant.parseLong(s);
		if (numObj != null) return numObj;

		if (len > 2 && s.charAt(0) == '0' && (s.charAt(1) == 'X' || s.charAt(1) == 'x')) {
			numObj = Variant.parseLong(s.substring(2), 16);
			if (numObj != null) return numObj;
		}

		if (s.endsWith("%")) { // 5%
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s.
					substring(0, s.length() - 1));
				if (fd != null)return new Double(fd.doubleValue() / 100);
			} catch (RuntimeException e) {
			}
		} else {
			try {
				FloatingDecimal fd = FloatingDecimal.readJavaFormatString(s);
				if (fd != null) return new Double(fd.doubleValue());
			} catch (RuntimeException e) {
			}
		}

		return null;
	}

	// 解析布尔值，不是布尔值则返回空
	private static Boolean parseBoolean(byte []bytes, int i, int e) {
		int count = e - i;
		if (count == 4) {
			if (bytes[i] == 't' && bytes[i+1] == 'r' && bytes[i+2] == 'u' && bytes[i+3] == 'e')
				return Boolean.TRUE;
		} else if (count == 5) {
			if (bytes[i] == 'f' && bytes[i+1] == 'a' && bytes[i+2] == 'l' && bytes[i+3] == 's' && bytes[i+4] == 'e')
				return Boolean.FALSE;
		}

		return null;
	}
	
	private Object[] readLineOnCheck(String line, byte []colTypes) throws IOException {
		int count = line.length();
		if (count < 1) {
			return null;
		}
		
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		
		byte colSeparator = this.colSeparator;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		boolean checkValueType = this.checkValueType;
		
		int colIndex = 0;
		int index = 0, start = 0;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		Next:
		while (index < count && colIndex < colCount) {
			char c = line.charAt(index);
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					String str = line.substring(start, index);
					if (checkValueType) {
						if (!parse(str, colIndex, values)) {
							return null;
						}
					} else {
						values[colIndex] = parse(str, colIndex);
					}
				}
				
				colIndex++;
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '"') {
						index++;
						if (escapeChar != '"' || index == count || line.charAt(index) != '"') {
							continue Next;
						}
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的引号返回空
				return null;
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '\'') {
						index++;
						continue Next;
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
				
				// 没找到匹配的单引号返回空
				return null;
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{' || c == '<' || c == '（' || c == '【' || c == '《') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}' || c == '>' || c == '）' || c == '】' || c == '》')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (BracketsLevel != 0) {
			// 有不匹配的括号
			return null;
		}
		
		if (colIndex < colCount) {
			if (checkColCount && colIndex + 1 < colCount) {
				return null;
			}
			
			if (selIndex == null || selIndex[colIndex] != -1) {
				String str = line.substring(start, index);
				if (checkValueType) {
					if (!parse(str, colIndex, values)) {
						return null;
					}
				} else {
					values[colIndex] = parse(str, colIndex);
				}
			}
		}

		return values;
	}
	
	private Object[] readLine(String line, byte []colTypes) throws IOException {
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		int count = line.length();
		if (count < 1) {
			return values;
		}
				
		byte colSeparator = this.colSeparator;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		int colIndex = 0;
		int index = 0, start = 0;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		while (index < count && colIndex < colCount) {
			char c = line.charAt(index);
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					String str = line.substring(start, index);
					values[colIndex] = parse(str, colIndex);
				}
				
				colIndex++;
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '"') {
						index++;
						if (escapeChar != '"' || index == count || line.charAt(index) != '"') {
							break;
						}
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '\'') {
						index++;
						break;
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{' || c == '<' || c == '（' || c == '【' || c == '《') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}' || c == '>' || c == '）' || c == '】' || c == '》')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (colIndex < colCount && (selIndex == null || selIndex[colIndex] != -1)) {
			String str = line.substring(start, index);
			values[colIndex] = parse(str, colIndex);
		}

		return values;
	}
	
	private Object[] readLine(String line) throws IOException {
		int count = line.length();
		if (count < 1) {
			return new Object[0];
		}
		
		byte colSeparator = this.colSeparator;
		char escapeChar = this.escapeChar;
		boolean isTrim = this.isTrim;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		ArrayList<Object> list = new ArrayList<Object>();
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		int index = 0, start = 0;
		
		while (index < count) {
			char c = line.charAt(index);
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (index > start) {
					String str = line.substring(start, index);
					if (isTrim) {
						str = str.trim();
					}
					
					list.add(parse(str));
				} else {
					list.add(null);
				}
				
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '"') {
						index++;
						if (escapeChar != '"' || index == count || line.charAt(index) != '"') {
							break;
						}
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < count; ++index) {
					if (line.charAt(index) == '\'') {
						index++;
						break;
					} else if (line.charAt(index) == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{' || c == '<' || c == '（' || c == '【' || c == '《') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}' || c == '>' || c == '）' || c == '】' || c == '》')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (count > start) {
			String str = line.substring(start, index);
			if (isTrim) {
				str = str.trim();
			}
			
			list.add(parse(str));
		} else {
			list.add(null);
		}
		
		return list.toArray();
	}
	
	// 根据字段类型把拆分行的列，并转成对象
	public Object[] readLine(byte[] bytes) throws IOException {
		LineBytes line = new LineBytes(bytes, 0, bytes.length);
		byte []colTypes = this.colTypes;
		
		if (colSeparators != null) {
			return readLine2(line, colTypes);
		}
		
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		int count = line.count;
		if (count < 1) {
			return values;
		}
		
		byte[] buffer = line.buffer;
		int index = line.i;
		int end = index + count;
		
		byte colSeparator = this.colSeparator;
		int []selIndex = this.selIndex;
		char escapeChar = this.escapeChar;
		boolean doQuoteMatch = this.doQuoteMatch; // 是否做引号匹配
		boolean doSingleQuoteMatch = this.doSingleQuoteMatch; // 是否做单引号匹配
		boolean doBracketsMatch = this.doBracketsMatch; // 是否做括号匹配（包括圆括号、中括号、花括号）
		
		int colIndex = 0;
		int start = index;
		int BracketsLevel = 0; // 括号的层数，有p选项时认为括号总是匹配出现的
		
		while (index < end && colIndex < colCount) {
			byte c = buffer[index];
			if (BracketsLevel == 0 && c == colSeparator) {
				// 列结束
				if (selIndex == null || selIndex[colIndex] != -1) {
					values[colIndex] = parse(buffer, start, index, colIndex);
				}
				
				colIndex++;
				start = ++index;
			} else if (doQuoteMatch && c == '"') {
				// 找引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '"') {
						index++;
						if (escapeChar != '"' || index == end || buffer[index] != '"') {
							break;
						}
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doSingleQuoteMatch && c == '\'') {
				// 找单引号匹配，忽略引号内的列分隔符
				for (++index; index < end; ++index) {
					if (buffer[index] == '\'') {
						index++;
						break;
					} else if (buffer[index] == escapeChar) {
						index++;
					}
				}
			} else if (doBracketsMatch) {
				if (c == '(' || c == '[' || c == '{') {
					BracketsLevel++;
				} else if (BracketsLevel > 0 && (c == ')' || c == ']' || c == '}')) {
					BracketsLevel--;
				}
				
				index++;
			} else {
				index++;
			}
		}
		
		if (colIndex < colCount && (selIndex == null || selIndex[colIndex] != -1)) {
			values[colIndex] = parse(buffer, start, end, colIndex);
		}

		return values;
	}
	
	private Object[] readFixedLengthLine() throws IOException {
		String line = readLineString();
		if (line == null) {
			return null;
		}
		
		int count = line.length();
		if (count < 1) {
			return new Object[colTypes.length];
		}

		byte []colTypes = this.colTypes;
		int []colLens = this.colLens;
		int []selIndex = this.selIndex;
		int colCount = colTypes.length;
		Object []values = new Object[colCount];
		
		for (int colIndex = 0, start = 0; colIndex < colCount; ++colIndex) {
			if (selIndex == null || selIndex[colIndex] != -1) {
				String str = line.substring(start, start + colLens[colIndex]);
				values[colIndex] = parse(str, colIndex);
			}
			
			start += colLens[colIndex];
		}

		return values;
	}
}
