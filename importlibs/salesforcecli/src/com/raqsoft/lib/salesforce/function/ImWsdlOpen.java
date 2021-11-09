package com.raqsoft.lib.salesforce.function;

import com.raqsoft.common.RQException;
import com.sforce.soap.enterprise.Connector;
import com.sforce.soap.enterprise.EnterpriseConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;

public class ImWsdlOpen extends ImFunction {
	public PartnerConnection m_parterConn;
    public EnterpriseConnection m_enterpriseConn; //for default;
    
      
    public Object doQuery(Object[] objs){
    	String username = null; 	//Salesforce账号中的用户名
    	String password = null;     //密码，这个密码有点特殊，需要在密码后面加入安全标记&O667v4Qs5LLKGDZ6eGfyvP0D
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
