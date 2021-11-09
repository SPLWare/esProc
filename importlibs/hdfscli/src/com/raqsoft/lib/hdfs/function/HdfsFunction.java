package com.raqsoft.lib.hdfs.function;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class HdfsFunction extends Function {
	protected int m_paramSize = 0;	//参数个数
	protected FileSystem m_hdfs = null;
	protected String m_colNames[];
	protected String m_paramTypes[]; //除去hdfs句柄参数类型
	protected Context m_ctx;
	
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		m_ctx = ctx;
		return this;
	}
	
	protected void setParamSize(int paramSize){
		m_paramSize = paramSize;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("hdfs_client" + mm.getMessage("function.missingParam"));
		}

		int size = param.getSubSize();
		//System.out.println("baseSize = " + size);

		Object o = new Object();
		List<Object> ls = new ArrayList<Object>();
		
		for(int i=0; i<size; i++){
			if (param.getSub(i) == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("hdfs_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (i==0){
				o = param.getSub(i).getLeafExpression().calculate(ctx);
				if (o instanceof FileSystem) {
					m_hdfs = ((FileSystem)o);
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("hdfs_client" + mm.getMessage("function.paramTypeError"));
				}
			}else{
				if (param.getSub(i).isLeaf()){
					o = param.getSub(i).getLeafExpression().calculate(ctx);
					if (o!=null && o instanceof String){
						ls.add(o);
					}else{
						throw new RQException("hdfs param " + o + " type is not String");
					}	
				}else{
					IParam subParam = param.getSub(i);
					int subSize = subParam.getSubSize();
					for(int j=0; j<subSize; j++){						
						o = subParam.getSub(j).getLeafExpression().calculate(ctx);
						if (o!=null && o instanceof String){
							ls.add(o);
						}else{
							throw new RQException("hdfs param " + o + " type is not String");
						}	
					}
				}
			}
		}

		Object objs[] = new Object[ls.size()];
		ls.toArray(objs);
		if ( m_hdfs == null){
			throw new RQException("HdfsClient is null");
		}
		if (objs.length<1){
			throw new RQException("hdfs_param is empty");
		}

		return doQuery(objs);
	}
	
	public Object doQuery( Object[] objs){
		return null;
	}
	
}