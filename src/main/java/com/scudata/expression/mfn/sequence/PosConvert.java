package com.scudata.expression.mfn.sequence;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.SequenceFunction;
import com.scudata.resources.EngineMessage;

/**
 * 把指定位置（或位置序列）转成序列的序号
 * A.p(i), A.p(p)
 * @author RunQian
 *
 */
public class PosConvert extends SequenceFunction {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("p" + mm.getMessage("function.missingParam"));
		} else if (!param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("p" + mm.getMessage("function.invalidParam"));
		}
	}
	
	public Object calculate(Context ctx) {
		Object pval = param.getLeafExpression().calculate(ctx);
		if (pval == null) {
			return null;
		}

		boolean isRepeat = false, reserveZero = true;
		if (option != null) {
			if (option.indexOf('r') != -1)isRepeat = true;
			if (option.indexOf('0') != -1)reserveZero = false;
		}

		int srcLen = srcSequence.length();
		if (pval instanceof Number) {
			int pos = ((Number)pval).intValue();
			if (isRepeat) {
				if (pos > srcLen) {
					pos %= srcLen;
					return pos == 0 ? new Integer(srcLen) : new Integer(pos);
				} else if (pos >= 0) {
					return pval;
				} else { // < 0
					pos %= srcLen;
					if (pos < 0) {
						return new Integer(pos + srcLen + 1);
					} else {
						return new Integer(1);
					}
				}
			} else {
				if (pos < 0) {
					pos += srcLen + 1;
					if (pos > 0) return new Integer(pos);
					return new Integer(0);
				} else {
					if (pos <= srcLen) return pval;
					return new Integer(0);
				}
			}
		} else if (pval instanceof Sequence) {
			Sequence posSequence = (Sequence)pval;
			int posCount = posSequence.length();
			int addNum = srcLen + 1;
			Sequence result = new Sequence(posCount);
			Integer zero = new Integer(0);

			if (isRepeat) {
				for (int i = 1; i <= posCount; ++i) {
					Object posObj = posSequence.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("p" + mm.getMessage("function.paramTypeError"));
					}

					int pos = ((Number)posObj).intValue();
					if (pos > srcLen) {
						pos %= srcLen;
						if (pos != 0) {
							result.add(new Integer(pos));
						} else {
							result.add(new Integer(srcLen));
						}
					} else if (pos > 0) {
						result.add(posObj);
					} else if (pos == 0) {
						if (reserveZero) result.add(zero);
					} else { // < 0
						pos %= srcLen;
						if (pos < 0) {
							result.add(new Integer(pos + addNum));
						} else {
							result.add(new Integer(1));
						}
					}
				}
			} else {
				for (int i = 1; i <= posCount; ++i) {
					Object posObj = posSequence.get(i);
					if (!(posObj instanceof Number)) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("p" + mm.getMessage("function.paramTypeError"));
					}

					int pos = ((Number)posObj).intValue();
					if (pos > srcLen) {
						if (reserveZero) result.add(zero);
					} else if (pos > 0) {
						result.add(posObj);
					} else if (pos == 0) {
						if (reserveZero) result.add(zero);
					} else { // < 0
						pos += addNum;
						if (pos > 0) {
							result.add(new Integer(pos));
						} else {
							if (reserveZero) result.add(zero);
						}
					}
				}
			}

			return result;
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException("p" + mm.getMessage("function.paramTypeError"));
		}
	}
}
