package com.scudata.expression.fn;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileGroup;
import com.scudata.dm.FileObject;
import com.scudata.dm.LocalFile;
import com.scudata.dm.Machines;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.parallel.Cluster;
import com.scudata.parallel.ClusterFile;
import com.scudata.parallel.PartitionUtil;
import com.scudata.resources.EngineMessage;

/**
 * file(fn:cs) file(fn:cs,h)由文件名和字符集创建文件
 * file(fn:z) file(fn:z,h) 由文件名、分区和节点机列表创建集群文件或远程文件
 * file(fn:z,f) 由文件名和集群文件创建同分布的集群文件
 * @author runqian
 *
 */
public class CreateFile extends Function {
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public byte calcExpValueType(Context ctx) {
		return Expression.TYPE_FILE;
	}

	private static FileObject createTempFile(String pathName, String cs, String opt, Context ctx) {
		if (pathName == null) {
			pathName = Env.getTempPath();
			if (pathName == null || pathName.length() == 0) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.missingParam"));
			}
		}
		
		FileObject fo = new FileObject(pathName, null, ctx);
		String str = fo.createTempFile();
		str = LocalFile.removeMainPath(str, ctx);
		return new FileObject(str, cs, opt, ctx);
	}
	
	public Object calculate(Context ctx) {
		IParam fnParam = param;		
		if (fnParam == null) {
			if (option != null && option.indexOf('t') != -1) {
				return createTempFile(null, null, option, ctx);
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.missingParam"));
			}
		}

		ClusterFile clusterFile = null;
		Machines mc = null;
		if (fnParam.getType() == IParam.Comma) {
			if (fnParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.invalidParam"));
			}
			
			IParam sub = fnParam.getSub(1);
			fnParam = fnParam.getSub(0);
			if (fnParam == null || sub == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.invalidParam"));
			}
			
			Object obj = sub.getLeafExpression().calculate(ctx);
			if (obj instanceof ClusterFile) {
				clusterFile = (ClusterFile)obj;
			} else {
				mc = new Machines();
				if (!mc.set(obj)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("file" + mm.getMessage("function.invalidParam"));
				}
			}
		}

		String pathName = null;
		//int []partitions = null;
		Sequence partSeq = null;
		int part = -1;
		String cs = null;
		
		if (fnParam.isLeaf()) {
			Object pathObj = fnParam.getLeafExpression().calculate(ctx);
			if (pathObj instanceof String) {
				pathName = (String)pathObj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.invalidParam"));
			}
		} else {
			if (fnParam.getSubSize() != 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.invalidParam"));
			}

			IParam sub0 = fnParam.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.invalidParam"));
			}

			Object pathObj = sub0.getLeafExpression().calculate(ctx);
			if (pathObj instanceof String) {
				pathName = (String)pathObj;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("file" + mm.getMessage("function.paramTypeError"));
			}

			IParam sub1 = fnParam.getSub(1);
			if (sub1 != null) {
				Object csObj = sub1.getLeafExpression().calculate(ctx);
				if (csObj instanceof String) {
					cs = (String)csObj;
				} else if (csObj instanceof Number) {
					part = ((Number)csObj).intValue();
				} else if (csObj instanceof Sequence) {
					//partitions = ((Sequence)csObj).toIntArray();
					partSeq = (Sequence)csObj;
				} else if (csObj != null) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("file" + mm.getMessage("function.invalidParam"));
				}
			}
		}

		FileObject fo;
		if (clusterFile != null) {
			int []partitions = null;
			if (part >= 0) {
				partitions = new int[]{part};
			} else if (partSeq != null) {
				partitions = partSeq.toIntArray();
			}
			
			return new ClusterFile(clusterFile, pathName, partitions, option);
		} else if (mc != null) {
			if (partSeq != null) {
				Cluster cluster = new Cluster(mc.getHosts(), mc.getPorts(), ctx);
				return new ClusterFile(cluster, pathName, partSeq, option);
			} else if (option != null && option.indexOf('w') != -1) {
				Cluster cluster = new Cluster(mc.getHosts(), mc.getPorts(), ctx);
				if (part >= 0) {
					return new ClusterFile(cluster, pathName, new int[]{part}, option);
				} else {
					return new ClusterFile(cluster, pathName, option);
				}
			} else {
				fo = PartitionUtil.locate(mc, pathName, part);
				fo.setCharset(cs);
				fo.setOption(option);
				fo.setContext(ctx);//xq add， 记录当前的上下文，里面要使用spaceID
			}
		} else {
			if (option == null || option.indexOf('t') == -1) {
				if (partSeq != null) {
					int []partitions = partSeq.toIntArray();
					if (partitions.length > 1) {
						return new FileGroup(pathName, partitions);
					} else {
						fo = new FileObject(pathName, cs, option, ctx);
						fo.setPartition(partitions[0]);
					}
				} else {
					fo = new FileObject(pathName, cs, option, ctx);
					if (part >= 0) {
						fo.setPartition(part);
					}
				}
			} else {
				return createTempFile(pathName, cs, option, ctx);
			}
		}
		
		return fo;
	}
}
