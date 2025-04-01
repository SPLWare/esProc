package com.scudata.lib.webservice.function;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.Dispatch;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import com.scudata.common.Logger;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.Expression;

public class WsClientImpl {

	private String url;
	
	private Table t = null;
	
	//private String serverUrl;
	
	private String targetNamespace;
	
	private Map<String,String> alias = new HashMap<String,String>();
	//private Map<String,String> aliasFan = new HashMap<String,String>();
	private String currAlias;
	
	private Map<String, Message> messages = new HashMap<String, Message>();
	
	private Map<String, PortType> portTypes = new HashMap<String, PortType>();
	
	private Map<String, Binding> bindings = new HashMap<String, Binding>();
	
	private Map<String, Service> services = new HashMap<String, Service>();

	private Sequence operations = null;

	public static final String XML_ATTR = "_a_";
	public static final String XML_CHILD = "_c_";
	public static final String XML_TEXT = "_t_";
	
	public static final int MAX = Integer.MAX_VALUE;
	
	public WsClientImpl(String url) {
		try {
			
			this.url = url;
			String xml = Http.httpsRequest(url, "GET", null);
			//xml = 
			//Logger.debug(xml);

			//xml = xml.replaceFirst("encoding=\".*\"", "encoding=\"UTF-8\"");
			//xml = "<aa><a1>ttt</a1></aa>";
			t = xml2Table(xml);
			
			//super.ds = t.dataStruct();
			//super.mems = t.getMems();
			
			Object o = getXmlValue(t,new String[]{attr("targetNamespace")},new int[]{});
			if (o == null || !(o instanceof String)) throw new Exception("wsdl targetNamespace not find");
			targetNamespace = o.toString();
			//Logger.debug("wsdl targetNamespace : "+targetNamespace);
			
			//uri
			String fs[] = t.dataStruct().getFieldNames();
			BaseRecord r1 = t.getRecord(1);
			for (int i=0; i<fs.length; i++) {
				if (!fs[i].startsWith(XML_ATTR)) continue;
				String fsi[] = fs[i].split(":");
				if (fsi.length>1) {
					String uri = r1.getFieldValue(fs[i]).toString();
					alias.put(fsi[1], uri);
					if (uri.equals(this.targetNamespace)) {
						currAlias = fsi[1];
						//Logger.debug("wsdl currAlias : "+currAlias);
					}
				}
			}
			
			//message
			for (int i=1; i<MAX; i++) {
				Table ti = getXmlTable(t,new String[]{child("message")},new int[]{i});
				if (ti == null) break;
				
				Message mi = new Message();
				mi.name = getXmlAttr(ti,new String[]{attr("name")},new int[]{});
				
				for (int j=1; j<MAX; j++) {
					Table tj = getXmlTable(ti,new String[]{child("part")},new int[]{j});
					if (tj == null) break;
					Part p = new Part();
					p.name = getXmlAttr(tj,new String[]{attr("name")},new int[]{});
					p.type = getXmlAttr(tj,new String[]{attr("type")},new int[]{});
					p.element = getXmlAttr(tj,new String[]{attr("element")},new int[]{});
					mi.parts.put(p.name, p);
					mi.partNames.add(p.name);
				}
				
				messages.put(mi.name, mi);
			}
			
			//portType
			for (int i=1; i<MAX; i++) {
				Table ti = getXmlTable(t,new String[]{child("portType")},new int[]{i});
				if (ti == null) break;
				
				PortType portType = new PortType();
				portType.name = getXmlAttr(ti,new String[]{attr("name")},new int[]{});
				
				for (int j=1; j<MAX; j++) {
					Table tj = getXmlTable(ti,new String[]{child("operation")},new int[]{j});
					if (tj == null) break;
					
					Operation mi = new Operation();
					mi.name = getXmlAttr(tj,new String[]{attr("name")},new int[]{});
					mi.input = messages.get(getRelationAndCheck(getXmlAttr(tj,new String[]{child("input"),attr("message")},new int[]{1}))); 
					mi.output = messages.get(getRelationAndCheck(getXmlAttr(tj,new String[]{child("output"),attr("message")},new int[]{1}))); 
					
					portType.operations.put(mi.name, mi);
				}
				portTypes.put(portType.name, portType);
			}

			for (int i=1; i<MAX; i++) {
				Table ti = getXmlTable(t,new String[]{child("binding")},new int[]{i});
				if (ti == null) break;
				
				Binding b = new Binding();
				b.name = getXmlAttr(ti,new String[]{attr("name")},new int[]{});
				b.port = portTypes.get(getRelationAndCheck(getXmlAttr(ti,new String[]{attr("type")},new int[]{})));
				b.style = getXmlAttr(ti,new String[]{child("binding"),attr("style")},new int[]{});
				b.transport = getXmlAttr(ti,new String[]{child("binding"),attr("transport")},new int[]{});
				
				
				if (b.port != null) {
					for (int j=1; j<MAX; j++) {
						Table tj = getXmlTable(ti,new String[]{child("operation")},new int[]{j});
						if (tj == null) break;
						BindingOperation bo = new BindingOperation();
						bo.operation = b.port.operations.get(getXmlAttr(tj,new String[]{attr("name")},new int[]{}));
						b.bindingOperations.put(bo.operation.name, bo);
						b.names.add(bo.operation.name);
					}
				}
				
				bindings.put(b.name, b);
			}


			for (int i=1; i<MAX; i++) {
				Table ti = getXmlTable(t,new String[]{child("service")},new int[]{i});
				if (ti == null) break;
				
				Service s = new Service();
				s.name = getXmlAttr(ti,new String[]{attr("name")},new int[]{});
				
				for (int j=1; j<MAX; j++) {
					Table tj = getXmlTable(ti,new String[]{child("port")},new int[]{j});
					if (tj == null) break;
					Port p = new Port();
					p.name = getXmlAttr(tj,new String[]{attr("name")},new int[]{});
					p.binding = bindings.get(getRelationAndCheck(getXmlAttr(tj,new String[]{attr("binding")},new int[]{})));
					p.location = getXmlAttr(tj,new String[]{child("address"),attr("location")},new int[]{1});
					s.ports.put(p.name, p);
					
				}
				this.services.put(s.name, s);
			}

			//Logger.debug(o);
			
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}
	
	private String getRelationAndCheck(String s) throws Exception {
		if (s == null) return null;
		String ss[] = s.split(":");
		if (ss.length==1) return s;
		else return ss[1];
//TODO
//		if (ss.length==1) return s;
//		if (!ss[0].equals(this.currAlias)) throw new Exception("目前不支持多wsdl混合调用");
//		return ss[1];
	}
	
	private static String child(String name) {
		return XML_CHILD+name;
	}

	
	private static String attr(String name) {
		return XML_ATTR+name;
	}

	
	private static String text() {
		return XML_TEXT;
	}

	private Table getOperation(String name) throws Exception {
		for (int i=1; i<=operations.length(); i++) {
			Table ti = (Table)operations.get(i);
			Object oi = ti.getRecord(1).getFieldValue(XML_ATTR+"name");
			if (oi == null)  throw new Exception("wsdl operation not found name");
			if (name.equals(oi.toString()))return ti;
		}
		return null;
	}

//	
//	private Table getMessage(String name) throws Exception {
//		for (int i=1; i<=messages.length(); i++) {
//			Table ti = (Table)messages.get(i);
//			Object oi = ti.getRecord(1).getFieldValue(XML_ATTR+"name");
//			if (oi == null)  throw new Exception("wsdl message not found name");
//			if (name.equals(oi.toString()))return ti;
//		}
//		return null;
//	}

//	<soapenv2:Envelope xmlns:soapenv2="http://schemas.xmlsoap.org/soap/envelope/" xmlns:test2="http://test.slots.com/">
//	   <soapenv2:Header/>
//	   <soapenv2:Body>
//	      <test2:SayHi>
//	         <name>bbbbb</name>
//	      </test2:SayHi>
//	   </soapenv2:Body>
//	</soapenv2:Envelope>
	
	public Object call2(String serviceName, String port, String functionName, String []values) throws Exception {
        //String ns = "http://test.slots.com/";  
        //String wsdlUrl = "http://localhost:6666/ws/service/sayHi?wsdl";  
        //1、创建服务(Service)  
        URL url = new URL(this.url);  
        QName sname = new QName(this.targetNamespace, serviceName);  //"SayHiServiceImplService"
        javax.xml.ws.Service service = javax.xml.ws.Service.create(url,sname);

        if (this.services.size() == 0) throw new Exception("no service found");
        Service serv = this.services.get(serviceName);
        if (serv == null) {
        	if (this.services.size() == 1 && !StringUtils.isValidString(serviceName)) {
        		serviceName = this.services.keySet().iterator().next();
        		serv = this.services.get(serviceName);
        	} else throw new Exception("no service found");
        }
        
        if (serv.ports.size() == 0) throw new Exception("no port found");
        Port portObj = serv.ports.get(port);
        if (portObj == null) {
        	if (serv.ports.size() == 1 && !StringUtils.isValidString(port)) {
        		port = serv.ports.keySet().iterator().next();
        		portObj = serv.ports.get(port);
        	} else throw new Exception("no port found");
        }
        
        
        //2、创建Dispatch  
        Dispatch<SOAPMessage> dispatch = service.createDispatch(new QName(this.targetNamespace,port),SOAPMessage.class,javax.xml.ws.Service.Mode.MESSAGE);  
                      
        dispatch.getRequestContext().put("javax.xml.ws.soap.http.soapaction.uri", "http://WebXml.com.cn/getWeatherbyCityName");
        //3、创建SOAPMessage  
        SOAPMessage msg = MessageFactory.newInstance().createMessage();  
        SOAPEnvelope envelope = msg.getSOAPPart().getEnvelope();  
        SOAPBody body = envelope.getBody();  
        
        //ws_client("wsdlUrl");
        //ws_call(ws_client,"serviceName","servicePort","functionName",[param1:param1Name,param2:paramName2,...])
        
        if (StringUtils.isValidString(functionName)) {
            Name n = envelope.createName(functionName,"raqsoftSoap",this.targetNamespace);
            SOAPBodyElement ele = body.addBodyElement(n);  
            // 传递参数
            if (values != null) {
            	for (int i=0; i<values.length; i++) {
            		String ss[] = values[i].split(":");
            		if (ss.length != 2) throw new Exception(values[i]+" 参数格式为“\"value\":paramName”");
                    ele.addChildElement(ss[1]).setValue(new String(ss[0].getBytes("GBK"),"UTF-8"));    
            	}
            }
        } else {
            if (values != null) {
            	for (int i=0; i<values.length; i++) {
            		String ss[] = values[i].split(":");
            		if (ss.length != 2) throw new Exception(values[i]+" 参数格式为“\"value\":paramName”");
                    body.addChildElement(ss[1]).setValue(new String(ss[0].getBytes("GBK"),"UTF-8"));    
            	}
            }
        }

        msg.writeTo(System.out);  
        System.out.println("\n invoking.....");
                              
        //5、通过Dispatch传递消息,会返回响应消息  
        SOAPMessage response = dispatch.invoke(msg);  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeTo(baos);
        String s = baos.toString();
        baos.close();
        Logger.debug("web service response:"+s);
        try {
			Context c1 = new Context();
			c1.setParamValue("xmlStr", s);
			Object r = new Expression("xml(xmlStr)").calculate(c1);
			BaseRecord rec = (BaseRecord)r;
		    Object enve = rec.getFieldValue(0);
		    if (enve == null) return null;
		    String ns[] = ((BaseRecord)enve).getFieldNames();
		    for (int i=0; i<ns.length; i++) {
		    	if (ns[i].indexOf("ody")>=0) {//body
		    		Object ody = ((BaseRecord)enve).getFieldValue(i);
		    		return ((BaseRecord)((BaseRecord)ody).getFieldValue(0)).getFieldValue(0);
		    	}
		    }
		} catch (Exception e) {
			Logger.error(e.getMessage());
			//Logger.debug("web service response:"+s);
			throw new Exception("web service response soap not support");
		}

        return null;
	}

	
	public Object call(String serviceName, String port, String functionName, String []values) throws Exception {
        if (this.services.size() == 0) throw new Exception("no service found ["+serviceName+"]");
        Service serv = this.services.get(serviceName);
        if (serv == null) {
        	if (this.services.size() == 1 && !StringUtils.isValidString(serviceName)) {
        		serviceName = this.services.keySet().iterator().next();
        		serv = this.services.get(serviceName);
        	} else throw new Exception("no service found ["+serviceName+"]");
        }
        
        
        if (serv.ports.size() == 0) throw new Exception("no port found ["+port+"]");
        Port portObj = serv.ports.get(port);
        if (portObj == null) {
        	if (serv.ports.size() == 1 && !StringUtils.isValidString(port)) {
        		port = serv.ports.keySet().iterator().next();
        		portObj = serv.ports.get(port);
        	} else throw new Exception("no port found ["+serviceName+"]");
        }
        String url = portObj.location;//"http://www.webxml.com.cn/WebServices/WeatherWebService.asmx";//
        String soap = getRequestSoap(portObj.binding.bindingOperations.get(functionName).operation,values);
        Logger.debug("web service request:"+soap);
        //String s = accessService(this.url,soap,"text/xml; charset=UTF-8");//
        //String s = HttpRequest.sendPost(this.url, soap);
        String s = Http.httpsRequest(url, "POST", soap);
        Logger.debug("web service response:"+s);
        try {
			Context c1 = new Context();
			c1.setParamValue("xmlStr", s);
			Object r = new Expression("xml(xmlStr)").calculate(c1);
			BaseRecord rec = (BaseRecord)r;
		    Object enve = rec.getFieldValue(0);
		    if (enve == null) return null;
		    String ns[] = ((BaseRecord)enve).getFieldNames();
		    for (int i=0; i<ns.length; i++) {
		    	if (ns[i].indexOf("ody")>=0) {//body
		    		Object ody = ((BaseRecord)enve).getFieldValue(i);
		    		return ((BaseRecord)((BaseRecord)ody).getFieldValue(0)).getFieldValue(0);
		    	}
		    }
		} catch (Exception e) {
			Logger.error(e.getMessage());
			throw new Exception("web service response soap not support");
		}

        return null;
	}

	public String getRequestSoap(Operation operation, String[] values) throws Exception {
		String ps = "";
		Message input = operation.input;
		for (int i=0; i<values.length; i++) {
			String[] vi = values[i].split(":");
			String name = null;
			if (vi.length>1) name = vi[1];
			else {
				if (input.partNames.size()<=i) throw new Exception("parameter number "+i+" not exist in wsdl");
				name = input.partNames.get(i);
			}
			//if (input.partNames.indexOf(name) == -1) throw new Exception("parameter name “"+name+"” not exist in wsdl");
			ps += "<"+name+">"+vi[0]+"</"+name+">";
		}
		
		String soap = "";
		soap += "<raqsoftSoap:Envelope xmlns:raqsoftSoap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns=\""+this.targetNamespace+"\">"
		   +"<raqsoftSoap:Header/>"
		   +"<raqsoftSoap:Body>"
		      +"<"+operation.name+">"
		         +ps
		      +"</"+operation.name+">"
		   +"</raqsoftSoap:Body>"
		+"</raqsoftSoap:Envelope>";
		
//		StringBuffer sb = new StringBuffer();
//    	sb.append("<soap:Envelope xmlns=\"http://WebXml.com.cn/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
//    	sb.append("<soap:Body>");
//    	sb.append("<getSupportCity>");
//    	sb.append("<byProvinceName>河北</byProvinceName>");
//	    sb.append("</getSupportCity>");
//	    sb.append("</soap:Body>");
//	    sb.append("</soap:Envelope>");
//		return sb.toString();
	    
		return soap;
	}

//    public synchronized static String accessService(String wsdl,String content,String contentType)throws Exception{    
//        //拼接参数    
//        String soapResponseData = "";    
//        //拼接SOAP    
//        PostMethod postMethod = new PostMethod(wsdl);    
//        // 然后把Soap请求数据添加到PostMethod中    
//        byte[] b=null;    
//        InputStream is=null;    
//        try {    
//            b = content.getBytes("utf-8");     
//            is = new ByteArrayInputStream(b, 0, b.length);    
//            RequestEntity re = new InputStreamRequestEntity(is, b.length,contentType);    
//            postMethod.setRequestEntity(re);    
//            HttpClient httpClient = new HttpClient();    
//            //methods
//            int status = httpClient.executeMethod(postMethod);    
//            System.out.println("status:"+status);    
//            if(status==200){ 
//            	return postMethod.getResponseBodyAsString();
//            }    
//        } catch (Exception e) {    
//            Logger.error(e.getMessage());    
//        } finally{    
//            if(is!=null){    
//                is.close();    
//            }    
//        }    
//        return soapResponseData;    
//    }    

	public static Table xml2Table(String xml) throws Exception {
		Table t = new Table();
		if(null == xml || "".equals(xml)) {    
		    return t;    
		}
		    
		Map<String,String> m = new HashMap<String,String>();     
		InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));    
		SAXBuilder builder = new SAXBuilder();    
		Document doc = builder.build(in);    
		Element root = doc.getRootElement();    
		t = getElementTable(root);
		//关闭流    
		in.close();
		return t;
	}
	//"_a_${attr}","_c_${child}","_t_"内容
	private static Table getElementTable(Element e) {
		String n = e.getName();
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<Object> objs = new ArrayList<Object>();
		List<Attribute> attrs = e.getAttributes();
		
		for (int i=0; i<attrs.size(); i++) {
			String ns = attrs.get(i).getNamespacePrefix();
			if (ns != null && ns.length()>0) ns = ns+":";
			else ns = "";
			ns = "";
			names.add(XML_ATTR+ns+attrs.get(i).getName());
			objs.add(attrs.get(i).getValue());
			//Logger.debug("attr i = " + attrs.get(i));
		}
		List childs = e.getChildren();
		for (int i=0; i<childs.size(); i++) {
			Object oi = childs.get(i);
			//Logger.debug("childs i = " + childs.get(i));
			if (oi instanceof Element) {
				String ns = ((Element)childs.get(i)).getNamespacePrefix();
				if (ns != null && ns.length()>0) ns =  ns + ":";
				else ns = "";
				//Logger.debug(ns);
				ns = "";
				String ni = XML_CHILD+ns+((Element) oi).getName();
				int idx = names.indexOf(ni);
				Sequence seq = null;
				if (names.indexOf(ni) == -1) {
					seq = new Sequence();
					names.add(ni);
					objs.add(seq);
				} else seq = (Sequence)objs.get(idx);
				
				seq.add(getElementTable((Element)oi));
			}
		}
		if (childs.size() == 0) {
			names.add(XML_TEXT);
			objs.add(e.getText());
			//Logger.debug("text = " + e.getText());
		}
			
		Table t = new Table(names.toArray(new String[names.size()]));
		BaseRecord r = t.insert(0);
		for (int i=0; i<names.size(); i++) r.set(names.get(i), objs.get(i));
		//Logger.debug("table = " + t);
		return t;
	}
	
	public static Table getXmlTable(Table t, String names[], int[] pos) {
		Object o = getXmlValue(t,names,pos);
		if (o != null && o instanceof Table) return (Table)o;
		else return null;
	}
	
	public static String getXmlAttr(Table t, String names[], int[] pos) {
		Object o = getXmlValue(t,names,pos);
		if (o != null) return o.toString();
		else return null;
	}
	
	public static Object getXmlValue(Table t, String names[], int[] pos) {
		try {
			Object o = t.getRecord(1).getFieldValue(names[0]);
			int pos0 = 1;
			if (pos != null && pos.length>0 && pos[0]>0) pos0 = pos[0];
			if (o instanceof Sequence) o = ((Sequence)o).get(pos0);
			
			for (int i=1; i<names.length; i++) {
				if (o == null) return null;
				if (o instanceof Table) o = ((Table)o).getRecord(1).getFieldValue(names[i]);
				int posi = 1;
				if (pos != null && pos.length>i && pos[i]>0)  posi = pos[i];
				if (o instanceof Sequence) o = ((Sequence)o).get(posi);
			}
			return o;
		} catch (Exception e) {
			return null;
		}
	}
	
    public static String getChildrenText(List children) {    
        StringBuffer sb = new StringBuffer();    
        if(!children.isEmpty()) {    
            Iterator it = children.iterator();    
            while(it.hasNext()) {    
                Element e = (Element) it.next();    
                String name = e.getName();    
                String value = e.getTextNormalize();    
                List list = e.getChildren();    
                sb.append("<" + name + ">");    
                if(!list.isEmpty()) {    
                    sb.append(getChildrenText(list));    
                }    
                sb.append(value);    
                sb.append("</" + name + ">");    
            }    
        }     
        return sb.toString();    
  }  

	
	public static void main(String args[]) {
		try {
			//String s1 = "http://localhost:6666/ws/service/sayHi?wsdl";
			String s2 = "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl";
			WsClientImpl wc = new WsClientImpl(s2);
			
			System.out.println(wc.call("WeatherWebService", "WeatherWebServiceSoap", "getSupportProvince", new String[]{}));
			//System.out.println(wc.call("WeatherWebService", "WeatherWebServiceSoap", "getWeatherbyCityName", new String[]{"上海:theCityName"}));
			//System.out.println(wc.call("WeatherWebService", "WeatherWebServiceSoap", "getSupportCity", new String[]{"河北:byProvinceName"}));
			//System.out.println(wc.call2("SayHiServiceImplService", "SayHiServiceImplPort", "SayHi", new String[]{"aa:name"}));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Logger.error(e.getMessage());
		}
//		System.out.println("aa:bb".split(":").length);
//		System.out.println("aa".split(":").length);
	}
}
