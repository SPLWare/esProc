package com.scudata.dw;

import java.io.IOException;
import java.util.Date;

import com.scudata.array.BoolArray;
import com.scudata.array.DateArray;
import com.scudata.array.DoubleArray;
import com.scudata.array.IArray;
import com.scudata.array.IntArray;
import com.scudata.array.LongArray;
import com.scudata.array.ObjectArray;
import com.scudata.array.SerialBytesArray;
import com.scudata.array.StringArray;
import com.scudata.common.RQException;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;

/**
 * 新版本组表时的读取处理类
 * @author LW
 *
 */
public class PureBufferReader extends BufferReader {
	private int blockType;//块类型
	private int dataType;//数据类型（只有字典块类型时才有意义）
	private Sequence dict;
	private int[] pos;
	private Object constValue;//  (递增块时是first value)
	private int step;//递增块step
	private boolean[] isNull;
	private int dataIndex = 0;//用于维护isNull
	
	public PureBufferReader(StructManager structManager, byte[] buffer, int recordCount, Sequence columnDict) {
		super(structManager, buffer);
		init(columnDict, recordCount);
	}
	
	public PureBufferReader(StructManager structManager, byte[] buffer, int index, int count, int recordCount, Sequence columnDict) {
		super(structManager, buffer, index, count);
		init(columnDict, recordCount);
	}
	
	public static boolean canUseBufferReader(byte[] buffer, int index) {
		int blockType = buffer[index];
		switch (blockType) {
		case DataBlockType.OBJECT:
		case DataBlockType.DATE:
		case DataBlockType.STRING:
			return true;
		default:
			return false;
		}
	}
	
	private void init(Sequence columnDict, int recordCount) {
		int dataSize = 0;
		try {
			blockType = readByte();
			switch (blockType) {
			case DataBlockType.NULL:
				blockType = DataBlockType.CONST;
				constValue = null;
				return;
			case DataBlockType.INT8:
			case DataBlockType.LONG8:
				dataSize = 1;
				break;
			case DataBlockType.INT16:
			case DataBlockType.LONG16:
				index++;
				dataSize = 2;
				break;
			case DataBlockType.INT32:
			case DataBlockType.LONG32:
				index += 3;
				dataSize = 4;
				break;
			case DataBlockType.LONG64:
			case DataBlockType.DOUBLE64:
				index += 7;
				dataSize = 8;
				break;
			case DataBlockType.STRING_ASSIC:
				int idx = this.index;
				byte[] buf = this.buffer;
				int[] len = new int[recordCount + 1];
				for (int i = 1; i <= recordCount; i++) {
					len[i] = buf[idx++];
				}
				this.index = idx;
				this.pos = len;
				return;
			case DataBlockType.SERIALBYTES:
				dataSize = 16;
				break;
			case DataBlockType.DICT:
				if (read() == DataBlockType.DICT_PUBLIC) {
					dict = columnDict;
				} else {
					dict = (Sequence) super.readObject();
				}
				dataType = read();
				boolean isConst = readBoolean();
				if (isConst) {
					blockType = DataBlockType.CONST;
					int pos = read();
					constValue = dict.get(pos);
					return;
				}
				int index = this.index;
				byte[] buffer = this.buffer;
				int[] pos = new int[recordCount + 1];
				for (int i = 1; i <= recordCount; i++) {
					pos[i] = buffer[index++];
				}
				this.pos = pos;
				return;
			case DataBlockType.INC_BLOCK:
				constValue = super.readObject();
				step = super.readInt();
				if (constValue instanceof Integer) {
					dataType = DataBlockType.INT;
				} else {
					dataType = DataBlockType.LONG;
				}
				return;
			case DataBlockType.INT:
			case DataBlockType.LONG:
			case DataBlockType.DOUBLE:
				return;
			default:
				throw new RuntimeException();
			}
		} catch (IOException e) {
			throw new RQException(e);
		}
		

		int index = this.index + (recordCount) * dataSize;
		byte[] buffer = this.buffer;
		if (buffer[index++] != 0) {
			isNull = new boolean[recordCount + 1];
			for (int i = 1; i <= recordCount; i++) {
				isNull[i] = buffer[index++] != 0;
			}
		}
	}
	
	private int readLittleEndianInt16() {
		byte[] in = buffer;
		int offset = index;
		index += 2;
		return ((in[offset + 1]) << 8) | (in[offset] & 0xff);
	}
	
	private int readLittleEndianInt32() {
		byte[] in = buffer;
		int offset = index;
		index += 4;
		return (in[offset + 3] << 24) + ((in[offset + 2] & 0xff) << 16) +
		((in[offset + 1] & 0xff) << 8) + (in[offset] & 0xff);
	}
	
	private long readLittleEndianLong64() {
		byte[] in = buffer;
		int offset = index;
		index += 8;
		return (((long)in[offset + 7] << 56) +
				((long)(in[offset + 6] & 0xff) << 48) +
				((long)(in[offset + 5] & 0xff) << 40) +
				((long)(in[offset + 4] & 0xff) << 32) +
				((long)(in[offset + 3] & 0xff) << 24) +
				((in[offset + 2] & 0xff) << 16) +
				((in[offset + 1] & 0xff) <<  8) +
				(in[offset] & 0xff));
	}
	
