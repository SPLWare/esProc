package com.scudata.dm.comparator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.Date;

import com.ibm.icu.text.Collator;
import com.scudata.common.ICloneable;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.resources.EngineMessage;
import com.scudata.util.Variant;

/**
 * null当做最小处理的比较器
 * @author WangXiaoJun
 *
 */
public class CommonComparator implements Comparator<Object>, ICloneable {
	private final Collator collator; // 语言比较器，用于字符串比较
	private final boolean isAsc; // 是否升序排序
	private final boolean isNullLast; // null放最后
	
	public CommonComparator(Collator collator, boolean isAsc, boolean isNullLast) {
		this.collator = collator;
		this.isAsc = isAsc;
		this.isNullLast = isNullLast;
	}
	
	public Object deepClone() {
		if (collator == null) {
			return new CommonComparator(null, isAsc, isNullLast);
		}
		
		try {
			return new CommonComparator((Collator)collator.clone(), isAsc, isNullLast);
		} catch (CloneNotSupportedException e) {
			throw new RQException(e);
		}
	}
	
	public int compare(Object o1, Object o2) {
		if (o1 == o2) {
			return 0;
		} else if (o1 == null) {
			return isNullLast ? 1 : (isAsc ? -1 : 1);
		} else if (o2 == null) {
			return isNullLast ? -1 : (isAsc ? 1 : -1);
		} else if (o1 instanceof String && o2 instanceof String) {
			if (collator == null) {
				if (isAsc) {
					return ((String)o1).compareTo((String)o2);
				} else {
					return ((String)o2).compareTo((String)o1);
				}
			} else {
				if (isAsc) {
					return collator.compare((String)o1, (String)o2);
				} else {
					return collator.compare((String)o2, (String)o1);
				}
			}
		} else if (o1 instanceof Number && o2 instanceof Number) {
			Number n1,n2;
			if (isAsc) {
				n1 = (Number)o1;
				n2 = (Number)o2;
			} else {
				n1 = (Number)o2;
				n2 = (Number)o1;
			}
			
			if (n1 instanceof Double || n1 instanceof Float) {
				if (n2 instanceof BigDecimal) {
					return new BigDecimal(n1.doubleValue()).compareTo((BigDecimal)n2);
				} else if (n2 instanceof BigInteger) {
					return new BigDecimal(n1.doubleValue()).compareTo(new BigDecimal((BigInteger)n2));
				} else {
					return Double.compare(n1.doubleValue(), n2.doubleValue());
				}
			} else if (n1 instanceof BigDecimal) {
				if (n2 instanceof BigDecimal) {
					return ((BigDecimal)n1).compareTo((BigDecimal)n2);
				} else if (n2 instanceof BigInteger) {
					return ((BigDecimal)n1).compareTo(new BigDecimal((BigInteger)n2));
				} else if (n2 instanceof Double || n2 instanceof Float) {
					return ((BigDecimal)n1).compareTo(new BigDecimal(n2.doubleValue()));
				} else {
					return ((BigDecimal)n1).compareTo(new BigDecimal(n2.longValue()));
				}
			} else if (n1 instanceof BigInteger) {
				if (n2 instanceof BigInteger) {
					return ((BigInteger)n1).compareTo((BigInteger)n2);
				} else if (n2 instanceof BigDecimal) {
					return new BigDecimal((BigInteger)n1).compareTo((BigDecimal)n2);
				} else if (n2 instanceof Double || n2 instanceof Float) {
					return new BigDecimal((BigInteger)n1).compareTo(new BigDecimal(n2.doubleValue()));
				} else {
					return ((BigInteger)n1).compareTo(BigInteger.valueOf(n2.longValue()));
				}
			} else {
				if (n2 instanceof BigDecimal) {
					return new BigDecimal(n1.longValue()).compareTo((BigDecimal)n2);
				} else if (n2 instanceof BigInteger) {
					return BigInteger.valueOf(n1.longValue()).compareTo((BigInteger)n2);
				} else if (n2 instanceof Double || n2 instanceof Float) {
					return Double.compare(n1.doubleValue(), n2.doubleValue());
				} else {
					return Long.compare(n1.longValue(), n2.longValue());
				}
			}
		} else if (o1 instanceof Date && o2 instanceof Date) {
			long t1 = ((Date)o1).getTime();
			long t2 = ((Date)o2).getTime();
			
			if (isAsc) {
				return (t1 < t2 ? -1 : (t1 > t2 ? 1 : 0));
			} else {
				return (t1 < t2 ? 1 : (t1 > t2 ? -1 : 0));
			}
		} else if (o1 instanceof Sequence && o2 instanceof Sequence) {
			return ((Sequence)o1).compareTo((Sequence)o2, this);
		} else if (o1 instanceof Boolean && o2 instanceof Boolean) {
			if (isAsc) {
				if (((Boolean)o1).booleanValue()) {
					return ((Boolean)o2).booleanValue() ? 0 : 1;
				} else {
					return ((Boolean)o2).booleanValue() ? -1 : 0;
				}
			} else {
				if (((Boolean)o1).booleanValue()) {
					return ((Boolean)o2).booleanValue() ? 0 : -1;
				} else {
					return ((Boolean)o2).booleanValue() ? 1 : 0;
				}
			}
		} else if (o1 instanceof BaseRecord && o2 instanceof BaseRecord) {
			// 为了保证group、id、join等能正常工作，但大小没意义
			if (isAsc) {
				return ((BaseRecord)o1).compareTo((BaseRecord)o2);
			} else {
				return ((BaseRecord)o2).compareTo((BaseRecord)o1);
			}
		} else if (o1 instanceof SerialBytes && o2 instanceof SerialBytes) {
			if (isAsc) {
				return ((SerialBytes)o1).compareTo((SerialBytes)o2);
			} else {
				return ((SerialBytes)o2).compareTo((SerialBytes)o1);
			}
		} else if (o1 instanceof byte[] && o2 instanceof byte[]) {
			if (isAsc) {
				return Variant.compareArrays((byte[])o1, (byte[])o2);
			} else {
				return Variant.compareArrays((byte[])o2, (byte[])o1);
			}
		} else {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("Variant2.illCompare", o1, o2,
					Variant.getDataType(o1), Variant.getDataType(o2)));
		}
	}
}
