package com.raqsoft.parallel;

import java.util.HashMap;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.JobSpace;
import com.raqsoft.dm.JobSpaceManager;
import com.raqsoft.dm.Machines;
import com.raqsoft.dm.Record;
import com.raqsoft.dm.ResourceManager;
import com.raqsoft.dm.Sequence;
import com.raqsoft.dm.cursor.ICursor;
import com.raqsoft.dm.op.Operable;
import com.raqsoft.dm.op.Operation;
import com.raqsoft.dw.pseudo.IPseudo;
import com.raqsoft.dw.pseudo.PseudoDefination;
import com.raqsoft.dw.pseudo.PseudoDerive;
import com.raqsoft.dw.pseudo.PseudoNew;
import com.raqsoft.dw.pseudo.PseudoNews;
import com.raqsoft.dw.pseudo.PseudoTable;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.FunctionLib;

public class ClusterPseudo implements IClusterObject, Operable, IPseudo {
	public static final int TYPE_TABLE = 0;
	public static final int TYPE_NEW = 1;
	public static final int TYPE_NEWS = 2;
	public static final int TYPE_DERIVE = 3;
	
	private Cluster cluster; // 节点机信息
	private int []pseudoProxyIds; // 对应的节点机虚表代理标识
	
	public ClusterPseudo(Cluster cluster, int[] pseudoProxyIds) {
		this.cluster = cluster;
		this.pseudoProxyIds = pseudoProxyIds;
	}

