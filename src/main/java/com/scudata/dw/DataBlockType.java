package com.scudata.dw;

import java.math.BigDecimal;
import java.util.Date;

import com.scudata.array.DoubleArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.LongArray;
import com.scudata.array.ObjectArray;
import com.scudata.common.StringUtils;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.dm.Table;

public class DataBlockType {
	private static final double AlignThreshold = 0.8;//当80%的数据是最长类型时，则都存为长类型，不再记录存储类型

	private static final int TYPE_LENGTH_8 = 0x01;
	private static final int TYPE_LENGTH_16 = 0x02;
	private static final int TYPE_LENGTH_32 = 0x04;
	private static final int TYPE_LENGTH_64 = 0x08;
	
	public static final int NULL = 0x00;
	
	public static final int INT = 0x10;
	public static final int INT8 = INT | TYPE_LENGTH_8;
	public static final int INT16 = INT | TYPE_LENGTH_16;
	public static final int INT32 = INT | TYPE_LENGTH_32;
	
	public static final int LONG = 0x20;
	public static final int LONG8 = LONG | TYPE_LENGTH_8;
	public static final int LONG16 = LONG | TYPE_LENGTH_16;
	public static final int LONG32 = LONG | TYPE_LENGTH_32;
	public static final int LONG64 = LONG | TYPE_LENGTH_64;
	
	public static final int DOUBLE = 0x30;
	public static final int DOUBLE32 = DOUBLE | TYPE_LENGTH_32;
	public static final int DOUBLE64 = DOUBLE | TYPE_LENGTH_64;
	
	public static final int DATE = 0x40;
	public static final int SEQUENCE = 0x42;
	public static final int TABLE = 0x43;
	public static final int RECORD = 0x44;
	public static final int DECIMAL = 0x45;
	public static final int SERIALBYTES = 0x46;
	public static final int STRING = 0x50;
	public static final int STRING_ASSIC = 0x51;//都是由ASSIC字符组成的字符串，且长度小于128
	
	public static final int CONST = 0x60;
	
	public static final int OBJECT = 0x7E;
	public static final int DICT = 0x7F;
	public static final int EMPTY = 0xFF;
	
	public static final int DICT_PUBLIC = 0;
	public static final int DICT_PRIVATE = 1;
	public static final int DICT_PRIVATE_CONST = 2;
	public static final int INC_BLOCK = 3; //递增块
	
	public static final int MAX_DICT_NUMBER = 127;
	
	private int type;//块数据类型（含长度信息）
	private boolean hasNull;
	private Sequence dict;
	private int dataType;//数据类型 (字典列，Object列时使用)
	
	public DataBlockType(int type, boolean hasNull) {
		this.setType(type);
		this.setDataType(type);
		this.setHasNull(hasNull);
	}
	
	public DataBlockType(int type, int dataType, boolean hasNull) {
		this.setType(type);
		this.setDataType(dataType);
		this.setHasNull(hasNull);
	}
	
	public DataBlockType(int type, Sequence dict) {
		this.setType(type);
		this.setDict(dict);
	}

	/**
	 * 计算data块的数据类型，精确到长度
	 * @param data 数据序列
	 * @param col 列号
	 * @param start 开始位置
	 * @param end 结束位置
	 * @return 
	 */
	public static DataBlockType getDataBlockType(Sequence data, int col, int start, int end) {
		Sequence seq = data.fieldValues(col);
		Object obj = null;
		for (int i = start; i <= end; ++i) {
			obj = seq.get(i);
			if (obj != null) break;
		}
		
		if (obj == null) {
			//整个块都是null
			seq = new Sequence();
			seq.add(null);
			return new DataBlockType(DICT, seq);
			//return new DataBlockType(NULL, true);
		}
		
		DataBlockType type = null;
		if (obj instanceof Integer) {
			type = checkIntBlockType(seq, start, end);
		} else if (obj instanceof Long) {
			type = checkLongBlockType(seq, start, end);
		} else if (obj instanceof Double) {
			type = checkDoubleBlockType(seq, start, end);
		} else if (obj instanceof java.util.Date) {
			type = checkDateBlockType(seq, start, end);
		} else if (obj instanceof String) {
			type = checkStringBlockType(seq, start, end);
		} else if (obj instanceof Table) {
			type = checkTableBlockType(seq, start, end);
		} else if (obj instanceof Sequence) {
			type = checkSequenceBlockType(seq, start, end);
		} else if (obj instanceof BaseRecord) {
			type = checkRecordBlockType(seq, start, end);
		} else if (obj instanceof BigDecimal) {
			type = checkDecimalBlockType(seq, start, end);
		} else if (obj instanceof SerialBytes) {
			type = checkSerialBytesBlockType(seq, start, end);
		} else {
			type = new DataBlockType(OBJECT, false);
		}
		
		if (type.type == INT8 || type.type == LONG8 || type.type == INC_BLOCK) {
			return type;//长度8时，或者是递增块时不用检查字典
		}
		
		DataBlockType dictType = checkDict(seq, start, end);
		if (dictType != null) {
			dictType.setDataType(type.getType());
			dictType.setHasNull(type.hasNull);
			return dictType;
		} else {
			return type;
		}
	}
	
