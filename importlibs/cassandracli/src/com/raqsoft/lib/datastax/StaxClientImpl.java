package com.raqsoft.lib.datastax;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Cluster.Initializer;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.UDTValue;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.IResource;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.Table;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;

public class StaxClientImpl implements IResource {
//	package com.raqsoft.lib.datastax;
//	public class StaxClient implements IResource
//	{ 
//		public StaxClient(String node);
//		public StaxClient(List<String> nodes);
//		public void withCredentials(String user, String pwd);
//		public void withPort(int port);
//		public void connect();
//		public void connect(String keyspace);
//		public Table query(String cql, List<Object> values);
//		public ICursor cursor(String cql, List<Object> values);
//		public void close();
//	}

	/*
	类型转换： 
	UUID转成string
	Set、List、TupleValue、UDTValue等转成Sequence
	ByteBuffer转成byte[]
	byte、short转成int
	float转成double
	InetAddress转成string
	BigInteger转成decimal
	Map转成table(列名为k、v)
	
集算器函数:
stax_connect(nodes[:port],[user:pwd][;[keyspace:xx][,compressor:"lz4"][,initializer:"com.raqsoft.lib.datastax.InitializerImpl"])	//还有很多配置选项，调研下能否用配置文件来初始化
stax_query(staxClient, cql, [value1], [value2], ...)
stax_cursor(staxClient, cql, [value1], [value2], ...)	//暂时不提供预解析
stax_close(staxClient)
	*/
	private Cluster cluster = null;
	private Builder builder = null;
	private Session session = null;
	public StaxClientImpl(String node) throws Exception {
	    builder = Cluster.builder();
    	builder.addContactPoint(node);
	}
	public StaxClientImpl(List<String> nodes) throws Exception {
	    builder = Cluster.builder();
	    for (int i=0; i<nodes.size(); i++) {
	    	builder.addContactPoint(nodes.get(i));
	    }
	    
    	//builder.withCompression(ProtocolOptions.Compression.SNAPPY);
	}
	
	public StaxClientImpl(Initializer i) throws Exception {
		cluster = Cluster.buildFrom(i);
	}

	public void withCredentials(String user, String pwd) throws Exception {
	    if (user != null) {
	    	builder.withCredentials(user, pwd);
	    }
	}
	public void withCompression(String c) throws Exception {
	    if ("lz4".equalsIgnoreCase(c)) {
	    	builder.withCompression(ProtocolOptions.Compression.LZ4);	    	
	    } else if ("snappy".equalsIgnoreCase(c)) {
	    	builder.withCompression(ProtocolOptions.Compression.SNAPPY);	    	
	    }
	}

	public void withPort(int port) throws Exception {
	    if (port>0) {
	    	builder.withPort(port);
	    }
	}
	public void connect() throws Exception {
		connect(null);
	}
	public void connect(String keyspace) throws Exception {
	    if (cluster == null) cluster = builder.build();
	    if (keyspace != null) session = cluster.connect(keyspace);
	    else session = cluster.connect();
	}
	public Table query(String cql, List<Object> values) throws Exception {
		Table t = null;
	    ResultSet rs = null;

	    if (values != null && values.size()>0) {
		    PreparedStatement ps = session.prepare(cql);
		    rs = session.execute(ps.bind(values.toArray(new Object[values.size()])));
//		    if (values.size()==1) rs = session.execute(ps.bind(values.get(0)));
//		    if (values.size()==2) rs = session.execute(ps.bind(values.get(0),values.get(1)));
//		    if (values.size()==3) rs = session.execute(ps.bind(values.get(0),values.get(1),values.get(2)));
	    }//"select release_version from system.local");
	    else rs = session.execute(cql);
	    if (rs == null) return null;
	    String[] al = new String[rs.getColumnDefinitions().size()];
	    
  		for(int i=0;i<rs.getColumnDefinitions().size();i++){
  			al[i] = (rs.getColumnDefinitions().getName(i));
  		}
  		t = new Table(al);
  		Row row = rs.one();
	    while (row != null) {
	  		Record r = t.insert(0);
	    	for(int i=0;i<al.length;i++){
	  			Object o=row.getObject(i);
	  			//Logger.debug(o);
  				r.set(i, getValue(o));
	  		}
	    	
			row = rs.one();
	    }
//		int remaining = rs.getAvailableWithoutFetching();
//		if (rs.isFullyFetched()&& remaining==0) return null;	
		return t;
	}
	
