package com.scudata.parallel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.scudata.cellset.INormalCell;
import com.scudata.common.IntArrayList;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.ResourceManager;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.resources.EngineMessage;

/**
 * 集群工具类
 * @author RunQian
 *
 */
final class ClusterUtil {
	/**
	 * 在节点机创建计算上下文
	 * @param js 任务空间
	 * @return Context
	 */
	public static Context createContext(JobSpace js) {
		Context ctx = new Context();
		ctx.setJobSpace(js);
		return ctx;
	}
	
	/**
	 * 在节点机创建计算上下文
	 * @param js 任务空间
	 * @param attributes  主机传来的参数
	 * @return Context
	 */
	public static Context createContext(JobSpace js, HashMap<String, Object> attributes) {
		if (attributes == null) {
			return createContext(js);
		}
		
		Context ctx = new Context();
		ctx.setJobSpace(js);
		
		// 取出参数创建成变量加入到上下文中
		String []paramNames = (String [])attributes.get("paramNames");
		if (paramNames != null) {
			Object []paramValues = (Object [])attributes.get("paramValues");
			int count = paramNames.length;
			for (int i = 0; i < count; ++i) {
				ctx.setParamValue(paramNames[i], paramValues[i]);
			}
		}
		
		return ctx;
	}
	
	// 判断集群内表是否只需要取当前节点机上的数据
	private static boolean isCurrentOnly(ClusterMemoryTable cmt, String func, String opt) {
		// 如果是分布内表并且没有c选项则使用所有分机的数据
		if (cmt.isDistributed()) {
			if (opt == null || opt.indexOf('c') == -1) {
				return false;
			}
			
			if (!func.equals("switch") && !func.equals("join")) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 在节点机创建计算上下文，集群内表变成相应unit对应的内表
	 * @param js 任务空间
	 * @param attributes 主机传来的参数
	 * @param func 调用此方法的函数名
	 * @param opt 选项
	 * @return Context
	 */
	public static Context createContext(JobSpace js, HashMap<String, Object> attributes, String func, String opt) {
		if (attributes == null) {
			return createContext(js);
		}
		
		Context ctx = new Context();
		ctx.setJobSpace(js);
		
		// 取出参数创建成变量加入到上下文中
		String []paramNames = (String [])attributes.get("paramNames");
		if (paramNames != null) {
			Object []paramValues = (Object [])attributes.get("paramValues");
			int count = paramNames.length;
			for (int i = 0; i < count; ++i) {
				Object val = paramValues[i];
				if (val instanceof ClusterMemoryTable) {
					ClusterMemoryTable cmt = (ClusterMemoryTable)val;
					if (isCurrentOnly(cmt, func, opt)) {
						// 复写内表或者有c选项时只取当前节点机上对应的部分
						int id = cmt.getCurrentClusterProxyId();
						ResourceManager rm = js.getResourceManager();
						TableProxy table = (TableProxy)rm.getProxy(id);
						if (table == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(func + mm.getMessage("function.invalidParam"));
						}
						
						val = table.getTable();
					}
				}
				
				ctx.setParamValue(paramNames[i], val);
			}
		}
		
		return ctx;
	}
	
	/**
	 * 把表达式中引用到的参数和网格设置到命令中传给节点机
	 * @param command 命令
	 * @param exp 表达式
	 * @param ctx 计算上下文
	 */
	public static void setParams(UnitCommand command, Expression exp, Context ctx) {
		if (exp != null) {
			ParamList paramList = new ParamList();
			List<INormalCell> cellList = new ArrayList<INormalCell>();
			exp.getUsedParams(ctx, paramList);
			exp.getUsedCells(cellList);
			setParams(command, paramList, cellList);
		}
	}
	
	/**
	 * 把表达式数组中引用到的参数和网格设置到命令中传给节点机
	 * @param command 命令
	 * @param exps 表达式数组
	 * @param ctx 计算上下文
	 */
	public static void setParams(UnitCommand command, Expression []exps, Context ctx) {
		if (exps != null) {
			ParamList paramList = new ParamList();
			List<INormalCell> cellList = new ArrayList<INormalCell>();
			for (Expression exp : exps) {
				exp.getUsedParams(ctx, paramList);
				exp.getUsedCells(cellList);
			}
			
			setParams(command, paramList, cellList);
		}
	}
	
	/**
	 * 把函数中引用到的参数和网格设置到命令中传给节点机
	 * @param command 命令
	 * @param function 函数
	 * @param ctx 计算上下文
	 */
	public static void setParams(UnitCommand command, Function function, Context ctx) {
		IParam param = function.getParam();
		if (param != null) {
			ParamList paramList = new ParamList();
			List<INormalCell> cellList = new ArrayList<INormalCell>();
			param.getUsedParams(ctx, paramList);
			param.getUsedCells(cellList);
			setParams(command, paramList, cellList);
		}
	}
	
	private static void setParams(UnitCommand command, ParamList paramList, List<INormalCell> cellList) {
		int paramCount = paramList.count();
		int cellCount = cellList.size();
		int total = paramCount + cellCount;
		if (total == 0) {
			return;
		}
		
		String []paramNames = new String[total];
		Object []paramValues = new Object[total];
		for (int i = 0; i < paramCount; ++i) {
			Param param = paramList.get(i);
			paramNames[i] = param.getName();
			paramValues[i] = param.getValue();
		}
		
		for (int c = 0, i = paramCount; c < cellCount; ++c, ++i) {
			INormalCell cell = cellList.get(c);
			paramNames[i] = cell.getCellId();
			paramValues[i] = cell.getValue(true);
		}
		
		command.setAttribute("paramNames", paramNames);
		command.setAttribute("paramValues", paramValues);
	}
	
	/**
	 * 列出节点机上有哪些分表文件
	 * @param host 节点机的IP地址
	 * @param port 节点机的端口
	 * @param fileName 文件路径名
	 * @param parts 要查找的分表
	 * @return 节点机上包含的分表
	 */
	public static int[] listFileParts(String host, int port, String fileName, int []parts) {
		UnitClient client = new UnitClient(host, port);
		try {
			UnitCommand command = new UnitCommand(UnitCommand.LIST_FILE_PARTS);
			command.setAttribute("fileName", fileName);
			command.setAttribute("parts", parts);
			Response response = client.send(command);
			return (int[])response.checkResult();
		} finally {
			client.close();
		}
	}

	/**
	 * 节点机上执行列出节点机上有哪些分表文件命令
	 * @param attributes
	 * @return
	 */
	public static Response executeListFileParts(HashMap<String, Object> attributes) {
		String fileName = (String)attributes.get("fileName");
		int []parts = (int[])attributes.get("parts");
		IntArrayList list = new IntArrayList(parts.length);
		
		try {
			for (int part : parts) {
				LocalFile localFile = new LocalFile(fileName, null, part);
				if (localFile.exists()) {
					list.addInt(part);
				}
			}
			
			if (list.size() > 0) {
				return new Response(list.toIntArray());
			} else {
				return new Response();
			}
		} catch (Exception e) {
			Response response = new Response();
			response.setException(e);
			return response;
		}
	}
}