	public static ClusterPseudo createClusterPseudo(Record rec, Machines hs, int n, Context ctx) {
		Cluster cluster = new Cluster(hs.getHosts(), hs.getPorts(), ctx);
		int count = cluster.getUnitCount();
		int[] newPseudoProxyIds = new int[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CREATE);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("rec", rec);
				command.setAttribute("n", new Integer(n));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}
		ClusterPseudo result = new ClusterPseudo(cluster, newPseudoProxyIds);
		return result;
	}
	
	
	public static Response executeCreateClusterPseudo(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		IPseudo pseudo = null;
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			Context ctx = ClusterUtil.createContext(js, attributes);
			
			Record rec = (Record) attributes.get("rec");
			Integer n = (Integer) attributes.get("n");
			Integer ptableId = (Integer) attributes.get("ptableId");
			String option = (String) attributes.get("option");
			Integer type = (Integer) attributes.get("type");
			
			if (type == null) {
				pseudo = new PseudoTable(rec, n, ctx);
			} else {
				PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(ptableId.intValue());
				IPseudo ptable = pseudoProxy.getPseudo();
				PseudoDefination pd = new PseudoDefination(rec, ctx);
				if (type == TYPE_NEW) {
					pseudo = new PseudoNew(pd, ptable, option);
				} else if (type == TYPE_NEWS) {
					pseudo = new PseudoNews(pd, ptable, option);
				} else if (type == TYPE_DERIVE) {
					pseudo = new PseudoDerive(pd, ptable, option);
				}
			}
			
			IProxy proxy = new PseudoProxy(pseudo);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public Cluster getCluster() {
		return cluster;
	}

	public void addColNames(String[] nameArray) {
		for (String name : nameArray) {
			addColName(name);
		}
	}

	public void addColName(String name) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_ADD_COLNAME);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				command.setAttribute("name", name);
				
 				Response response = client.send(command);
				response.checkResult();
			} finally {
				client.close();
			}
		}
	}
	
	public static Response executeAddColName(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer)attributes.get("pseudoProxyId");
		String name = (String)attributes.get("name");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudo = (PseudoProxy)rm.getProxy(pseudoProxyId.intValue());
			pseudo.getPseudo().addColName(name);
			return new Response();
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public ICursor cursor(Expression[] exps, String[] names) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		int[] cursorProxyIds = new int[count];
		
		String []expStrs = null;
		if (exps != null) {
			int len = exps.length;
			expStrs = new String[len];
			for (int i = 0; i < len; ++i) {
				if (exps[i] != null) {
					expStrs[i] = exps[i].toString();
				}
			}
		}
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CURSOR);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				
				command.setAttribute("expStrs",  expStrs);
				command.setAttribute("names", names);
				command.setAttribute("unit", new Integer(i));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				cursorProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}

		ClusterCursor result = new ClusterCursor(this, cursorProxyIds, true);
		return result;
	}

	public static Response executeCreateCursor(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer) attributes.get("pseudoProxyId");
		String []expStrs = (String[]) attributes.get("expStrs");
		String []names = (String[]) attributes.get("names");
		Integer unit = (Integer) attributes.get("unit");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(pseudoProxyId.intValue());
			IPseudo pseudo = pseudoProxy.getPseudo();
			Context ctx = ClusterUtil.createContext(js, attributes, "cursor", null);
			
			Expression []exps = null;
			if (expStrs != null) {
				int len = expStrs.length;
				exps = new Expression[len];
				for (int i = 0; i < len; ++i) {
					exps[i] = new Expression(ctx, expStrs[i]);
				}
			}
			
			ICursor cursor = pseudo.cursor(exps, names);
			IProxy proxy = new CursorProxy(cursor, unit);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public void addPKeyNames() {
		throw new RQException("never run to here");
	}

	public boolean isColumn(String col) {
		throw new RQException("never run to here");
	}

	public Context getContext() {
		throw new RQException("never run to here");
	}

	public Object clone(Context ctx) throws CloneNotSupportedException {
		int count = cluster.getUnitCount();
		int[] newPseudoProxyIds = new int[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));

			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_CLONE);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				
				Response response = client.send(command);
				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}

		ClusterPseudo result = new ClusterPseudo(cluster, newPseudoProxyIds);
		return result;
	}
	
	public static Response executeClone(HashMap<String, Object> attributes) {
		String jobSpaceID = (String) attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer) attributes.get("pseudoProxyId");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudoProxy = (PseudoProxy) rm.getProxy(pseudoProxyId.intValue());
			IPseudo pseudo = pseudoProxy.getPseudo();
			Context ctx = ClusterUtil.createContext(js, attributes, "cursor", null);
			
			Object obj = pseudo.clone(ctx);
			IProxy proxy = new PseudoProxy((IPseudo) obj);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
	
	public void append(ICursor cursor, String option) {
		// TODO Auto-generated method stub
		
	}

	public Sequence update(Sequence data, String opt) {
		// TODO Auto-generated method stub
		return null;
	}

	public Operable addOperation(Operation op, Context ctx) {
		Cluster cluster = getCluster();
		int count = cluster.getUnitCount();
		Function function = op.getFunction();
		String functionName = function.getFunctionName();
		String option = function.getOption();
		String param = function.getParamString();
		int[] newPseudoProxyIds = new int[count];
		
		for (int i = 0; i < count; ++i) {
			UnitClient client = new UnitClient(cluster.getHost(i), cluster.getPort(i));
			
			try {
				UnitCommand command = new UnitCommand(UnitCommand.PSEUDO_ADD_OPERATION);
				command.setAttribute("jobSpaceId", cluster.getJobSpaceId());
				command.setAttribute("pseudoProxyId", new Integer(pseudoProxyIds[i]));
				command.setAttribute("functionName", functionName);
				command.setAttribute("option", option);
				command.setAttribute("param", param);
				ClusterUtil.setParams(command, function, ctx);
				
 				Response response = client.send(command);
 				Integer id = (Integer) response.checkResult();
				newPseudoProxyIds[i] = id.intValue();
			} finally {
				client.close();
			}
		}
		
		ClusterPseudo result = new ClusterPseudo(cluster, newPseudoProxyIds);
		return result;
	}
	
	public static Response executeAddOperation(HashMap<String, Object> attributes) {
		String jobSpaceID = (String)attributes.get("jobSpaceId");
		Integer pseudoProxyId = (Integer)attributes.get("pseudoProxyId");
		String functionName = (String)attributes.get("functionName");
		String option = (String)attributes.get("option");
		String param = (String)attributes.get("param");
		
		try {
			JobSpace js = JobSpaceManager.getSpace(jobSpaceID);
			Context ctx = ClusterUtil.createContext(js, attributes, functionName, option);
			
			ResourceManager rm = js.getResourceManager();
			PseudoProxy pseudo = (PseudoProxy)rm.getProxy(pseudoProxyId.intValue());
			Object obj = FunctionLib.executeMemberFunction(pseudo.getPseudo(), functionName, param, option, ctx);
			
			IProxy proxy = new PseudoProxy((IPseudo) obj);
			rm.addProxy(proxy);
			return new Response(new Integer(proxy.getProxyId()));		
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
}
