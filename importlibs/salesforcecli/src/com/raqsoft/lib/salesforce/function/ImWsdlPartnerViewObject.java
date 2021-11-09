package com.raqsoft.lib.salesforce.function;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Table;
import com.sforce.soap.partner.DescribeSObjectResult;
import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.PartnerConnection;

public class ImWsdlPartnerViewObject extends ImFunction{
	private PartnerConnection m_conn = null;
	
	public ImWsdlPartnerViewObject(PartnerConnection c) {
		m_conn = c;
	}
	
	private Object viewObject(String objectName) {
		Table ret = null;
        try {
        	DescribeSObjectResult rst = m_conn.describeSObject(objectName);
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
        } catch (Exception ex) {
                ex.printStackTrace();
        }
        
        return ret;
	}
	
	public Object doQuery(Object[] objs) {
		if (objs==null || objs.length!=2) {
			throw new RQException("WSDL viewObject function.missingParam ");
		}
		
		String objName = null;
		if (objs[1] instanceof String) {
			objName = (String)objs[1];
		}
				
		return viewObject( objName );
	}
	
//	public static void main(String[] args) {
//		ImWsdlPartnerViewObject c = new ImWsdlPartnerViewObject(null);
//		c.viewObject("Account");
//	}
}
