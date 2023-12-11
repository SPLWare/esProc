package com.scudata.dw;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.resources.EngineMessage;

/**
 * 记录值查找器
 * @author runqian
 *
 */
class RecordValSearcher {
	private Cursor cs;
	private String []fields;
	private Sequence pkeyData;//取数缓存区
	private long recNUM;//当前伪号
	private ComTableRecord curRecord;//当前记录
	private Record curModifyRecord;//当前记录（补区）
	private int cur;//块内序号
	private int len;//当前块个数
	private int []index;//字段取出时对应的位置

	private int indexSize;
	private ArrayList<ModifyRecord> modifyRecords;

	public RecordValSearcher() {
	}
	
	public void setData(Sequence pkeyData) {
		this.pkeyData = pkeyData;
		if (pkeyData.hasRecord()) {
			curRecord = (ComTableRecord) pkeyData.getMem(1);
			len = pkeyData.length();
			cur = 1;
			recNUM = curRecord.getRecordSeq();
		}
		this.fields = curRecord.getFieldNames();
	}

	public RecordValSearcher(ColPhyTable table, String []fields) {
		cs = (Cursor) table.cursor(fields);
		pkeyData = cs.fetch(ICursor.FETCHCOUNT);
		if (pkeyData.hasRecord()) {
			curRecord = (ComTableRecord) pkeyData.getMem(1);
			len = pkeyData.length();
			cur = 1;
			recNUM = curRecord.getRecordSeq();
		}
		this.fields = curRecord.getFieldNames();
	}
	
	public void setIndex(int[] index) {
		this.index = index;
		if (index == null) return;
		indexSize = index.length;
	}
	
	public long getRecNum() {
		return recNUM;
	}
	
	public void setModifyRecords(ArrayList<ModifyRecord> modifyRecords) {
		this.modifyRecords = modifyRecords;
	}
	
	public void next() {
		cur++;
		if (cur > len) {
			return;
		}
		ComTableRecord rec = (ComTableRecord) pkeyData.getMem(cur);
		curRecord = rec;
		this.recNUM = rec.getRecordSeq();
	}
	
	public int getRecordCount() {
		return pkeyData.length();
	}

	/**
	 * 按记录号取出key值
	 * 有filter时用这个
	 * @param recNum 记录号
	 * @param r 输出
	 * @return true成功，false没找到
	 */
	public boolean getKeyVals(long recNum, Record r) {
		ComTableRecord rec;
		if (recNum < this.recNUM) {
			return false;
		} else if (recNum == this.recNUM) {
			rec = curRecord;
		} else {
			while (true) {
				cur++;
				if (cur > len) {
					return false;
				}
				rec = (ComTableRecord) pkeyData.getMem(cur);
				if (recNum < rec.getRecordSeq()) {
					curRecord = rec;
					this.recNUM = rec.getRecordSeq();
					return false;
				}
				
				if (recNum == rec.getRecordSeq()) {
					curRecord = rec;
					this.recNUM = recNum;
					break;
				}
			}
		}
		
		int []index = this.index;
		int colCount = indexSize;
		for (int i = 0; i < colCount; ++i) {
			int id = index[i];
			if (id >= 0) {
				r.set(id, rec.getFieldValue(i));
			}
		}
		return true;
	}
	
	/**
	 * 按记录号取出key值
	 * 无filter时用这个，因为子表的记录肯定可以在主表找到
	 * @param recNum 记录号
	 * @param r 输出
	 */
	public void getKeyValues(long recNum, Record r) {
		ComTableRecord rec;		
		if (recNum == this.recNUM) {
			rec = curRecord;
		} else {
			while (true) {
				cur++;
				if (cur > len) {
					//异常
					MessageManager mm = EngineMessage.get();
					throw new RQException(r.toString(null) + mm.getMessage("grouptable.invalidData"));
				}
				rec = (ComTableRecord) pkeyData.getMem(cur);
				if (recNum == rec.getRecordSeq()) {
					curRecord = rec;
					this.recNUM = recNum;
					break;
				}
			}
		}
		
		int []index = this.index;
		int colCount = indexSize;
		for (int i = 0; i < colCount; ++i) {
			int id = index[i];
			if (id >= 0) {
				r.set(id, rec.getFieldValue(i));
			}
		}
	}

	/**
	 * 取补区记录值
	 * @param recNum 记录号，为负值时表示的是在补区的序号
	 * @param r 输出
	 * @param tableId 目前只可能是0
	 */
	public void getMKeyValues(long recNum, Record r,int tableId) {
		Record record = null;
		
		if (modifyRecords == null) {
			return;
		}
		if (tableId == 0) {
			recNum = -recNum;
			ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
			for (ModifyRecord mr : modifyRecords) {
				if (mr.getRecordSeq() == recNum) {
					record = mr.getRecord();
					curModifyRecord = record;
					break;
				}
			}
			
//			record = modifyRecords.get((int) recNum - 1).getRecord();
//			curModifyRecord = record;
		} else {
			ArrayList<ModifyRecord> modifyRecords = this.modifyRecords;
			for (ModifyRecord mr : modifyRecords) {
				if (mr.getParentRecordSeq() == recNum) {
					record = mr.getRecord();
					curModifyRecord = record;
					break;
				}
			}
		}
		
		String []fields = this.fields;
		int []index = this.index;
		int colCount = indexSize;
		for (int i = 0; i < colCount; ++i) {
			int id = index[i];
			if (id >= 0) {
				r.set(id, record.getFieldValue(fields[i]));
			}
		}
		return;
		
	}
	
	/**
	 * 获得当前记录值
	 * @param r 输出
	 */
	public void getRecordValue(Record r) {
		ComTableRecord rec = curRecord;

		int []index = this.index;
		int colCount = indexSize;
		for (int i = 0; i < colCount; ++i) {
			int id = index[i];
			if (id >= 0) {
				r.set(id, rec.getFieldValue(i));
			}
		}
	}
	
	/**
	 * 取当前记录的字段值
	 * @param index 字段位置
	 * @return
	 */
	public Object getRecordValue(int index) {
		return curRecord.getNormalFieldValue(index);
	}
	
	/**
	 * 取当前补记录的字段值
	 * @param index 字段位置
	 * @return
	 */
	public Object getModifyRecordValue(int index) {
		return curModifyRecord.getNormalFieldValue(index);
	}
}