	private Object getValue(Object o) {
//			类型转换： 
//			UUID转成string
//			Set、List、TupleValue、UDTValue等转成Sequence
//			ByteBuffer转成byte[]
//			byte、short转成int
//			float转成double
//			InetAddress转成string
//			BigInteger转成decimal
//			Map转成table(列名为k、v)
		if (o == null) return null;
		if (o instanceof java.util.UUID){
			java.util.UUID uuid= (java.util.UUID) o;
			return uuid.toString();
		}else if (o instanceof java.util.Set){
			Set s= (Set) o;
			Sequence seq = new Sequence();
			Iterator iter = s.iterator();
			while (iter.hasNext()) {
				seq.add(getValue(iter.next()));
			}
			return seq;			
		}else if (o instanceof List){
			List s= (List) o;
			Sequence seq = new Sequence();
			Iterator iter = s.iterator();
			while (iter.hasNext()) {
				seq.add(getValue(iter.next()));
			}
			return seq;
		}else if (o instanceof UDTValue){
			UDTValue s = (UDTValue)o;
			Sequence seq = new Sequence();
			for (int i=0; i<Integer.MAX_VALUE; i++) {
				try {
					seq.add(getValue(s.getObject(i)));
				} catch (Exception e) {
					break;
				}
			}
			return seq;
		}else if (o instanceof ByteBuffer){
			ByteBuffer s = (ByteBuffer)o;
			return s.array();
		}else if (o instanceof Byte){
			return new Integer((Byte)o);
		}else if (o instanceof Short){
			return new Integer((Short)o);
		}else if (o instanceof Float){
			return new Double((Float)o);
		}else if (o instanceof InetAddress){
			return ((InetAddress)o).toString();
		}else if (o instanceof Map){
			Map m = (Map)o;
			Iterator iter = m.keySet().iterator();
			Table t = new Table(new String[]{"k","v"});
			while (iter.hasNext()) {
				Object om = iter.next();
				Object vm = m.get(om);
				Record r = t.insert(0);
				r.set("k", getValue(om));
				r.set("v", getValue(vm));
			}
			return t;
		}
		return o.toString();
	}
	
	public ICursor cursor(String cql, List<Object> values) throws Exception {
		return null;
	}

	public String toString() {
		return info;
	}
	private String info = "";

