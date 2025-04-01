package com.scudata.expression.mfn.record;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Record;
import com.scudata.expression.IParam;
import com.scudata.expression.RecordFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更改序表的数据结构
 * r.alter(Fi,…;F’i,…)
 * @author RunQian
 *
 */
public class Alter extends RecordFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("function.missingParam"));
		}
		
		DataStruct ds = srcRecord.dataStruct();
		String []oldFields = ds.getFieldNames();
		int oldCount = oldFields.length;
		int []state = new int[oldCount]; // -1表示删除、1表示选出
		ArrayList<String> newFieldList = new ArrayList<String>(oldCount);
		
		IParam oldParam;
		if (param.getType() == IParam.Semicolon) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("alter" + mm.getMessage("function.invalidParam"));
			}
			
			oldParam = param.getSub(0);
			IParam deleteParam = param.getSub(1);
			if (deleteParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("alter" + mm.getMessage("function.invalidParam"));
			} else if (deleteParam.isLeaf()) {
				String field = deleteParam.getLeafExpression().getIdentifierName();
				int index = ds.getFieldIndex(field);
				if (index == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
				}
				
				state[index] = -1;
			} else {
				for (int i = 0, size = deleteParam.getSubSize(); i < size; ++i) {
					IParam sub = deleteParam.getSub(i);
					if (sub == null) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("alter" + mm.getMessage("function.invalidParam"));
					}
					
					String field = sub.getLeafExpression().getIdentifierName();
					int index = ds.getFieldIndex(field);
					if (index == -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException(field + mm.getMessage("ds.fieldNotExist"));
					}
					
					state[index] = -1;
				}
			}
		} else {
			oldParam = param;
		}
		
		if (oldParam == null) {
		} else if (oldParam.isLeaf()) {
			String field = oldParam.getLeafExpression().getIdentifierName();
			int index = ds.getFieldIndex(field);
			if (index != -1) {
				if (state[index] == -1) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("alter" + mm.getMessage("function.invalidParam"));
				}
				
				state[index] = 1;
				field = oldFields[index];
			}
			
			newFieldList.add(field);
		} else {
			for (int i = 0, size = oldParam.getSubSize(); i < size; ++i) {
				IParam sub = oldParam.getSub(i);
				if (sub == null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("alter" + mm.getMessage("function.invalidParam"));
				}
				
				String field = sub.getLeafExpression().getIdentifierName();
				int index = ds.getFieldIndex(field);
				if (index != -1) {
					if (state[index] == -1) {
						MessageManager mm = EngineMessage.get();
						throw new RQException("alter" + mm.getMessage("function.invalidParam"));
					}

					state[index] = 1;
					field = oldFields[index];
				}
				
				newFieldList.add(field);
			}
		}
		
		for (int i = 0; i < oldCount; ++i) {
			if (state[i] != -1 && state[i] != 1) {
				newFieldList.add(oldFields[i]);
			}
		}
		
		int newCount = newFieldList.size();
		String []newFields = new String[newCount];
		newFieldList.toArray(newFields);
		Object []newValues = new Object[newCount];
		
		for (int i = 0; i < newCount; ++i) {
			int f = ds.getFieldIndex(newFields[i]);
			if (f != -1) {
				// 字段可能以#i表示
				newFields[i] = ds.getFieldName(f);
				newValues[i] = srcRecord.getNormalFieldValue(f);
			}
		}
		
		DataStruct newDs = ds.create(newFields);
		return new Record(newDs, newValues);
	}
}
