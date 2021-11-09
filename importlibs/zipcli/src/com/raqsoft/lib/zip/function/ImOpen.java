package com.raqsoft.lib.zip.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;

public class ImOpen extends Function {
	private Map<String, Object> m_params;
	
	public Node optimize(Context ctx) {
		m_params = new HashMap<String, Object>();
		return this;
	}
	
	public Map<String, Object> getParams(){
		return m_params;
	}

	public Object calculate(Context ctx) {
		try{
			if (param == null ) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("unzip" + mm.getMessage("there is no param"));
			}

			Object obj = null;
			String file = null;
			String code = null;
			String passwd = "";
			if(param.isLeaf()){
				obj = param.getLeafExpression().calculate(ctx).toString();
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("zip" + mm.getMessage("zipName type is error"));
				}
				file = obj.toString();
			}else{
				ArrayList<Expression> ls = new ArrayList<Expression>();	
				param.getAllLeafExpression(ls);
				file = ls.get(0).calculate(ctx).toString();
				if (param.getType()==IParam.Comma){					
					if(ls.size()==3){					
						code = ls.get(1).calculate(ctx).toString();
						passwd = ls.get(2).calculate(ctx).toString();
					}else if(ls.size()==2){
						passwd = ls.get(1).calculate(ctx).toString();
					}
				}else if(param.getType()==IParam.Colon){
					if(ls.size()==2){
						code = ls.get(1).calculate(ctx).toString();
					}
				}
			}
			
			ZipFile zipfile = ImZipUtil.resetZipFile(file);			
			ZipParameters parameters = ImZipUtil.setZipParam(zipfile, code, passwd);
 
			m_params.put("zip", zipfile);
			m_params.put("param", parameters);
			
			return this;
		}catch(Exception e){
			throw new RQException("zip " + e.getMessage());
		}
	}	
		
	public void close() {
		try {
			if (m_params==null ){
				return;
			}
			
			ZipFile zfile = (ZipFile)m_params.get("zip");
			if (zfile!=null){
				zfile.close();
				zfile = null;
			}
			m_params.clear();
			m_params = null;		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

