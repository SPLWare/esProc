package com.scudata.lib.salesforce.function;

import com.scudata.common.RQException;
import com.sforce.soap.enterprise.Connector;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

public class ImWsdlOpen extends ImFunction {
	public PartnerConnection m_parterConn;
    public EnterpriseConnection m_enterpriseConn; //for default;
    
      
    public Object doQuery(Object[] objs){
    	String username = null; 	//Salesforce璐﹀彿涓殑鐢ㄦ埛鍚?
    	String password = null;     //瀵嗙爜锛岃繖涓瘑鐮佹湁鐐圭壒娈婏紝闇?瑕佸湪瀵嗙爜鍚庨潰鍔犲叆瀹夊叏鏍囪&O667v4Qs5LLKGDZ6eGfyvP0D
		if (objs==null || objs.length<2){
			throw new RQException("WSDL open function.missingParam ");
		}
		if(objs[0] instanceof String) {
			username = objs[0].toString();
		}
		if(objs[1] instanceof String) {
			password = objs[1].toString();
		}
		
		if (username==null || password==null) {
			throw new RQException("WSDL open function.type error ");
		}
		
		try {
			if (this.option!=null && option.contains("p")) {
				m_parterConn = com.sforce.soap.partner.Connector.newConnection(username, password);
			}else {
				m_enterpriseConn = Connector.newConnection(username, password);
			}
        } catch (ConnectionException e1) {
            e1.printStackTrace();
        }
		
		return this;
	}
}
