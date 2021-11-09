package com.raqsoft.lib.datastax;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.Cluster.Initializer;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.Node;
import com.raqsoft.resources.EngineMessage;

public class StaxClient  extends Function {

	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		return this;
	}

	public Object calculate(Context ctx) {
		MessageManager mm = EngineMessage.get();
		if (param == null) {
			throw new RQException("stax_client" + mm.getMessage("function.missingParam"));
		}

		try {
			String option = this.option;
			if (option == null) option = "";
	
			//stax_connect(nodes[:port],[user:pwd][;[keyspace:xx][,compressor:"lz4"][,initializer:"com.raqsoft.lib.datastax.InitializerImpl"])	//还有很多配置选项，调研下能否用配置文件来初始化
			List<String> ns = new ArrayList<String>();
			int port = 9042;
			String user = null;
			String pwd = null;
			String keyspace = null;
			String compressor = null;
			String initializer = null;
			
			ArrayList<ArrayList<ArrayList<Object>>> params = StaxClientImpl.getParams(param,ctx);
			
			if (params.size()==0) {
				throw new RQException("stax_client" + mm.getMessage("function.invalidParam"));
			}
	
			Object nodes = params.get(0).get(0).get(0);
			if (nodes == null) {
				throw new RQException("stax_client" + mm.getMessage("function.invalidParam"));
			} else if (nodes instanceof String) ns.add(nodes.toString());
			else if (nodes instanceof Sequence) {
				Sequence seq = (Sequence)nodes;
				if (seq.length()==0) {
					throw new RQException("stax_client" + mm.getMessage("function.invalidParam"));
				}
				for (int i=1; i<=seq.length(); i++) ns.add(seq.get(i).toString());
			} else {
				throw new RQException("stax_client" + mm.getMessage("function.invalidParam"));
			}
			
			if (params.get(0).get(0).size()>1) port = Integer.parseInt(params.get(0).get(0).get(1).toString());
			if (params.get(0).size()>1) {
				user = params.get(0).get(1).get(0).toString();
				if (params.get(0).get(1).size()>1) pwd = params.get(0).get(1).get(1).toString();
				else pwd = "";
			}
			if (params.size()>1) {
				for (int i=0; i<params.get(1).size(); i++) {
					ArrayList<Object> oi = params.get(1).get(i);
					if (oi.size() != 2) {
						throw new RQException("stax_client" + mm.getMessage("function.invalidParam"));
					} else {
						if ("keyspace".equalsIgnoreCase(oi.get(0).toString())) {
							keyspace = oi.get(1).toString();
						} else if ("compressor".equalsIgnoreCase(oi.get(0).toString())) {
							compressor = oi.get(1).toString();
						} else if ("initializer".equalsIgnoreCase(oi.get(0).toString())) {
							initializer = oi.get(1).toString();
						}
					} 
				}
			}
			Logger.debug(user);
			Logger.debug(pwd);
			Logger.debug(keyspace);
			Logger.debug(compressor);
			Logger.debug(initializer);
			
			if (initializer != null) {
				Class c = Class.forName(initializer);
				Object o = c.newInstance();
				if (!(o instanceof InitializerProxy)) {
					throw new RQException("stax_client : initializer must implement [com.datastax.driver.core.Cluster.Initializer]");
				}
				
				InitializerProxy proxy = (InitializerProxy)o;
//				List<InetSocketAddress> addresses = new ArrayList<InetSocketAddress>();
//				for (int i=0; i<ns.size(); i++) {
//					addresses.add(new InetSocketAddress(ns.get(i),port));
//				}
				proxy.addresses = ns;
				proxy.port = port;
				proxy.compressor = compressor;
				proxy.username = user;
				proxy.password = pwd;				
				StaxClientImpl stax = new StaxClientImpl(proxy.buildInitializer());
				stax.connect(keyspace);
				return stax;
			} else {
				StaxClientImpl stax = new StaxClientImpl(ns);
				stax.withPort(port);
				stax.withCredentials(user, pwd);
				stax.connect(keyspace);
				return stax;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RQException("stax_client : " + e.getMessage());
		}
		
	}
}
