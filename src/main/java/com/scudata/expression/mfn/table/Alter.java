package com.scudata.expression.mfn.table;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.expression.IParam;
import com.scudata.expression.TableFunction;
import com.scudata.resources.EngineMessage;

/**
 * 更改序表的数据结构
 * T.alter(Fi,…;F’i,…)
 * @author RunQian
 *
 */
public class Alter extends TableFunction {
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("alter" + mm.getMessage("function.missingParam"));
		}
		
		DataStruct ds = srcTable.dataStruct();
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
		srcTable.alter(newFields);
		return srcTable;
	}
}
