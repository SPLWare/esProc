package com.raqsoft.lib.informix.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Set;
import com.raqsoft.common.DBConfig;
import com.raqsoft.common.DBSession;
import com.raqsoft.common.Logger;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.DBObject;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;

public class IfxConn extends DBObject implements IResource {
	public ImConnection m_connect;
	private boolean m_bFragMod = false;
	private String m_fragFile;
	private Map<String, Fragment> m_mapFrag;
	private String[] m_cols= null;
	private List<ICursor> m_cursors;

	public IfxConn(Context ctx, DBSession dbsess, String fragmentFile) {
		super(dbsess);
		ctx.addResource(this);
		m_fragFile = fragmentFile;
		m_mapFrag = new HashMap<String, Fragment>();
		m_cursors = new ArrayList<ICursor>();
		m_cols = new String[]{"TableName", "FieldName","FieldType","MinValue"};
		DBConfig info = (DBConfig) dbsess.getInfo();
		boolean bOk = initJdbc(info.getDriver(), info.getUrl(), info.getUser(), info.getPassword());
		if (!bOk){
			Logger.warn("initJdbc error");
			return;
		}
		initFrag(fragmentFile);
	}

	public Fragment getFragment(String tableName) // 取分片信息
	{
		if (tableName == null) return null;

		Set<Entry<String, Fragment>> set = m_mapFrag.entrySet();
		Iterator<Entry<String, Fragment>> iterator = set.iterator();
		Fragment frag;
		Fragment retFrag = null;
		while (iterator.hasNext())
		{
			Map.Entry<String, Fragment> mapentry = iterator.next();
			frag = (Fragment)mapentry.getValue();		
			if (tableName.equalsIgnoreCase(frag.getTableName())){
				retFrag = frag;
				break;
			}
		}

		return retFrag;
	}

	public Fragment takeFragment(String tableName) // 从数据库提取分片信息
	{
		String table = "'"+tableName+"'";
		Map<String, Fragment> map = ImSQLParser.parseFragInfo(m_connect.conn, table);
		Set<Entry<String, Fragment>> set = map.entrySet();
		Iterator<Entry<String, Fragment>> iterator = set.iterator();
		Fragment frag;
		Fragment retFrag = null;
		while (iterator.hasNext())
		{
			Map.Entry<String, Fragment> mapentry = iterator.next();
			frag = (Fragment)mapentry.getValue();		
			if (frag.getTableName()==tableName){
				retFrag = frag;
				break;
			}
		}
		
		return retFrag;
	}

	public void delFrag(String tableName) {
		if (m_mapFrag.containsKey(tableName)){
			m_bFragMod = true;
			m_mapFrag.remove(tableName);
		}
	}

	public void setFrag(Fragment frag) {
		if (frag!=null){
			m_bFragMod = true;
			String tblName = frag.getTableName();
			if (frag.getPartitionCount()==0){
				delFrag(tblName);
			}else{
				Fragment oldFrag = m_mapFrag.get(tblName);
				frag.setFieldType(oldFrag.getFieldType());
				frag.setFieldTypeStr(oldFrag.getFieldTypeStr());
				m_mapFrag.put(frag.getTableName(), frag);
			}
		}
	}

