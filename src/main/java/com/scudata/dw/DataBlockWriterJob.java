package com.scudata.dw;

import java.io.IOException;

import com.scudata.array.IArray;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.SerialBytes;
import com.scudata.thread.Job;
import com.scudata.util.Variant;

public class DataBlockWriterJob  extends Job {
	//private boolean isDim;
	private BufferWriter bufferWriter;
	private Sequence data;
	private Sequence dict;
	private int col;
	private int start;
	private int end;
	private Object[] maxValues;
	private Object[] minValues;
	private Object[] startValues;
	private int[] dataType;
	
	public DataBlockWriterJob(BufferWriter bufferWriter, Sequence data, Sequence dict, int col,
			int start, int end, Object[] maxValues, Object[] minValues, Object[] startValues, int[] dataType) {
		//this.isDim = true;
		this.bufferWriter = bufferWriter;
		this.data = data;
		this.dict = dict;
		this.col = col;
		this.start = start;
		this.end = end;
		this.maxValues = maxValues;
		this.minValues = minValues;
		this.startValues = startValues;
		this.dataType = dataType;
	}
	
	public void run() {
		try {
			writeDataBlock(bufferWriter, data, dict, col, start, end, 
						maxValues, minValues, startValues, dataType);
		} catch (IOException e) {
			throw new RQException(e);
		}
	}
	
