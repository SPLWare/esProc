package com.scudata.expression.mfn.sequence;

import com.scudata.array.IArray;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.IParam;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 取序列成员，越界返回空
 * A.m(i) A.m(p)
 * @author RunQian
 *
 */
public class MGet extends SequenceFunction {
	public static int convert(int srcLen, int pos, boolean isRepeat) {
		if (pos > srcLen) {
			if (isRepeat) {
				pos %= srcLen;
				return pos == 0 ? srcLen : pos;
			} else {
				return 0;
			}
		} else if (pos > 0) {
			return pos;
		} else if (pos == 0) {
			return 0;
		} else { // < 0
			if (isRepeat) {
				pos %= srcLen;
				return pos < 0 ? pos + srcLen + 1 : 1;
			} else {
				pos += srcLen + 1;
				return pos > 0 ? pos : 0;
			}
		}
	}
	
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("m" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		IArray mems = srcSequence.getMems();
		int srcLen = mems.size();

		boolean isRepeat = false, reserveZero = true;
		if (option != null) {
			if (option.indexOf('r') != -1) isRepeat = true;
			if (option.indexOf('0') != -1) reserveZero = false;
		}

		if (param.isLeaf()) {
			Object pval = param.getLeafExpression().calculate(ctx);
			if (pval == null) return null;
			
			if (pval instanceof Number) {
				int pos = convert(srcLen, ((Number)pval).intValue(), isRepeat);
				if (pos > 0) {
					return mems.get(pos);
				} else {
					return null;
				}
			} else if (pval instanceof Sequence) {
				Sequence posSequence = (Sequence)pval;
				int posCount = posSequence.length();
				Sequence result = new Sequence(posCount);
				
				for (int i = 1; i <= posCount; ++i) {
					Object posObj = posSequence.get(i);
					if (posObj instanceof Number) {
						int pos = convert(srcLen, ((Number)posObj).intValue(), isRepeat);
						if (pos > 0) {
							result.add(mems.get(pos));
						} else if (reserveZero) {
							result.add(null);
						}
					} else if (posObj == null) {
						if (reserveZero) {
							result.add(null);
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("m" + mm.getMessage("function.paramTypeError"));
					}
				}
				
				return result;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("m" + mm.getMessage("function.paramTypeError"));
			}
		} else if (param.getType() == IParam.Colon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("m" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub0 = param.getSub(0);
			int pos0;
			if (sub0 != null) {
				Object obj0 = sub0.getLeafExpression().calculate(ctx);
				if (!(obj0 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("m" + mm.getMessage("function.paramTypeError"));
				}
				
				int n = ((Number)obj0).intValue();
				pos0 = convert(srcLen, n, isRepeat);
				if (pos0 == 0) {
					if (n < 0) {
						// 起始位置左越界则从1开始
						pos0 = 1;
					} else {
						return new Sequence();
					}
				}
			} else {
				pos0 = 1;
			}
			
			IParam sub1 = param.getSub(1);
			int pos1;
			if (sub1 != null) {
				Object obj1 = sub1.getLeafExpression().calculate(ctx);
				if (!(obj1 instanceof Number)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("m" + mm.getMessage("function.paramTypeError"));
				}
				
				int n = ((Number)obj1).intValue();
				pos1 = convert(srcLen, n, isRepeat);
				if (pos1 == 0) {
					if (n > srcLen) {
						// 结束位置右越界则取到结尾
						pos1 = srcLen;
					} else {
						return new Sequence();
					}
				}
			} else {
				pos1 = srcLen;
			}
			
			if (pos0 <= pos1) {
				Sequence result = new Sequence(pos1 - pos0 + 1);
				for (int i = pos0; i <= pos1; ++i) {
					result.add(mems.get(i));
				}
				
				return result;
			} else {
				Sequence result = new Sequence(pos0 - pos1 + 1);
				for (int i = pos0; i >= pos1; --i) {
					result.add(mems.get(i));
				}
				
				return result;
			}
		} else if (param.getType() == IParam.Comma) {
			Sequence result = new Sequence();
			for (int p = 0, size = param.getSubSize(); p < size; ++p) {
				IParam sub = param.getSub(p);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("m" + mm.getMessage("function.invalidParam"));
				}
				
				if (sub.isLeaf()) {
					Object pval = sub.getLeafExpression().calculate(ctx);
					if (pval == null) {
						if (reserveZero) {
							result.add(null);
						}
					} else if (pval instanceof Number) {
						int pos = convert(srcLen, ((Number)pval).intValue(), isRepeat);
						if (pos > 0) {
							result.add(mems.get(pos));
						} else {
							if (reserveZero) {
								result.add(null);
							}
						}
					} else if (pval instanceof Sequence) {
						Sequence posSequence = (Sequence)pval;
						int posCount = posSequence.length();
						
						for (int i = 1; i <= posCount; ++i) {
							Object posObj = posSequence.get(i);
							if (posObj instanceof Number) {
								int pos = convert(srcLen, ((Number)posObj).intValue(), isRepeat);
								if (pos > 0) {
									result.add(mems.get(pos));
								} else if (reserveZero) {
									result.add(null);
								}
							} else if (posObj == null) {
								if (reserveZero) {
									result.add(null);
								}
							} else {
								MessageManager mm = EngineMessage.get();
								throw new RQException("m" + mm.getMessage("function.paramTypeError"));
							}
						}
					} else {
						MessageManager mm = EngineMessage.get();
						throw new RQException("m" + mm.getMessage("function.paramTypeError"));
					}
				} else {
					if (sub.getSubSize() != 2) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("m" + mm.getMessage("function.invalidParam"));
					}
					
					IParam sub0 = sub.getSub(0);
					int pos0;
					if (sub0 != null) {
						Object obj0 = sub0.getLeafExpression().calculate(ctx);
						if (!(obj0 instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("m" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = ((Number)obj0).intValue();
						pos0 = convert(srcLen, n, isRepeat);
						if (pos0 == 0) {
							if (n < 0) {
								// 起始位置左越界则从1开始
								pos0 = 1;
							} else {
								continue;
							}
						}
					} else {
						pos0 = 1;
					}
					
					
					IParam sub1 = sub.getSub(1);
					int pos1;
					if (sub1 != null) {
						Object obj1 = sub1.getLeafExpression().calculate(ctx);
						if (!(obj1 instanceof Number)) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("m" + mm.getMessage("function.paramTypeError"));
						}
						
						int n = ((Number)obj1).intValue();
						pos1 = convert(srcLen, n, isRepeat);
						if (pos1 == 0) {
							if (n > srcLen) {
								// 结束位置右越界则取到结尾
								pos1 = srcLen;
							} else {
								continue;
							}
						}
					} else {
						pos1 = srcLen;
					}

					if (pos0 <= pos1) {
						for (int i = pos0; i <= pos1; ++i) {
							result.add(mems.get(i));
						}
					} else {
						for (int i = pos0; i >= pos1; --i) {
							result.add(mems.get(i));
						}
					}
				}
			}
			
			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("m" + mm.getMessage("function.invalidParam"));
		}
	}
}
