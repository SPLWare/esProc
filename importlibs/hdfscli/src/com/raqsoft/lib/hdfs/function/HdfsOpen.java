package com.raqsoft.lib.hdfs.function;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

/****
 * 
 * @author 
 * hdfs_open(xmlfile, ...; uri)	返回HdfsOpen,参数均可省略
 */
public class HdfsOpen extends HdfsFunction {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			HdfsClient client = new HdfsClient(ctx, null, null, "root");
			return client.getFileSystem();
		}
		
		int size = 0;
		String userName = "root";
		String sUrl = null;
		String objs[] = null;		
		
		if (param.getType() == IParam.Semicolon){ //xml and uri
			size = param.getSubSize();
			if (size == 2) {
				IParam paramFiles = param.getSub(0);
				IParam paramUrl = param.getSub(1);	
				if (paramUrl!=null){
					if (paramUrl.isLeaf()){
						sUrl = (String)paramUrl.getLeafExpression().calculate(ctx);
					}else if(paramUrl.getSubSize()>=2){
						sUrl = (String)paramUrl.getSub(0).getLeafExpression().calculate(ctx);
						userName = (String)paramUrl.getSub(1).getLeafExpression().calculate(ctx);
					}
				}
				
				if (paramFiles!=null){
					objs = getXmlFiles(ctx, paramFiles);
				}
			}
		}else{ //xml file
			objs = getXmlFiles(ctx, param);
		}
		HdfsClient client = new HdfsClient(ctx, objs, sUrl, userName);
		return client.getFileSystem();
	}	
	
	private String[] getXmlFiles(Context ctx, IParam param){
		if (param==null) return null;
		String[] objs = null;
		
		if (param.isLeaf()){
			objs = new String[1];
			objs[0] = (String)param.getLeafExpression().calculate(ctx);
			if (objs[0].endsWith("*.xml")){
				String path = objs[0].replace("*.xml","");
				File file = new File(path);
				File[] files = file.listFiles();     //遍历该目录所有文件和文件夹对象
				List<String> ls = new ArrayList<String>();
				String name;
				for(int i = 0; i<files.length;i++){		
					name = files[i].getName();
					if(name.endsWith(".xml")){			
						ls.add(path+name);
					}
				}
				objs = ls.toArray(new String[ls.size()]);
			}
		}else{
			int fileSize = param.getSubSize();
			Object obj = new Object();
			objs = new String[fileSize];
			for(int i=0; i<fileSize; i++){
				if (param.getSub(i) == null ) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hdfs_client" + mm.getMessage("function.invalidParam"));
				}
				obj= param.getSub(i).getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("hdfs_client" + mm.getMessage("function.paramTypeError"));
				}
				objs[i] = (String)obj;
			}
		}
		
		return objs;
	}
	
}