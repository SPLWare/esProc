package com.raqsoft.parallel;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.resources.EngineMessage;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.FileObject;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dw.ITableMetaData;
import com.raqsoft.expression.Expression;

/**
 * 组表代理
 * @author RunQian
 *
 */
public class TableMetaDataProxy extends IProxy {
	private ITableMetaData tableMetaData;
	private FileObject tempFile;//append临时文件
	
	public TableMetaDataProxy(ITableMetaData tableMetaData) {
		this.tableMetaData = tableMetaData;
	}
	
	public ITableMetaData getTableMetaData() {
		return tableMetaData;
	}
	
	public ITableMetaData attach(String tableName) {
		ITableMetaData table = tableMetaData.getAnnexTable(tableName);
		if (table == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(tableName + mm.getMessage("dw.tableNotExist"));
		}
		
		return table;
	}
	
	// 取append数据用的临时文件
	public FileObject getTempFile() {
		return tempFile;
	}

	// 创建临时文件，用于存放append数据
	public void createTempFile() {
		tempFile = FileObject.createTempFileObject();
	}

	public void close() {
		tableMetaData.close();
	}
		
	public ICursor icursor(String []fields, Expression filter, String iname, String opt, Context ctx) {
		return tableMetaData.icursor(fields, filter, iname, opt, ctx);
	}
	
	public String[] getAllSortedColNames() {
		return tableMetaData.getAllSortedColNames();
	}
}