	@Override
	public void close() {
		try {
		    if (cluster != null) cluster.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<ArrayList<ArrayList<Object>>> getParams(IParam ip, Context ctx) {
		ArrayList<ArrayList<ArrayList<Object>>> p = new ArrayList<ArrayList<ArrayList<Object>>>();
		char type = ip.getType();
		if (type == IParam.Semicolon) {
			int size = ip.getSubSize();
			for (int i=0; i<size; i++) {
				ArrayList<ArrayList<Object>> pi = new ArrayList<ArrayList<Object>>();
				p.add(pi);
				IParam ipi = ip.getSub(i);
				char typei = ipi.getType();
				if (typei == IParam.Comma) {
					int sizei = ipi.getSubSize();
					for (int j=0; j<sizei; j++) {
						ArrayList<Object> pj = new ArrayList<Object>();
						pi.add(pj);
						IParam ipj = ipi.getSub(j);
						char typej = ipj.getType();
						if (typej == IParam.Colon) {
							int sizej = ipj.getSubSize();
							for (int k=0; k<sizej; k++) {
								pj.add(getValue(ipj.getSub(k).getLeafExpression(),ctx));
							}
						} else {
							pj.add(getValue(ipj.getLeafExpression(),ctx));
						}
					}
				} else if (typei == IParam.Colon) {
					ArrayList<Object> pj = new ArrayList<Object>();
					pi.add(pj);
					int sizei = ipi.getSubSize();
					for (int k=0; k<sizei; k++) {
						pj.add(getValue(ipi.getSub(k).getLeafExpression(),ctx));
					}
				} else {
					ArrayList<Object> pj = new ArrayList<Object>();
					pi.add(pj);
					pj.add(getValue(ipi.getLeafExpression(),ctx));
				}
			}
		} else if (type == IParam.Comma) {
			int size = ip.getSubSize();
			ArrayList<ArrayList<Object>> pi = new ArrayList<ArrayList<Object>>();
			p.add(pi);
			for (int i=0; i<size; i++) {
				IParam ipi = ip.getSub(i);
				char typei = ipi.getType();
				if (typei == IParam.Colon) {
					ArrayList<Object> pj = new ArrayList<Object>();
					pi.add(pj);
					int sizei = ipi.getSubSize();
					for (int k=0; k<sizei; k++) {
						pj.add(getValue(ipi.getSub(k).getLeafExpression(),ctx));
					}
				} else {
					ArrayList<Object> pj = new ArrayList<Object>();
					pi.add(pj);
					pj.add(getValue(ipi.getLeafExpression(),ctx));
				}
			}
		} else if (type == IParam.Colon) {
			int size = ip.getSubSize();
			ArrayList<ArrayList<Object>> pi = new ArrayList<ArrayList<Object>>();
			p.add(pi);
			for (int i=0; i<size; i++) {
				IParam ipi = ip.getSub(i);
				ArrayList<Object> pj = new ArrayList<Object>();
				pi.add(pj);
				pj.add(getValue(ipi.getLeafExpression(),ctx));
			}
		} else {
			ArrayList<ArrayList<Object>> pi = new ArrayList<ArrayList<Object>>();
			p.add(pi);
			ArrayList<Object> pj = new ArrayList<Object>();
			pi.add(pj);
			pj.add(getValue(ip.getLeafExpression(),ctx));
		}
		return p;
	}
	
	private static Object getValue(Expression exp, Context ctx) {
		try {
			return exp.calculate(ctx);
		} catch (Exception e) {
			return exp.getIdentifierName();
		}
	}


	public static void main(String args[]) throws Exception {
//		StaxClientImpl stax = new StaxClientImpl("127.0.0.1");
//		stax.connect();
//		Table t = stax.query("select release_version from system.local", null);
//		Logger.debug("t is ["+t+"]");
//		stax.close();
		
		
		java.util.UUID uuid2 = java.util.UUID.fromString("62c36092-82a1-3a00-93d1-46196ee77205");
		System.out.println(uuid2);
		
		Cluster cluster = null;
		try {
//			Initializer myInitializer = new Initializer(){
//
//				@Override
//				public String getClusterName() {
//					// TODO Auto-generated method stub
//					return null;
//				}
//
//				@Override
//				public Configuration getConfiguration() {
//					//Configuration c = new Configuration(null, null, null, null, null, null, null, null, null);
//					//return c;
//					return null;
//				}
//
//				@Override
//				public List<InetSocketAddress> getContactPoints() {
//					// TODO Auto-generated method stub
//					return null;
//				}
//
//				@Override
//				public Collection<StateListener> getInitialListeners() {
//					// TODO Auto-generated method stub
//					return null;
//				}
//				
//			};
			
		    cluster = Cluster.builder()
		            .addContactPoint("127.0.0.1")
		            .withCredentials("test", "test")
		            .build();
		    Session session = cluster.connect("mykeyspace");

		    ResultSet rs = session.execute("select * from users2");
		    Row row = rs.one();
		    System.out.println(row.getString("user_name"));

		    //		    ResultSet rs = session.execute("select release_version from system.local");
//		    Row row = rs.one();
//		    System.out.println(row.getString("release_version"));
		} finally {
		    if (cluster != null) cluster.close();
		}		
	}
}
