package com.scudata.dw;

import java.io.IOException;

import com.scudata.common.RQException;
import com.scudata.dm.DataStruct;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;

public abstract class IDWCursor extends ICursor {
	abstract public void setAppendData(Sequence seq);
	abstract public void setSegment(int startBlock, int endBlock);
	abstract public PhyTable getTableMetaData();
	abstract protected Sequence get(int n);
	abstract protected Sequence getStartBlockData(int n);//只读第一块的数据,不含appendData
	abstract public void setCache(Sequence cache);
	
	abstract public int getStartBlock();
	abstract public int getEndBlock();
	abstract public void setEndBlock(int endBlock);
	
	private String option;
	
	public void setOption(String option) {
		this.option = option;
		if (option != null && option.indexOf('x') != -1) {
			getTableMetaData().groupTable.openCursorEvent();
		}
	}
	
	public void close() {
		super.close();
		if (option != null && option.indexOf('x') != -1) {
			getTableMetaData().groupTable.closeCursorEvent();
		}
	}
	
	/**
	 * 取分段游标的起始值，如果有分段字段则返回分段字段的值，没有则返回维字段的值
	 * @return 分段游标首条记录的分段字段的值，如果当前段数为0则返回null
	 */
	public Object[] getSegmentStartValues() {
		ColPhyTable table = (ColPhyTable) getTableMetaData();
		int startBlock = getStartBlock();
		String segmentCol = table.getSegmentCol();
		
		ColumnMetaData[] cols;
		DataStruct ds = getDataStruct();
		if (segmentCol != null) {
			if (ds.getFieldIndex(segmentCol) == -1) {
				return null;
			} else {
				cols = new ColumnMetaData[] {table.getColumn(segmentCol)};
			}
		} else {
			ColumnMetaData[] tempCols = table.getSortedColumns();
			int count = 0;
			for (ColumnMetaData col : tempCols) {
				if (ds.getFieldIndex(col.getColName()) != -1) {
					count++;
				} else {
					break;
				}
			}
			
			if (count == 0) return  null;
			cols = new ColumnMetaData[count];
			for (int i = 0; i < count; i++) {
				cols[i] = tempCols[i];
			}
		}
		
		try {
			int colCount = cols.length;
			Object[] startValues = new Object[colCount];
			ObjectReader []readers = new ObjectReader[colCount];
			for (int f = 0; f < colCount; ++f) {
				readers[f] = cols[f].getSegmentReader();
			}
			
			for (int i = 0; i < startBlock; ++i) {
				for (int f = 0; f < colCount; ++f) {
					readers[f].readLong40();
					readers[f].skipObject();
					readers[f].skipObject();
					readers[f].skipObject();
				}
				
			}
			for (int f = 0; f < colCount; ++f) {
				readers[f].readLong40();
				readers[f].skipObject();
				readers[f].skipObject();
				startValues[f] = readers[f].readObject();
			}
			return startValues;
		
		} catch (IOException e) {
			throw new RQException(e.getMessage(), e);
		}
	}
}