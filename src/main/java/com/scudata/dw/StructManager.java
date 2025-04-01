package com.scudata.dw;

/**
 * 结构管理器
 */
import java.util.ArrayList;

import com.scudata.dm.DataStruct;

public final class StructManager {
	private ArrayList<DataStruct> dsList; // 复列的数据结构
	private transient DataStruct prevDs; // 前一个引用字段的数据结构
	private transient int prevDsID = -1; // 数据结构的标识，即在dsList中的索引
	
	public StructManager() {
		dsList = new ArrayList<DataStruct>();
	}
	
	public StructManager(ArrayList<DataStruct> list) {
		this.dsList = list;
	}
	
	public ArrayList<DataStruct> getStructList() {
		return dsList;
	}
	
	public int getDataStructID(DataStruct ds) {
		if (prevDs != null && prevDs.isCompatible(ds)) {
			return prevDsID;
		}
		
		ArrayList<DataStruct> dsList = this.dsList;
		for (int i = 0, size = dsList.size(); i < size; ++i) {
			if (ds.isCompatible(dsList.get(i))) {
				prevDs = ds;
				prevDsID = i;
				return i;
			}
		}
		
		prevDs = ds;
		prevDsID = dsList.size();
		dsList.add(ds);
		return prevDsID;
	}

	public DataStruct getDataStruct(int id) {
		return dsList.get(id);
	}
}