package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * blob数据与数列互换，每字节对应一个成员
 * blob(b)
 * @author RunQian
 *
 */
public class Blob extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("blob" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("blob" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		Object obj = param.getLeafExpression().calculate(ctx);
		if (obj instanceof Sequence) {
			return toBlob((Sequence)obj, option);
		} else if (obj instanceof byte[]) {
			return toSequence((byte[])obj, option);
		} else if (obj == null) {
			return null;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
		}
	}
	
	private static Sequence toSequence(byte []bytes, String opt) {
		int len = bytes.length;
		if (len == 0) {
			return null;
		}
		
		if (opt != null) {
			if (opt.indexOf('4') != -1) {
				if (len % 4 != 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("blob" + mm.getMessage("function.invalidParam"));
				}
				
				Sequence seq = new Sequence(len / 4);
				if (opt.indexOf('s') == -1) {
					for (int index = 0; index < len; index += 4) {
						int n = (bytes[index] << 24) + ((bytes[index + 1] & 0xff) << 16) +
								((bytes[index + 2] & 0xff) << 8) + (bytes[index + 3] & 0xff);
						seq.add(n);
					}
				} else {
					for (int index = 0; index < len; index += 4) {
						int n = (bytes[index + 3] << 24) + ((bytes[index + 2] & 0xff) << 16) +
								((bytes[index + 1] & 0xff) << 8) + (bytes[index] & 0xff);
						seq.add(n);
					}
				}
				
				return seq;
			} else if (opt.indexOf('8') != -1) {
				if (len % 8 != 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("blob" + mm.getMessage("function.invalidParam"));
				}
				
				Sequence seq = new Sequence(len / 8);
				if (opt.indexOf('s') == -1) {
					for (int index = 0; index < len; index += 8) {
						long n = (((long)bytes[index] << 56) +
								((long)(bytes[index + 1] & 0xff) << 48) +
								((long)(bytes[index + 2] & 0xff) << 40) +
								((long)(bytes[index + 3] & 0xff) << 32) +
								((long)(bytes[index + 4] & 0xff) << 24) +
								((bytes[index + 5] & 0xff) << 16) +
								((bytes[index + 6] & 0xff) <<  8) +
								(bytes[index + 7] & 0xff));
						seq.add(n);
					}
				} else {
					for (int index = 0; index < len; index += 8) {
						long n = (((long)bytes[index + 7] << 56) +
								((long)(bytes[index + 6] & 0xff) << 48) +
								((long)(bytes[index + 5] & 0xff) << 40) +
								((long)(bytes[index + 4] & 0xff) << 32) +
								((long)(bytes[index + 3] & 0xff) << 24) +
								((bytes[index + 2] & 0xff) << 16) +
								((bytes[index + 1] & 0xff) <<  8) +
								(bytes[index] & 0xff));
						seq.add(n);
					}
				}
				
				return seq;
			} else if (opt.indexOf('f') != -1) {
				if (len % 4 != 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("blob" + mm.getMessage("function.invalidParam"));
				}
				
				Sequence seq = new Sequence(len / 4);
				if (opt.indexOf('s') == -1) {
					for (int index = 0; index < len; index += 4) {
						int n = (bytes[index] << 24) + ((bytes[index + 1] & 0xff) << 16) +
								((bytes[index + 2] & 0xff) << 8) + (bytes[index + 3] & 0xff);
						seq.add(Float.intBitsToFloat(n));
					}
				} else {
					for (int index = 0; index < len; index += 4) {
						int n = (bytes[index + 3] << 24) + ((bytes[index + 2] & 0xff) << 16) +
								((bytes[index + 1] & 0xff) << 8) + (bytes[index] & 0xff);
						seq.add(Float.intBitsToFloat(n));
					}
				}
				
				return seq;
			} else if (opt.indexOf('d') != -1) {
				if (len % 8 != 0) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("blob" + mm.getMessage("function.invalidParam"));
				}
				
				Sequence seq = new Sequence(len / 8);
				if (opt.indexOf('s') == -1) {
					for (int index = 0; index < len; index += 8) {
						long n = (((long)bytes[index] << 56) +
								((long)(bytes[index + 1] & 0xff) << 48) +
								((long)(bytes[index + 2] & 0xff) << 40) +
								((long)(bytes[index + 3] & 0xff) << 32) +
								((long)(bytes[index + 4] & 0xff) << 24) +
								((bytes[index + 5] & 0xff) << 16) +
								((bytes[index + 6] & 0xff) <<  8) +
								(bytes[index + 7] & 0xff));
						seq.add(Double.longBitsToDouble(n));
					}
				} else {
					for (int index = 0; index < len; index += 8) {
						long n = (((long)bytes[index + 7] << 56) +
								((long)(bytes[index + 6] & 0xff) << 48) +
								((long)(bytes[index + 5] & 0xff) << 40) +
								((long)(bytes[index + 4] & 0xff) << 32) +
								((long)(bytes[index + 3] & 0xff) << 24) +
								((bytes[index + 2] & 0xff) << 16) +
								((bytes[index + 1] & 0xff) <<  8) +
								(bytes[index] & 0xff));
						seq.add(Double.longBitsToDouble(n));
					}
				}
				
				return seq;
			}
		}
		
		Sequence seq = new Sequence(len);
		for (byte b : bytes) {
			seq.add(ObjectCache.getInteger(b & 0xff));
		}
		
		return seq;
	}
	
	private static byte[] toBlob(Sequence seq, String opt) {
		int len = seq.length();
		if (len == 0) {
			return null;
		}
		
		if (opt != null) {
			if (opt.indexOf('4') != -1) {
				byte []bytes = new byte[len * 4];
				int index = 0;
				
				if (opt.indexOf('s') == -1) {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = ((Number)obj).intValue();
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)n;
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = ((Number)obj).intValue();
						bytes[index++] = (byte)n;
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 24);
					}
				}
				
				return bytes;
			} else if (opt.indexOf('8') != -1) {
				byte []bytes = new byte[len * 8];
				int index = 0;
				
				if (opt.indexOf('s') == -1) {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						long n = ((Number)obj).longValue();
						bytes[index++] = (byte)(n >>> 56);
						bytes[index++] = (byte)(n >>> 48);
						bytes[index++] = (byte)(n >>> 40);
						bytes[index++] = (byte)(n >>> 32);
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)n;
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						long n = ((Number)obj).longValue();
						bytes[index++] = (byte)n;
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 32);
						bytes[index++] = (byte)(n >>> 40);
						bytes[index++] = (byte)(n >>> 48);
						bytes[index++] = (byte)(n >>> 56);
					}
				}
				
				return bytes;
			} else if (opt.indexOf('f') != -1) {
				byte []bytes = new byte[len * 4];
				int index = 0;
				
				if (opt.indexOf('s') == -1) {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = Float.floatToIntBits(((Number)obj).floatValue());
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)n;
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = Float.floatToIntBits(((Number)obj).floatValue());
						bytes[index++] = (byte)n;
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 24);
					}
				}
				
				return bytes;
			} else if (opt.indexOf('d') != -1) {
				byte []bytes = new byte[len * 8];
				int index = 0;
				
				if (opt.indexOf('s') == -1) {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						long n = Double.doubleToLongBits(((Number)obj).doubleValue());
						bytes[index++] = (byte)(n >>> 56);
						bytes[index++] = (byte)(n >>> 48);
						bytes[index++] = (byte)(n >>> 40);
						bytes[index++] = (byte)(n >>> 32);
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)n;
					}
				} else {
					for (int i = 1; i <= len; ++i) {
						Object obj = seq.getMem(i);
						if (!(obj instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("blob" + mm.getMessage("function.paramTypeError"));
						}
						
						long n = Double.doubleToLongBits(((Number)obj).doubleValue());
						bytes[index++] = (byte)n;
						bytes[index++] = (byte)(n >>> 8);
						bytes[index++] = (byte)(n >>> 16);
						bytes[index++] = (byte)(n >>> 24);
						bytes[index++] = (byte)(n >>> 32);
						bytes[index++] = (byte)(n >>> 40);
						bytes[index++] = (byte)(n >>> 48);
						bytes[index++] = (byte)(n >>> 56);
					}
				}
				
				return bytes;
			}
		}
		
		byte []bytes = new byte[len];
		for (int i = 1; i <= len; ++i) {
			Object obj = seq.getMem(i);
			if (obj instanceof Number) {
				bytes[i - 1] = ((Number)obj).byteValue();
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needIntSeries"));
			}
		}
		
		return bytes;
	}
}