	/**
	 * 把data序列的指定范围的数据写出到byte[]
	 * @param data 数据序列
	 * @param dict 全局字典
	 * @param col 列号
	 * @param start 开始位置
	 * @param end 结束位置
	 * @return 
	 * @throws IOException
	 */
	private static void writeDataBlock(BufferWriter bufferWriter, Sequence data, Sequence dict, 
			int col, int start, int end, int[] dataType) throws IOException {
		BaseRecord r;
		boolean writeNull = false;
		boolean isConst = false;
		
		IArray mems = data.getMems();
		DataBlockType blockType = DataBlockType.getDataBlockType(data, col, start, end);
		dataType[col] = blockType.getDataType();
		
		if (blockType == null || blockType.getType() == DataBlockType.OBJECT) {
			bufferWriter.write(DataBlockType.OBJECT);
			for (int i = start; i <= end; ++i) {
				r = (BaseRecord) mems.get(i);
				Object[] vals = r.getFieldValues();
				Object obj = vals[col];
				bufferWriter.writeObject(obj);
			}
			bufferWriter.flush();
		} else if (blockType.getType() == DataBlockType.DICT) {
			bufferWriter.write(DataBlockType.DICT);
			Sequence seq = blockType.getDict();
			if (checkDict(dict, seq)) {
				bufferWriter.write(DataBlockType.DICT_PUBLIC);
				seq = dict;
			} 
			else 
			{
				bufferWriter.write(DataBlockType.DICT_PRIVATE);
				bufferWriter.writeObject(seq);
				bufferWriter.flush();
			}
			bufferWriter.write(blockType.getDataType());
			
			byte[] pos = new byte[end - start + 1];
			int offset = 0;
			for (int i = start; i <= end; ++i) {
				r = (BaseRecord) mems.get(i);
				Object obj = r.getNormalFieldValue(col);
				int idx = seq.firstIndexOf(obj);
				pos[offset++] = (byte) idx;
			}
			isConst = checkSame(pos);
			bufferWriter.writeBoolean(isConst);
			if (!isConst) {
				bufferWriter.write(pos);
			} else {
				pos = new byte[]{pos[0]};
				bufferWriter.write(pos);
			}
			//writeNull = blockType.isHasNull();
		} else {
			int type = blockType.getType();
			bufferWriter.write(type);
			
			switch (type) {
			case DataBlockType.NULL:
			case DataBlockType.INT:
			case DataBlockType.LONG:
			case DataBlockType.DOUBLE:
			case DataBlockType.DATE:
			case DataBlockType.OBJECT:
			default:
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					bufferWriter.writeObject(obj);
				}
				bufferWriter.flush();
				break;
			case DataBlockType.INT8:
			case DataBlockType.LONG8:
				writeNull = true;
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.write(0);
					} else {
						bufferWriter.write(((Number) obj).intValue());
					}
				}
				break;
			case DataBlockType.INT16:
				writeNull = true;
				bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.writeLittleEndianShort(0);
					} else {
						bufferWriter.writeLittleEndianShort((Integer) obj);
					}
				}
				break;
			case DataBlockType.INT32:
				writeNull = true;
				for (int i = 0; i < 3; i++)
					bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.writeLittleEndianInt(0);
					} else {
						bufferWriter.writeLittleEndianInt((Integer) obj);
					}
				}
				break;
			case DataBlockType.LONG16:
				writeNull = true;
				bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.writeLittleEndianShort(0);
					} else {
						bufferWriter.writeLittleEndianShort(((Long) obj).intValue());
					}
				}
				break;
			case DataBlockType.LONG32:
				writeNull = true;
				for (int i = 0; i < 3; i++)
					bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.writeLittleEndianInt(0);
					} else {
						bufferWriter.writeLittleEndianInt(((Long) obj).intValue());
					}
				}
				break;
			case DataBlockType.LONG64:
				writeNull = true;
				for (int i = 0; i < 7; i++)
					bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						bufferWriter.writeLittleEndianLong(0);
					} else {
						bufferWriter.writeLittleEndianLong((Long) obj);
					}
				}
				break;
			case DataBlockType.DOUBLE64:
				writeNull = true;
				for (int i = 0; i < 7; i++)
					bufferWriter.writeNone();
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					if (obj == null) {
						long v = Double.doubleToLongBits(0D);
						bufferWriter.writeLittleEndianLong(v);
					} else {
						long v = Double.doubleToLongBits((Double) obj);
						bufferWriter.writeLittleEndianLong(v);
					}
				}
				break;
			case DataBlockType.SERIALBYTES:
				writeNull = true;
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					SerialBytes sb = (SerialBytes) r.getNormalFieldValue(col);
					bufferWriter.writeLong64(sb.getValue1());
					bufferWriter.writeLong64(sb.getValue2());
				}
				break;
			case DataBlockType.STRING_ASSIC:
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					String v = (String) obj;
					bufferWriter.write(v.length());
				
				}
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					String v = (String) obj;
					bufferWriter.write(v.getBytes());
				
				}
				break;
			}
		}

		if (writeNull) {
			bufferWriter.writeBoolean(blockType.isHasNull());
			if (blockType.isHasNull()) {
				for (int i = start; i <= end; ++i) {
					r = (BaseRecord) mems.get(i);
					Object obj = r.getNormalFieldValue(col);
					bufferWriter.writeBoolean(obj == null);
				}
			}
		}
	}
	
	//需要统计MAX、MIN时用这个
	public static void writeDataBlock(BufferWriter bufferWriter, Sequence data, Sequence dict, int col, int start, int end, 
			Object[] maxValues, Object[] minValues, Object[] startValues, int[] dataType) throws IOException {
		BaseRecord r;
		IArray mems = data.getMems();
		for (int i = start; i <= end; ++i) {
			r = (BaseRecord) mems.get(i);
			
			Object obj = r.getNormalFieldValue(col);

			try {
				if (Variant.compare(obj, maxValues[col], true) > 0)
					maxValues[col] = obj;
			} catch (RQException e) {
				maxValues[col] = null;
			}
			if (i == start) {
				minValues[col] = obj;//第一个要赋值，因为null表示最小
				startValues[col] = obj;
			}
			try {
				if (Variant.compare(obj, minValues[col], true) < 0)
					minValues[col] = obj;
			} catch (RQException e) {
				maxValues[col] = null;
			}
		}
		writeDataBlock(bufferWriter, data, dict, col, start, end, dataType);
	}
	
	/**
	 * 检查是否可以用全局字典,必要时合并
	 * @param column_dict
	 * @param block_dict
	 * @return
	 */
	private static boolean checkDict(Sequence column_dict, Sequence block_dict) {
		//成员里有ds时，暂时不作为字典处理
		for (int i = 1, len = block_dict.length(); i <= len; i++) {
			Object obj = block_dict.get(i);
			if (obj instanceof Sequence || obj instanceof BaseRecord) {
				return false;
			}
		}
		if (column_dict.length() == 0) {
			column_dict.addAll(block_dict);
			return true;
		}
		
		Sequence newObjs = null;
		int len = block_dict.length();
		for (int i = 1; i <= len; ++i) {
			Object obj = block_dict.get(i);
			if (!column_dict.contains(obj, false)) {
				if (newObjs == null) {
					newObjs = new Sequence();
				}
				newObjs.add(obj);
			}
		}
		
		if (newObjs != null) {
			if (column_dict.length() + newObjs.length() > DataBlockType.MAX_DICT_NUMBER) {
				return false;
			} else {
				column_dict.addAll(newObjs);
			}
		}
		return true;
	}
	
	private static boolean checkSame(byte[] pos) {
		int len = pos.length;
		if (len == 0) return false;
		byte p = pos[0];
		for (int i = 0; i < len; i++) {
			if (p != pos[i]) return false;
		}
		return true;
	}
}