	/**
	 * 计算序列的数据类型，精确到长度
	 * @param seq 数据序列
	 * @param col 列号
	 * @param start 开始位置
	 * @param end 结束位置
	 * @return 
	 */
	public static int getSequenceDataType(Sequence seq, int start, int end) {
		IArray mems = seq.getMems();
		if (mems instanceof IntArray) {
			return checkIntBlockType16or32(seq, start, end);
		} else if (mems instanceof LongArray) {
			return checkLongBlockType64(seq, start, end);
		} else if (mems instanceof DoubleArray) {
			return checkDoubleBlockType64(seq, start, end);
		} else {
			return NULL;
		}
	}
	
	private static DataBlockType checkDict(Sequence data, int start, int end) {
		Sequence seq;
		try {
			seq = (Sequence) data.id("u");
		} catch (Exception e) {
			return null;
		}
		int len = seq.length();
		if (len > MAX_DICT_NUMBER) {
			return null;
		} else {
			return new DataBlockType(DICT, seq);
		}
	}
	
	private static int getIntTypeLength(int v) {
		if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
			return TYPE_LENGTH_8;
		} else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
			return TYPE_LENGTH_16;
		} else {
			return TYPE_LENGTH_32;
		}
	}
	
	private static int getLongTypeLength(long v) {
		if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
			return TYPE_LENGTH_8;
		} else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
			return TYPE_LENGTH_16;
		} else if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
			return TYPE_LENGTH_32;
		} else {
			return TYPE_LENGTH_64;
		}
	}

	private static int getDoubleLength(double d, long v, int scale) {
		if (v <= 0x3FFF) {
			return TYPE_LENGTH_16;
		} else if (v <= 0x3FFFFFFF) {
			return TYPE_LENGTH_32;
		} else {
			return TYPE_LENGTH_64;
		}
	}
	
	private static int getDoubleTypeLength(double d) {
		if (d > 0.0 && d <= 0x3FFFFFFF) {
			double v = Math.ceil(d);
			if (v - d < BufferWriter.MINFLOAT) {
				long l = (long)v;
				if (l % 100 == 0) {
					return getDoubleLength(d, l / 100, 0xC0);
				} else {
					return getDoubleLength(d, (long)v, 0x00);
				}
			} else {
				double d1 = d * 100;
				v = Math.ceil(d1);
				if (v - d1 < BufferWriter.MINFLOAT) {
					return getDoubleLength(d, (long)v, 0x40);
				} else {
					d1 = d * 10000;
					v = Math.ceil(d1);
					if (v - d1 < BufferWriter.MINFLOAT) {
						return getDoubleLength(d, (long)v, 0x80);
					} else {
						return TYPE_LENGTH_64;
					}
				}
			}
		} else if (d == 0.0) {
			return TYPE_LENGTH_16;
		} else {
			return TYPE_LENGTH_64;
		}
	}
	
	private static DataBlockType checkIntBlockType(Sequence data, int start, int end) {
		int typeLength = 0;
		int maxTypeLength = 0;
		int maxTypeLengthCount = 0;
		boolean hasNull = false;
		boolean increasing = true;
		
		//检查是否递增
		if (end - start > 16) {
			Object obj1 = data.get(start);
			Object obj2 = data.get(start + 1);
			if (obj1 != null && obj2 != null && obj1 instanceof Integer && obj2 instanceof Integer) {
				int lastVal = (Integer) obj2;
				int stepVal = (Integer) obj2 - (Integer) obj1;
				for (int i = start + 2; i <= end; ++i) {
					Object obj = data.get(i);
					if (obj == null || !(obj instanceof Integer)) {
						increasing = false;
						break;
					}
					int val = (Integer) obj;
					if (stepVal == val - lastVal) {
						lastVal = val;	
					} else {
						increasing = false;
						break;
					}
				}
			} else {
				increasing = false;
			}
			
			if (increasing) {
				return new DataBlockType(INC_BLOCK, INT, false);
			}
		}
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof Integer)) {
				return new DataBlockType(OBJECT, hasNull);
			}
			
			Integer val = (Integer) obj;
			int tl = getIntTypeLength(val);
			typeLength |= tl;
			if (tl > maxTypeLength) {
				maxTypeLength = tl;
				maxTypeLengthCount = 0;
			}
			if (tl  == maxTypeLength) {
				maxTypeLengthCount++;
			}
		
		}
		
		if (typeLength == TYPE_LENGTH_8) {
			return new DataBlockType(INT8, hasNull);
		} else if (typeLength == TYPE_LENGTH_16) {
			return new DataBlockType(INT16, hasNull);
		} else if (typeLength == TYPE_LENGTH_32) {
			return new DataBlockType(INT32, hasNull);
		} else {
			/**
			 * 如果最长的类型占大多数
			 */
			double sum = end - start + 1;
			if ((maxTypeLengthCount/ sum) >= AlignThreshold) {
				return new DataBlockType(INT | maxTypeLength, hasNull);
			}
		}
		
		//默认返回INT混合类型
		return new DataBlockType(INT, hasNull);
	}
	
	private static DataBlockType checkLongBlockType(Sequence data, int start, int end) {
		int typeLength = 0;
		int maxTypeLength = 0;
		int maxTypeLengthCount = 0;
		boolean hasNull = false;
		boolean Increasing = true;
		
		//检查是否递增
		if (end - start > 16) {
			Object obj1 = data.get(start);
			Object obj2 = data.get(start + 1);
			if (obj1 != null && obj2 != null && obj1 instanceof Long && obj2 instanceof Long) {
				long lastVal = (Long) obj2;
				long stepVal = (Long) obj2 - (Long) obj1;
				for (int i = start + 2; i <= end; ++i) {
					Object obj = data.get(i);
					if (obj == null || !(obj instanceof Long)) {
						Increasing = false;
						break;
					}
					Long val = (Long) obj;
					if (stepVal == val - lastVal) {
						lastVal = val;	
					} else {
						Increasing = false;
						break;
					}
				}
			} else {
				Increasing = false;
			}
			
			if (Increasing) {
				return new DataBlockType(INC_BLOCK, LONG, false);
			}
		}
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (obj instanceof Integer) {
				obj = ((Integer)obj).longValue();
				data.set(i, obj);
			}
			if (!(obj instanceof Long)) {
				return new DataBlockType(OBJECT, hasNull);
			}
			
			Long val = (Long) obj;
			int tl = getLongTypeLength(val);
			typeLength |= tl;
			if (tl > maxTypeLength) {
				maxTypeLength = tl;
				maxTypeLengthCount = 0;
			}
			if (tl  == maxTypeLength) {
				maxTypeLengthCount++;
			}
		
		}
		
		if (typeLength == TYPE_LENGTH_8) {
			return new DataBlockType(LONG8, hasNull);
		} else if (typeLength == TYPE_LENGTH_16) {
			return new DataBlockType(LONG16, hasNull);
		} else if (typeLength == TYPE_LENGTH_32) {
			return new DataBlockType(LONG32, hasNull);
		} else if (typeLength == TYPE_LENGTH_64) {
			return new DataBlockType(LONG64, hasNull);
		} else {
			/**
			 * 如果最长的类型占大多数
			 */
			double sum = end - start + 1;
			if ((maxTypeLengthCount/ sum) >= AlignThreshold) {
				return new DataBlockType(LONG | maxTypeLength, hasNull);
			}
		}
		
		//默认返回LONG混合类型
		return new DataBlockType(LONG, hasNull);
	}
	
	private static DataBlockType checkDoubleBlockType(Sequence data, int start, int end) {
		int typeLength = 0;
		int typeLengthCount = 0;
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof Double)) {
				return new DataBlockType(OBJECT, hasNull);
			}
			
			Double val = (Double) obj;
			int tl = getDoubleTypeLength(val);
			typeLength |= tl;
			if (tl  == TYPE_LENGTH_64) {
				typeLengthCount++;
			}
		}
		
		if (typeLength == TYPE_LENGTH_64) {
			return new DataBlockType(DOUBLE64, hasNull);
		} else {
			/**
			 * 如果DOUBLE64占大多数
			 */
			double sum = end - start + 1;
			if ((typeLengthCount / sum) >= AlignThreshold) {
				return new DataBlockType(DOUBLE64, hasNull);
			}
		}
		
		//默认返回DOUBLE混合类型
		return new DataBlockType(DOUBLE, hasNull);
	}
	
	private static DataBlockType checkDateBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof java.util.Date)) {
				return new DataBlockType(OBJECT, hasNull);
			}
		}
		return new DataBlockType(DATE, hasNull);
	}
	
	private static DataBlockType checkStringBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		boolean isAssic = true;
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				isAssic = false;
				continue;
			}
			if (!(obj instanceof String)) {
				return new DataBlockType(OBJECT, hasNull);
			}
			
			if (isAssic) {
				String str = (String) obj;
				isAssic = StringUtils.isAssicString(str) && str.length() < 128;
			}
			
		}
		if (isAssic)
			return new DataBlockType(STRING_ASSIC, hasNull);
		else
			return new DataBlockType(STRING, hasNull);
	}
	
	private static DataBlockType checkSequenceBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof Sequence)) {
				return new DataBlockType(OBJECT, hasNull);
			}
		}
		return new DataBlockType(OBJECT, SEQUENCE, hasNull);
	}
	
	private static DataBlockType checkTableBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof Table)) {
				return checkSequenceBlockType(data, start, end);
			}
		}
		return new DataBlockType(OBJECT, TABLE, hasNull);
	}
	
	private static DataBlockType checkRecordBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof BaseRecord)) {
				return new DataBlockType(OBJECT, hasNull);
			}
		}
		return new DataBlockType(OBJECT, RECORD, hasNull);
	}
	
	private static DataBlockType checkDecimalBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof BigDecimal)) {
				return new DataBlockType(OBJECT, hasNull);
			}
		}
		return new DataBlockType(OBJECT, DECIMAL, hasNull);
	}
	
	private static DataBlockType checkSerialBytesBlockType(Sequence data, int start, int end) {
		boolean hasNull = false;
		
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				hasNull = true;
				continue;
			}
			if (!(obj instanceof SerialBytes)) {
				return new DataBlockType(OBJECT, hasNull);
			}
		}
		return new DataBlockType(SERIALBYTES, hasNull);
	}
	
	public boolean isHasNull() {
		return hasNull;
	}

	public void setHasNull(boolean hasNull) {
		this.hasNull = hasNull;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Sequence getDict() {
		return dict;
	}

	public void setDict(Sequence dict) {
		this.dict = dict;
	}

	public int getDataType() {
		return dataType;
	}

	public void setDataType(int dataType) {
		this.dataType = dataType;
	}
	
	public static String getTypeName(int type) {
		switch (type) {
		case INT:
		case INT8:
		case INT16:
		case INT32:
			return "Integer";
		case LONG:
		case LONG8:
		case LONG16:
		case LONG32:
		case LONG64:
			return "Long";
		case DOUBLE:
		case DOUBLE64:
			return "Double";
		case DATE:
			return "Date";
		case STRING:
		case STRING_ASSIC:
			return "String";
		case SEQUENCE:
			return "Sequence";
		case TABLE:
			return "Table";
		case RECORD:
			return "Record";
		case DECIMAL:
			return "BigDecimal";
		case EMPTY:
			return "Empty";
		case SERIALBYTES:
			return "SerialBytes";
		case 0:
			return "Unknown";
		default:
			return "Object : " + type;	
		}
	}
	
	public static String getTypeLen(int type) {
		switch (type) {
		case INT8:
		case LONG8:
			return "8";
		case INT16:
		case LONG16:
			return "16";
		case INT32:
		case LONG32:
			return "32";
		case LONG64:
		case DOUBLE64:
			return "64";
		case INT:
			return "VAR";
		case EMPTY:
			return "0";
		case 0:
			return "8";
		case LONG:
		case DOUBLE:
		case DATE:
			return "VAR";	
		case SERIALBYTES:
			return "128";
		default:
			return "";	
		}
	}
	
	/**
	 * 把字典序列转换成数组 （基础类型时，结果数组位置0放的是null对象的位置） 
	 * @param dict
	 * @param dataType
	 * @return 
	 */
	public static Object dictToArray(Sequence dict, int dataType) {
		Object resultArray;
		int size = dict.length();
		
		switch (dataType) {
		case DataBlockType.INT:
		case DataBlockType.INT8:
		case DataBlockType.INT16:
		case DataBlockType.INT32:
			int[] intArray = new int[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				if (obj != null) {
					intArray[i] = ((Number)obj).intValue();
				} else {
					intArray[0] = i;
				}
			}
			resultArray = intArray;
			break;
		case DataBlockType.LONG:
		case DataBlockType.LONG8:
		case DataBlockType.LONG16:
		case DataBlockType.LONG32:
		case DataBlockType.LONG64:
			long[] longArray = new long[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				if (obj != null) {
					longArray[i] = ((Number)obj).longValue();
				} else {
					longArray[0] = i;
				}
			}
			resultArray = longArray;
			break;
		case DataBlockType.DOUBLE:
		case DataBlockType.DOUBLE64:
			double[] doubleArray = new double[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				if (obj != null) {
					doubleArray[i] = ((Number)obj).doubleValue();
				} else {
					doubleArray[0] = i;
				}
			}
			resultArray = doubleArray;
			break;
		case DataBlockType.DATE:
			Date[] dateArray = new Date[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				dateArray[i] = (Date) obj;
			}
			resultArray = dateArray;
			break;
		case DataBlockType.STRING:
		case DataBlockType.STRING_ASSIC:
			String[] strArray = new String[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				strArray[i] = (String) obj;
			}
			resultArray = strArray;
			break;
		case DataBlockType.SERIALBYTES:
			SerialBytes[] serArray = new SerialBytes[size + 1];
			for (int i = 1; i <= size; i++) {
				Object obj = dict.getMem(i);
				serArray[i] = (SerialBytes) obj;
			}
			resultArray = serArray;
			break;
		default:
			return ((ObjectArray)dict.getMems()).getDatas();
		}
		return resultArray;
	}
	

	/**
	 * 检查是否是double32
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
//	private static int checkDoubleBlockType32(Sequence data, int start, int end) {
//		for (int i = start; i <= end; ++i) {
//			Object obj = data.get(i);
//			if (obj == null) {
//				return NULL;
//			}
//			if (!(obj instanceof Double)) {
//				return NULL;
//			}
//			
//			Double val = (Double) obj;
//			int tl = getDoubleTypeLength(val);
//			if (tl  == TYPE_LENGTH_64) {
//				return NULL;
//			}
//		}
//		return DOUBLE32;
//	}
	
	/**
	 * 检查是否是double64
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	private static int checkDoubleBlockType64(Sequence data, int start, int end) {
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				return NULL;
			}
			if (!(obj instanceof Double)) {
				return NULL;
			}
		}
		return DOUBLE64;
	}
	
	/**
	 * 检查是否是long64
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	private static int checkLongBlockType64(Sequence data, int start, int end) {
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				return NULL;
			}
			if (!(obj instanceof Long)) {
				return NULL;
			}
		}
		return LONG64;
	}
	
	/**
	 * 检查是否是int16或int32
	 * @param data
	 * @param start
	 * @param end
	 * @return
	 */
	private static int checkIntBlockType16or32(Sequence data, int start, int end) {
		boolean has32 = false;
		for (int i = start; i <= end; ++i) {
			Object obj = data.get(i);
			if (obj == null) {
				return NULL;
			}
			if (!(obj instanceof Integer)) {
				return NULL;
			}
			
			Integer val = (Integer) obj;
			int tl = getIntTypeLength(val);
			if (tl == TYPE_LENGTH_64) {
				return NULL;
			} else if (tl == TYPE_LENGTH_32) {
				has32 = true;
			}
		}
		
		if (has32) 
			return INT32;
		else
			return INT16;
	}
}
