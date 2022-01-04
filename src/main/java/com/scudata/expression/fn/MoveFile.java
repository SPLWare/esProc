package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Machines;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.parallel.PartitionUtil;
import com.scudata.resources.EngineMessage;

/**
 * 移动、删除或对文件重命名
 * movefile(fn,path) movefile(fn:z,path)
 * 将文件fn移动到指定路径文件path中，path省略代表将文件删除，path只有文件名时表示对文件重命名。
 * movefile(fn:z,h;p,hs)
 * 将分机h上的文件fn移到hs分机的p路径下，hs可是序列；h省略本机hs省略为改名，p,hs省略删除；h和p省略但hs不空则删除hs下的文件
 * 
 * @author runqian
 *
 */
public class MoveFile extends Function {
	public Node optimize(Context ctx) {
		if (param != null) {
			param.optimize(ctx);
		}
		
		return this;
	}

	// 本机文件移动movefile(fn:z,path)
	private static Object localFileMove(IParam param, String option, Context ctx) {
		IParam fnParam;
		String path = null;
		if (param.getType() == IParam.Comma) {
			if (param.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			fnParam = param.getSub(0);
			if (fnParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = param.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
			}
			
			path = (String)obj;
		} else {
			fnParam = param;
		}
		
		FileObject file;
		if (fnParam.isLeaf()) {
			Object pathObj = fnParam.getLeafExpression().calculate(ctx);
			if (pathObj instanceof FileObject) {
				file = (FileObject)pathObj;
			} else if (pathObj instanceof String) {
				file = new FileObject((String)pathObj, null, ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
			}
		} else {
			if (fnParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = fnParam.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}

			Object pathObj = sub0.getLeafExpression().calculate(ctx);
			if (!(pathObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
			}

			file = new FileObject((String)pathObj, null, ctx);
			IParam sub1 = fnParam.getSub(1);
			if (sub1 != null) {
				Object obj = sub1.getLeafExpression().calculate(ctx);
				if (obj instanceof Number) {
					int part = ((Number)obj).intValue();
					if (part > 0) {
						file.setPartition(part);
					}
				} else if (obj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
				}
			}
		}
		
		if (path == null || path.length() == 0) {
			if (option == null || option.indexOf('y') == -1) {
				return Boolean.valueOf(file.delete());
			} else {
				return Boolean.valueOf(file.deleteDir());
			}
		} else {
			return Boolean.valueOf(file.move(path, option));
		}
	}
	
	// 集群文件移动movefile(fn:z,h;p,hs)
	private static Object clusterFileMove(IParam param, String option, Context ctx) {
		if (param.getSubSize() != 2) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
		}
		
		IParam leftParam = param.getSub(0);
		if (leftParam == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
		}
		
		String srcFile; // 源文件
		int part = -1; // 分表号
		String host = null; // 源文件所在节点机
		int port = -1;
		IParam fnParam;
		
		if (leftParam.getType() == IParam.Comma) {
			// fn:z,h
			if (leftParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			fnParam = leftParam.getSub(0);
			if (fnParam == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = leftParam.getSub(1);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			Machines mc = new Machines();
			if (!mc.set(obj) || mc.size() != 1) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			host = mc.getHost(0);
			port = mc.getPort(0);
		} else {
			fnParam = leftParam;
		}
		
		if (fnParam.isLeaf()) {
			Object pathObj = fnParam.getLeafExpression().calculate(ctx);
			if (pathObj instanceof String) {
				srcFile = (String)pathObj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
		} else {
			if (fnParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}

			IParam sub = fnParam.getSub(0);
			if (sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}

			Object pathObj = sub.getLeafExpression().calculate(ctx);
			if (pathObj instanceof String) {
				srcFile = (String)pathObj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
			}

			sub = fnParam.getSub(1);
			if (sub != null) {
				Object csObj = sub.getLeafExpression().calculate(ctx);
				if (csObj instanceof Number) {
					part = ((Number)csObj).intValue();
				} else if (csObj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
				}
			}
		}
		
		String path = null; // 目标路径
		Machines hs = null; // 目标机器
		IParam rightParam = param.getSub(1);
		if (rightParam == null) {
		} else if (rightParam.isLeaf()) {
			Object obj = rightParam.getLeafExpression().calculate(ctx);
			if (!(obj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
			}
			
			path = (String)obj;
		} else {
			if (rightParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = rightParam.getSub(0);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				if (!(obj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("movefile" + mm.getMessage("function.paramTypeError"));
				}
				
				path = (String)obj;
			}
			
			sub = rightParam.getSub(1);
			if (sub != null) {
				Object obj = sub.getLeafExpression().calculate(ctx);
				hs = new Machines();
				if (!hs.set(obj)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("movefile" + mm.getMessage("function.invalidParam"));
				}
			}
		}
		
		return PartitionUtil.moveFile(host, port, srcFile, part, hs, path, option);
	}
	
	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("movefile" + mm.getMessage("function.missingParam"));
		} else if (param.getType() == IParam.Semicolon) {
			// movefile(fn:z,h;p,hs)
			return clusterFileMove(param, option, ctx);
		} else {
			// movefile(fn:z,path)
			return localFileMove(param, option, ctx);
		}
	}
}