	public void saveFrag(OutputStream out) // 保存分片信息
	{
		try {
			String s = "<Fragments>\n";
			out.write(s.getBytes());
			Set<Entry<String, Fragment>> set = m_mapFrag.entrySet();
			Iterator<Entry<String, Fragment>> iterator = set.iterator();
			
			while (iterator.hasNext())
			{
				Map.Entry<String, Fragment> mapentry = iterator.next();
				Fragment frag = (Fragment)mapentry.getValue();				
				s = "\t<Table name=\""+frag.getTableName()+"\"" +
					" fieldName=\""+frag.getFieldName()+"\"" +
					" fieldtype=\""+frag.getFieldType()+"\"";
					if (frag.getMaxValue()!=null){
						s += " maxValue=\""+frag.getMaxValue()+"\"";
					}
					if (frag.getInterval()!=0){
						s += " interval=\""+frag.getInterval()+"\"";
					}
					s += " ruleType=\""+frag.getRuleType()+"\"" +
					" minValue=\""+frag.getPartitionString()+"\" />\n";
	
				out.write(s.getBytes());
			}
			s = "</Fragments>";
			out.write(s.getBytes());
			m_bFragMod = false;
		} catch (IOException e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	public void saveFragment(String fileName){
		try {
			FileOutputStream fos = new FileOutputStream(fileName);
			saveFrag(fos);
			fos.close();
		} catch (IOException e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	private void doFragment(Table tbl, Fragment frag) {
		do{
			if (tbl==null) break;
			if (frag == null) break;
			
			Object vals[] = new Object[m_cols.length];			
			vals[0]=frag.getTableName();
			vals[1]=frag.getFieldName();
			vals[2]=frag.getFieldTypeStr();
			List<Object> ls = frag.getPartitionMap();
			for(Object o:ls){
				vals[3]=o;
				tbl.newLast(vals);
			}
		}while(false);
	}
	
	public Table listFragment(Fragment frag) {
		Table tbl = new Table(m_cols);
		doFragment(tbl, frag);		
		
		return tbl;
	}
	
	public Table listFrag(String tableName) {
		Table tbl = new Table(m_cols);		
		Set<Entry<String, Fragment>> set = m_mapFrag.entrySet();
		Iterator<Entry<String, Fragment>> iterator = set.iterator();		
		while (iterator.hasNext())
		{
			Map.Entry<String, Fragment> mapentry = iterator.next();
			Fragment frag = (Fragment)mapentry.getValue();		
			if (frag.getTableName().compareToIgnoreCase(tableName)==0){
				doFragment(tbl, frag);
				break;
			}
		}
		
		return tbl;
	}

	public Table listFrag() {
		return listFragByMap(m_mapFrag);
	}
	
	public Table listFragByMap(Map<String, Fragment> map) {		
		Table tbl = new Table(m_cols);
	
		Set<Entry<String, Fragment>> set = map.entrySet();
		Iterator<Entry<String, Fragment>> iterator = set.iterator();		
		while (iterator.hasNext())
		{
			Map.Entry<String, Fragment> mapentry = iterator.next();
			Fragment frag = (Fragment)mapentry.getValue();		
			doFragment(tbl, frag);
		}
		
		return tbl;
	}

	public void close() {		
		try {
			if(m_bFragMod){
				m_bFragMod = false;
				saveFragment(m_fragFile);
			}

			if (m_connect!=null){
				m_connect.close();
				m_connect = null;
			}
			
			for(ICursor c : m_cursors){
				if (c!=null){
					c.close();
					c = null;
				}
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}

	public boolean initJdbc(String drv, String url, String user, String pwd) {
		boolean bRet = false;
		try {
			do {
				m_connect = new ImConnection(drv, url, user, pwd);
				bRet = (m_connect.conn != null);
			} while (false);
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}

		return bRet;
	}

	public String getDbName() {
		DBConfig info = (DBConfig) getDbSession().getInfo();
		return info.getName();
	}
	
	/////////////////////////////////////////////////////////////
	public void initFrag(String fragFile){
		try {
			boolean bFragMod = false;
			File f = new File(fragFile);
			if (!f.exists()){
				f.createNewFile();
				bFragMod = true;
			}
			if (f.length()<22){
				bFragMod = true;
				m_bFragMod = true;
			}
			
			if (!bFragMod){			
				InputStream is = new FileInputStream(f);
				
				DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
				Document xmlDocument = docBuilder.parse(is);
				Element root = xmlDocument.getDocumentElement(); 
				
				if (root != null) {
					listNodes(root); 
				}
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}	
	}
	
	public void listNodes(Node node) {  
        // 节点是什么类型的节点  
        if (node.getNodeType() == Node.ELEMENT_NODE) {// 判断是否是元素节点  
            Element element = (Element) node;  
            //判断此元素节点是否有属性  
            if(element.hasAttributes()){  
                //获取属性节点的集合  
                NamedNodeMap namenm =   element.getAttributes();//Node  
                Fragment frag = new Fragment();
                //遍历属性节点的集合  
                for(int k=0;k<namenm.getLength();k++){
                    //获取具体的某个属性节点  
                    Attr attr = (Attr) namenm.item(k);  
                    //System.out.println("name:::"+attr.getNodeName()+" value::"  
                    //                 +attr.getNodeValue()+"  type::"+attr.getNodeType());  
                    if ("name".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setTableName(attr.getNodeValue());
                    }else if ("fieldName".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setFieldName(attr.getNodeValue());
                    }else if ("fieldtype".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setFieldType(Integer.parseInt(attr.getNodeValue()));
                    }else if ("minValue".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setPartitionVal(attr.getNodeValue());
                    }else if ("maxValue".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setMaxValue(attr.getNodeValue());
                    }else if ("ruleType".compareToIgnoreCase(attr.getNodeName())==0){
                    	frag.setRuleType(attr.getNodeValue().charAt(0));
                    }else if ("interval".compareToIgnoreCase(attr.getNodeName())==0){
                    	Object o = attr.getNodeValue();
                    	if (o!=null){
                    		frag.setInterval(Integer.parseInt(o.toString()));
                    	}
                    }
                }  
                String table = frag.getTableName();
                m_mapFrag.put(table, frag);
            }  
            //获取元素节点的所有孩子节点  
            NodeList listnode = element.getChildNodes();  
            //遍历  
            for (int j = 0; j < listnode.getLength(); j++) {  
                //得到某个具体的节点对象  
                Node nd = listnode.item(j);  
                //System.out.println("name::" + nd.getNodeName() + "  value:::"  
                //       + nd.getNodeValue() + "  type:::" + nd.getNodeType());  
                //重新调用遍历节点的操作的方法  
                listNodes(nd);  
            }  
        }  
    }
	
	public void setTakeFrag(Map<String, Fragment> mapFrag){
		m_mapFrag = mapFrag;
	}
	
	public void addCursor(ICursor c){
		m_cursors.add(c);
	}
}