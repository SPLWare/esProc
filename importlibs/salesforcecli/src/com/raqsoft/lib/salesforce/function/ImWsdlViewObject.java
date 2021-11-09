package com.raqsoft.lib.salesforce.function;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Table;
import com.sforce.soap.enterprise.DescribeSObjectResult;
import com.sforce.soap.enterprise.Field;

public class ImWsdlViewObject extends ImFunction{
	private ImWsdlOpen m_wsdl = null;
	
	private Object viewObject(String objectName) {
		Table ret = null;
        try {
        	DescribeSObjectResult rst = m_wsdl.m_enterpriseConn.describeSObject(objectName);
        	if (rst==null) return ret;
        	ret = new Table(new String[] {"FieldName", "FieldType"});
        	Object[] os = new Object[2];
    		for(Field f:rst.getFields()) {
    			if (f.isNillable()) {
    				os[0] = f.getName();
    				os[1] = f.getType();
    				//System.out.println("rst = "+f.getName()+"; type="+os[1]);
    				ret.newLast(os);
    			}
    		}
    	
            //System.out.println("Start");
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        
        return ret;
	}
	
	public Object doQuery(Object[] objs) {
		if (objs==null || objs.length!=2) {
			throw new RQException("WSDL viewObject function.missingParam ");
		}
		if (objs[0] instanceof ImWsdlOpen) {
			m_wsdl = (ImWsdlOpen)objs[0];
		}
		String objName = null;
		if (objs[1] instanceof String) {
			objName = (String)objs[1];
		}
		if (m_wsdl==null || objName==null) {
			throw new RQException("WSDL viewObject function.type error ");
		}
		
		if (m_wsdl.m_parterConn!=null) {
			ImWsdlPartnerViewObject c = new ImWsdlPartnerViewObject(m_wsdl.m_parterConn);
			return c.doQuery(objs);
		}else {
			return viewObject( objName );
		}
	}
	
	public static void main(String[] args) {
		ImWsdlViewObject c = new ImWsdlViewObject();
		c.viewObject("Account");
	}
}
