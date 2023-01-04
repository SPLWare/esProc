package com.scudata.dw;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.scudata.common.DateFactory;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.expression.Constant;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.expression.ParamInfo2;
import com.scudata.expression.UnknownSymbol;
import com.scudata.expression.fn.Between;
import com.scudata.expression.fn.datetime.Month;
import com.scudata.expression.fn.datetime.Year;
import com.scudata.expression.mfn.sequence.Contain;
import com.scudata.expression.operator.And;
import com.scudata.expression.operator.DotOperator;
import com.scudata.expression.operator.Equals;
import com.scudata.expression.operator.Greater;
import com.scudata.expression.operator.NotGreater;
import com.scudata.expression.operator.NotSmaller;
import com.scudata.expression.operator.Or;
import com.scudata.expression.operator.Smaller;
import com.scudata.parallel.ClusterCursor;
import com.scudata.parallel.ClusterPhyTable;
import com.scudata.resources.EngineMessage;
import com.scudata.util.EnvUtil;
import com.scudata.util.Variant;

/**
 * 立方体类
 * 继承自行存组表，用于存储一个表的预汇总结果
 * @author runqian
 *
 */
public class Cuboid extends RowComTable {
	public static final String CUBE_PREFIX = "_CUBOID@";//立方体文件前缀
	private static final int FIXED_OBJ_LEN = 9;//定长字段的长度
	//文件头增加以下内容
	protected String[] exps;//分组表达式
	protected String[] newExps;//汇总表达式
	private long srcCount;//原表里已分组条数 

	protected CuboidTable baseTable;
	
	/**
	 * 打开,会检查备份标志
	 * @param file 立方体文件
	 * @param ctx
	 * @throws IOException
	 */
	public Cuboid(File file, Context ctx) throws IOException {
		super();
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		this.ctx = ctx;
		if (ctx != null)
			ctx.addResource(this);
		readHeader();
	}

	/**
	 * 新建一个立方体
	 * @param file 文件对象
	 * @param colNames 列名称
	 * @param serialBytesLen 每个列的排号长度
	 * @param ctx
	 * @param writePsw 写密码
	 * @param readPsw 读密码
	 * @param exps 分组表达式
	 * @param newExps 汇总表达式
	 * @throws IOException
	 */
	public Cuboid(File file, String []colNames, int []serialBytesLen, Context ctx, 
			String writePsw, String readPsw, String[] exps, String[] newExps) throws IOException {
		super(file, colNames, null, null, ctx);
		this.exps = exps;
		this.newExps = newExps;
		srcCount = 0;
	}
	