	private double readLittleEndianLongDouble64() {
		byte[] in = buffer;
		int offset = index;
		index += 8;
		long v = (((long)in[offset + 7] << 56) +
				((long)(in[offset + 6] & 0xff) << 48) +
				((long)(in[offset + 5] & 0xff) << 40) +
				((long)(in[offset + 4] & 0xff) << 32) +
				((long)(in[offset + 3] & 0xff) << 24) +
				((in[offset + 2] & 0xff) << 16) +
				((in[offset + 1] & 0xff) <<  8) +
				(in[offset] & 0xff));
		return Double.longBitsToDouble(v);
	}
	
	public Object readObject() throws IOException {
		switch (blockType) {
		case DataBlockType.CONST:
			return constValue;
		case DataBlockType.INT8:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index++;
					return null;
				}
			}
			return (int)readByte();
		case DataBlockType.LONG8:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index++;
					return null;
				}
			}
			return (long)readByte();
		case DataBlockType.INT16:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 2;
					return null;
				}
			}
			return readLittleEndianInt16();
		case DataBlockType.LONG16:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 2;
					return null;
				}
			}
			return (long)readLittleEndianInt16();
		case DataBlockType.INT32:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 4;
					return null;
				}
			}
			return readLittleEndianInt32();
		case DataBlockType.LONG32:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 4;
					return null;
				}
			}
			return (long)readLittleEndianInt32();
		case DataBlockType.LONG64:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 8;
					return null;
				}
			}
			return readLittleEndianLong64();
		case DataBlockType.DOUBLE64:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 8;
					return null;
				}
			}
			return readLittleEndianLongDouble64();
		case DataBlockType.SERIALBYTES:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 16;
					return null;
				}
			}
			long v1 = readLong64();
			long v2 = readLong64();
			return new SerialBytes(v1, v2);
		case DataBlockType.STRING_ASSIC:
			dataIndex++;
			int len = pos[dataIndex];
			if (len == 0) {
				return new String();
			} else {
				byte[] ba = new byte[len];
				super.read(ba);
				return new String(ba);	
			}
		case DataBlockType.DICT:
			dataIndex++;
			return dict.get(pos[dataIndex]);
		case DataBlockType.INC_BLOCK:
			if (this.dataType == DataBlockType.INT) {
				int value = (Integer)constValue + dataIndex * step;
				dataIndex++;
				return value;
			} else {
				long value = (Long)constValue + dataIndex * step;
				dataIndex++;
				return value;
			}
		case DataBlockType.INT:
		case DataBlockType.LONG:
		case DataBlockType.DOUBLE:
			return super.readObject();
		default:
			throw new RuntimeException();
		}
	}
	
	public void skipObject() throws IOException {
		switch (blockType) {
		case DataBlockType.CONST:
			return;
		case DataBlockType.INT8:
		case DataBlockType.LONG8:
			if (isNull != null) {
				dataIndex++;
			}
			index++;
			return;
		case DataBlockType.INT16:
		case DataBlockType.LONG16:
			if (isNull != null) {
				dataIndex++;
			}
			index += 2;
			return;
		case DataBlockType.INT32:
		case DataBlockType.LONG32:
			if (isNull != null) {
				dataIndex++;
			}
			index += 4;
			return;
		case DataBlockType.LONG64:
		case DataBlockType.DOUBLE64:
			if (isNull != null) {
				dataIndex++;
			}
			index += 8;
			return;
		case DataBlockType.SERIALBYTES:
			if (isNull != null) {
				dataIndex++;
			}
			index += 16;
			return;
		case DataBlockType.STRING_ASSIC:
			dataIndex++;
			int len = pos[dataIndex];
			if (len != 0) {
				index += len;
			}
			return;
		case DataBlockType.INC_BLOCK:
		case DataBlockType.DICT:
			dataIndex++;
			return;
		case DataBlockType.INT:
		case DataBlockType.LONG:
		case DataBlockType.DOUBLE:
			super.skipObject();
			return;
		default:
			throw new RuntimeException();
		}
	}
	
	/**
	 * 读取一个对象到array的指定位置
	 * @param array
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public void readObject(IArray array, int index) throws IOException {
		switch (blockType) {
		case DataBlockType.CONST:
			array.set(index, constValue);
			return;
		case DataBlockType.INT8:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index++;
					array.set(index, null);
					return;
				}
			}
			((IntArray)array).setInt(index, (int)readByte());
			return;
			
		case DataBlockType.LONG8:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index++;
					array.set(index, null);
					return ;
				}
			}
			((LongArray)array).setLong(index, (long)readByte());
		case DataBlockType.INT16:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 2;
					array.set(index, null);
					return ;
				}
			}
			((IntArray)array).setInt(index, readLittleEndianInt16());
			return;
		case DataBlockType.LONG16:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 2;
					array.set(index, null);
					return ;
				}
			}
			((LongArray)array).setLong(index, readLittleEndianInt16());
			return;
		case DataBlockType.INT32:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 4;
					array.set(index, null);
					return ;
				}
			}
			((IntArray)array).setInt(index, readLittleEndianInt32());
			return;
		case DataBlockType.LONG32:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 4;
					array.set(index, null);
					return ;
				}
			}
			((LongArray)array).setLong(index, readLittleEndianInt32());
			return;
		case DataBlockType.LONG64:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 8;
					array.set(index, null);
					return ;
				}
			}
			((LongArray)array).setLong(index, readLittleEndianLong64());
			return;
		case DataBlockType.DOUBLE64:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 8;
					array.set(index, null);
					return ;
				}
			}
			((DoubleArray)array).setDouble(index, readLittleEndianLongDouble64());
			return;
		case DataBlockType.SERIALBYTES:
			if (isNull != null) {
				dataIndex++;
				if (isNull[dataIndex]) {
					index += 16;
					array.set(index, null);
					return;
				}
			}
			long v1 = readLong64();
			long v2 = readLong64();
			((SerialBytesArray)array).set(index, new SerialBytes(v1, v2));
			return;
		case DataBlockType.STRING_ASSIC:
			dataIndex++;
			int len = pos[dataIndex];
			if (len == 0) {
				array.set(index, new String());
			} else {
				byte[] ba = new byte[len];
				super.read(ba);
				array.set(index, new String(ba));	
			}
			return;
		case DataBlockType.DICT:
			dataIndex++;
			array.set(index, dict.get(pos[dataIndex]));
			return;
		case DataBlockType.INC_BLOCK:
			if (this.dataType == DataBlockType.INT) {
				int value = (Integer)constValue + dataIndex * step;
				dataIndex++;
				array.set(index, value);
			} else {
				long value = (Long)constValue + dataIndex * step;
				dataIndex++;
				array.set(index, value);
			}
			return;
		case DataBlockType.INT:
			if (super.isNull()) {
				super.readObject();
				array.set(index, null);
			} else {
				((IntArray)array).setInt(index, super.readBaseInt());
			}
			return;
		case DataBlockType.LONG:
			if (super.isNull()) {
				super.readObject();
				array.set(index, null);
			} else {
				((LongArray)array).setLong(index, super.readBaseLong());
			}
			return;
		case DataBlockType.DOUBLE:
			if (super.isNull()) {
				super.readObject();
				array.set(index, null);
			} else {
				((DoubleArray)array).setDouble(index, super.readBaseDouble());
			}
			return;
		default:
			array.set(index, super.readObject());
		}
	}
	
	/**
	 * 根据块类型获得一个数组
	 * @param count
	 * @return
	 */
	public IArray getEmptyArray(int count) {
		if (blockType == DataBlockType.DICT)
			return getArray(dataType, count);
		else
			return getArray(blockType, count);
	}
	
	private IArray getArray(int dataType, int count) {
		switch (dataType) {
		case DataBlockType.CONST:
			return newArray(constValue, count);
		case DataBlockType.INT:
		case DataBlockType.INT8:
		case DataBlockType.INT16:
		case DataBlockType.INT32:
			return new IntArray(count);
		case DataBlockType.LONG:
		case DataBlockType.LONG8:
		case DataBlockType.LONG16:
		case DataBlockType.LONG32:
		case DataBlockType.LONG64:
			return new LongArray(count);
		case DataBlockType.DOUBLE:
		case DataBlockType.DOUBLE64:
			return new DoubleArray(count);
		case DataBlockType.SERIALBYTES:
			return new SerialBytesArray(count);
		case DataBlockType.STRING:
		case DataBlockType.STRING_ASSIC:
			return new StringArray(count);
		case DataBlockType.DATE:
			return new DateArray(count);
		case DataBlockType.INC_BLOCK:
			return getArray(this.dataType, count);
		case DataBlockType.DICT:
		default:
			throw new RuntimeException();
		}
	}
	
	private static IArray newArray(Object value, int capacity) {
		if (value instanceof Integer) {
			IntArray result = new IntArray(capacity);
			result.pushInt(((Integer)value).intValue());
			return result;
		} else if (value instanceof Long) {
			LongArray result = new LongArray(capacity);
			result.pushLong(((Long)value).longValue());
			return result;
		} else if (value instanceof Double) {
			DoubleArray result = new DoubleArray(capacity);
			result.pushDouble(((Double)value).doubleValue());
			return result;
		} else if (value instanceof Date) {
			DateArray result = new DateArray(capacity);
			result.pushDate((Date)value);
			return result;
		} else if (value instanceof SerialBytes) {
			SerialBytesArray result = new SerialBytesArray(capacity);
			result.push((SerialBytes)value);
			return result;
		} else if (value instanceof String) {
			StringArray result = new StringArray(capacity);
			result.pushString((String)value);
			return result;
		} else if (value instanceof Boolean) {
			BoolArray result = new BoolArray(capacity);
			result.pushBool(((Boolean)value).booleanValue());
			return result;
		} else {
			ObjectArray result = new ObjectArray(capacity);
			result.push(value);
			return result;
		}
	}
}
