package com.scudata.server.odbc;

import com.scudata.dm.*;
import com.scudata.dm.cursor.ICursor;
import com.scudata.server.IProxy;

/**
 * 结果集代理
 * @author Joancy
 *
 */
public class ResultSetProxy extends IProxy {
	ICursor cs;
	String[] columns=null;
	Sequence row = null;
	
	/**
	 * 创建结果集代理
	 * @param sp Statement代理
	 * @param id 代理号
	 * @param cs 游标
	 */
	public ResultSetProxy(StatementProxy sp, int id, ICursor cs) {
		super(sp, id);
		this.cs = cs;
		access();
	}

	/**
	 * 获取游标对象
	 * @return 游标
	 */
	public ICursor getCursor() {
		return cs;
	}

	/**
	 * 获取字段名
	 * @return 字段名数组
	 */
	public String[] getColumns(){
		if(columns==null){
			row = fetch(1);
			if(row==null){
				DataStruct ds = cs.getDataStruct();
				if(ds==null){
					return null;
				}
				columns = ds.getFieldNames();
			}else{
				if(row instanceof Table){
					columns = row.dataStruct().getFieldNames();
				}else{
					Object tmp = row.get(1);
					if(tmp instanceof Record){
						Record rec = (Record)tmp;
						columns = rec.getFieldNames();
					}
				}
			}
			
			if(columns==null){
				columns = new String[]{"_1"};
			}
		}
		return columns;
	}

	/**
	 * 取数
	 * @param n 取数条数
	 * @return 数据序表
	 */
	public Sequence fetch(int n) {
		Sequence tmp;
		if(row!=null){
			Object val = row.get(1);
			tmp = cs.fetch(n-1);
			if(tmp==null){//如果结果集刚好只有一行记录时
				tmp = row;
			}else{
				tmp.insert(1, val);
			}
			row = null;
		}else{
			tmp = cs.fetch(n);
		}
		access();
		return tmp;
	}

	/**
	 * 实现toString接口
	 */
	public String toString() {
		return "ResultSet "+getId();
	}
	
	/**
	 * 关闭当前代理器
	 */
	public void close(){
		cs.close();
	}
}