	/**
	 * 读文件头
	 */
	protected void readHeader() throws IOException {
		Object syncObj = getSyncObject();
		synchronized(syncObj) {
			restoreTransaction();
			raf.seek(0);
			byte []bytes = new byte[32];
			raf.read(bytes);
			if (bytes[0] != 'r' || bytes[1] != 'q' || bytes[2] != 'd' || bytes[3] != 'w' || bytes[4] != 'g' || bytes[5] != 't' || bytes[6] != 'r') {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			
			BufferReader reader = new BufferReader(structManager, bytes, 7, 25);
			setBlockSize(reader.readInt32());
			headerBlockLink = new BlockLink(this);
			headerBlockLink.readExternal(reader);
			
			BlockLinkReader headerReader = new BlockLinkReader(headerBlockLink);
			bytes = headerReader.readBlocks();
			headerReader.close();
			reader = new BufferReader(structManager, bytes);
			reader.read(); // r
			reader.read(); // q
			reader.read(); // d
			reader.read(); // w
			reader.read(); // g
			reader.read(); // t
			reader.read(); // r
			
			blockSize = reader.readInt32();
			headerBlockLink.readExternal(reader);
			
			reader.read(reserve); // 保留位
			freePos = reader.readLong40();
			fileSize = reader.readLong40();
			
			if (reserve[0] > 0) {
				writePswHash = reader.readString();
				readPswHash = reader.readString();
				checkPassword(null);
				
				if (reserve[0] > 1) {
					distribute = reader.readString();
				}
			}
	
			int dsCount = reader.readInt();
			if (dsCount > 0) {
				ArrayList<DataStruct> dsList = new ArrayList<DataStruct>(dsCount);
				for (int i = 0; i < dsCount; ++i) {
					String []fieldNames = reader.readStrings();
					DataStruct ds = new DataStruct(fieldNames);
					dsList.add(ds);
				}
				
				structManager = new StructManager(dsList);
			} else {
				structManager = new StructManager();
			}

			baseTable = new CuboidTable(this, null);
			baseTable.readExternal(reader);
			super.baseTable = baseTable;
			
			exps = reader.readStrings();
			newExps = reader.readStrings();
			srcCount = reader.readLong40();
			
		}
	}
	
	/**
	 * 写出文件头
	 */
	public void writeHeader() throws IOException {
		Object syncObj = getSyncObject();
		synchronized(syncObj) {
			beginTransaction(null);
			BufferWriter writer = new BufferWriter(structManager);
			writer.write('r');
			writer.write('q');
			writer.write('d');
			writer.write('w');
			writer.write('g');
			writer.write('t');
			writer.write('r');
			
			writer.writeInt32(blockSize);
			headerBlockLink.writeExternal(writer);
			
			reserve[0] = 3; // 1增加密码，2增加分布函数，3增加预分组
			writer.write(reserve); // 保留位
			
			writer.writeLong40(freePos);
			writer.writeLong40(fileSize);
			
			// 下面两个成员版本1增加的
			writer.writeString(writePswHash);
			writer.writeString(readPswHash);
			
			writer.writeString(distribute); // 版本2增加
	
			ArrayList<DataStruct> dsList = structManager.getStructList();
			if (dsList != null) {
				writer.writeInt(dsList.size());
				for (DataStruct ds : dsList) {
					String []fieldNames = ds.getFieldNames();
					writer.writeStrings(fieldNames);
				}
			} else {
				writer.writeInt(0);
			}
			super.baseTable.writeExternal(writer);
			
			writer.writeStrings(exps);
			writer.writeStrings(newExps);
			writer.writeLong40(srcCount);
			
			BlockLinkWriter headerWriter = new BlockLinkWriter(headerBlockLink, false);
			headerWriter.rewriteBlocks(writer.finish());
			headerWriter.close();
			
			// 重写headerBlockLink
			writer.write('r');
			writer.write('q');
			writer.write('d');
			writer.write('w');
			writer.write('g');
			writer.write('t');
			writer.write('r');
			
			writer.writeInt32(blockSize);
			headerBlockLink.writeExternal(writer);
			raf.seek(0);
			raf.write(writer.finish());
			raf.getChannel().force(true);
			commitTransaction(0);
		}
	}

	public void save() throws IOException {
		super.save();
	}
	
	public void setCount(long count) {
		this.setSrcCount(count);
	}

	/**
	 * 格式化表达式，如果跟预分组字段匹配则加''，如果不匹配则认为是表达式
	 * @param exps
	 * @param ds
	 */
	private static void formatExps(Expression exps[], DataStruct ds) {
		int expsLen = exps == null ? 0 : exps.length;
		for (int i = 0; i < expsLen; i++) {
			if (ds.getFieldIndex(exps[i].getIdentifierName()) >= 0) {
				//如果直接就是预分组的字段
				exps[i] = new Expression("'" + exps[i].getIdentifierName() + "'");
			}
		}
	}
	
	/**
	 * 取得分组表达式exps和聚合表达式newExps里用到的字段，并返回原表游标
	 * @param srcTable 原表
	 * @param exps 分组表达式
	 * @param newExps 聚合表达式
	 * @param w 过滤表达式
	 * @param hasM 是否多路
	 * @param n 并行路数
	 * @param ctx 上下文
	 * @return
	 */
	private static ICursor makeCursor(PhyTable srcTable, Expression []exps, Expression []newExps,
			Expression w, boolean hasM, int n, Context ctx) {
		List<String> list = new ArrayList<String>();
		if (exps != null) {
			for (Expression exp : exps) {
				exp.getUsedFields(ctx, list);
			}
		}
		if (newExps != null) {
			for (Expression exp : newExps) {
				exp.getUsedFields(ctx, list);
			}
		}
		String fields[] = new String[list.size()];
		list.toArray(fields);
		
		if (hasM) {
			return srcTable.cursor(null, fields, w, null, null, null, n, ctx);
		} else {
			return srcTable.cursor(fields, w, ctx);
		}
	}
	
	/**
	 * 分组
	 * @param exps 分组表达式
	 * @param names 分组表达式新名字
	 * @param newExps 汇总表达式
	 * @param newNames 汇总表达式新名字
	 * @param srcTable 要分组的组表
	 * @param w 过滤条件
	 * @param hasM 是否并行
	 * @param n 并行数
	 * @param option
	 * @param ctx
	 * @return
	 */
	public static Sequence cgroups(String []expNames, String []names, String []newExpNames, String []newNames,
			PhyTable srcTable, Expression w, boolean hasM, int n, String option,  Context ctx) {
		//注意，cgroups_得到的是包含补文件cuboid的
		
		return cgroups_(expNames, names, newExpNames, newNames, srcTable, w, hasM, n, option, ctx);
		
		/*TableMetaData tmd = srcTable.getSupplementTable(false);
		if (tmd == null) {
			return seq;
		} else {			
			//组织分组表达式
			int count = names.length;
			Expression exps[] = new Expression[count];
			for (int i = 0; i < count; i++) {
				exps[i] = new Expression(names[i]);
			}
			
			//组织再聚合表达式
			Expression newExps[] = new Expression[count = newNames.length];//用来存储要聚合的原字段
			for (int i = 0, len = count; i < len; i++) {
				String str = newExpNames[i];
				//对count再聚合，要变为累加
				if (str.indexOf("count(") != -1) {
					str = str.replaceFirst("count", "sum");
				}
				
				//再聚合时要替换一下字段名
				String sub = str.substring(str.indexOf('(') + 1, str.indexOf(')'));
				str = str.replaceAll(sub, "'" + newNames[i] + "'");
				newExps[i] = new Expression(str);
			}
			return seq.groups(exps, names, newExps, newNames, option, ctx);
		}*/
	}
	
	private static Sequence cgroups_(String []expNames, String []names, String []newExpNames, String []newNames,
			PhyTable srcTable,	Expression w, boolean hasM, int n, String option,  Context ctx) {
		Expression []exps = null;
		if (expNames != null) {
			int len = expNames.length;
			exps = new Expression[len];
			for (int i = 0; i < len; i++) {
				exps[i] = new Expression(expNames[i]);
			}
		}
		
		Expression []newExps = null;
		Expression []newExpsBak = null;
		if (newExpNames != null) {
			int len = newExpNames.length;
			newExps = new Expression[len];
			newExpsBak = new Expression[len];
			for (int i = 0; i < len; i++) {
				newExps[i] = new Expression(newExpNames[i]);
				newExpsBak[i] = new Expression(newExpNames[i]);
			}
		}
		
		int fcount = exps == null ? 0 : exps.length;
		if (newExps != null) fcount += newExps.length;
		if (newExpNames != null) {
			for (int i = 0; i < newExpNames.length; i++) {
				String s = new String(newExpNames[i]);
				newExpNames[i] = s;// s.replaceAll(regex, "_");
			}
		}

		Object obj = findCuboid(srcTable, expNames, newExpNames, w, ctx);
		try {
			if (obj instanceof ArrayList) {
				//有多个预分组立方体
				@SuppressWarnings("unchecked")
				ArrayList<PhyTable> tableList = (ArrayList<PhyTable>) obj;
				int size = tableList.size();
				if (size == 0) {
					//没有预分组立方体
					ICursor cs = makeCursor(srcTable, exps, newExps, w, hasM, n, ctx);
					return cs.groups(exps, names, newExps, newNames, option, ctx);
				} else {
					CuboidTable table = null;
					if (size == 1) {
						table = (CuboidTable) tableList.get(0);
					} else {
						//选一个记录条数最少的
						int idx = 0;
						long cnt = tableList.get(0).getTotalRecordCount();
						for (int i = 1; i < size; i++) {
							long l = tableList.get(i).getTotalRecordCount();
							if (cnt < l) {
								idx = i;
								cnt = l;
							}
						}
						for (int i = 0; i < size; i++) {
							if (i != idx) {
								tableList.get(i).close();
							}
						}
						table = (CuboidTable) tableList.get(idx);
					}

					//检查预分组是否是最新的
					if (((Cuboid) table.groupTable).getSrcCount() != srcTable.getActualRecordCount()) {
						try {
							((Cuboid) table.groupTable).update(srcTable);
						} catch (Exception e) {
							if (table != null)
								table.close();
							for (PhyTable tbl : tableList) {
								tbl.close();
							}
							throw new RQException(e.getMessage(), e);
						}
					}

					ArrayList<Expression> fieldExpList = new ArrayList<Expression>();
					Expression[] fieldExps;//再聚合时要取出来的字段
					int expsLen = exps == null ? 0 : exps.length;
					DataStruct ds = table.getDataStruct();
					for (int i = 0; i < expsLen; i++) {
						if (ds.getFieldIndex(expNames[i]) >= 0) {
							//如果直接就是预分组的字段
							fieldExpList.add(new Expression("'" + expNames[i] + "'"));
						} else {
							//提取涉及到的字段
							Expression exp = new Expression(expNames[i]);
							ArrayList<String> strList = new ArrayList<String>();
							exp.getUsedFields(ctx, strList);
							for (String str : strList) {
								fieldExpList.add(new Expression(str));
							}
						}
					}
					for (int i = expsLen; i < fcount; i++) {
						fieldExpList.add(new Expression("'" + newExpNames[i - expsLen] + "'"));
					}
					fieldExps = new Expression[fieldExpList.size()];
					fieldExpList.toArray(fieldExps);

					Expression newSrcExps[] = new Expression[newExps.length];//用来存储要聚合的原字段
					for (int i = 0, len = newExps.length; i < len; i++) {
						String str = new String(newExps[i].getIdentifierName());
						if (newNames[i] == null) {
							newNames[i] = str;
						}

						//对count再聚合，要变为累加
						if (str.indexOf("count(") != -1) {
							str = str.replaceFirst("count", "sum");
						}

						//对预分组立方体再聚合时要替换一下字段名
						String sub = str.substring(str.indexOf('(') + 1, str.indexOf(')'));
						str = str.replaceAll(sub, "'" + newExpNames[i] + "'");
						newExps[i] = new Expression(str);
						if (str.indexOf("top(") == 0) {
							sub = sub.substring(sub.indexOf(',') + 1);
						}
						newSrcExps[i] = new Expression(sub);
					}

					//处理时间区间优化
					while (w != null) {
						//从立方体里取出来日期分组的字段，可能有多个
						ArrayList<Object> list = new ArrayList<Object>();
						getDateFields(table, list);

						//用这些日期字段去匹配w
						int count = list.size();
						if (count > 0) {
							Object[] interval = new Object[2];
							boolean[] isEQ = new boolean[2];
							ArrayList<Node> otherNodes = new ArrayList<Node>();
							for (int i = 0; i < count; i += 2) {
								String fname = (String) list.get(i);
								getDateInterval(fname, w.getHome(), interval, isEQ, otherNodes, (Integer) list.get(i + 1), ctx);
								if (interval[0] != null || interval[1] != null) {
									//如果w里有时间区间
									Node nodes[] = makeNode(fname, table.colNames, interval, isEQ,
											(Integer) list.get(i + 1));
									if (nodes != null) {
										if (nodes[0] == null) {
											//只需要访问立方体
											otherNodes.add(nodes[1]);
											Expression w2 = conbineNodes(otherNodes);
											ICursor cursor2 = table.cursor(null, w2, ctx);
											return cursor2.groups(exps, names, newExps, newNames, null, ctx);
										}
										@SuppressWarnings("unchecked")
										ArrayList<Node> otherNodes2 = (ArrayList<Node>) otherNodes.clone();

										otherNodes.add(nodes[0]);
										Expression w1 = conbineNodes(otherNodes);
										Expression tempExps[] = fieldExps.clone();
										System.arraycopy(newSrcExps, 0, tempExps, expsLen, newSrcExps.length);
										ICursor cursor1 = srcTable.cursor(null, w1, ctx);

										ICursor tempCursor = new MemoryCursor(cursor1.groups(exps, names,
												newExpsBak, newExpNames, null, ctx));

										otherNodes2.add(nodes[1]);
										Expression w2 = conbineNodes(otherNodes2);
										ICursor cursor2 = table.cursor(null, w2, ctx);

										ICursor[] cursors = new ICursor[] { tempCursor, cursor2 };
										ICursor mcursor = new MultipathCursors(cursors, ctx);//ICursor mcursor = new ConjxCursor(cursors);
										return mcursor.groups(exps, names, newExps, newNames, null, ctx);
									}
								}

								interval[0] = null;
								interval[1] = null;
								otherNodes.clear();
							}
						} else {
							//没有日期字段
							break;
						}

						//无法使用预分组立方体(当w区间小于立方体分组的单位，比如不够一个年，才会运行到这里，实际上这里可以再检查下一个立方体)
						for (int i = 0; i < newExps.length; i++) {
							newExps[i] = new Expression(newNames[i]);
						}
						ICursor cs = makeCursor(srcTable, exps, newExpsBak, w, hasM, n, ctx);
						return cs.groups(exps, names, newExpsBak, newNames, option, ctx);
					}

					try {
						ICursor cs;
						if (hasM) {
							cs = table.cursor(null, null, w, ctx, n);
						} else {
							cs = table.cursor(null, null, w, ctx);
						}
						formatExps(exps, ds);
						return cs.groups(exps, names, newExps, newNames, null, ctx);
					} catch (Exception e) {
						table.close();
						throw new RQException("cgroups:" + e.getMessage(), e);
					}
				}
			} else {
				PhyTable table = (PhyTable) obj;
				Expression[] fieldExps = new Expression[fcount];
				for (int i = exps.length; i < fcount; i++) {
					fieldExps[i] = new Expression("'" + newExpNames[i - exps.length] + "'");
				}

				String[] fieldNames = new String[fcount];
				for (int i = 0, len = names.length; i < len; i++) {
					if (names[i] == null) {
						names[i] = exps[i].getIdentifierName();
					}
					fieldExps[i] = new Expression(exps[i].getIdentifierName());
				}
				for (int i = 0, len = newNames.length; i < len; i++) {
					if (newNames[i] == null) {
						newNames[i] = newExps[i].getIdentifierName();
					}
				}
				System.arraycopy(names, 0, fieldNames, 0, names.length);
				System.arraycopy(newNames, 0, fieldNames, names.length, newNames.length);

				if (hasM && table.dataBlockCount < n) {
					hasM = false;
				}

				//转换w
				if (w != null) {
					String str = w.getIdentifierName();
					for (int i = 0, len = names.length; i < len; i++) {
						str = str.replace(fieldNames[i], "'" + fieldNames[i] + "'");
					}
					w = new Expression(str);
				}
				Sequence result;
				if (hasM) {
					result = table.cursor(fieldExps, fieldNames, w, null, null, null, n, ctx).fetch();
				} else {
					result = table.cursor(fieldExps, fieldNames, w, null, null, null, ctx).fetch();
				}
				if (!(result instanceof Table)) {
					Table seq = new Table(result.dataStruct());
					seq.addAll(result);
					return seq;
				}
				return result;
			}
		} finally {
			if (obj != null) {
				if (obj instanceof ArrayList) {
					@SuppressWarnings("unchecked")
					ArrayList<PhyTable> tableList = (ArrayList<PhyTable>) obj;
					for (PhyTable table : tableList) {
						table.close();
					}
				} else {
					PhyTable table = (PhyTable) obj;
					table.close();
				}
				
			}
		}
	}
	
	/**
	 * 集群表的cgroups
	 * @param exps 分组表达式
	 * @param names 分组表达式新名字
	 * @param newExps 汇总表达式
	 * @param newNames 汇总表达式新名字
	 * @param srcTable 要分组的组表
	 * @param w 过滤条件
	 * @param hasM 是否并行
	 * @param n 并行路数
	 * @param option 分组属性
	 * @param ctx 上下文
	 * @return
	 */
	public static Sequence cgroups(String []expNames, String []names, String []newExpNames, String []newNames,
			ClusterPhyTable srcTable,	Expression w, boolean hasM, int n, String option,  Context ctx) {
		Expression []exps = null;
		if (expNames != null) {
			int len = expNames.length;
			exps = new Expression[len];
			for (int i = 0; i < len; i++) {
				exps[i] = new Expression(expNames[i]);
			}
		}
		
		Expression []newExps = null;
		Expression []newExpsBak = null;
		if (newExpNames != null) {
			int len = newExpNames.length;
			newExps = new Expression[len];
			newExpsBak = new Expression[len];
			for (int i = 0; i < len; i++) {
				newExps[i] = new Expression(newExpNames[i]);
				newExpsBak[i] = new Expression(newExpNames[i]);
			}
		}
		
		int fcount = exps == null ? 0 : exps.length;
		if (newExps != null) fcount += newExps.length;
		if (newExpNames != null) {
			for (int i = 0; i < newExpNames.length; i++) {
				String s = new String(newExpNames[i]);
				newExpNames[i] = s;
			}
		}

		Object obj = findCuboid(srcTable, expNames, newExpNames, w, ctx);
		try {
			if (obj instanceof ArrayList) {
				//有多个预分组立方体
				@SuppressWarnings("unchecked")
				ArrayList<PhyTable> tableList = (ArrayList<PhyTable>) obj;
				int size = tableList.size();
				if (size == 0) {
					//没有预分组立方体
					ClusterCursor cc = srcTable.cursor(null, null, null, null, null, null, 0, null, ctx);
					return (Sequence) cc.groups(exps, names, newExps, newNames, option, ctx);
				} else {
					CuboidTable table = null;
					if (size == 1) {
						table = (CuboidTable) tableList.get(0);
					} else {
						//选一个记录条数最少的
						int idx = 0;
						long cnt = tableList.get(0).getTotalRecordCount();
						for (int i = 1; i < size; i++) {
							long l = tableList.get(i).getTotalRecordCount();
							if (cnt > l) {
								idx = i;
								cnt = l;
							}
						}
						for (int i = 1; i < size; i++) {
							if (i != idx) {
								tableList.get(i).close();
							}
						}
						table = (CuboidTable) tableList.get(idx);
					}

					ArrayList<Expression> fieldExpList = new ArrayList<Expression>();
					Expression[] fieldExps;//再聚合时要取出来的字段
					int expsLen = exps == null ? 0 : exps.length;
					DataStruct ds = table.getDataStruct();
					for (int i = 0; i < expsLen; i++) {
						if (ds.getFieldIndex(expNames[i]) > 0) {
							//如果直接就是预分组的字段
							fieldExpList.add(new Expression("'" + expNames[i] + "'"));
						} else {
							//提取涉及到的字段
							Expression exp = new Expression(expNames[i]);
							ArrayList<String> strList = new ArrayList<String>();
							exp.getUsedFields(ctx, strList);
							for (String str : strList) {
								fieldExpList.add(new Expression(str));
							}
						}
					}
					for (int i = expsLen; i < fcount; i++) {
						fieldExpList.add(new Expression("'" + newExpNames[i - expsLen] + "'"));
					}
					fieldExps = new Expression[fieldExpList.size()];
					fieldExpList.toArray(fieldExps);

					Expression newSrcExps[] = new Expression[newExps.length];//用来存储要聚合的原字段
					for (int i = 0, len = newExps.length; i < len; i++) {
						String str = new String(newExps[i].getIdentifierName());
						if (newNames[i] == null) {
							newNames[i] = str;
						}

						//对count再聚合，要变为累加
						if (str.indexOf("count(") != -1) {
							str = str.replaceFirst("count", "sum");
						}

						//对预分组立方体再聚合时要替换一下字段名
						String sub = str.substring(str.indexOf('(') + 1, str.indexOf(')'));
						str = str.replaceAll(sub, "'" + newExpNames[i] + "'");
						newExps[i] = new Expression(str);
						if (str.indexOf("top(") == 0) {
							sub = sub.substring(sub.indexOf(',') + 1);
						}
						newSrcExps[i] = new Expression(sub);
					}
					try {
						ICursor cs = table.cursor(fieldExps, null, w, ctx);
						return cs.groups(exps, names, newExps, newNames, null, ctx);
					} catch (Exception e) {
						table.close();
						throw new RQException("cgroups:" + e.getMessage(), e);
					}
				}
			} else {
				PhyTable table = (PhyTable) obj;
				Expression[] fieldExps = new Expression[fcount];
				for (int i = exps.length; i < fcount; i++) {
					fieldExps[i] = new Expression("'" + newExpNames[i - exps.length] + "'");
				}

				String[] fieldNames = new String[fcount];
				for (int i = 0, len = names.length; i < len; i++) {
					if (names[i] == null) {
						names[i] = exps[i].getIdentifierName();
					}
					fieldExps[i] = new Expression("'" + names[i] + "'");
				}
				for (int i = 0, len = newNames.length; i < len; i++) {
					if (newNames[i] == null) {
						newNames[i] = newExps[i].getIdentifierName();
					}
				}
				System.arraycopy(names, 0, fieldNames, 0, names.length);
				System.arraycopy(newNames, 0, fieldNames, names.length, newNames.length);

				if (hasM && table.dataBlockCount < n) {
					hasM = false;
				}

				//转换w
				if (w != null) {
					String str = w.getIdentifierName();
					for (int i = 0, len = names.length; i < len; i++) {
						str = str.replace(fieldNames[i], "'" + fieldNames[i] + "'");
					}
					w = new Expression(str);
				}
				Sequence result;
				if (hasM) {
					result = table.cursor(fieldExps, fieldNames, w, null, null, null, n, ctx).fetch();
				} else {
					result = table.cursor(fieldExps, fieldNames, w, null, null, null, ctx).fetch();
				}
				if (!(result instanceof Table)) {
					Table seq = new Table(result.dataStruct());
					seq.addAll(result);
					return seq;
				}
				return result;
			} 
		} finally {
			if (obj != null) {
				if (obj instanceof ArrayList) {
					@SuppressWarnings("unchecked")
					ArrayList<PhyTable> tableList = (ArrayList<PhyTable>) obj;
					for (PhyTable table : tableList) {
						table.close();
					}
				} else {
					PhyTable table = (PhyTable) obj;
					table.close();
				}
				
			}
		}
	}
	
	/**
	 * 利用预分组进行再分组
	 * @param sub0 分组字段
	 * @param sub1 分组表达式
	 * @param srcTable 要分组的组表
	 * @param w 过滤条件
	 * @param hasM 是否并行
	 * @param n 并行数
	 * @param opt
	 * @param ctx
	 * @return
	 */
	public static Sequence cgroups(IParam sub0, IParam sub1, PhyTable srcTable,
			Expression w, boolean hasM, int n, String opt, Context ctx) {
		String []expNames = null;
		String []names = null;
		String []newExpNames = null;
		String []newNames = null;
		
		if (sub0 != null) {
			ParamInfo2 pi0 = ParamInfo2.parse(sub0, "cuboid", true, false);
			names = pi0.getExpressionStrs2();
			expNames = pi0.getExpressionStrs1();
		}
		
		ParamInfo2 pi1 = null;
		if (sub1 != null) {
			pi1 = ParamInfo2.parse(sub1, "cuboid", true, false);
			newExpNames = pi1.getExpressionStrs1();
			newNames = pi1.getExpressionStrs2();
		}
		return cgroups(expNames, names, newExpNames, newNames, srcTable, w, hasM, n, opt, ctx);
	}
	
	private static Expression conbineNodes(ArrayList<Node> nodes) {
		int size = nodes.size();
		if (size == 0) {
			return null;
		}
		Node node = nodes.get(0);
		for (int i = 1; i < size; i++) {
			And and = new And();
			and.setLeft(node);
			and.setRight(nodes.get(i));
			node = and;
		}
		return new Expression(node);
	}
	
	/**
	 * 查找合适的立方体
	 * @param srcTable 原表
	 * @param names 要分组的字段名称
	 * @param expNames 聚合表达式名称
	 * @param w 过滤表达式
	 * @return
	 */
	public static Object findCuboid(PhyTable srcTable, String names[], String expNames[], Expression w) {
		return findCuboid(srcTable, names, expNames, w, null);
	}
	
	/**
	 * 查找合适的立方体
	 * @param srcTable 原表
	 * @param names 要分组的字段名称
	 * @param expNames 聚合表达式名称
	 * @param w 过滤表达式
	 * @param ctx
	 * @return
	 */
	public static Object findCuboid(PhyTable srcTable, String names[], String expNames[], 
			Expression w, Context ctx) {
		String dir = srcTable.getGroupTable().getFile().getAbsolutePath() + "_";
		String cuboids[] = srcTable.getCuboids();
		ArrayList<PhyTable> tableList = new ArrayList<PhyTable>();
		if (cuboids == null) return tableList;
		ArrayList<PhyTable> tableList2 = new ArrayList<PhyTable>();
		
		//检查exps，avg不能做再聚合
		boolean flag = false;//表示不能再聚合
		for (String exp : expNames) {
			if (exp.indexOf("avg(") != -1) {
				flag = true;//return tableList;
			}
			if (exp.indexOf("top(") != -1) {
				flag = true;//return tableList;
			}
		}
		
		ArrayList<String> filterFields = null;
		ArrayList<String> fieldList = null;
		
		for (String cuboid: cuboids) {
			FileObject fo = new FileObject(dir + srcTable.getTableName() + CUBE_PREFIX + cuboid);
			if (!fo.isExists()) {
				continue;
			}
			File file = fo.getLocalFile().file();
			RowComTable table = null;
			try {
				table = new Cuboid(file, null);
				table.checkPassword("cuboid");
				PhyTable baseTable = table.getBaseTable();
				String fields[] = baseTable.getAllColNames();
				if (w != null) {
					filterFields = new ArrayList<String>();
					fieldList = new ArrayList<String>();
					for (String f : fields) {
						fieldList.add(f);
					}
					parseFilter(fieldList, w.getHome(), filterFields);
				}
				int match = check(fields, baseTable.getAllSortedColNames().length, names, expNames, filterFields, ctx);
				if (match == 1 && !flag) {
					tableList.add(baseTable);
				} else if ( match == 2 && !flag) {
					tableList2.add(baseTable);
				} else if (match == 3) {
					//最匹配，返回表对象
					for (PhyTable tbl : tableList) {
						tbl.close();
					}
					for (PhyTable tbl : tableList2) {
						tbl.close();
					}
					return baseTable;
				} else {
					table.close();
				}
			} catch (Exception e) {
				if (table != null) table.close();
				for (PhyTable tbl : tableList) {
					tbl.close();
				}
				throw new RQException(e.getMessage(), e);
			}
		}

		//去掉匹配度低的（如果有2的，则去掉1的）
		if (tableList2.size() != 0) {
			for (PhyTable tbl : tableList) {
				tbl.close();
			}
			return tableList2;
		}
		return tableList;
	}
	
	/**
	 * 查找合适的立方体 (集群)
	 * @param srcTable 原表
	 * @param names 要分组的字段名称
	 * @param expNames 聚合表达式名称
	 * @param w 过滤表达式
	 * @param ctx
	 * @return
	 */
	public static Object findCuboid(ClusterPhyTable srcTable, String names[], String expNames[], 
			Expression w, Context ctx) {
		List<String> cuboids = getCuboids(srcTable);
		ArrayList<PhyTable> tableList = new ArrayList<PhyTable>();
		if (cuboids == null) return tableList;
		ArrayList<PhyTable> tableList2 = new ArrayList<PhyTable>();
		
		//检查exps，avg不能做再聚合
		for (String exp : expNames) {
			if (exp.indexOf("avg(") != -1) {
				return tableList;
			}
			if (exp.indexOf("top(") != -1) {
				return tableList;
			}
		}
		
		ArrayList<String> filterFields = null;
		ArrayList<String> fieldList = null;
		
		for (String cuboid: cuboids) {
			FileObject fo = new FileObject(cuboid);
			if (!fo.isExists()) {
				continue;
			}
			File file = fo.getLocalFile().file();
			RowComTable table = null;
			try {
				table = new Cuboid(file, null);
				table.checkPassword("cuboid");
				PhyTable baseTable = table.getBaseTable();
				String fields[] = baseTable.getAllColNames();
				if (w != null) {
					filterFields = new ArrayList<String>();
					fieldList = new ArrayList<String>();
					for (String f : fields) {
						fieldList.add(f);
					}
					parseFilter(fieldList, w.getHome(), filterFields);
				}
				int match = check(fields, baseTable.getAllSortedColNames().length, names, expNames, filterFields, ctx);
				if (match == 1) {
					tableList.add(baseTable);
				} else if ( match == 2) {
					tableList2.add(baseTable);
				} else if (match == 3) {
					//最匹配，返回表对象
					for (PhyTable tbl : tableList) {
						tbl.close();
					}
					for (PhyTable tbl : tableList2) {
						tbl.close();
					}
					return baseTable;
				} else {
					table.close();
				}
			} catch (Exception e) {
				if (table != null) table.close();
				for (PhyTable tbl : tableList) {
					tbl.close();
				}
				throw new RQException(e.getMessage(), e);
			}
		}
		
		//去掉匹配度低的（如果有2的，则去掉1的）
		if (tableList2.size() != 0) {
			for (PhyTable tbl : tableList) {
				tbl.close();
			}
			return tableList2;
		}
		return tableList;
	}
	
	/**
	 * 检查分组字段和立方体(组表文件)字段的匹配性
	 * @param fields 立方体的字段
	 * @param kcount 立方体的维字段个数
	 * @param names 要分组的字段
	 * @param expNames 聚合表达式
	 * @param filterFields 过滤表达式w里的字段
	 * @return 0 不匹配,1 匹配,2 条件里有字段匹配,3全匹配
	 */
	private static int check(String fields[], int kcount, String names[], String expNames[], ArrayList<String> filterFields, Context ctx) {
		int fcount = names == null ? 0 : names.length;
		int ecount = expNames.length;
		int size = fields.length;
		//boolean hasYearDate = false;//不直接匹配，而是匹配到year、date表达式
		
		//检查w里的字段是否都在立方体字段里出现
		if (filterFields != null) {
			ArrayList<String> flist = new ArrayList<String>();
			for (int i = 0; i < kcount; i++) {
				flist.add(fields[i]);
			}
			for (String f : filterFields) {
				boolean flag = flist.contains(f);
				if (!flag) {
					//如果不存在这个字段,但是存在包含时间区间的表达式也可以
					if (flist.contains("year(" + f + ")")) {
						//hasYearDate = true;
						flag = true;
					}
					if (flist.contains("month@y(" + f + ")")) {
						flag = true;
					}
					if (flist.contains("date(" + f + ")")) {
						//hasYearDate = true;
						flag = true;
					}
				}
				if (!flag) {
					return 0;
				}
			}
		}
		
		if (size >= fcount + ecount) {
			//检查立方体里是否有exp
			for (String exp : expNames) {
				boolean find = false;
				for (int i = 0; i < size; i++) {
					if (exp.equals(fields[i])) {
						find = true;
						break;
					}
				}
				if (!find) return 0;
			}
			
			//判断是不是contain
			if (ctx != null && fcount == 1 && names[0].indexOf("contain") != -1) {
				//检查contain表达式涉及字段是否是立方体里的Fi
				Expression exp = new Expression(names[0]);
				if (exp.getHome() instanceof DotOperator 
						&& exp.getHome().getRight() instanceof Contain) {
					ArrayList<String> list = new ArrayList<String>();
					exp.getUsedFields(ctx, list);
					//要求list里的字段都能在fields里找到
					boolean find = false;
					for (String str : list) {
						find = false;
						for (int i = 0; i < size; i++) {
							if (str.equals(fields[i])) {
								find = true;
								break;
							}
						}
						if (!find) {
							break;
						}
					}
					if (find) {
						return 1;
					}
				}
			}
			
			//按顺序检查Fi和立方体里的Fi
			for (int i = 0; i < fcount; i++) {
				String name = names[i];
				if (! name.equals(fields[i])) {
					return 0;
				}
			}
			if (fcount == kcount && filterFields == null) {
				return 3;
			} else if (filterFields != null && filterFields.size() != 0) {
				return 2;//条件里有字段匹配
			} else {
				return 1;
			}
		}
		else {
			return 0;
		}
	}

	/**
	 * 取出table里转换为年月日的日期类型的原字段
	 * @param table
	 * @param list 输出{字段名，分组层次} 分组层次:1 year ,2 month,3 day
	 */
	private static void getDateFields(PhyTable table, ArrayList<Object> list) {
		String[] fields = table.getAllSortedColNames();
		ArrayList<String> flist = new ArrayList<String>();
		for (String f : fields) {
			flist.add(f);
		}
		for (String f : fields) {
			if (f.indexOf("year(") == 0) {
				String s = f.substring(5);
				s = s.substring(0, s.length() - 1);
				list.add(s);
				list.add(1);
			} else if (f.indexOf("month@y(") == 0) {
				String s = f.substring(8);
				s = s.substring(0, s.length() - 1);
				list.add(s);
				list.add(2);
			} else if (f.indexOf("date(") == 0) {
				String s = f.substring(5);
				s = s.substring(0, s.length() - 1);
				list.add(s);
				list.add(3);
			}
		}
		return;
	}
	
	/**
	 * 从表达式里提取区间
	 * @param field 区间字段名
	 * @param node 表达式节点
	 * @param interval 区间输出
	 * @param isEQ 区间等号标志
	 * @param otherNodes 不涉及区间的其它node
	 * @param ctx
	 */
	private static void getDateInterval(String field, Node node, Object[] interval,
			boolean[] isEQ, ArrayList<Node> otherNodes, Integer timeUnit, Context ctx) {
		if (node instanceof And) {
			getDateInterval(field, node.getLeft(), interval, isEQ, otherNodes, timeUnit, ctx);
			getDateInterval(field, node.getRight(), interval, isEQ, otherNodes, timeUnit, ctx);
			return;
		} else if (node instanceof Between) {
			Between bt = (Between) node;
			IParam sub0 = bt.getParam().getSub(0);
			IParam sub1 = bt.getParam().getSub(1);
			if (sub0 == null || sub1 == null) {
				otherNodes.add(node);
				return;
			}
			Expression f = sub0.getLeafExpression();
			if (field.equals(f.getIdentifierName())) {
				interval[0] = sub1.getSub(0).getLeafExpression().calculate(ctx);
				interval[1] = sub1.getSub(1).getLeafExpression().calculate(ctx);
				isEQ[0] = isEQ[1] = true;
				return;
			} else {
				otherNodes.add(node);
				return;
			}
		} else if (node instanceof Or) {
			otherNodes.add(node);
			return;
		} else {
			Node left = node.getLeft();
			Node right = node.getRight();
			String fname = null;
			if (left instanceof UnknownSymbol || left instanceof Year || left instanceof Month) {
				if (left instanceof UnknownSymbol) {
					fname = ((UnknownSymbol)left).getName();
				} else if (left instanceof Year && timeUnit == 1) {
					fname = ((Year)left).getParamString();
				} else if (left instanceof Month && timeUnit == 2){
					fname = ((Month)left).getParamString();
				}
				if (fname == null || !fname.equals(field)) {
					otherNodes.add(node);
					return;
				}
				
				if (node instanceof Equals) {
					interval[0] = interval[1] = right.calculate(ctx);
					isEQ[0] = isEQ[1] = true;
				} else if (node instanceof Greater) {
					interval[0] = right.calculate(ctx);
					isEQ[0] = false;
				} else if (node instanceof NotSmaller) {
					interval[0] = right.calculate(ctx);
					isEQ[0] = true;
				} else if (node instanceof Smaller) {
					interval[1] = right.calculate(ctx);
					isEQ[1] = false;
				} else if (node instanceof NotGreater) {
					interval[1] = right.calculate(ctx);
					isEQ[1] = true;
				} else {
					otherNodes.add(node);
					return;
				}
			} else if (right instanceof UnknownSymbol) {
				if (right instanceof UnknownSymbol) {
					fname = ((UnknownSymbol)right).getName();
				} else if (right instanceof Year && timeUnit == 1) {
					fname = ((Year)right).getParamString();
				} else if (right instanceof Month && timeUnit == 2){
					fname = ((Month)right).getParamString();
				}
				if (fname == null || !fname.equals(field)) {
					otherNodes.add(node);
					return;
				}
				
				if (node instanceof Equals) {
					interval[0] = interval[1] = left.calculate(ctx);
					isEQ[0] = isEQ[1] = true;
				} else if (node instanceof Greater) {
					interval[1] = left.calculate(ctx);
					isEQ[1] = false;
				} else if (node instanceof NotSmaller) {
					interval[1] = left.calculate(ctx);
					isEQ[1] = true;
				} else if (node instanceof Smaller) {
					interval[0] = left.calculate(ctx);
					isEQ[0] = false;
				} else if (node instanceof NotGreater) {
					interval[0] = left.calculate(ctx);
					isEQ[0] = true;
				} else {
					otherNodes.add(node);
					return;
				}
			} else {
				otherNodes.add(node);
				return;
			}
		}
	}
	
	/**
	 * 把时间区间转换为两个表达式：1 访问原组表的 2 访问立方体的
	 * @param field	原组表字段名
	 * @param field	立方体取出字段
	 * @param array		时间区间
	 * @param isEQ		是否相等
	 * @param timeUnit
	 * @return
	 */
	private static Node[] makeNode(String field, String fields[], Object array[], boolean isEQ[], Integer timeUnit) {
		Node nodes[] = null;
		
		//访问立方体的字段名转换为#i
		String year = "year(" + field + ")";
		String month = "month@y(" + field + ")";
		String date = "date(" + field + ")";
		String yearFieldName = null;
		String monthFieldName = null;
		String dateFieldName = null;

		for (int i = 0, len = fields.length; i < len; i++) {
			String f = fields[i];
			if (f.indexOf(year) != -1) {
				yearFieldName = "#" + (i + 1);
			} else if (f.indexOf(month) != -1) {
				monthFieldName = "#" + (i + 1);
			} else if (f.indexOf(date) != -1) {
				dateFieldName = "#" + (i + 1);
			}
		}
		
		if (timeUnit == 1) {//year
			if ((array[0] != null && array[0] instanceof Integer) ||
					(array[1] != null && array[1] instanceof Integer)) {//Integer表示是按照Year函数提条件
				Integer from = null, to = null;
				if (array[0] != null && array[0] instanceof Integer) {
					from = (Integer) array[0];
				}
				if (array[1] != null && array[1] instanceof Integer) {
					to = (Integer) array[1];
				}
				String s;
				if (from != null && to != null) {
					s = yearFieldName + ">=" + from;
					s += "&&" + yearFieldName + "<=" + to;
				} else if (from != null) {
					s = yearFieldName + ">=" + from;
				} else {
					s = yearFieldName + "<=" + to;
				}
				nodes = new Node[2];
				nodes[1] = new Expression(s).getHome();
				return nodes;
			}
			//这里是按区间把条件拆分为3个，左右的去原表里取数，中间的条件是利用立方体
			Integer from = array[0] == null ? null : DateFactory.get().year( (Date) array[0]);
			Integer to = array[1] == null ? null : DateFactory.get().year( (Date) array[1]);
			if (from != null && to != null && (to - from < 2)) {
				return null;//用不到立方体
			}
			
			nodes = new Node[2];
			Node n1, n2;
			Node and1 = null;//用于T的左区间
			Node and2 = null;
			if (array[0] != null) {
				if (!isEQ[0]) {
					n1 = new Greater();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				} else {
					n1 = new NotSmaller();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				}
				Node small = new Smaller();
				small.setLeft(n1.getLeft());
				try {
					small.setRight(new Constant(DateFactory.parseDate(""+(from + 1) + "-1-1")));
				} catch (ParseException e) {
					throw new RQException("cgroups:" + e.getMessage(), e);
				}
				and1 = new And();
				and1.setLeft(n1);
				and1.setRight(small);
			}
			
			if (array[1] != null) {
				if (!isEQ[1]) {
					n2 = new Smaller();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				} else {
					n2 = new NotGreater();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				}
				Node great = new NotSmaller();
				great.setLeft(n2.getLeft());
				try {
					great.setRight(new Constant(DateFactory.parseDate("" + to + "-1-1")));
				} catch (ParseException e) {
					throw new RQException("cgroups:" + e.getMessage(), e);
				}
				and2 = new And();
				and2.setLeft(n2);
				and2.setRight(great);
			}
			
			if (and1 != null && and2 != null) {
				nodes[0] = new Or();
				nodes[0].setLeft(and1);
				nodes[0].setRight(and2);
			} else if (and1 != null) {
				nodes[0] = and1;
			} else if (and2 != null) {
				nodes[0] = and2;
			}
			
			//组织用于立方体的node
			String s;
			if (from != null && to != null) {
				int begin = from + 1;
				s = yearFieldName + ">=" + begin;
				int end = to - 1;
				s += "&&" + yearFieldName + "<=" + end;
			} else if (from != null) {
				int begin = from + 1;
				s = yearFieldName + ">=" + begin;
			} else {
				int end = to - 1;
				s = yearFieldName + "<=" + end;
			}
			nodes[1] = new Expression(s).getHome();
		} else if (timeUnit == 2) {
			if ((array[0] != null && array[0] instanceof Integer) ||
					(array[1] != null && array[1] instanceof Integer)) {//Integer表示是按照Year函数提条件
				Integer from = null, to = null;
				if (array[0] != null && array[0] instanceof Integer) {
					from = (Integer) array[0];
				}
				if (array[1] != null && array[1] instanceof Integer) {
					to = (Integer) array[1];
				}
				String s;
				if (from != null && to != null) {
					s = monthFieldName + ">=" + from;
					s += "&&" + monthFieldName + "<=" + to;
				} else if (from != null) {
					s = monthFieldName + ">=" + from;
				} else {
					s = monthFieldName + "<=" + to;
				}
				nodes = new Node[2];
				nodes[1] = new Expression(s).getHome();
				return nodes;
			}
			
			//month
			boolean hasInterval1 = array[0] != null;
			boolean hasInterval2 = array[1] != null;
			int fromYear = 0, toYear = 0, fromMonth = 0, toMonth = 0;
			if (hasInterval1) {
				fromYear = DateFactory.get().year( (Date) array[0]);
				fromMonth = DateFactory.get().month( (Date) array[0]);
			}
			if (hasInterval2) {
				toYear = DateFactory.get().year( (Date) array[1]);
				toMonth = DateFactory.get().month( (Date) array[1]);
			}
			int toYearBak = toYear;
			int toMonthBak = toMonth;
			if (hasInterval1) {
				fromMonth ++;
				if (fromMonth == 13) {
					fromMonth = 1;
					fromYear++;
				}
			}
			if (hasInterval2) {
				toMonth --;
				if (toMonth == 0) {
					toMonth = 12;
					toYear--;
				}
			}
			
			if (hasInterval1 && hasInterval2) {
				if (fromYear > toYear) {
					return null;
				}
				else if (fromYear == toYear) {
					if (fromMonth > toMonth) {
						return null;
					}
				}
			}
			
			nodes = new Node[2];
			Node n1, n2;//用于T的
			Node and1 = null, and2 = null;
			if (hasInterval1) {
				if (!isEQ[0]) {
					n1 = new Greater();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				} else {
					n1 = new NotSmaller();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				}
				Node small = new Smaller();
				small.setLeft(n1.getLeft());
				try {
					small.setRight(new Constant(DateFactory.parseDate("" + fromYear + "-" + fromMonth + "-1")));
				} catch (ParseException e) {
					throw new RQException("cgroups:" + e.getMessage(), e);
				}
				and1 = new And();
				and1.setLeft(n1);
				and1.setRight(small);
			}
			
			if (hasInterval2) {
				if (!isEQ[1]) {
					n2 = new Smaller();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				} else {
					n2 = new NotGreater();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				}
				Node great = new NotSmaller();
				great.setLeft(n2.getLeft());
				try {
					great.setRight(new Constant(DateFactory.parseDate("" + toYearBak + "-" + toMonthBak + "-1")));
				} catch (ParseException e) {
					throw new RQException("cgroups:" + e.getMessage(), e);
				}
				and2 = new And();
				and2.setLeft(n2);
				and2.setRight(great);
			}
			
			if (hasInterval1 && hasInterval2) {
				nodes[0] = new Or();
				nodes[0].setLeft(and1);
				nodes[0].setRight(and2);
			} else if (hasInterval1) {
				nodes[0] = and1;
			} else {
				nodes[0] = and2;
			}
			
			//组织用于立方体的node
			String yearAndMonth = monthFieldName;
			if (hasInterval1 && hasInterval2) {
				String s = yearAndMonth +">=" + (fromYear*100 + fromMonth);
				s += "&& " + yearAndMonth + "<=" + (toYear*100 + toMonth);
				nodes[1] = new Expression(s).getHome();
			} else if (hasInterval1) {
				String s = yearAndMonth + ">=" + (fromYear*100 + fromMonth);
				nodes[1] = new Expression(s).getHome();
			} else {
				String s = yearAndMonth + "<=" + (toYear*100 + toMonth);
				nodes[1] = new Expression(s).getHome();
			}
		} else if (timeUnit == 3) {
			//date
			boolean hasInterval1 = array[0] != null;
			boolean hasInterval2 = array[1] != null;
			Date from = null, to = null;
			Date toBak = null; 
			if (hasInterval1) {
				from = (Date) array[0];
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(from);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.add(Calendar.DATE, 1);
				from = (Date) from.clone();
				from.setTime(calendar.getTimeInMillis());
			}
			if (hasInterval2) {
				to = (Date) array[1];
				toBak = (Date) to.clone();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(to);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				toBak.setTime(calendar.getTimeInMillis());
				calendar.add(Calendar.DATE, -1);
				to = (Date) to.clone();
				to.setTime(calendar.getTimeInMillis());
			}
			if (hasInterval1 && hasInterval2) {
				if (Variant.compare(from, to) > 0) {
					return null;
				}
			}
			
			nodes = new Node[2];
			Node n1, n2;//用于T的
			Node and1 = null, and2 = null;
			if (hasInterval1) {
				if (!isEQ[0]) {
					n1 = new Greater();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				} else {
					n1 = new NotSmaller();
					n1.setLeft(new UnknownSymbol(field));
					n1.setRight(new Constant(array[0]));
				}
				Node small = new Smaller();
				small.setLeft(n1.getLeft());
				small.setRight(new Constant(from));
				and1 = new And();
				and1.setLeft(n1);
				and1.setRight(small);
			}
			
			if (hasInterval2) {
				if (!isEQ[1]) {
					n2 = new Smaller();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				} else {
					n2 = new NotGreater();
					n2.setLeft(new UnknownSymbol(field));
					n2.setRight(new Constant(array[1]));
				}
				Node great = new NotSmaller();
				great.setLeft(n2.getLeft());
				great.setRight(new Constant(toBak));
				and2 = new And();
				and2.setLeft(n2);
				and2.setRight(great);
			}
			
			if (hasInterval1 && hasInterval2) {
				nodes[0] = new Or();
				nodes[0].setLeft(and1);
				nodes[0].setRight(and2);
			} else if (hasInterval1) {
				nodes[0] = and1;
			} else {
				nodes[0] = and2;
			}
			
			//组织用于立方体的node
			String fieldExp = dateFieldName;
			if (hasInterval1 && hasInterval2) {
				n1 = new NotSmaller();
				n1.setLeft(new UnknownSymbol(fieldExp));
				n1.setRight(new Constant(from));
				n2 = new NotGreater();
				n2.setLeft(new UnknownSymbol(fieldExp));
				n2.setRight(new Constant(to));
				nodes[1] = new And();
				nodes[1].setLeft(n1);
				nodes[1].setRight(n2);
			} else if (hasInterval1) {
				n1 = new NotSmaller();
				n1.setLeft(new UnknownSymbol(fieldExp));
				n1.setRight(new Constant(from));
				nodes[1] = n1;
			} else {
				n2 = new NotGreater();
				n2.setLeft(new UnknownSymbol(fieldExp));
				n2.setRight(new Constant(to));
				nodes[1] = n2;
			}
		}
		return nodes;
	}
	
	/**
	 * 提取node表达式里的字段名 （遇到year节点时，不在fields里的会被过滤掉）
	 * @param fields 已知的字段名
	 * @param node
	 * @param list
	 */
	private static void parseFilter(ArrayList<String> fields, Node node, ArrayList<String> list) {
		if (node == null) return;
		if (node instanceof UnknownSymbol) {
			list.add(((UnknownSymbol) node).getName());
			return;
		}
		else if (node instanceof Year) {
			IParam param = ((Year) node).getParam();
			if (param.isLeaf()) {
				Node home = param.getLeafExpression().getHome();
				if (home instanceof UnknownSymbol) {
					String name = ((UnknownSymbol) home).getName();
					if (fields.contains(name)) {
						list.add(name);
						return;
					}  
					name = "year(" + name + ")";
					if (fields.contains(name)) {
						list.add(name);
						return;
					} 
				}
			}
			return;
		}
		else if (node instanceof Month 
				&& ((Month) node).getOption() != null
				&& ((Month) node).getOption().indexOf("y") != -1) {
			IParam param = ((Month) node).getParam();
			if (param.isLeaf()) {
				Node home = param.getLeafExpression().getHome();
				if (home instanceof UnknownSymbol) {
					String name = ((UnknownSymbol) home).getName();
					if (fields.contains(name)) {
						list.add(name);
						return;
					}  
					name = "month@y(" + name + ")";
					if (fields.contains(name)) {
						list.add(name);
						return;
					} 
				}
			}
			return;
		}
		
		parseFilter(fields, node.getLeft(), list);
		parseFilter(fields, node.getRight(), list);
	}
	
	public void append(ICursor cursor) throws IOException {
		baseTable.append(cursor);
	}
	
	/**
	 * 原表更新后重新汇总
	 * @param srcTable
	 * @throws IOException
	 */
	public void update(PhyTable srcTable) throws IOException {
		//1 把原表里未汇总的部分进行汇总
		
		//得到要汇总的条数
		long count = srcTable.getActualRecordCount();
		count = count - getSrcCount();
		if (count <= 0) return;
		//计算缓冲条数
		int fcount = exps == null ? 0 : exps.length;
		if (newExps != null) fcount += newExps.length;
		int capacity = EnvUtil.getCapacity(srcTable.getAllColNames().length + fcount);
		//得到原表要汇总的记录的游标
		ICursor cursor = srcTable.cursor();
		cursor.skip(getSrcCount());//跳过已经汇总的
		//组织汇总调用的参数
		int expsLength = exps.length;
		int newExpsLength = newExps.length;
		Expression expressions[] = new Expression[expsLength];//分组字段表达式
		for (int i = 0; i < expsLength; i++) {
			expressions[i] = new Expression(exps[i]);
		}
		Expression newExpressions[] = new Expression[newExpsLength];//汇总字段表达式
		for (int i = 0; i < newExpsLength; i++) {
			newExpressions[i] = new Expression(newExps[i]);
		}
		String names[] = new String[expsLength];//新的分组字段名
		String newNames[] = new String[newExpsLength];//新的汇总字段名
		String cols[] = baseTable.colNames;
		System.arraycopy(cols, 0, names, 0, names.length);
		System.arraycopy(cols, names.length, newNames, 0, newNames.length);
		
		//得到新追加数据的分组汇总结果游标
		ICursor cs = cursor.groupx(expressions, names, newExpressions,
				newNames, null, ctx, capacity / 2);
		
		//2把cs归并到预分组立方体(baseTable)中
		//得到总块数，下面按块处理
		CuboidTable baseTable = this.baseTable;
		int totalBlockCount = baseTable.getDataBlockCount();
		if (totalBlockCount == 0) {
			baseTable.append(cs);
			baseTable.appendCache();
			setSrcCount(srcTable.getActualRecordCount());
			writeHeader();
			flush();
			return;
		}
		
		BlockLinkReader rowReader = baseTable.getRowReader(true);//数据reader
		ObjectReader segmentReader = baseTable.getSegmentObjectReader();//分段信息reader
		Object []maxValues = new Object[expsLength];//每一段的最大值
		Sequence dataToInsert = new Sequence();//存放要插入的数据
		
		//取一段数据出来
		Sequence data = cs.fetch(ICursor.FETCHCOUNT);
		if (data == null || data.length() == 0) {
			return;
		}
		int idx = 1;
		int len = data.length();
		BaseRecord record = (BaseRecord) data.get(idx);
		Object vals[] = record.getFieldValues();
		
		//定义读写定长字段的reader writer
		byte[] bytes = new byte[FIXED_OBJ_LEN];
		RowBufferReader bufferReader = new RowBufferReader(null, bytes);
		RowBufferWriter bufferWriter = new RowBufferWriter(null, bytes);
		
		RandomAccessFile rafile = raf;//new RandomAccessFile(file, "rw");
		ObjectReader reader = new ObjectReader(rowReader, blockSize - ComTable.POS_SIZE);;
		//遍历每一块进行处理
		NEXT:
		for(int i = 0; i < totalBlockCount; i++) {
			int curRecordCount = 0;
			int curRecordSum = segmentReader.readInt32();//当前块的条数
			segmentReader.readLong40();//当前块的地址		
			for (int k = 0; k < expsLength; ++k) {
				segmentReader.skipObject();//跳过最小值
				maxValues[k] = segmentReader.readObject();//得到最大值
			}
			Object curObjs[] = null;
			int blockSize = reader.readInt32();//这是块大小，偏移过去
			long position = reader.position();
			//从本段里读一条出来对比
			curObjs = new Object[fcount];
			reader.skipObject();//跳过伪号
			for (int k = 0; k < expsLength; ++k) {
				curObjs[k] = reader.readObject();
			}
			curRecordCount = 1;
			
			while(true) {
				//判断record是否在这个块里
				int cmp = Variant.compare(vals[0], maxValues[0]);
				if (cmp <= 0) {
					//可能在本段里
					cmp = Variant.compareArrays(vals, curObjs, expsLength);
					if (cmp < 0) {
						//这一条是插入，读下一条record
						dataToInsert.add(record);
						
						//如果data取完了，就再取出来一些
						idx++;
						if (idx > len) {
							data = cs.fetch(ICursor.FETCHCOUNT);
							if (data == null || data.length() == 0) {
								break NEXT;//如果没有下一条record了
							}
							idx = 1;
							len = data.length();
						}
						record = (BaseRecord) data.get(idx);
						vals = record.getFieldValues();
					} else if (cmp == 0) {
						//找到了，更新，读下一条record，读本块下一条
						
						//进行再汇总
						for (int k = 0; k < newExpsLength; ++k) {
							long pos = calcPosition(rowReader, reader);//要更新的地址
							reader.readFully(bytes);//读取定长9字节
							bufferReader.reset();//buffer reader 复位
							Object obj = bufferReader.readObject();//读取一个定长的汇总字段
							obj = regroup(obj, vals[expsLength + k], newNames[k]);//再汇总
							bufferWriter.reset();
							bufferWriter.writeFixedLengthObject(obj);
							rewrite(rafile, pos, bytes);//更新
						}
						
						//读下一条record,如果data取完了，就再取出来一些
						idx++;
						if (idx > len) {
							data = cs.fetch(ICursor.FETCHCOUNT);
							if (data == null || data.length() == 0) {
								break NEXT;//如果没有下一条record了
							}
							idx = 1;
							len = data.length();
						}
						record = (BaseRecord) data.get(idx);
						vals = record.getFieldValues();
						
						//读本块下一条
						curRecordCount++;
						if (curRecordCount > curRecordSum) {
							break;//判断本块是否读完了
						}
						
						//跳过伪号
						reader.skipObject();
						//读出来(维)分组字段
						for (int k = 0; k < expsLength; ++k) {
							curObjs[k] = reader.readObject();
						}
					} else {
						//读本块下一条
						
						//跳过没读出来的汇总字段
						for (int k = 0; k < newExpsLength; ++k) {
							reader.skipBytes(FIXED_OBJ_LEN);
						}
						
						//判断本块是否读完了
						curRecordCount++;
						if (curRecordCount > curRecordSum) {
							break;
						}
						
						//跳过伪号
						reader.skipObject();
						//读出来(维)分组字段
						for (int k = 0; k < expsLength; ++k) {
							curObjs[k] = reader.readObject();
						}
					}
				} else {
					//下一段
					reader.skip(blockSize - (reader.position() - position));
					break;
				}
			
			}
		}
		
		segmentReader.close();
		rowReader.close();
		
		//如果data里还有数据
		if (data != null && data.length() != 0 && idx <= len) {
			Sequence seq = data.split(idx);
			baseTable.append(new MemoryCursor(seq));
		}
		//cs里可能还有数据
		baseTable.append(cs);
		
		//有插入数据则进行归并
		if (dataToInsert.length() != 0) {
			baseTable.append(new MemoryCursor(dataToInsert), "m");
		}
		
		setSrcCount(srcTable.getActualRecordCount());
		writeHeader();
		flush();
	}
	
	/**
	 * 根据src新建一个立方体，写出到file
	 * @param file
	 * @param srcCount 目前固定为0，可能会有其它用处
	 * @param src
	 * @throws IOException
	 */
	public Cuboid(File file, int srcCount, Cuboid src) throws IOException {
		this.file = file;
		this.raf = new RandomAccessFile(file, "rw");
		this.ctx = src.ctx;
		if (ctx != null) {
			ctx.addResource(this);
		}
		
		System.arraycopy(src.reserve, 0, reserve, 0, reserve.length);
		blockSize = src.blockSize;
		enlargeSize = src.enlargeSize;
		
		headerBlockLink = new BlockLink(this);
		headerBlockLink.setFirstBlockPos(applyNewBlock());
		
		baseTable = new CuboidTable(this, null, (RowPhyTable)src.baseTable);
		super.baseTable = baseTable;
		
		exps = src.exps;
		newExps = src.newExps;
		this.srcCount = srcCount;
		
		structManager = new StructManager();
		
		writePswHash = src.writePswHash;
		readPswHash = src.readPswHash;
		distribute = src.distribute;
		
		save();
	}
	
	private Object regroup(Object obj1, Object obj2, String op) {
		if (obj1 == null) {
			return obj2;
		}
		if (obj2 == null) {
			return obj1;
		}
		if (op.indexOf("sum(") == 0) {
			return Variant.add(obj1, obj2);
		} else if (op.indexOf("count(") == 0) {
			return Variant.add(obj1, obj2);
		} else if (op.indexOf("max(") == 0) {
			if (Variant.compare(obj1, obj2, true) > 0) {
				return obj1;
			} else {
				return obj2;
			}
		} else if (op.indexOf("min(") == 0) {
			if (Variant.compare(obj1, obj2, true) < 0) {
				return obj1;
			} else {
				return obj2;
			}
		}
		MessageManager mm = EngineMessage.get();
		throw new RQException(op + ":" + mm.getMessage("engine.unknownGroupsMethod"));
	}
	
	private long calcPosition(BlockLinkReader rowReader, ObjectReader rowDataReader) throws IOException {
		rowDataReader.hasNext();
		return rowReader.position() + (rowDataReader.position() % (blockSize - POS_SIZE));
	}
	
	private void rewrite(RandomAccessFile raf, long pos, byte[] bytes) throws IOException {
		raf.seek(pos);
		int offset = (int) (pos % this.blockSize);
		int rest = blockSize - POS_SIZE - offset;
		if (rest < FIXED_OBJ_LEN) {
			raf.write(bytes, 0, rest);
			byte[] nextPos = new byte[POS_SIZE];
			raf.readFully(nextPos);
			pos = (((long)(nextPos[0] & 0xff) << 32) +
					((long)(nextPos[1] & 0xff) << 24) +
					((nextPos[2] & 0xff) << 16) +
					((nextPos[3] & 0xff) <<  8) +
					(nextPos[4] & 0xff));
			raf.seek(pos);
			raf.write(bytes, rest, FIXED_OBJ_LEN - rest);
		} else {
			raf.write(bytes);
		}
	}

	public long getSrcCount() {
		return srcCount;
	}

	public void setSrcCount(long srcCount) {
		this.srcCount = srcCount;
	}
	
	/**
	 * 复制当前立方体到file （仅复制结构，不含数据）
	 * @param file
	 * @return
	 */
	public Cuboid dup(File file) {
		try {
			String []srcColNames = baseTable.getColNames();
			int len = srcColNames.length;
			String []colNames = new String[len];
			boolean[] isDim = ((RowPhyTable)baseTable).getDimIndex();
			for (int i = 0; i < len; i++) {
				if (isDim[i]) {
					colNames[i] = "#" + srcColNames[i];
				} else {
					colNames[i] = srcColNames[i];
				}
			}
			
			Cuboid cuboid = new Cuboid(file, colNames, baseTable.serialBytesLen, ctx,
					"cuboid", "cuboid", exps, newExps);
			cuboid.save();
			return cuboid;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
	}
	
	/**
	 * 得到这个表的所有立方体
	 * @param srcTable
	 * @return
	 */
    public static List<String> getCuboids(ClusterPhyTable srcTable) {
    	String folderPath = Env.getMainPath();
    	String queryStr = srcTable.getClusterFile().getFileName() + Cuboid.CUBE_PREFIX;
        List<String> fileNameList = new ArrayList<String>();//文件名列表
        File f = new File(folderPath);
        if (!f.exists()) { //路径不存在
            return null;
        }else{
                File fa[] = f.listFiles();
                queryStr = queryStr==null ? "" : queryStr;//若queryStr传入为null,则替换为空（indexOf匹配值不能为null）
                for (int i = 0; i < fa.length; i++) {
                    File fs = fa[i];
                    if(fs.getName().indexOf(queryStr)!=-1){
                         if (fs.isFile()) {
                             fileNameList.add(fs.getName());
                         }
                     }
                }
        }
        return fileNameList;
    }
    
    /**
     * 更新集群表的立方体
     * 当集群表有写入新记录时使用
     * @param srcTable
     * @param ctx
     */
    public static void update(ClusterPhyTable srcTable, Context ctx) {
    	 List<String> cuboids =  getCuboids(srcTable);
    	 if (cuboids != null) {
    		 for (String C : cuboids) {
    				FileObject fo = new FileObject(C);
    				if (!fo.isExists()) {
    					continue;
    				}
    				File file = fo.getLocalFile().file();
    				RowComTable table = null;
    				try {
    					table = new Cuboid(file, null);
    					table.checkPassword("cuboid");
    					PhyTable baseTable = table.getBaseTable();
    					String fields[] = baseTable.getAllColNames();
    					String expNames[] = baseTable.getSortedColNames();
    					
    					int  expLength = expNames.length;
    					Expression exps[] = new Expression[expLength];
    					for (int i = 0; i < expLength; i++) {
    						exps[i] = new Expression(expNames[i]);
    					}
    					
    					int newExpsLength = fields.length - expLength;
    					Expression newExps[] = new Expression[newExpsLength];
    					for (int i = 0; i < newExpsLength; i++) {
    						newExps[i] = new Expression(fields[i + expLength]);
    					}
    					table.close();
    					
    					String names[] = new String[expLength];
    					String newNames[] = new String[newExpsLength];
    					ClusterCursor cc = srcTable.cursor(null, null, null, null, null, null, 0, null, ctx);
    					ICursor cursor = new MemoryCursor((Sequence) cc.groups(exps, names, newExps, newNames, null, ctx));

    					if (fo.isExists())
    					{
    						fo.delete();
    					}
    					
    					//保存
    					file = fo.getLocalFile().file();
    					String colNames[] = new String[fields.length];
    					int sbytes[] = new int[fields.length];
    					int i = 0;
    					System.arraycopy(fields, 0, colNames, 0,  fields.length);
    					System.arraycopy(fields, expLength, newNames, 0,  newExpsLength);
    					for(String n : expNames) {
    						colNames[i++] = "#" + n;
    					}
    					
    					Cuboid ctable = null;
    					try {
    						ctable = new Cuboid(file, colNames, sbytes, ctx, "cuboid", "cuboid",
    								expNames, newNames);
    						ctable.save();
    						ctable.close();
    						ctable = new Cuboid(file, ctx);//重新打开
    						ctable.checkPassword("cuboid");
    						ctable.append(cursor);
    						ctable.writeHeader();
    						ctable.close();
    					} catch (Exception e) {
    						if (ctable != null) ctable.close();
    						file.delete();
    						throw new RQException(e.getMessage(), e);
    					}
    				} catch (Exception e) {
    					if (table != null) table.close();
    					throw new RQException(e.getMessage(), e);
    				}
    			
    		 }
    	 }
    }
    
	public String[] getExps() {
		return exps;
	}

	public String[] getNewExps() {
		return newExps;
	}

}

/**
 * 立方体基表类
 * 预汇总的数据实际存放在这个类
 * @author runqian
 *
 */
class CuboidTable extends RowPhyTable {
	public CuboidTable(ComTable groupTable, RowPhyTable parent) {
		super(groupTable, parent);
	}
	
	public CuboidTable(ComTable groupTable, RowPhyTable parent, RowPhyTable src) throws IOException {
		super(groupTable, parent, src);
	}
	
	protected void init() {
		super.init();
	}
	
	/**
	 * 写入新数据
	 */
	public void append(ICursor cursor, String opt) throws IOException {
		if (opt == null || opt.indexOf('m') == -1 || !isSorted) {
			append(cursor);
			return;
		}
		
		// 不支持带附表的组表归并追加
		if (!isSingleTable()) {
			throw new RQException("'append@m' is unimplemented in annex table!");
		}
		
		int []serialBytesLen = this.serialBytesLen;
		
		// 归并读的数据先保存到临时文件
		Cuboid groupTable = (Cuboid)getGroupTable();
		File srcFile = groupTable.getFile();
		File tmpFile = File.createTempFile("tmpdata", "", srcFile.getParentFile());
		try {
			Context ctx = new Context();
			String colNames[] = this.colNames.clone();
			for (int i = 0; i < colNames.length; i++) {
				if (isDim[i]) {
					colNames[i] = "#" + colNames[i];
				}
			}
			Cuboid tmpGroupTable = new Cuboid(tmpFile, colNames, serialBytesLen, ctx,
					"cuboid", "cuboid", groupTable.exps, groupTable.newExps);
			tmpGroupTable.readHeader();
			tmpGroupTable.checkPassword("cuboid");
			
			CuboidTable baseTable = (CuboidTable) tmpGroupTable.baseTable;
			if (segmentCol != null) {
				baseTable.setSegmentCol(segmentCol, segmentSerialLen);
			}
			
			int dcount = sortedColNames.length;
			Expression []mergeExps = new Expression[dcount];
			for (int i = 0; i < dcount; ++i) {
				mergeExps[i] = new Expression(sortedColNames[i]);
			}
			
			// 做归并
			RowCursor srcCursor = new RowCursor(this);
			ICursor []cursors = new ICursor[]{srcCursor, cursor};
			MergesCursor mergeCursor = new MergesCursor(cursors, mergeExps, ctx);
			baseTable.append(mergeCursor);
			baseTable.close();
			
			// 关闭并删除组表文件，把临时文件重命名为组表文件名
			groupTable.raf.close();
			groupTable.file.delete();
			tmpFile.renameTo(groupTable.file);
			
			// 重新打开组表
			groupTable.reopen();
		} finally {
			tmpFile.delete();
		}
	}
	
	protected void appendDataBlock(Sequence data, int start, int end) throws IOException {
		BaseRecord r;
		int count = colNames.length;
		boolean isDim[] = getDimIndex();
		Object []minValues = null;//一块的最小维值
		Object []maxValues = null;//一块的最大维值

		if (sortedColNames != null) {
			minValues = new Object[count];
			maxValues = new Object[count];
		}

		RowBufferWriter bufferWriter= new RowBufferWriter(groupTable.getStructManager());
		long recNum = totalRecordCount;
		
		for (int i = start; i <= end; ++i) {
			r = (BaseRecord) data.get(i);
			Object[] vals = r.getFieldValues();
			//把一条的所有列写到buffer
			bufferWriter.writeObject(++recNum);//行存要先写一个伪号
			for (int j = 0; j < count; j++) {
				Object obj = vals[j];
				if (isDim[j]) {
					bufferWriter.writeObject(obj);
					if (Variant.compare(obj, maxValues[j], true) > 0)
						maxValues[j] = obj;
					if (i == start)
						minValues[j] = obj;//第一个要赋值，因为null表示最小
					if (Variant.compare(obj, minValues[j], true) < 0)
						minValues[j] = obj;
				} else {
					bufferWriter.writeFixedLengthObject(obj);
				}
			}
		}
		
		//写数据时不压缩
		if (sortedColNames == null) {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end - start + 1);
			objectWriter.writeLong40(pos);
		} else {
			//提交buffer到行块
			long pos = colWriter.writeDataBuffer(bufferWriter.finish());
			//更新分段信息
			appendSegmentBlock(end - start + 1);
			objectWriter.writeLong40(pos);
			for (int i = 0; i < count; ++i) {
				if (isDim[i]) {
					objectWriter.writeObject(minValues[i]);
					objectWriter.writeObject(maxValues[i]);
				}
			}
		}
	}
}