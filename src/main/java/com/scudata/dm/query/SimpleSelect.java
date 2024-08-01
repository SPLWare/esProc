package com.scudata.dm.query;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.UUID;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.BaseRecord;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Param;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.BFileCursor;
import com.scudata.dm.cursor.ConjxCursor;
import com.scudata.dm.cursor.FileCursor;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MultipathCursors;
import com.scudata.dm.cursor.SubCursor;
import com.scudata.dm.cursor.SyncCursor;
import com.scudata.dm.op.Derive;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Operation;
import com.scudata.dm.op.Select;
import com.scudata.dm.query.SimpleSelect.ParamNode;
import com.scudata.dm.query.utils.ExpressionTranslator;
import com.scudata.dm.query.utils.FileUtil;
import com.scudata.dm.sql.FunInfoManager;
import com.scudata.dw.ComTable;
import com.scudata.dw.PhyTable;
import com.scudata.excel.ExcelTool;
import com.scudata.expression.Expression;
import com.scudata.resources.ParseMessage;
import com.scudata.util.CursorUtil;
import com.scudata.util.EnvUtil;
import com.scudata.util.JSONUtil;

public class SimpleSelect
{
	private boolean hasDistinct;
	private int topNumber;
	private int limitNumber;
	private int offsetNumber;
	private int parallelNumber;
	private String distinctGatherField;
	private Map<String, String> levelMap;
	private List<String> finalList;
	private List<String> aliasList;
	private List<ExpressionNode> columnList;
	private List<Node> groupList;
	private List<Node> sortList;
	private TableNode tableNode;
	private ExpressionNode havingNode;
	private ExpressionNode whereNode;
	private List<String> selectFieldList;
	private List<GatherNode> gatherNodeList;
	private List<Object> parameterList;
	private ICursor icur;
	private DataStruct ds;
	private int columnIndex;
	private Context ctx;
	private Map<String, String> tablePathMap;
	private ICellSet ics;
	private List<Map.Entry<String, Token[]>> subQueryOfExistsEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfSelectEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfInEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfWhereEntryList;
	private Map<String, String> subQueryOfExistsMap;
	private Map<String, String> subQueryOfSelectMap;
	private Map<String, String> subQueryOfInMap;
	private Map<String, String> subQueryOfWhereMap;
	private String password;
	private boolean isMemory;
	private PhyTable tmd;
	private String topFilter;
	private Sequence fromSeq;	//20240731 xingjl
	
	abstract class Node 
	{
		abstract public boolean hasGather();
		abstract public boolean hasField(Boolean isOrder); //用于group by和order by子句判断是否包含字段，聚合函数与一般函数无差别
		abstract public boolean hasField(String fieldName); //用于select子句判断是否包含某字段，聚合函数需区别于一般函数
		abstract public boolean hasFieldNotGroup();
		abstract public void optimize();
		abstract public void collect();
		abstract public String toExpression();
		abstract public void setFromHaving();
		abstract public void setFromWhere();
	}
	
	class TableNode extends Node
	{
		final public static int TYPE_ICR = -1;
		final public static int TYPE_BIN = 0;
		final public static int TYPE_TXT = 1;
		final public static int TYPE_CSV = 2;
		final public static int TYPE_XLS = 3;
		final public static int TYPE_XLSX = 4;
		final public static int TYPE_GTB = 5;
		final public static int TYPE_JSON = 6;
		
		private String name;
		private String alias;
		private FileObject file;
		private ArrayList<FileObject> files = new ArrayList<FileObject>();
		public ArrayList<FileObject> getFiles() {
			return files;
		}

		public void setFiles(ArrayList<FileObject> files) {
			this.files = files;
			if (this.files == null || this.files.size()==0 && this.file != null) {
				files = new ArrayList<FileObject>();
				files.add(file);
			}
		}

		private PhyTable meta;
		private ArrayList<PhyTable> metas;
		private int type;
		private ICursor cursor;
		private DataStruct struct;
		private String[] fields;
		private Expression where;
		private boolean fileAttrQuery=false;
		
		public boolean isFileAttrQuery() {
			return fileAttrQuery;
		}

		public void setFileAttrQuery(boolean fileAttrQuery) {
			this.fileAttrQuery = fileAttrQuery;
		}

		public TableNode(String tableName, String aliasName, FileObject fileObject, int type)
		{
			if(tableName != null)
			{
				this.name = tableName;
			}
			if(aliasName != null)
			{
				if(aliasName.startsWith("\"") && aliasName.endsWith("\"") && aliasName.substring(1, aliasName.length() - 1).indexOf("\"") == -1)
				{
					aliasName = aliasName.substring(1, aliasName.length() - 1);
				}
				this.alias = aliasName;
			}
			
			if(fileObject == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.TableNode, 文件对象不能为空值");
			}

			if(type == TableNode.TYPE_ICR)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.TableNode, 表文件类型异常");
			}
			else
			{
				this.cursor = null;
				this.struct = null;
				this.file = fileObject;
				this.meta = null;
				this.type = type;
				this.fields = null;
				this.where = null;
			}
		}
		
		public TableNode(String tableName, String aliasName, ICursor cs, DataStruct ds)
		{
			if(tableName != null)
			{
				this.name = tableName;
			}
			if(aliasName != null)
			{
				if(aliasName.startsWith("\"") && aliasName.endsWith("\"") && aliasName.substring(1, aliasName.length() - 1).indexOf("\"") == -1)
				{
					aliasName = aliasName.substring(1, aliasName.length() - 1);
				}
				this.alias = aliasName;
			}
			this.cursor = cs;
			this.struct = ds;
			this.file = null;
			this.meta = null;
			this.type = TableNode.TYPE_ICR;
			this.fields = null;
		}
		
		public TableNode(String tableName, String aliasName, PhyTable meta)
		{
			if(tableName != null)
			{
				this.name = tableName;
			}
			if(aliasName != null)
			{
				if(aliasName.startsWith("\"") && aliasName.endsWith("\"") && aliasName.substring(1, aliasName.length() - 1).indexOf("\"") == -1)
				{
					aliasName = aliasName.substring(1, aliasName.length() - 1);
				}
				this.alias = aliasName;
			}
			this.cursor = null;
			this.struct = null;
			this.file = null;
			this.meta = meta;
			this.type = TableNode.TYPE_GTB;
			this.fields = null;
		}
		
		public String getName()
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getName, 无效的表节点");
			}
			return this.name;
		}
		
		public String getAlias()
		{
			if(this.alias == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getAlias, 无效的表节点");
			}
			return this.alias;
		}
		
		public int getType()
		{
			return this.type;
		}
		
		public void setWhere(String whereExp) // 组表专用提前过滤
		{
			if(whereExp != null && !whereExp.isEmpty())
			{
				this.where = new Expression(whereExp);
			}
		}
		
		private String[] getDataField() {
			if (this.fields == null || this.fields.length == 0) return this.fields;
			String n = "";
			for (int i=0; i<this.fields.length; i++) {
				if (SimpleSelect.fnames.indexOf(fields[i])!=-1) continue;
				n += "," + fields[i];
			}
			if (n.length()>0) return n.substring(1).split(",");
			else return new String[0];
		}
		private String[] getFileField() {
			if (this.fields == null || this.fields.length == 0) return this.fields;
			String n = "";
			for (int i=0; i<this.fields.length; i++) {
				if (SimpleSelect.fnames.indexOf(fields[i])==-1) continue;
				n += "," + fields[i];
			}
			if (n.length()>0) return n.substring(1).split(",");
			else return new String[0];
		}
		
		public ICursor getCursor()
		{
			ICursor icursor = null;
			if(this.file != null || this.meta != null)
			{
				if (this.fileAttrQuery) {
					//return new FileCursor(new FileObject("d:/test/fileAttr.txt"), 1, 1, this.fields, null, null, "t", ctx);
				}
				
				if(this.type == TableNode.TYPE_GTB)	// 组表文件
				{
					ICursor []cursors2 = new ICursor[this.files.size()];
					metas = new ArrayList<PhyTable>(); 
					for (int z=0; z<this.files.size(); z++) {
						
						ComTable group = null;
						try 
						{
							group = ComTable.open(this.files.get(z).getLocalFile().getFile(), ctx);
							group.checkPassword(password);
						} 
						catch (Exception e) 
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 打开组表文件失败", e);
						}
						
						PhyTable meta = group.getBaseTable();
						if(meta == null)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 组表中没有创建基表");
						}
						metas.add(meta);
	
						String indexName = null;
						if(this.where != null)
						{
							String[] indexFields = PhyTable.getExpFields(this.where, meta.getColNames());
							if(indexFields != null)
							{
								indexName = this.meta.chooseIndex(indexFields);
							}
						}
						
						if(indexName != null)
						{
							cursors2[z] = meta.icursor(this.fields, this.where, indexName, null, ctx);
							if(parallelNumber > 1)
							{
								ICursor []cursors = new ICursor[parallelNumber];
								for (int i = 0; i < parallelNumber; ++i) 
								{
									cursors[i] = new SyncCursor(icursor);
								}
								cursors2[z] = new MultipathCursors(cursors, ctx);
							}
						}
						else
						{
							if(parallelNumber == 1)
							{
								cursors2[z] = meta.cursor(this.fields, this.where, ctx);
							}
							else if(parallelNumber > 1)
							{
								ICursor []cursors = new ICursor[parallelNumber];
								for (int i = 0; i < parallelNumber; ++i) 
								{
									cursors[i] = meta.cursor(null, fields, where, null, null, null, i+1, parallelNumber, null, ctx);
								}
								cursors2[z] = new MultipathCursors(cursors, ctx);
							}
						}
					}
					if (this.files.size() == 1) icursor = cursors2[0];
					else icursor = new ConjxCursor(cursors2);
				}
				else if(this.type == TableNode.TYPE_BIN) // 集文件
				{
					ICursor []cursors2 = new ICursor[this.files.size()];
					for (int z=0; z<this.files.size(); z++) {
						//非分段二进制文件不能并行读取
						if(parallelNumber == 1 || SimpleSQL.checkParallel(this.file) == BFileWriter.TYPE_NORMAL)
						{
//							System.out.println("-------------1" + this.files.size());
							FileObject foi = this.files.get(z);
							BFileCursor bf = new BFileCursor(foi, this.fields, 1, 1, null, ctx);
							cursors2[z] = bf;

//							String[] ff = SimpleSelect.fnames.toArray( new String[SimpleSelect.fnames.size()]);
//							Expression[] exps = new Expression[ff.length];
//							for (int m=0; m<ff.length; m++) {
//								if ("_file".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().replace("\\", "/")+"\"");
//								else if ("_ext".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().substring(foi.getFileName().lastIndexOf("."))+"\"");
//								else if ("_date".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().lastModified()+"\"");
//								else if ("_size".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().size()+"\"");
//							}
//							Operation op = new Derive(exps, ff, null);
//							cursors2[z].addOperation(op, ctx);
//
							if (this.fields != null) {
								Expression[] exps2 = new Expression[fields.length];
								for (int m=0; m<fields.length; m++) {
									exps2[m] = new Expression("'"+fields[m]+"'");
								}

								cursors2[z].addOperation(new New(null, exps2, this.fields, null), ctx);
							}
						}
						else
						{
//							System.out.println("-------------2");
							ICursor []cursors = new ICursor[parallelNumber];
							for (int i = 0; i < parallelNumber; ++i) 
							{
								cursors[i] = new BFileCursor(this.file, this.fields, i+1, parallelNumber, null, ctx);
							}		
	
							cursors2[z] = new MultipathCursors(cursors, ctx);

//							String[] ff = SimpleSelect.fnames.toArray( new String[SimpleSelect.fnames.size()]);
//							Expression[] exps = new Expression[ff.length];
//							for (int m=0; m<ff.length; m++) {
//								if ("_file".equals(ff[m])) exps[m] = new Expression("\""+this.file.getFileName().replace("\\", "/")+"\"");
//								else if ("_ext".equals(ff[m])) exps[m] = new Expression("\""+this.file.getFileName().substring(this.file.getFileName().lastIndexOf("."))+"\"");
//								else if ("_date".equals(ff[m])) exps[m] = new Expression("\""+this.file.getFile().lastModified()+"\"");
//								else if ("_size".equals(ff[m])) exps[m] = new Expression("\""+this.file.getFile().size()+"\"");
//							}
//							Operation op = new Derive(exps, ff, null);
//							cursors2[z].addOperation(op, ctx);
//						
							if (this.fields != null) {
								Expression[] exps2 = new Expression[fields.length];
								for (int m=0; m<fields.length; m++) {
									exps2[m] = new Expression("'"+fields[m]+"'");
								}

								cursors2[z].addOperation(new New(null, exps2, this.fields, null), ctx);
							}
						}


					}
					if (this.files.size() == 1) icursor = cursors2[0];
					else icursor = new ConjxCursor(cursors2);
				}
				else if(this.type == TableNode.TYPE_CSV)// CSV文件
				{
					ICursor []cursors = new ICursor[this.files.size()];
					for (int i=0; i<this.files.size(); i++) {
						FileObject foi = this.files.get(i);
						cursors[i] = new FileCursor(foi, 1, 1, null, null, null, "tc", ctx);
						
						String[] ff = SimpleSelect.fnames.toArray( new String[SimpleSelect.fnames.size()]);
						Expression[] exps = new Expression[ff.length];
						for (int m=0; m<ff.length; m++) {
							if ("_file".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().replace("\\","/")+"\"");
							else if ("_ext".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().substring(foi.getFileName().lastIndexOf("."))+"\"");
							else if ("_date".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().lastModified()+"\"");
							else if ("_size".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().size()+"\"");
						}
						Operation op = new Derive(exps, ff, null);
						cursors[i].addOperation(op, ctx);

						if (this.fields != null) {
							Expression[] exps2 = new Expression[fields.length];
							for (int m=0; m<fields.length; m++) {
								exps2[m] = new Expression("'"+fields[m]+"'"); 
							}

							cursors[i].addOperation(new New(null, exps2, this.fields, null), ctx);
						}
					}
					if (this.files.size() == 1) icursor = cursors[0];
					else icursor = new ConjxCursor(cursors);
				}
				else if(this.type == TableNode.TYPE_TXT)// TXT文件
				{
					ICursor []cursors = new ICursor[this.files.size()];
					for (int i=0; i<this.files.size(); i++) {
						FileObject foi = this.files.get(i);
						cursors[i] = new FileCursor(foi, 1, 1, null, null, null, "t", ctx);
						
						String[] ff = SimpleSelect.fnames.toArray( new String[SimpleSelect.fnames.size()]);
						Expression[] exps = new Expression[ff.length];
//						System.out.println("1------------"+foi.getFileName());
						for (int m=0; m<ff.length; m++) {
							if ("_file".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().replace("\\","/")+"\"");
							else if ("_ext".equals(ff[m])) exps[m] = new Expression("\""+foi.getFileName().substring(foi.getFileName().lastIndexOf("."))+"\"");
							else if ("_date".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().lastModified()+"\"");
							else if ("_size".equals(ff[m])) exps[m] = new Expression("\""+foi.getFile().size()+"\"");
						}
						Operation op = new Derive(exps, ff, null);
						cursors[i].addOperation(op, ctx);

						if (this.fields != null) {
							Expression[] exps2 = new Expression[fields.length];
							for (int m=0; m<fields.length; m++) {
								exps2[m] = new Expression("'" + fields[m] + "'");
							}

							cursors[i].addOperation(new New(null, exps2, this.fields, null), ctx);
						}
					}
					if (this.files.size() == 1) icursor = cursors[0];
					else icursor = new ConjxCursor(cursors);
				}
				else if(this.type == TableNode.TYPE_XLS || this.type == TableNode.TYPE_XLSX)// XLS文件
				{
					ICursor []cursors = new ICursor[this.files.size()];
					for (int z=0; z<this.files.size(); z++) {
						boolean isXlsx = ((this.type == TableNode.TYPE_XLSX) ? true : false);
						InputStream in = this.files.get(z).getInputStream();
						BufferedInputStream bis = new BufferedInputStream(in, Env.FILE_BUFSIZE);
						ExcelTool importer = new ExcelTool(bis, isXlsx, password);
						FileObject foi = this.files.get(z);
						try 
						{
//							System.out.println("1------------"+foi.getFileName());
							Table t2 = FileObject.import_x(importer, "t");
							t2 = t2.derive(new String[]{"_file","_ext","_date","_size"}, new Expression[]{new Expression("\""+foi.getFileName().replace("\\","/")+"\"")
									,new Expression("\""+foi.getFileName().substring(foi.getFileName().lastIndexOf("."))+"\"")
									,new Expression("\""+foi.getFile().lastModified()+"\"")
									,new Expression("\""+foi.getFile().size()+"\"")}, null, ctx);						
							if (this.fields != null && this.fields.length>0) {
								Expression[] exps2 = new Expression[this.fields.length];
								for (int p=0; p<this.fields.length; p++) exps2[p] = new Expression("'"+fields[p]+"'");
								t2 = t2.newTable(this.fields, exps2, ctx);
							}	
							cursors[z] = new MemoryCursor(t2);

							//							if(this.fields != null)
//							{
//								Expression[] colExps = new Expression[this.fields.length];
//								for(int i=0, len=this.fields.length; i<len; i++)
//								{
//									int index = -1;
//									for(int j=0, sz=this.struct.getFieldCount(); j<sz; j++)
//									{
//										if(this.struct.getFieldName(j).equalsIgnoreCase(this.fields[i]))
//										{
//											index = j;
//											break;
//										}
//									}
//									if(index == -1)
//									{
//										MessageManager mm = ParseMessage.get();
//										throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 要查询的字段名不在临时表中");
//									}
//									colExps[i] = new Expression(String.format("#%d", index+1));
//								}
//								cursors[z].addOperation(new New(null, colExps, this.fields, null), ctx);
//							}
						}
						catch(IOException e)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 读取XLS格式的表文件失败", e);
						}
						finally 
						{
							try 
							{
								if (in != null)
								{
									in.close();
								}
								if (bis != null)
								{
									bis.close();
								}
							} 
							catch (IOException e) 
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 关闭XLS文件失败", e);
							}
						}
					}
					if (this.files.size() == 1) icursor = cursors[0];
					else icursor = new ConjxCursor(cursors);

				}
				else if(this.type == TableNode.TYPE_JSON)
				{
					try 
					{
						ICursor []cursors = new ICursor[this.files.size()];
						for (int i=0; i<this.files.size(); i++) {
							char[] jsonArray = ((String)this.files.get(i).read(0, -1, null)).toCharArray();
							Object result = JSONUtil.parseJSON(jsonArray, 0, jsonArray.length - 1);
							Sequence seq = null;
							if(result instanceof Sequence)
							{
								seq = (Sequence)result;
							}
							else if(result instanceof BaseRecord)
							{
								seq = new Sequence();
								seq.add(result);
							}
							else
							{
								DataStruct datastruct = new DataStruct(new String[]{"_1"});
								BaseRecord record = new Record(datastruct, new Object[]{result});
								seq = new Sequence();
								seq.add(record);
							}
							FileObject foi = this.files.get(i);
							if (!(seq instanceof Table)) {
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 读取文件数据失败");
							}
							Table t2 = (Table)seq;
							t2 = t2.derive(new String[]{"_file","_ext","_date","_size"}, new Expression[]{new Expression("\""+foi.getFileName().replace("\\","/")+"\"")
									,new Expression("\""+foi.getFileName().substring(foi.getFileName().lastIndexOf("."))+"\"")
									,new Expression("\""+foi.getFile().lastModified()+"\"")
									,new Expression("\""+foi.getFile().size()+"\"")}, null, ctx);						
							if (this.fields != null && this.fields.length>0) {
								Expression[] exps2 = new Expression[this.fields.length];
								for (int z=0; z<this.fields.length; z++) exps2[z] = new Expression("'" + this.fields[z] + "'");
								t2 = t2.newTable(this.fields, exps2, ctx);
							}	
							cursors[i] = new MemoryCursor(t2);

						}
						if (this.files.size() == 1) icursor = cursors[0];
						else icursor = new ConjxCursor(cursors);
						
					} 
					catch(Exception e) 
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 读取JSON文件失败", e);
					}
				}
			}
			else if(this.cursor != null) //子查询临时表
			{
				if(this.cursor instanceof MemoryCursor)
				{
					icursor = new MemoryCursor(this.cursor.fetch());
				}
				else
				{
					icursor = new SyncCursor(this.cursor);
				}
				
				if(this.fields != null)
				{
					Expression[] colExps = new Expression[this.fields.length];
					for(int i=0, len=this.fields.length; i<len; i++)
					{
						int index = -1;
						for(int j=0, sz=this.struct.getFieldCount(); j<sz; j++)
						{
							if(this.struct.getFieldName(j).equalsIgnoreCase(this.fields[i]))
							{
								index = j;
								break;
							}
						}
						if(index == -1)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 要查询的字段名不在临时表中");
						}
						colExps[i] = new Expression(String.format("#%d", index+1));
					}
					icursor.addOperation(new New(null, colExps, this.fields, null), ctx);
				}
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.getCursor, 表节点结构异常");
			}
			
			this.fields = null;//setAccessColumn只能起一次作用
			
			if(isMemory && !(icursor instanceof MemoryCursor))
			{
				icursor = new MemoryCursor(icursor.fetch());
			}
			
			return icursor;
		}
		
		public void setAccessColumn(String[] colNames)
		{
			this.fields = colNames;//getCursor后置null
		}
		
		public DataStruct dataStruct() //这个是求原始表的数据结构而不是最终游标的
		{
			if(this.struct == null)
			{
				String[] fields = this.fields;//备份
				Expression where = this.where;//备份
				
				this.fields = null;//setAccessColumn不应该影响dataStruct
				this.where = null;//防止提前过滤造成fetch为空时获取不到数据结构
				
				if(this.file != null || this.meta != null || this.cursor != null)
				{
					if(this.type == TableNode.TYPE_GTB)	// 组表文件
					{
						if(this.meta != null)
						{
							this.struct = new DataStruct(this.meta.getColNames());
						}
						else if(this.file != null)
						{
							ComTable group = null;
							try 
							{
								group = ComTable.open(this.file.getLocalFile().getFile(), ctx);
							} 
							catch (Exception e) 
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":TableNode.dataStruct, 打开组表文件失败", e);
							}
							
							PhyTable table = group.getBaseTable();
							if(table == null)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":TableNode.dataStruct, 组表中没有创建基表");
							}
							
							this.struct = new DataStruct(table.getColNames());
						}
					}
					else
					{
						ICursor icursor = getCursor();
						if(icursor != null)
						{
							Sequence seq = icursor.peek(1);
							//System.out.println(seq);
							if(seq != null)
							{
								this.struct = seq.dataStruct();
							}
							else
							{
								this.struct = icursor.getDataStruct();
							}
							icursor.close();
						}
						
						if(this.cursor != null)
						{
							this.cursor.reset();
						}
					}
				}
				
				this.fields = fields;//还原setAccessColumn
				this.where = where; //还原提前过滤
			}
			
			//System.out.println("this.struct="+Arrays.toString(this.struct.getFieldNames()));
			
			return this.struct;
		}
		
		public boolean isIdentic(String symbol)
		{
			if(this.alias == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.isIdentic, 无效的表节点");
			}
			
			if(symbol == null)
			{
				return false;
			}
			
			return symbol.equalsIgnoreCase(this.alias) || symbol.equalsIgnoreCase("\"" + this.alias + "\"");
		}
		
		public void optimize()
		{
		}

		public String toExpression() 
		{
			ICursor icursor = getCursor();
			if(icursor == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.toExpression, 无效的表节点");
			}
			
			Sequence sequence = icursor.peek(1);
			if(sequence == null || sequence.length() != 1 || sequence.dataStruct() == null ||sequence.dataStruct().getFieldCount() != 1)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":TableNode.toExpression, 非IN/EXISTS子句的子查询必须能解析为单一值");
			}
			else
			{
				String value = null;
				Object param = ((BaseRecord)sequence.get(1)).getFieldValue(0);
				if(param == null)
				{
					value = "null";
				}
				else if(param instanceof String)
				{
					value = String.format("\"%s\"", param.toString());
				}
				else if(param instanceof Boolean)
				{
					value = param.toString();
				}
				else if(param instanceof Number)
				{
					value = param.toString();
				}
				else if(param instanceof java.sql.Date)
				{
					value = String.format("date(\"%s\",\"yyyy-MM-dd\")", new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date)param));
				}
				else if(param instanceof java.sql.Time)
				{
					value = String.format("time(\"%s\",\"HH:mm:ss.SSS\")", new SimpleDateFormat("HH:mm:ss.SSS").format((java.sql.Time)param));
				}
				else if(param instanceof java.sql.Timestamp)
				{
					value = String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((java.sql.Timestamp)param));
				}
				else if(param instanceof Date)
				{
					value = String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date)param));
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("function.paramError") + ":scanExp, 尚不支持的数据类型");
				}
				return value;
			}
		}

		public boolean hasGather()  //不会被select不考虑
		{
			return false;
		}

		public boolean hasFieldNotGroup()  //不会被select不考虑
		{
			return false;
		}

		public void setFromHaving() 
		{
		}

		public void setFromWhere() 
		{
		}

		public void collect() 
		{
		}

		public boolean hasField(Boolean isOrder) 
		{
			return false;
		}
		
		public boolean hasField(String fieldName) 
		{
			return false;
		}
	}

	class FieldNode extends Node
	{
		private String name;
		private String original;
		
		public FieldNode(String fieldName)
		{
			if(fieldName != null)
			{
				if(fieldName.startsWith("\"") && fieldName.endsWith("\"") && fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
				{
					Token[] fieldTokens = Tokenizer.parse(fieldName.substring(1, fieldName.length() - 1));
					if(fieldTokens != null && fieldTokens.length == 1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
				}
				
				this.original = fieldName;
				this.name = fieldName.toLowerCase();
			}
		}
		
		public String getName()
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.getName, 无效的字段节点");
			}
			
			return this.name;
		}
		
		public String getOriginal()
		{
			if(this.original == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.getOriginal, 无效的字段节点");
			}
			
			return this.original;
		}
		
		public void optimize()
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.optimize, 无效的字段节点");
			}
			
			String name = this.name.toLowerCase();
			if(selectFieldList != null && !selectFieldList.contains(name))
			{
				selectFieldList.add(name);
			}
		}

		public String toExpression() 
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.toExpression, 无效的字段节点");
			}
			
			return "'"+this.name+"'";
		}

		public boolean hasGather() 
		{
			return false;
		}
		
		public boolean isIdentic(String fieldName)
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.isIdentic, 无效的字段节点");
			}
			
			return this.name.equalsIgnoreCase(fieldName);
		}

		public boolean hasFieldNotGroup() 
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":FieldNode.hasFieldNotGroup, 无效的字段节点");
			}
			
			if(groupList == null && groupList.size() == 0)
			{
				return true;
			}
			
			for(Node node : groupList)
			{
				if(node.hasField(this.name))
				{
					return false;
				}
			}
			
			return true;
		}

		public void setFromHaving() 
		{
		}

		public void setFromWhere() 
		{
		}
		
		public void collect() 
		{
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			return true;
		}
		
		public boolean hasField(String fieldName) 
		{
			return isIdentic(fieldName);
		}
	}

	class NormalNode extends Node
	{
		private String value;
		
		public NormalNode(String nodeValue)
		{
			this.value = nodeValue;
		}
		
		public String getValue()
		{
			if(this.value == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":NormalNode.getValue, 无效的常数节点");
			}
			
			return this.value;
		}

		public void optimize()
		{
		}

		public String toExpression() 
		{
			if(this.value == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":NormalNode.toExpression, 无效的常数节点");
			}
			
			return this.value;
		}

		public boolean hasGather() 
		{
			return false;
		}
		
		public boolean hasFieldNotGroup() 
		{
			return false;
		}

		public void setFromHaving() 
		{
		}

		public void setFromWhere() 
		{
		}
		
		public void collect() 
		{
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			if(isOrder)
			{
				if(this.value == null)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":NormalNode.hasField, 无效的常数节点");
				}
				if(aliasList != null)
				{
					for(int i=0, sz=aliasList.size(); i<sz; i++)
					{
						String aliasName = aliasList.get(i);
						if(this.value.equalsIgnoreCase("'"+aliasName+"'"))
						{
							return true;
						}
					}
				}
			}
			return false;
		}
		
		public boolean hasField(String fieldName) 
		{
			return false;
		}
	}
	
	class ExpressionNode extends Node
	{
		private ArrayList<Node> list;
		
		public ExpressionNode(ArrayList<Node> nodeList)
		{
			this.list = nodeList;
		}
		
		public ArrayList<Node> getNodeList()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.getNodeList, 无效的表达式节点");
			}
			
			return this.list;
		}
		
		public boolean hasGather()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.hasGather, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasGather())
				{
					return true;
				}
			}
			
			return false;
		}
		
		public boolean hasFieldNotGroup()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.hasFieldNotGroup, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasFieldNotGroup())
				{
					return true;
				}
			}
			
			return false;
		}

		public void optimize()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.optimize, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.optimize();
				}
			}
		}

		public String toExpression() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.toExpression, 无效的表达式节点");
			}
			
			StringBuffer sb = new StringBuffer();
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					//针对coalesce函数的局部优化
					boolean optimize = false;
					if(node instanceof FunctionNode && ((FunctionNode)node).getName().equalsIgnoreCase("coalesce"))
					{
						boolean constExp = true;
						StringBuffer expBuffer = new StringBuffer();
						String[] paramsStr = getParams(((FunctionNode)node).getParamNode().toExpression());
						if(paramsStr.length == 2)
						{
							Token[] tmpTokens = Tokenizer.parse(paramsStr[1]);
							for(Token tmpToken : tmpTokens)
							{
								if(tmpToken.getType() == Tokenizer.LPAREN
								|| tmpToken.getType() == Tokenizer.RPAREN
								|| tmpToken.getType() == Tokenizer.NUMBER
								|| tmpToken.getType() == Tokenizer.OPERATOR
								|| tmpToken.getType() == Tokenizer.IDENT && tmpToken.getOriginString().startsWith("\"") && tmpToken.getOriginString().endsWith("\""))
								{
									;
								}
								else
								{
									constExp = false;
								}
							}
						}
						if(constExp)
						{
							expBuffer.append(paramsStr[1]);
							for(int j = i + 1; j < z; j++)
							{
								Node nextNode = this.list.get(j);
								String nextExp = ((NormalNode)nextNode).toExpression();
								if(nextNode instanceof NormalNode && !nextExp.equals("&&") && !nextExp.equals("||"))
								{
									expBuffer.append(nextExp);
								}
								else
								{
									break;
								}
							}
							Expression exp = new Expression(expBuffer.toString());
							try
							{
								Object objVal = exp.calculate(ctx);
								if(objVal != null && objVal.equals(false))
								{
									optimize = true;
									sb.append(paramsStr[0]);
								}
							}
							catch(Exception ex)
							{
								throw new RQException(ex.getMessage(), ex);
							}
						}
					}
					//优化结束
					if(!optimize)
					{
						sb.append(node.toExpression());
					}
				}
			}
			return sb.toString();
		}

		public void setFromHaving() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.setFromHaving, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.setFromHaving();
				}
			}
		}

		public void setFromWhere() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.setFromWhere, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.setFromWhere();
				}
			}
		}
		
		public void collect() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.collect, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.collect();;
				}
			}
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.hasField, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasField(isOrder))
				{
					return true;
				}
			}
			
			return false;
		}
		
		public boolean hasField(String fieldName) 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ExpressionNode.hasField, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasField(fieldName))
				{
					return true;
				}
			}
			
			return false;
		}
	}
	
	class ParenNode extends Node
	{
		private ExpressionNode node;
		
		public ParenNode(ExpressionNode expNode)
		{
			this.node = expNode;
		}
		
		public ExpressionNode getNode()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.getNode, 无效的括号节点");
			}
			
			return this.node;
		}
		
		public boolean hasGather()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.hasGather, 无效的括号节点");
			}
			
			return this.node.hasGather();
		}

		public void optimize()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.optimize, 无效的括号节点");
			}
			
			this.node.optimize();
		}

		public String toExpression() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.toExpression, 无效的括号节点");
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("(");
			sb.append(this.node.toExpression());
			sb.append(")");
			
			return sb.toString();
		}
		
		public boolean hasFieldNotGroup() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.hasFieldNotGroup, 无效的括号节点");
			}
			
			return this.node.hasFieldNotGroup();
		}

		public void setFromHaving() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.setFromHaving, 无效的括号节点");
			}
			
			this.node.setFromHaving();
		}

		public void setFromWhere() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.setFromWhere, 无效的括号节点");
			}
			
			this.node.setFromWhere();
		}
		
		public void collect() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.collect, 无效的括号节点");
			}
			
			this.node.collect();;
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.hasField, 无效的括号节点");
			}
			
			return this.node.hasField(isOrder);
		}
		
		public boolean hasField(String fieldName) 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":ParenNode.hasField, 无效的括号节点");
			}
			
			return this.node.hasField(fieldName);
		}
	}

	class GatherNode extends Node
	{
		private String name;
		private Node node;
		
		public GatherNode(String gatherName, Node paramNode)
		{
			if(gatherName != null)
			{
				this.name = gatherName.toLowerCase();
			}
			
			this.node = paramNode;
		}
		
		public String getName()
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.getName, 无效的聚合节点");
			}
			
			return this.name;
		}
		
		public Node getParamNode()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.getParamNode, 无效的聚合节点");
			}
			
			return this.node;
		}
		
		public void setParamNode(Node paramNode)
		{
			this.node = paramNode;
			
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.setParamNode, 参数不可为空");
			}
		}

		public void optimize()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.optimize, 无效的聚合节点");
			}
			
			this.node.optimize();
		}
		
		public String toExpression() 
		{
			if(this.name == null || this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.toExpression, 无效的聚合节点");
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append(this.name);
			sb.append("(");
			sb.append(this.node.toExpression());
			sb.append(")");
			
			return sb.toString();
		}
		
		public boolean hasGather() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.setFromHaving, 无效的聚合节点");
			}
			
			if(this.node.hasGather())
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.hasGather, 聚合函数参数中不允许嵌套聚合函数");
			}
			
			return true;
		}
		
		public boolean hasFieldNotGroup() 
		{
			return false;
		}

		public void setFromHaving() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.setFromHaving, 无效的聚合节点");
			}
			
			this.node.setFromHaving();
		}

		public void setFromWhere() 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":GatherNode.setFromWhere, where子句中不应包含聚合函数");
		}
		
		public void collect()
		{
			gatherNodeList.add(this);
		}
		
		public boolean hasField(Boolean isOrder)
		{
			return true;
		}
		
		public boolean hasField(String fieldName)
		{
			return false;
		}
	}
	
	class DimNode extends Node
	{
		String field;
		String level;
		ExpressionNode expression;
		
		public DimNode(String levelName)
		{
			if(levelName != null)
			{
				this.level = levelName.toLowerCase();
			}
		}
		
		public String getLevel()
		{
			if(this.level == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":DimNode.getLevel, 无效的维节点");
			}
			
			return this.level;
		}
		
		public String getField()
		{
			return this.field;
		}
		
		public void setField(String fieldName)
		{
			if(fieldName != null)
			{
				this.field = fieldName.toLowerCase();
			}
			
			if(field != null)
			{
				String function = getLevelFunction(this.level);
				function = function.replace("?", this.field);
				Token[] funTokens = Tokenizer.parse(function);
				this.expression = scanExp(funTokens, 0, funTokens.length);
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("search.needDimFieldWord"));
			}
		}
		
		public boolean hasGather() 
		{
			if(this.expression == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("search.needDimFieldWord"));
			}
			
			if(this.expression.hasGather())
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":DimNode.hasGather, 维节点表达式中不允许出现聚合函数");
			}
			
			return false;
		}

		public void optimize() 
		{
			if(this.expression == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("search.needDimFieldWord"));
			}
			
			this.expression.optimize();
		}

		public String toExpression() 
		{
			if(this.expression == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("search.needDimFieldWord"));
			}
			
			return this.expression.toExpression();
		}
		
		public boolean isIdentic(String levelName, String fieldName)
		{
			if(this.level == null || this.field == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":DimNode.isIdentic, 无效的维节点");
			}
			
			return this.level.equalsIgnoreCase(levelName) && this.field.equalsIgnoreCase(fieldName);
		}
		
		public boolean hasFieldNotGroup() 
		{
			if(this.level == null || this.field == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":DimNode.hasFieldNotGroup, 无效的维节点");
			}
			
			if(groupList == null && groupList.size() == 0)
			{
				return true;
			}
			
			for(Node node : groupList)
			{
				if(node instanceof DimNode)
				{
					if(((DimNode)node).isIdentic(this.level, this.field))
					{
						return false;
					}
				}
			}
			
			return true;
		}

		public void setFromHaving() 
		{
		}

		public void setFromWhere() 
		{
		}
		
		public void collect() 
		{
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			return true;
		}

		public boolean hasField(String fieldName)
		{
			return this.field.equalsIgnoreCase(fieldName);
		}
	}
	
	class InNode extends Node
	{
		private ExpressionNode node;
		private ExpressionNode param;
		private boolean not;
		private boolean isFromHaving;
		private boolean isFromWhere;
		
		public InNode(ExpressionNode expNode, ExpressionNode paramNode, boolean hasNot)
		{
			this.node = expNode;
			this.param = paramNode;
			this.not = hasNot;
		}
		
		public ExpressionNode getNode()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":InNode.getNode, 无效的In节点");
			}
			
			return this.node;
		}
		
		public ExpressionNode getParam()
		{
			if(this.param == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":InNode.getNodeList, 无效的In节点");
			}
			
			return this.param;
		}
		
		public boolean getNot()
		{
			return this.not;
		}

		public void optimize()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":InNode.optimize, 无效的In节点");
			}
			
			this.node.optimize();
			this.param.optimize();
		}
		
		public String toExpression() 
		{
			if(this.node == null || this.param == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":InNode.toExpression, 无效的In节点");
			}
			
			String expression = this.node.toExpression();
			if(this.param.getNodeList().size() == 1 && this.param.getNodeList().get(0) instanceof TableNode)//子查询式In子句
			{
				//子查询默认涉及表都可以内存化
				TableNode tbn = (TableNode) this.param.getNodeList().get(0);
				String[] names = new String[]{tbn.dataStruct().getFieldName(0)};
				tbn.setAccessColumn(names);
				
				Sequence tab = null;
				ICursor cursor = tbn.getCursor();
				if(cursor != null)
				{
					tab = cursor.fetch();
				}
				
				if(tab == null || tab.length() == 0)
				{
					if(this.not)
					{
						return "true";
					}
					else
					{
						return "false";
					}
				}
				else
				{
					if(!(tab.get(1) instanceof BaseRecord) || tab.dataStruct() == null || tab.dataStruct().getFieldCount() != 1)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":toExpression, IN中子查询结果异常");
					}
					
					StringBuffer sb = new StringBuffer();
					int size = tab.length();
					Sequence seq = new Sequence();
					for(int i = 1; i <= size; i++)
					{
						seq.add(((BaseRecord)tab.get(i)).getFieldValue(0));
					}
					if(size  <= 2)
					{
						sb.append("(");
						for(int i = 1; i <= size; i++)
						{
							sb.append(getProcValue(seq.get(i)));
							if(this.not)
							{
								sb.append("!=");
							}
							else
							{
								sb.append("==");
							}
							sb.append("(");
							sb.append("$?");
							sb.append(")");
							if(i != size)
							{
								if(this.not)
								{
									sb.append("&&");
								}
								else
								{
									sb.append("||");
								}
							}
						}
						sb.append(")");
					}
					else
					{
						seq = seq.sort("o");//准备用二分法查找所以先排序
						sb.append("(");
						sb.append("[");
						for(int i = 1; i <= size; i++)
						{
							if(i >= 2)
							{
								sb.append(",");
							}
							sb.append(getProcValue(seq.get(i)));
						}
						sb.append("]");
						sb.append(".pos@b(");
						sb.append("$?");
						sb.append(")");
						if(this.not)
						{
							sb.append("==");
						}
						else
						{
							sb.append("!=");
						}
						sb.append("null");
						sb.append(")");
					}
					
					//针对coalesce函数的局部优化
					String expStr = sb.toString();
					if(this.node.getNodeList().size() == 1  
					&& this.node.getNodeList().get(0) instanceof FunctionNode
					&& ((FunctionNode)this.node.getNodeList().get(0)).getName().equalsIgnoreCase("coalesce"))
					{
						FunctionNode coalesce = (FunctionNode) this.node.getNodeList().get(0);
						String[] paramsStr = getParams(coalesce.getParamNode().toExpression());
						if(paramsStr.length == 2)
						{
							String tmpExpStr = expStr.replace("$?", paramsStr[1]);
							Expression tmpExp = new Expression(tmpExpStr);
							try
							{
								Object objVal = tmpExp.calculate(ctx);
								if(objVal != null && objVal.equals(false))
								{
									expression = paramsStr[0];
								}
							}
							catch(Exception ex)
							{
								throw new RQException(ex.getMessage(), ex);
							}
						}
					}
					//优化结束
					expStr = expStr.replace("$?", expression);
					return expStr;
				}
			}
			else //普通序列式的IN子句，或含主查询字段的子查询的IN子句
			{
				String paramStr = this.param.toExpression().trim();
				
				//非join传入的子查询字段因为是后添加的
				//不算作选出字段故不需要去除单引号
				if(subQueryOfInMap.containsValue(paramStr))
				{
					//二分法搜索不再适合，因为需要对搜索值排序
					//而随主查询字段值不同，搜素值范围也会变化
					//故每行数据都需要排序再搜索，更加浪费时间
					StringBuffer sb = new StringBuffer();
					if(this.not)
					{
						sb.append("!");
					}
					sb.append(paramStr);
					sb.append(".contain(");
					sb.append(expression);
					sb.append(")");
					//针对coalesce函数的局部优化这里也不能使用
					//因为随主查询字段值不同搜素范围也会变化
					//故不能确定coalesce第二个参数是否被包含
					String expStr = sb.toString();
					return expStr;
				}
				else if(paramStr.startsWith("(") && paramStr.endsWith(")"))
				{
					//解析时为方便兼容子查询流程多加了一个括号需要在此去除,
					//同时这也作为区别含主查询字段的子查询的标识符的标志
					paramStr = paramStr.substring(1, paramStr.length()-1);
					
					StringBuffer sb = new StringBuffer();
					String[] params = paramStr.split(",");
					if(params.length <= 2)
					{
						sb.append("(");
						for(int pi = 0, pl = params.length; pi < pl; pi++)
						{
							sb.append(params[pi].trim());
							if(this.not)
							{
								sb.append("!=");
							}
							else
							{
								sb.append("==");
							}
							sb.append("(");
							sb.append("$?");
							sb.append(")");
							if(pi != pl - 1)
							{
								if(this.not)
								{
									sb.append("&&");
								}
								else
								{
									sb.append("||");
								}
							}
						}
						sb.append(")");
					}
					else
					{
						String[] values = getParams(paramStr);
						Map<Object, String> valueMap = new LinkedHashMap<Object, String>();
						Sequence seq = new Sequence();
						for(String value : values)
						{
							Object obj = new Expression(value).calculate(ctx);
							valueMap.put(obj, value);
							seq.add(obj);
						}
						seq.sort("o");//准备用二分法查找所以先排序
						StringBuffer tmp = new StringBuffer();
						for(int i = 1; i <= seq.length(); i++)
						{
							if(i > 1)
							{
								tmp.append(",");
							}
							tmp.append(valueMap.get(seq.get(i)));
						}
						sb.append("(");
						sb.append("[");
						sb.append(tmp.toString());
						sb.append("]");
						sb.append(".pos@b(");
						sb.append("$?");
						sb.append(")");
						if(this.not)
						{
							sb.append("==");
						}
						else
						{
							sb.append("!=");
						}
						sb.append("null");
						sb.append(")");
					}
					//针对coalesce函数的局部优化
					String expStr = sb.toString();
					if(this.node.getNodeList().size() == 1  
					&& this.node.getNodeList().get(0) instanceof FunctionNode
					&& ((FunctionNode)this.node.getNodeList().get(0)).getName().equalsIgnoreCase("coalesce"))
					{
						FunctionNode coalesce = (FunctionNode) this.node.getNodeList().get(0);
						String[] paramsStr = getParams(coalesce.getParamNode().toExpression());
						if(paramsStr.length == 2)
						{
							String tmpExpStr = expStr.replace("$?", paramsStr[1]);
							Expression tmpExp = new Expression(tmpExpStr);
							try
							{
								Object objVal = tmpExp.calculate(ctx);
								if(objVal != null && objVal.equals(false))
								{
									expression = paramsStr[0];
								}
							}
							catch(Exception ex)
							{
								throw new RQException(ex.getMessage(), ex);
							}
						}
					}
					//优化结束
					expStr = expStr.replace("$?", expression);
					return expStr;
				}
				else
				{
					//当join调用scanExp解析where时会进入此处
					//传入的子查询字段因为已经存在所以算作选出字段
					if(paramStr.startsWith("'$") && paramStr.endsWith("'"))
					{
						//二分法搜索不再适合，因为需要对搜索值排序
						//而随主查询字段值不同，搜素值范围也会变化
						//故每行数据都需要排序再搜索，更加浪费时间
						StringBuffer sb = new StringBuffer();
						if(this.not)
						{
							sb.append("!");
						}
						sb.append(paramStr);
						sb.append(".contain(");
						sb.append(expression);
						sb.append(")");
						//针对coalesce函数的局部优化这里也不能使用
						//因为随主查询字段值不同搜素范围也会变化
						//故不能确定coalesce第二个参数是否被包含
						String expStr = sb.toString();
						return expStr;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":InNode.toExpression, 无法识别的IN子句");
					}
				}
			}
		}

		public boolean hasGather() //不会被select不考虑
		{
			return false;
		}
		
		public boolean hasFieldNotGroup()  //不会被select不考虑
		{
			return false;
		}

		public void setFromHaving() 
		{
			this.isFromHaving = true;
		}

		public void setFromWhere() 
		{
			this.isFromWhere = true;
		}
		
		public void collect() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":InNode.collect, 无效的In节点");
			}
			
			this.node.collect();
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":InNode.hasField, In子句不可用于分组字段");
		}
		
		public boolean hasField(String fieldName)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":InNode.hasField, In子句不可用于查询字段");
		}
	}
	
	class LikeNode extends Node
	{
		private ExpressionNode node;
//		private String pattern;
		private boolean not;
		private boolean isFromHaving;
		private boolean isFromWhere;
		private Token token;
		private SimpleSelect select;

		
		public LikeNode(ExpressionNode expNode, Token token, boolean hasNot, SimpleSelect select)
		{
			this.node = expNode;
			//this.pattern = pattern;
			this.not = hasNot;
			this.token = token;
			this.select = select;
		}
		
		public String getPattern()
		{
			if(this.token == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.getPattern, 无效的Like节点");
			}
			return this.token.getString();
		}
		
		public boolean getNot()
		{
			return this.not;
		}

		public void optimize()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.optimize, 无效的Like节点");
			}
			
			this.node.optimize();
		}
		
		public String toExpression() 
		{
			if(this.node == null || this.token == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.toExpression, 无效的Like节点");
			}
			
			//
			String tt = this.token.toString();
			if (tt.indexOf("'") == 0 && tt.lastIndexOf("'") == tt.length() - 1) tt = tt.substring(1,tt.length()-1);
			
			String pattern = "\""+tt+"\"";
			
			if (token.getType() == Tokenizer.PARAMMARK) {
				ParamNode paramNode = new ParamNode();
				String strIndex = token.getString().substring(1);
					
				if(strIndex.length() != 0)
				{
					int paramIndex = Integer.parseInt(strIndex);
					paramNode.setIndex(paramIndex);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 参数占位符解析错误");
				}
				
				pattern = paramNode.toExpression();

			}
			
			pattern = pattern.replace("\\", "\\\\");
			pattern = pattern.replace("*", "\\\\*");
			pattern = pattern.replace("?", "\\\\?");
			
			pattern = pattern.replace("[_]", "" + (char)18 + (char)19);
			pattern = pattern.replace("_", "?");
			pattern = pattern.replace("" + (char)18 + (char)19, "_");
			
			pattern = pattern.replace("[%]", "" + (char)18 + (char)19);
			pattern = pattern.replace("%", "*");
			pattern = pattern.replace("" + (char)18 + (char)19, "%");
			
			pattern = pattern.replace("[[]", "[");
			
			return "like@c(" + this.node.toExpression() +"," + pattern + ")";
		}

		public boolean hasGather()  //不会被select不考虑
		{
			return false;
		}
		
		public boolean hasFieldNotGroup()  //不会被select不考虑
		{
			return false;
		}

		public void setFromHaving() 
		{
			this.isFromHaving = true;
		}

		public void setFromWhere() 
		{
			this.isFromWhere = true;
		}
		
		public void collect() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.collect, 无效的Like节点");
			}
			
			this.node.collect();
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.hasField, Like子句不可用于分组字段");
		}
		
		public boolean hasField(String fieldName)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":LikeNode.hasField, Like子句不可用于查询字段");
		}
	}
	
	class CaseNode extends Node
	{
		private List<Node> list;
		
		public CaseNode(List<Node> nodeList)
		{
			this.list = nodeList;
		}
		
		public List<Node> getNodeList()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.getNodeList, 无效的Case节点");
			}
			return this.list;
		}
		
		public void optimize()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.optimize, 无效的Case节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.optimize();
				}
			}
		}
		
		public String toExpression() 
		{
			StringBuffer buf = new StringBuffer(this.list.get(0).toExpression());
			int sz = this.list.size();
			if(this.list.size() % 2 == 0)
			{
				sz = sz - 1;
			}
			for(int i = 1; i < sz; i += 2)
			{
				buf.append(",");
				buf.append(String.format("%s:%s", this.list.get(i).toExpression(), this.list.get(i + 1).toExpression()));
			}
			if(this.list.size() % 2 == 0)
			{
				buf.append(";");
				buf.append(this.list.get(this.list.size() - 1).toExpression());
			}
			return String.format("case(%s)", buf.toString());
		}

		public boolean hasGather()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.hasGather, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasGather())
				{
					return true;
				}
			}
			
			return false;
		}
		
		public boolean hasFieldNotGroup()
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.hasFieldNotGroup, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasFieldNotGroup())
				{
					return true;
				}
			}
			
			return false;
		}

		public void setFromHaving() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.setFromHaving, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.setFromHaving();
				}
			}
		}

		public void setFromWhere() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.setFromWhere, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.setFromWhere();
				}
			}
		}
		
		public void collect() 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.collect, 无效的表达式节点");
			}
			
			for(int i = 0, z = this.list.size(); i < z; i++)
			{
				Node node = this.list.get(i);
				if(node != null)
				{
					node.collect();;
				}
			}
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.hasField, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasField(isOrder))
				{
					return true;
				}
			}
			
			return false;
		}
		
		public boolean hasField(String fieldName) 
		{
			if(this.list == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":CaseNode.hasField, 无效的表达式节点");
			}
			
			for(int i=0,z=this.list.size();i<z;i++)
			{
				Node node = this.list.get(i);
				if(node != null && node.hasField(fieldName))
				{
					return true;
				}
			}
			
			return false;
		}
	}
	
	class BetweenNode extends Node
	{
		private ExpressionNode node;
		private ExpressionNode floor;
		private ExpressionNode ceil;
		private boolean not;
		private boolean isFromHaving;
		private boolean isFromWhere;
		
		public BetweenNode(ExpressionNode expNode, ExpressionNode floorValue, ExpressionNode ceilValue, boolean hasNot)
		{
			this.node = expNode;
			this.floor = floorValue;
			this.ceil = ceilValue;
			this.not = hasNot;
		}
		
		public ExpressionNode getNode()
		{
			return this.node;
		}
		
		public ExpressionNode getFloor()
		{
			return this.floor;
		}
		
		public ExpressionNode getCeil()
		{
			return this.ceil;
		}
		
		public boolean getNot()
		{
			return this.not;
		}

		public void optimize()
		{
			if(this.node == null || this.floor == null || this.ceil == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":BetweenNode.optimize, 无效的Between节点");
			}
			
			this.node.optimize();
			this.floor.optimize();
			this.ceil.optimize();
		}
		
		public String toExpression()
		{
			if(this.node == null || this.floor == null || this.ceil == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":BetweenNode.toExpression, 无效的Between节点");
			}
			
			String x = this.node.toExpression();
			String a = this.floor.toExpression();
			String b = this.ceil.toExpression();
			String exp = null;
			if(this.not)
			{
				exp = String.format("!between(%s,%s:%s)", x, a, b);
			}
			else
			{
				exp = String.format("between(%s,%s:%s)", x, a, b);
			}
			return exp;
		}
		
		public boolean hasGather() //不会被select不用考虑
		{
			return false;
		}
		
		public boolean hasFieldNotGroup()  //不会被select不用考虑
		{
			return false;
		}

		public void setFromHaving() 
		{
			this.isFromHaving = true;
		}

		public void setFromWhere() 
		{
			this.isFromWhere = true;
		}
		
		public void collect() 
		{
			if(this.node == null || this.floor == null || this.ceil == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":BetweenNode.collect, 无效的Between节点");
			}
			
			this.node.collect();
			this.floor.collect();
			this.ceil.collect();
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":BetweenNode.hasField, Between子句不可用于分组字段");
		}
		
		public boolean hasField(String fieldName)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":BetweenNode.hasField, Between子句不可用于查询字段");
		}
	}
	
	class ParamNode extends Node
	{
		private boolean isFromHaving;
		private boolean isFromWhere;
		private int index;
		
		public ParamNode()
		{
			this.isFromHaving = false;
			this.isFromWhere = false;
			this.index = 0;
		}
		
		public void setIndex(int paramIndex)
		{
			this.index = paramIndex - 1;
		}
		
		public boolean hasGather()  //不会被select不考虑
		{
			return false;
		}

		public boolean hasFieldNotGroup()  //不会被select不考虑
		{
			return false;
		}

		public void optimize() 
		{
		}

		public String toExpression()
		{
			return getSQLParameters(this.index).toString();
		}

		public void setFromHaving() 
		{
			this.isFromHaving = true;
		}

		public void setFromWhere() 
		{
			this.isFromWhere = true;
		}
		
		public void collect() 
		{
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":ParamNode.hasField, '?'占位符不可用于分组字段");
		}
		
		public boolean hasField(String fieldName)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":ParamNode.hasField, '?'占位符不可用于查询字段");
		}
	}
	
	class FunctionNode extends Node
	{
		private String name;
		private Node node;
		
		public FunctionNode(String funcName, Node paramNode)
		{
			if(funcName != null)
			{
				this.name = funcName.toLowerCase();
			}
			this.node = paramNode;
		}
		
		public String getName()
		{
			if(this.name == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.getName, 无效的其他函数节点");
			}
			
			return this.name;
		}
		
		public Node getParamNode()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.getParamNode, 无效的其他函数节点");
			}
			
			return this.node;
		}
		
		public void setParamNode(Node paramNode)
		{
			this.node = paramNode;
			
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.setParamNode, 参数不可为空");
			}
		}

		public void optimize()
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.optimize, 无效的其他函数节点");
			}
			
			this.node.optimize();
		}
		
		public boolean hasGather() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.hasGather, 无效的其他函数节点");
			}
			
			return this.node.hasGather();
		}
		
		public boolean hasFieldNotGroup() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.hasFieldNotGroup, 无效的其他函数节点");
			}
			
			return this.node.hasFieldNotGroup();
		}

		public void setFromHaving() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.setFromHaving, 无效的其他函数节点");
			}
			
			this.node.setFromHaving();
		}

		public void setFromWhere() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.setFromWhere, 无效的其他函数节点");
			}
			
			this.node.setFromWhere();
		}
		
		public void collect() 
		{
			if(this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.collect, 无效的其他函数节点");
			}
			
			this.node.collect();;
		}
		
		public boolean hasField(Boolean isOrder) 
		{
			return this.node.hasField(isOrder);
		}

		public boolean hasField(String fieldName)
		{
			return this.node.hasField(fieldName);
		}
		
		public String toExpression() 
		{
			if(this.name == null || this.node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":OtherNode.toExpression, 无效的其他函数节点");
			}
			
			String[] paramsStr = getParams(this.node.toExpression());
			String exp = FunInfoManager.getFunctionExp("ESPROC", this.name, paramsStr);
			if(exp == null || exp.isEmpty())
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(this.name + mm.getMessage("function.unknownFunction"));
			}
			
			return exp;
		}
	}
	
	class SubQueryCursor extends SyncCursor
	{
		public static final int Exist_Type = 0; //不需要求出值证明存在即可，exists
		public static final int Select_Type = 1; //需要求出确切值且值必须唯一，select
		public static final int In_Type = 2; //需要求出确切值且值可不唯一, in
		public static final int Where_Type = 3; //需要求出确切值且值必须唯一, where
		
		private int type = -1;
		private ICellSet ics = null;
		private List<Object> paramList = null;
		private DataStruct ds = null;
		private DataStruct tabDs = null;
		private List<List<List<Token>>> subQueryListListList = null;
		private List<List<String>> fieldNameListList = null;
		private List<String> colNameList = null;
		
		public SubQueryCursor(ICursor cursor, int type, ICellSet ics, List<Object> paramList, List<String> colNameList, List<List<List<Token>>> subQueryListListList, List<List<String>> fieldNameListList, Context ctx, DataStruct tabDs)
		{
			super(cursor);
			this.type = type;
			if(this.type != Exist_Type && this.type != Select_Type && this.type != In_Type && this.type != Where_Type)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("function.paramError") + ":SubQueryCursor, 子查询游标初始化参数错误");
			}
			this.ics = ics;
			this.paramList = paramList;
			this.subQueryListListList = subQueryListListList;
			if(this.subQueryListListList == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("function.paramError") + ":SubQueryCursor, 子查询游标初始化参数错误");
			}
			this.fieldNameListList = fieldNameListList;
			if(this.fieldNameListList == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("function.paramError") + ":SubQueryCursor, 子查询游标初始化参数错误");
			}
			this.colNameList = colNameList;
			if(this.colNameList == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("function.paramError") + ":SubQueryCursor, 子查询游标初始化参数错误");
			}
			this.ctx = ctx;
			
			List<String> finalFieldNameList = new ArrayList<String>();
			finalFieldNameList.addAll(Arrays.asList(tabDs.getFieldNames()));
			finalFieldNameList.addAll(this.colNameList);
			String[] finalFieldNames = new String[finalFieldNameList.size()];
			finalFieldNameList.toArray(finalFieldNames);
			this.tabDs = new DataStruct(finalFieldNames);
			this.colNameList.clear();
		}
		
		protected Sequence get(int n) 
		{
			Sequence tab = super.get(n);
			if(tab != null && tab.dataStruct() != null)
			{
				int init = tab.dataStruct().getFieldCount();
				int size = this.fieldNameListList.size();
				for(int i = 0; i < size; i++)
				{
					DataStruct struct = new DataStruct(Arrays.copyOfRange(this.tabDs.getFieldNames(), 0, init + i + 1));
					Table res = new Table(struct);

					List<List<Token>> subQueryListList = this.subQueryListListList.get(i);
					List<String> fieldNameList = this.fieldNameListList.get(i);
					
					for(int m = 1; m <= tab.getMems().size(); m++)
					{
						Object obj = tab.getMems().get(m);
						if(!(obj instanceof BaseRecord))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, 查询结果序列必须由记录组成");
						}
						BaseRecord rec = (BaseRecord) obj;
						
						int index = 0;
						List<Token> subQueryList = new ArrayList<Token>();
						subQueryList.addAll(subQueryListList.get(index++));
						for(String fieldName : fieldNameList)
						{
							String fieldValue = getSQLValue(rec.getFieldValue(fieldName));
							Token[] fieldValueTokens = Tokenizer.parse(fieldValue);
							subQueryList.addAll(Arrays.asList(fieldValueTokens));
							if(index < subQueryListList.size())
							{
								subQueryList.addAll(subQueryListList.get(index++));
							}
						}
						Token[] subQuery = new Token[subQueryList.size()];
						subQueryList.toArray(subQuery);
						
						SimpleSQL lq = new SimpleSQL(this.ics, subQuery, 0, subQuery.length, this.paramList, this.ctx, false);
						lq.setMemory(true);
						Object result = lq.execute();
						Sequence sq = result instanceof ICursor ? ((ICursor)result).fetch() : (result instanceof Sequence ? (Sequence)result : null);
						
						Object val = null;
						if(sq != null)
						{
							if(this.type == SubQueryCursor.Exist_Type)
							{
								val = "1";
							}
							else if(this.type == SubQueryCursor.Select_Type || this.type == SubQueryCursor.Where_Type)
							{
								if(sq.length() != 1 || !(sq.get(1) instanceof BaseRecord) || sq.dataStruct() == null || sq.dataStruct().getFieldCount() != 1)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, SELECT/WHERE子句中子查询结果异常");
								}
								val = ((BaseRecord)sq.get(1)).getFieldValue(0);
							}
							else if(this.type == SubQueryCursor.In_Type)
							{
								Sequence v = new Sequence();
								for(int p = 1, q = sq.length(); p <= q; p++)
								{
									if(sq.length() == 0 ||!(sq.get(1) instanceof BaseRecord) || sq.dataStruct() == null || sq.dataStruct().getFieldCount() != 1)
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, IN子句中子查询结果异常");
									}
									v.add(((BaseRecord)sq.get(p)).getFieldValue(0));
								}
								val = v;
							}
						}
						
						BaseRecord newRec = new Record(struct);
						newRec.set(rec);
						newRec.set(init + i, val);
						res.add(newRec);
					}
					
					tab = res;
				}
			}

			return tab;
		}
		
		public DataStruct getDataStruct()
		{
			return this.ds;
		}
		
		public void setDataStruct(DataStruct ds)
		{
			this.ds = ds;
		}
		
		public DataStruct getTableDataStruct()
		{
			return this.tabDs;
		}
	}

	public SimpleSelect(ICellSet ics, Context ctx)
	{
		this.icur = null;
		this.ds = null;
		this.ctx = ctx;
		this.ics = ics;;
		init();
	}
	
	private void init()
	{
		this.hasDistinct = false;
		this.topNumber = -1;
		this.limitNumber = -1;
		this.offsetNumber = -1;
		this.parallelNumber = 1;
		this.distinctGatherField = null; // must be null when init
		this.levelMap = new LinkedHashMap<String, String>();
		this.finalList = new ArrayList<String>();
		this.aliasList = new ArrayList<String>();
		this.columnList = new ArrayList<ExpressionNode>();
		this.groupList = new ArrayList<Node>();
		this.sortList = new ArrayList<Node>();
		this.tableNode = null;
		this.havingNode = null;
		this.whereNode = null;
		this.selectFieldList = new ArrayList<String>();
		this.gatherNodeList = new ArrayList<GatherNode>();
		this.parameterList = new ArrayList<Object>();
		this.columnIndex = 1;
		if(this.ctx == null)
		{
			this.ctx = new Context();
		}
		this.tablePathMap = new HashMap<String, String>();
		this.subQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfSelectEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfExistsMap = new HashMap<String, String>();
		this.subQueryOfSelectMap = new HashMap<String, String>();
		this.subQueryOfInMap = new HashMap<String, String>();
		this.subQueryOfWhereMap = new HashMap<String, String>();
		this.password = null;
		this.tmd = null;
		this.topFilter = null;
	}
	
	public void setTablePath(String name, String path)
	{
		if(name != null)
		{
			this.tablePathMap.put(name.toLowerCase(), path);
		}
	}
	
	public void setLevelFunction(String levelName, String levelFunction)
	{
		if(levelName == null || levelName.trim().isEmpty() || levelFunction == null || levelFunction.trim().isEmpty())
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("function.paramError") + ":setLevelFunction, 参数不能为空值");
		}
		levelName = levelName.toLowerCase();
		levelFunction = levelFunction.toLowerCase();
		Token[] tokens = Tokenizer.parse(levelFunction);
		for(int i=0; i<tokens.length; i++)
		{
			if(tokens[i].getType() == Tokenizer.LPAREN)
			{
				i = Tokenizer.scanParen(tokens, i, tokens.length);
			}
		}
		if(tokens[0].getType() != Tokenizer.LPAREN || tokens[tokens.length - 1].getType() != Tokenizer.RPAREN)
		{
			levelFunction = "(" + levelFunction + ")";
		}
		this.levelMap.put(levelName, levelFunction);
	}
	
	private String getLevelFunction(String levelName)
	{
		if(levelName == null || levelName.trim().isEmpty())
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("function.paramError") + ":getLevelFunction, 参数不能为空值");
		}
		String function = this.levelMap.get(levelName.toLowerCase());
		if(function == null)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("config.lessDimFormula"));
		}
		return function; 
	}
	
	public Boolean isLevelFunction(String levelName)
	{
		if(levelName == null || levelName.trim().isEmpty())
		{
			return false;
		}
		return this.levelMap.containsKey(levelName.toLowerCase()); 
	}
	
	public void setSQLParameters(Object param)
	{
		if(param == null)
		{
			this.parameterList.add("null");
		}
		else if(param instanceof String)
		{
			if(((String)param).startsWith("\"") && ((String)param).endsWith("\"") 
				&& ((String)param).substring(1, ((String)param).length() - 1).indexOf("\"") == -1 ||
			((String)param).startsWith("date(") && ((String) param).endsWith(")") ||
			((String)param).startsWith("datetime(") && ((String) param).endsWith(")") ||
			((String)param).startsWith("time(") && ((String) param).endsWith(")") ||
			((String)param).equalsIgnoreCase("true") ||
			((String)param).equalsIgnoreCase("false") ||
			((String)param).equalsIgnoreCase("null"))
			{
				this.parameterList.add(param);
			}
			else
			{
				this.parameterList.add(String.format("\"%s\"", param.toString()));
			}
		}
		else if(param instanceof Boolean)
		{
			this.parameterList.add(param);
		}
		else if(param instanceof Number)
		{
			this.parameterList.add(param);
		}
		else if(param instanceof java.sql.Date)
		{
			this.parameterList.add(String.format("date(\"%s\",\"yyyy-MM-dd\")", new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date)param)));
		}
		else if(param instanceof java.sql.Time)
		{
			this.parameterList.add(String.format("time(\"%s\",\"HH:mm:ss.SSS\")", new SimpleDateFormat("HH:mm:ss.SSS").format((java.sql.Time)param)));
		}
		else if(param instanceof java.sql.Timestamp)
		{
			this.parameterList.add(String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((java.sql.Timestamp)param)));
		}
		else if(param instanceof Date)
		{
			this.parameterList.add(String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date)param)));
		}
		else if(param instanceof byte[])
		{
			this.parameterList.add(new String((byte[])param));
		}
		else if(param instanceof ICursor)
		{
			this.icur = (ICursor)param;
		}
		else if(param instanceof DataStruct)
		{
			this.ds = (DataStruct)param;
		}

		//20240731 xingjl
		else if(param instanceof Sequence)
		{
			this.fromSeq = (Sequence)param;
			ComputeStack stack = this.ctx.getComputeStack();
			stack.pushArg(this.fromSeq);
			this.parameterList.add("null");
		}

		else if(param instanceof PhyTable)
		{
			this.tmd = (PhyTable)param;
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("function.paramError") + ":setSQLParameters, 尚不支持的参数类型");
		}
	}
	
	public void setSQLParameters(List<Object> paramList)
	{
		if(paramList != null && !paramList.isEmpty())
		{
			for(Object param : paramList)
			{
				setSQLParameters(param);
			}
		}
	}
	
	private Object getSQLParameters(int paramIndex)
	{		
		if(this.parameterList != null && this.parameterList.size() > paramIndex)
		{
			Object param = this.parameterList.get(paramIndex);
			return param;
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("search.lessParam") + ":getDQLParameters");
		}
	}
	
	private int pos(int p1, int def) 
	{
		if (p1 > 0) return p1;
		return def;
	}
	
	private int pos(int p1, int p2, int def) 
	{
		if (p1 > 0) return p1;
		if (p2 > 0) return p2;
		return def;
	}
	
	private int pos(int p1, int p2, int p3, int def) 
	{
		if (p1 > 0) return p1;
		if (p2 > 0) return p2;
		if (p3 > 0) return p3;
		return def;
	}
	
	private int pos(int p1, int p2, int p3, int p4, int def) 
	{
		if (p1 > 0) return p1;
		if (p2 > 0) return p2;
		if (p3 > 0) return p3;
		if (p4 > 0) return p4;
		return def;
	}
	
	private int pos(int p1, int p2, int p3, int p4, int p5, int p6, int def) 
	{
		if (p1 > 0) return p1;
		if (p2 > 0) return p2;
		if (p3 > 0) return p3;
		if (p4 > 0) return p4;
		if (p5 > 0) return p5;
		if (p6 > 0) return p6;
		return def;
	}
	
	public ICursor query(String dql)
	{
		Token[] dqlTokens = Tokenizer.parse(dql);
		return query(dqlTokens, 0, dqlTokens.length);
	}
	
	public ICursor query(Token[] tokens, int start, int end)
	{
		scanSelect(tokens, start, end);
		execute();
		init();
		return this.icur;
	}
	
	public void scanSelect(Token []tokens, int start, int next) 
	{
		if(tokens[start].isKeyWord("SELECT"))
		{
			start++; // skip over "select"
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, SQL语句必须以SELECT关键字开头");
		}
		
		if (start >= next) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, 起始位置超出结束位置");
		}
		
		start = scanQuantifies(tokens, start, next);

		int colPos = start;
		int fromPos = -1;
		int havePos = -1;
		int orderPos = -1;
		int limitPos = -1;
		int offsetPos = -1;
		
		for (int i = colPos; i < next; ++i) 
		{
			Token token = tokens[i];
			if(token.isKeyWord()) 
			{
				if (fromPos == -1) 
				{
					if (token.equals("FROM")) 
					{
						fromPos = i;
					}
				} 
				else if (havePos == -1 && token.equals("HAVING")) 
				{
					havePos = i;
				} 
				else if (token.equals("ORDER")) 
				{
					orderPos = i;
				}
				else if (token.equals("LIMIT")) 
				{
					limitPos = i;
				}
				else if (token.equals("OFFSET")) 
				{
					offsetPos = i;
					break;
				}
			}
			else if (token.getType() == Tokenizer.LPAREN) // 跳过()
			{
				i = Tokenizer.scanParen(tokens, i, next);
			}
		}
		
		if (fromPos < 0) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.lessTable"));
		}
		
		int fromEnd = pos(havePos, orderPos, limitPos, offsetPos, next);
		scanFrom(tokens, fromPos, fromEnd);
		
		int colEnd = fromPos;
		scanColumns(tokens, colPos, colEnd);
		
		if (havePos > 0) 
		{
			int haveEnd = pos(orderPos, limitPos, offsetPos, next);
			Token[] havingTokens = Arrays.copyOfRange(tokens, havePos + 1, haveEnd);
			havingTokens = optimizeWhere(havingTokens, this.parameterList);
			this.havingNode = scanExp(havingTokens, 0, havingTokens.length);
		}

		if (orderPos > 0) 
		{
			int orderEnd = pos(limitPos, offsetPos, next);
			scanOrders(tokens, orderPos, orderEnd);
		}
		
		if (limitPos > 0) 
		{
			int limitEnd = pos(offsetPos, next);
			scanLimit(tokens, limitPos, limitEnd);
		}
		
		if (offsetPos > 0) 
		{
			scanOffset(tokens, offsetPos, next);
		}
	}
	
	private int scanQuantifies(Token []tokens, int start, int next) 
	{
		Token t = tokens[start];
		if(t.isKeyWord()) 
		{
			int c = start;
			if (t.getString().equalsIgnoreCase("DISTINCT")) 
			{
				c++;
				if (c >= next) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanQuantifies, 起始位置超出结束位置");
				}
				
				this.hasDistinct = true;
				
				t = tokens[c];
			}

			if(t.isKeyWord() && t.getString().equalsIgnoreCase("TOP")) 
			{
				c++;
				try
				{
					this.topNumber = Integer.parseInt(tokens[c].getString());
					if(this.topNumber < 0)
					{
						throw new NumberFormatException();
					}
				}
				catch(NumberFormatException nfe)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanQuantifies, TOP关键字后面必须接非负整数", nfe);
				}
				
				c++;
				if (c >= next) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanQuantifies, 起始位置超出结束位置");
				}
			}
			
			return c;
		} 
		else 
		{
			return start;
		}
	}
	
	private void scanLimit(Token []tokens, int start, int next) 
	{
		Token t = tokens[start];
		if(t.isKeyWord("LIMIT")) 
		{
			int c = start + 1;
			if (c >= next) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanLimit, 起始位置超出结束位置");
			}
			try
			{
				if(tokens[c].isKeyWord("ALL"))
				{
					this.limitNumber = -1;
				}
				else
				{
					this.limitNumber = Integer.parseInt(tokens[c].getString());
					if(this.limitNumber < 0)
					{
						throw new NumberFormatException();
					}
				}
			}
			catch(NumberFormatException nfe)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanLimit, Limit关键字后面必须接非负整数", nfe);
			}
		}
	}
	
	private void scanOffset(Token []tokens, int start, int next) 
	{
		Token t = tokens[start];
		if(t.isKeyWord("OFFSET"))
		{
			int c = start + 1;
			if (c >= next) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanOffset, 起始位置超出结束位置");
			}
			try
			{
				this.offsetNumber = Integer.parseInt(tokens[c].getString());
				if(this.offsetNumber < 0)
				{
					throw new NumberFormatException();
				}
			}
			catch(NumberFormatException nfe)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanOffset, Offset关键字后面必须接非负整数", nfe);
			}
		}
	}
	
	private void scanFrom(Token[] tokens, int start, int next) 
	{
		start++; // skip over "from"
		
		if (start < next) 
		{
			int wherePos = Tokenizer.scanKeyWord("WHERE", tokens, start, next);
			int byPos = Tokenizer.scanKeyWord("BY", tokens, pos(wherePos, start), next);
			int groupPos = Tokenizer.scanKeyWord("GROUP", tokens, pos(wherePos, start), pos(byPos, next));
			int tableNext = pos(wherePos, pos(groupPos, byPos), next);
			if (start >= tableNext) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 起始位置超出结束位置");
			}

			if(tokens[start].getString().equals("{"))
			{
				int pos = start + 1;
				if(pos == tableNext)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 集算器表达式起始位置超出结束位置");
				}
				
				int end = Tokenizer.scanBrace(tokens, start, tableNext);
				
				String tableName = null;
				String aliasName = null;
				
				if(end + 3 == tableNext && tokens[end + 1].isKeyWord("AS"))
				{
					tableName = "";
					aliasName = tokens[end + 2].getString();
				}
				else if(end + 2 == tableNext)
				{
					tableName = "";
					aliasName = tokens[end + 1].getString();
				}
				else if(end + 1 == tableNext)
				{
					tableName = "";
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 集算器表达式的结尾处有误");
				}
				
				if(aliasName == null)
				{
					aliasName = "";
				}
				
				Token[] expTokens = Arrays.copyOfRange(tokens, pos, end);
				StringBuffer expBuf = new StringBuffer();
				for(int i = 0; i < expTokens.length; i++)
				{
					expBuf.append(expTokens[i].getOriginString());
					expBuf.append(expTokens[i].getSpaces());
				}
				String expStr = expBuf.toString().trim();
				
				Object obj = null;
				try
				{
					//20240731 xingjl
					Sequence seq = new Sequence();
					seq.add(this.fromSeq);
					this.ctx.getComputeStack().pushArg(seq);

					obj = new Expression(this.ics, this.ctx, expStr).calculate(this.ctx);
				}
				catch(Exception ex)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+expStr);
				}

				if(obj instanceof ICursor)
				{
					ICursor cursor = (ICursor)obj;
					DataStruct struct = cursor.getDataStruct();
					if(struct == null)
					{
						Sequence sq = cursor.peek(1);
						if(sq != null)
						{
							struct = sq.dataStruct();
						}
						//cursor.reset();
					}
					this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
				}
				else if(obj instanceof Table)
				{
					ICursor cursor = new MemoryCursor((Table)obj);
					DataStruct struct = ((Table)obj).dataStruct();
					this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
				}
				else if(obj instanceof PhyTable)
				{
					this.tableNode = new TableNode(tableName, aliasName, (PhyTable)obj);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的表变量类型");
				}
			}
			else if(tokens[start].getType() == Tokenizer.LPAREN)
			{
				int pos = start + 1;
				if(pos == tableNext)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 临时表子句起始位置超出结束位置");
				}
				
				int end = Tokenizer.scanParen(tokens, start, tableNext);
				
				String tableName = null;
				String aliasName = null;
				
				if(end + 2 == tableNext)
				{
					tableName = "";
					aliasName = tokens[end + 1].getString();
				}
				else if(end + 1 == tableNext)
				{
					tableName = "";
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 临时表子句的结尾处有误");
				}
				
				if(aliasName == null)
				{
					aliasName = "";
				}
				
				if(tokens[pos].getType() == Tokenizer.PARAMMARK)
				{
					if(pos + 1 != end)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 临时表占位符格式错误");
					}
					if(this.ds != null)
					{
						if(this.icur == null)
						{
							this.icur = new MemoryCursor(new Table(this.ds));
						}
						this.tableNode = new TableNode(tableName, aliasName, this.icur, this.ds);
						this.icur = null;
						this.ds = null;
					}
					else if(this.tmd != null)
					{
						this.tableNode = new TableNode(tableName, aliasName, this.tmd);
						this.tmd = null;
					}
				}
				else
				{
					SimpleSQL lq = new SimpleSQL(this.ics, tokens, pos, end, parameterList, this.ctx, false);
					lq.setMemory(this.isMemory);
					ICursor cursor = lq.query();
					DataStruct struct = lq.getDataStruct();
					this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
				}
			}
			else
			{
				String tableName = "";
				String aliasName = null;
				int pos = start;
				while(pos < tableNext)
				{
					tableName = tableName + tokens[pos].getOriginString();
					//xingjl 20230323 from D://test/1集算器/emps.txt，去掉1后面空格 
					//       20231106 去掉影响表别名，优先支持表别名，以数字开头的文件夹暂不支持。
					tableName = tableName + tokens[pos].getSpaces();
					pos++;
				}
				tableName = tableName.trim();
				if (tableNext - 2 >= start && tokens[tableNext - 1].getType() == Tokenizer.IDENT)
				{
					int splitPos = tableName.lastIndexOf(" ");
					if(splitPos != -1)
					{
						aliasName = tableName.substring(splitPos + 1);
						if(aliasName.equals(tokens[tableNext - 1].getOriginString()))
						{
							tableName = tableName.substring(0, splitPos).trim();
						}
						else
						{
							aliasName = null;
						}
					}
					if(tableNext - 3 >= start && tokens[tableNext - 2].isKeyWord("AS"))
					{
						splitPos = tableName.lastIndexOf(" ");
						if(splitPos != -1)
						{
							String asKeyWord = tableName.substring(splitPos + 1);
							if(asKeyWord.equals(tokens[tableNext - 2].getOriginString()))
							{
								tableName = tableName.substring(0, splitPos).trim();
							}
						}
					}
				}
				if(tableName.startsWith("\"") && tableName.endsWith("\"") 
				&& tableName.substring(1, tableName.length()-1).indexOf("\"") == -1)
				{
					tableName = tableName.substring(1, tableName.length() - 1);
				}
				
				String paramStr = null;
				if(tableName.endsWith(")"))
				{
					int lParenPos = tableName.lastIndexOf("(");
					if(lParenPos != -1)
					{
						paramStr = tableName.substring(lParenPos + 1, tableName.length() - 1).trim();
						tableName = tableName.substring(0, lParenPos).trim()+".dfx";
					}
				}
				
				boolean fileExists = false;
				//System.out.println("tableName " + tableName);
				File[] fs = FileUtil.getFiles(tableName);
				if (fs == null) {
					fs = FileUtil.getFiles(Env.getMainPath()+"/"+tableName);
					if (fs == null && Env.getPaths() != null) {
						for (int i=0; i<Env.getPaths().length; i++) {
							fs = FileUtil.getFiles(Env.getPaths()[i]+"/"+tableName);
							if (fs != null) break;
						}
					}
				}
				if (fs == null || fs.length == 0) {
					MessageManager mm = ParseMessage.get();
					throw new RQException("not found data file["+tableName+"]、["+Env.getMainPath()+"/"+tableName+"]");
				}
				//System.out.println("fs.length " + fs.length);
				FileObject fileObject = new FileObject(fs[0].getAbsolutePath(), null, "s", this.ctx);
				ArrayList<FileObject> objs = new ArrayList<FileObject>();
				for (int i=0; i<fs.length; i++) objs.add(new FileObject(fs[i].getAbsolutePath(), null, "s", this.ctx));
				if(!fileObject.isExists())
				{
					String password = null;
					int index = tableName.lastIndexOf(":"); //可能有密码，尝试去除
					if(index != -1)
					{
						String newName = tableName.substring(0, index).trim();
						password = tableName.substring(index + 1).trim();
						fileObject = new FileObject(newName, null, "s", this.ctx);
					}
					if(!fileObject.isExists())
					{
						String newName = this.tablePathMap.get(tableName.toLowerCase());
						if(newName == null || newName.isEmpty())
						{
							Param param = this.ctx.getParam(tableName);
							if(param != null)
							{
								Object paramNet = param.getValue();
								newName = ((paramNet == null) ? null : paramNet.toString());
							}
						}
						else if(aliasName == null || aliasName.isEmpty())
						{
							aliasName = tableName;
						}
						if(newName != null && !newName.isEmpty())
						{
							fileObject = new FileObject(newName, null, "s", this.ctx);
							if(fileObject.isExists())
							{
								tableName = newName;
								fileExists = true;
							}
						}
					}
					else
					{
						if(!password.startsWith("'") || !password.endsWith("'"))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 文件密码必须使用单引号括起来");
						}
						this.password = password.substring(1, password.length() - 1);
						tableName = fileObject.getFileName();
						fileExists = true;
					}
				}
				else
				{
					fileExists = true;
				}
				
				if(aliasName == null)
				{
					aliasName = "";
				}
				
				int fileType = TableNode.TYPE_ICR;
				if(fileExists && tableName.toLowerCase().endsWith(".ctx"))
				{
					fileType = TableNode.TYPE_GTB;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".btx"))
				{
					fileType = TableNode.TYPE_BIN;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".txt"))
				{
					fileType = TableNode.TYPE_TXT;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".csv"))
				{
					fileType = TableNode.TYPE_CSV;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".xls"))
				{
					fileType = TableNode.TYPE_XLS;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".xlsx"))
				{
					fileType = TableNode.TYPE_XLSX;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".json"))
				{
					fileType = TableNode.TYPE_JSON;
					this.tableNode = new TableNode(tableName, aliasName, fileObject, fileType);
				}
				else if(fileExists && tableName.toLowerCase().endsWith(".dfx"))
				{
					String dfxName = "\"" + tableName + "\"";
					if(paramStr.isEmpty())
					{
						paramStr = dfxName;
					}
					else
					{
						paramStr = dfxName + "," + paramStr;
					}
					
					Object obj = null;
					try
					{
						obj = new Expression(this.ctx, String.format("call@r(%s)", paramStr)).calculate(this.ctx);
					}
					catch(Exception ex)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+paramStr);
					}
					
					if(obj instanceof ICursor)
					{
						ICursor cursor = (ICursor)obj;
						DataStruct struct = cursor.getDataStruct();
						if(struct == null)
						{
							Sequence sq = cursor.peek(1);
							if(sq != null)
							{
								struct = sq.dataStruct();
							}
							cursor.reset();
						}
						this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
					}
					else if(obj instanceof Table)
					{
						ICursor cursor = new MemoryCursor((Table)obj);
						DataStruct struct = ((Table)obj).dataStruct();
						this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
					}
					else if(obj instanceof PhyTable)
					{
						this.tableNode = new TableNode(tableName, aliasName, (PhyTable)obj);
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的表变量类型");
					}
				}
				else if(tokens[start].getType() == Tokenizer.IDENT) // 尝试解析为直接写变量名的形式，否则抛出异常
				{
					pos = start + 1;
					if(pos == tableNext)
					{
						tableName = tokens[start].getOriginString();
						aliasName = null;
					}
					else if(pos + 1 == tableNext && tokens[pos].getType() == Tokenizer.IDENT)
					{
						tableName = tokens[start].getOriginString();
						aliasName = tokens[pos].getOriginString();
					}
					else if(pos + 2 == tableNext && tokens[pos].isKeyWord("AS") && tokens[pos + 1].getType() == Tokenizer.IDENT)
					{
						tableName = tokens[start].getOriginString();
						aliasName = tokens[pos+1].getOriginString();
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+tokens[start].getOriginString());
					}
					
					if(aliasName == null)
					{
						aliasName = "";
					}
					
					Object obj = null;
					try
					{
						obj = new Expression(this.ics, this.ctx, tableName).calculate(this.ctx);
					}
					catch(Exception ex)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+tableName);
					}
					
					if(obj instanceof ICursor)
					{
						ICursor cursor = (ICursor)obj;
						DataStruct struct = cursor.getDataStruct();
						if(struct == null)
						{
							Sequence sq = cursor.peek(1);
							if(sq != null)
							{
								struct = sq.dataStruct();
							}
							//cursor.reset();
						}
						this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
					}
					else if(obj instanceof Table)
					{
						ICursor cursor = new MemoryCursor((Table)obj);
						DataStruct struct = ((Table)obj).dataStruct();
						this.tableNode = new TableNode(tableName, aliasName, cursor, struct);
					}
					else if(obj instanceof PhyTable)
					{
						this.tableNode = new TableNode(tableName, aliasName, (PhyTable)obj);
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的表变量类型");
					}
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+tokens[start].getOriginString());
				}
				
				if (this.tableNode != null) this.tableNode.setFiles(objs);
			}
			
			if (wherePos > 0) 
			{
				int end = pos(groupPos, byPos, next);
				Token[] whereTokens = Arrays.copyOfRange(tokens, wherePos + 1, end);
				whereTokens = optimizeWhere(whereTokens, this.parameterList);
				ExpressionNode expNode = scanExp(whereTokens, 0, whereTokens.length);
				this.whereNode = expNode;
			}
			
			if (byPos > 0) 
			{
				scanBy(tokens, byPos, next);
			}
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			String n = ":";
			try {n += tokens[start].getString();}catch(Exception e){}
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名(注意表名不能为关键字或以数字开头):"+n);
		}
	}
	
	private void scanColumns(Token []tokens, int start, int next) 
	{
		while (start < next) 
		{
			int comma = Tokenizer.scanComma(tokens, start, next);

			if (comma < 0) 
			{
				scanColumn(tokens, start, next);
				break;
			} 
			else 
			{
				scanColumn(tokens, start, comma);
				start = comma + 1;
				if (start >= next) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanColumns, 起始位置超出结束位置");
				}
			}
		}
	}
	
	private void scanColumn(Token []tokens, int start, int next) 
	{
		if (start >= next) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 起始位置超出结束位置");
		}
		
		if (start + 1 == next && tokens[start].equals("*")) 
		{//select *
			String columnName = null;
			String aliasName = null;
			DataStruct ds = this.tableNode.dataStruct();
			if(ds != null)
			{
				String[] fieldNames = ds.getFieldNames();
				for(int i=0, len = fieldNames.length; i<len; i++)
				{
					String name = fieldNames[i];
					if (SimpleSelect.fnames.indexOf(name)>=0) continue;
					ArrayList<Node> expList = new ArrayList<Node>();
					expList.add(new FieldNode(name));
					ExpressionNode expNode = new ExpressionNode(expList);
					columnName = name;
					aliasName = columnName;
					this.finalList.add(aliasName);
					this.aliasList.add(aliasName.toLowerCase());
					this.columnList.add(expNode);
				}
			}
		} 
		else if (start + 3 == next 
				&& this.tableNode.isIdentic(tokens[start].getString())
				&& tokens[start + 1].getType() == Tokenizer.DOT 
				&& tokens[start + 2].equals("*"))
		{//select T.*
			String columnName = null;
			String aliasName = null;
			DataStruct ds = this.tableNode.dataStruct();
			if(ds != null)
			{
				String[] fieldNames = ds.getFieldNames();
				for(int i=0, len = fieldNames.length; i<len; i++)
				{
					String name = fieldNames[i];
					ArrayList<Node> expList = new ArrayList<Node>();
					expList.add(new FieldNode(name));
					ExpressionNode expNode = new ExpressionNode(expList);
					columnName = name;
					aliasName = columnName;
					this.finalList.add(aliasName);
					this.aliasList.add(aliasName.toLowerCase());
					this.columnList.add(expNode);
				}
			}
		}
		else if(start + 3 < next
				&& tokens[start].getType() == Tokenizer.LPAREN
				&& tokens[start + 1].isKeyWord("SELECT"))
		{//select (select ...from...)
			int end = Tokenizer.scanParen(tokens, start, next);
			Token[] subQueryTokens = Arrays.copyOfRange(tokens, start + 1, end);
			boolean needDelayed = false;
			for(int n = 0, len = subQueryTokens.length; n < len; n++)
			{
				if(n < len - 2 
				&& subQueryTokens[n].getString().equalsIgnoreCase(this.tableNode.getAlias())
				&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
				&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
				{
					String theFieldName = subQueryTokens[n + 2].getString();
					if(theFieldName.startsWith("\"") && theFieldName.endsWith("\"")
					&& theFieldName.substring(1, theFieldName.length() - 1).indexOf("\"") == -1)
					{
						theFieldName = theFieldName.substring(1, theFieldName.length() - 1);
					}
					new FieldNode(theFieldName).optimize();
					n += 2;
					
					needDelayed = true;
				}
			}
			
			String aliasName = null;
			if(end == next - 1)
			{
				aliasName = "_" + this.columnIndex;
			}
			else
			{
				int begin = end + 1;
				if(tokens[end + 1].isKeyWord("AS"))
				{
					if(end + 2 == next)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, As关键字后面缺少列别名");
					}
					begin = end + 2;
				}
				aliasName = "";
				for(int p = begin; p < next; p++)
				{
					aliasName += tokens[p].getOriginString();
				}
			}
			
			this.finalList.add(aliasName);
			this.aliasList.add(aliasName.toLowerCase());
			
			if(needDelayed)
			{
				List<String> checkList = new ArrayList<String>();
				for(Token subQueryToken : subQueryTokens)
				{
					checkList.add(subQueryToken.getString().toUpperCase());
				}
				
				if(!this.subQueryOfSelectMap.containsKey(checkList.toString()))
				{
					String uuid = UUID.randomUUID().toString().replace("-", "_");
					
					ArrayList<Node> expList = new ArrayList<Node>();
					expList.add(new NormalNode("$" + uuid));
					ExpressionNode expNode = new ExpressionNode(expList);
					this.columnList.add(expNode);
					
					Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
					subQueryMap.put("$" + uuid, subQueryTokens);
					this.subQueryOfSelectEntryList.add(subQueryMap.entrySet().iterator().next());
					
					this.subQueryOfSelectMap.put(checkList.toString(), "$" + uuid);
				}
				else
				{
					String dollar_uuid = this.subQueryOfSelectMap.get(checkList.toString());
					
					ArrayList<Node> expList = new ArrayList<Node>();
					expList.add(new NormalNode(dollar_uuid));
					ExpressionNode expNode = new ExpressionNode(expList);
					this.columnList.add(expNode);
				}
			}
			else
			{
				SimpleSQL lq = new SimpleSQL(this.ics, subQueryTokens, 0, subQueryTokens.length, this.parameterList, this.ctx, true);
				lq.setMemory(true);
				ICursor cursor = lq.query();
				DataStruct ds = lq.getDataStruct();
				if(ds.getFieldCount() == 1)
				{
					Sequence seq = null;
					if(cursor != null)
					{
						seq = cursor.fetch(2);
					}
					if(seq == null || seq.length() != 1 || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 在SELECT子句中返回常数值的子查询只能为单行单列");
					}
					Object val = seq.get(1);
					if(val instanceof BaseRecord)
					{
						val = ((BaseRecord)val).getFieldValue(0);
					}
					Token[] valTokens = Tokenizer.parse(getSQLValue(val));
					ExpressionNode expNode = scanExp(valTokens, 0, valTokens.length);
					this.columnList.add(expNode);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 返回常数值的子查询只能为单列");
				}
			}
			
			this.columnIndex++;
		}
		else
		{
			String aliasName = null;
			int expNext = next;
			if(next - 1 > start) 
			{
				Token alias = tokens[next - 1];
				Token prevToken = tokens[next - 2];
				
				if (prevToken.isKeyWord("AS")) 
				{
					if (alias.getType() == Tokenizer.IDENT)
					{
						expNext = next - 2;
						aliasName = alias.getString();
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 新起列别名不应为纯数字或特殊字符");
					}
				} 
				else if (!prevToken.canHaveRightExp()) 
				{
					if (alias.getType() == Tokenizer.IDENT)
					{
						expNext = next - 1;
						aliasName = alias.getString();
					}
					else if (alias.getType() != Tokenizer.KEYWORD && alias.getType() != Tokenizer.RPAREN && alias.getType() != Tokenizer.LEVELMARK && alias.getType() != Tokenizer.TABLEMARK)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 某个待查询列的表达式语法错误");
					}
				}
			}
			
			ExpressionNode expNode = scanExp(tokens, start, expNext);
			if(expNode.getNodeList() == null || expNode.getNodeList().size() == 0)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanColumn, 某个待查询列的表达式为空值");
			}
			
			if(aliasName != null)
			{
				if(aliasName.startsWith("\"") && aliasName.endsWith("\"")
				&& aliasName.substring(1, aliasName.length() - 1).indexOf("\"") == -1)
				{
					aliasName = aliasName.substring(1, aliasName.length() - 1);
				}
			}
			else
			{
				String columnName = null;
				if(expNode.getNodeList().size() == 1 && expNode.getNodeList().get(0) instanceof FieldNode)
				{
					columnName = ((FieldNode)expNode.getNodeList().get(0)).getOriginal();
				}
				else
				{
					columnName = "_" + this.columnIndex;
				}
				aliasName = columnName;	
			}
			this.columnIndex++;
			
			this.finalList.add(aliasName);
			this.aliasList.add(aliasName.toLowerCase());
			this.columnList.add(expNode);
		}
	}
	
	public static String scanExp(Token []tokens, List<Object> paramList)
	{
		SimpleSelect sdql = new SimpleSelect(null, null);//仅用于解析而不获取游标，不需要上下文对象
		sdql.setSQLParameters(paramList);
		SimpleSelect.ExpressionNode expNode = sdql.scanExp(tokens, 0, tokens.length);
		expNode.setFromWhere();
		return expNode.toExpression();
	}
	
	private ExpressionNode scanExp(Token []tokens, int start, int next)
	{
		ArrayList<Node> expList = new ArrayList<Node>();
		boolean hasNot = false;
		for (int i = start; i < next; i++) 
		{
			//if (expList.size()>0) System.out.println(expList.get(expList.size()-1).toExpression());
			//System.out.println(new ExpressionNode(expList).toExpression());
			Token token = tokens[i];
			char type = token.getType();
			if (type == Tokenizer.IDENT) // F, D, A, T.F, T.fun()
			{
				int pos = i + 1;
				if (pos == next) // F, A 
				{
					int begin = i;
					String name = tokens[begin].getString();
					
					if(name.startsWith("\"") && name.endsWith("\"")
					&& name.substring(1, name.length() - 1).indexOf("\"") == -1)
					{
						name = name.substring(1, name.length() - 1);
					}
					
					if(aliasList != null && aliasList.size() != 0)
					{
						if(aliasList.contains(name.toLowerCase()))
						{
							//System.out.println("4----"+name);
							expList.add(new NormalNode("'"+name.toLowerCase()+"'"));
							continue;
						}
					}
					
					expList.add(new FieldNode(name));
				} 
				else if (tokens[pos].getType() == Tokenizer.DOT) //T.F
				{
					pos++;
					if (pos == next) 
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 起始位置超出结束位置");
					}
					else if (pos + 1 < next && tokens[pos + 1].getType() == Tokenizer.LPAREN) // T.sum(exp)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 函数表达式不支持这种写法");
					}
					else // T.F
					{
						int begin = i;
						
						if (tokens[begin + 2].getType() != Tokenizer.IDENT) 
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 某待查询列表名后接非字段类型");
						}
						
						int fieldNext = begin + 3;
						
						if(begin + 3 < next)
						{
							if (tokens[begin + 3].getType() == Tokenizer.LEVELMARK) 
							{
								fieldNext = begin + 4;
							}
						}

						String tableName = tokens[begin].getString();
						if (this.tableNode != null && !this.tableNode.isIdentic(tableName))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(tokens[i].getString() + mm.getMessage("syntax.unknownTable") + ":scanExp");
						}
						
						String fieldName = tokens[begin + 2].getString();
						
						if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
						&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
						{
							fieldName = fieldName.substring(1, fieldName.length() - 1);
						}
						
						expList.add(new FieldNode(fieldName));
						
						i = fieldNext - 1;
					}
				} 
				else if(tokens[pos].getType() == Tokenizer.LPAREN) //sum(exp), year(exp)
				{
					int end = Tokenizer.scanParen(tokens, pos, next);
					String functionName = token.getString();
					
					if(Tokenizer.isGatherFunction(functionName))
					{
						boolean isCountIf = false;
						boolean isFirst = false;
						boolean isLast = false;
						if(functionName.equalsIgnoreCase("COUNTIF"))
						{
							isCountIf = true;
							functionName = "count";
						}
						else if(functionName.equalsIgnoreCase("FIRST"))
						{
							isFirst = true;
							functionName = "top@1";
						}
						else if(functionName.equalsIgnoreCase("LAST"))
						{
							isLast = true;
							functionName = "top@1";
						}
						
						ExpressionNode paramNode = null;
						int begin = i + 2;
						ArrayList<Node> tempList = new ArrayList<Node>();
						if(isFirst || isLast)
						{
							if(isFirst)
							{
								tempList.add(new NormalNode("1"));
							}
							else if(isLast)
							{
								tempList.add(new NormalNode("-1"));
							}
							tempList.add(new NormalNode(","));
							
							ExpressionNode expNode = scanExp(tokens, begin, end);
							List<Node> nodeList = expNode.getNodeList();
							
							List<Node> tailList = new ArrayList<Node>();
							for(int n = nodeList.size() - 1; n >= 0; n--)
							{
								Node node = nodeList.get(n);
								tailList.add(0, node);
								nodeList.remove(n);
								if(node instanceof NormalNode && ((NormalNode)node).getValue().equals(","))
								{
									break;
								}
							}
							
							Node headNode = nodeList.get(0);
							Node tailNode = nodeList.get(nodeList.size() - 1);
							if(headNode instanceof NormalNode && ((NormalNode)headNode).getValue().equals("[") 
							&& tailNode instanceof NormalNode && ((NormalNode)tailNode).getValue().equals("]"))
							{
								tempList.add(headNode);
								nodeList.remove(headNode);
								tailList.add(0, tailNode);
								nodeList.remove(tailNode);
							}
							
							ArrayList<Node> subNodeList = new ArrayList<Node>();
							boolean desc = false;
							for(int n = 0; n < nodeList.size(); n++)
							{
								Node node = nodeList.get(n);
								if(node instanceof NormalNode && ((NormalNode)node).getValue().equals("desc"))
								{
									desc = true;
									node = new NormalNode("");
								}
								
								if(node instanceof NormalNode && ((NormalNode)node).getValue().equals(","))
								{
									if(desc)
									{
										tempList.add(new NormalNode("-"));
										tempList.add(new ParenNode(new ExpressionNode(subNodeList)));
									}
									else
									{
										tempList.addAll(subNodeList);
									}
									
									tempList.add(node);
									
									subNodeList = new ArrayList<Node>();
									desc = false;
								}
								else
								{
									subNodeList.add(node);
								}
								
								if(n == nodeList.size() - 1)
								{
									if(desc)
									{
										tempList.add(new NormalNode("-"));
										tempList.add(new ParenNode(new ExpressionNode(subNodeList)));
									}
									else
									{
										tempList.addAll(subNodeList);
									}
									
									subNodeList = null;
									desc = false;
								}
							}
							tempList.addAll(tailList);
						}
						else
						{
							while (begin < end) 
							{
								int comma = Tokenizer.scanComma(tokens, begin, end);
								if(comma == -1) 
								{
									if(tempList.size() == 0)
									{
										if(tokens[begin].getString().equals("*") && begin + 1 == end)
										{
											tokens[begin].setString("1");
											tokens[begin].setType(Tokenizer.NUMBER);
										}
										paramNode = scanExp(tokens, begin, end);
									}
									else if(isCountIf && tempList.size() > 0)
									{
										tempList.add(new NormalNode("||"));
										tempList.add(new NormalNode("("));
										tempList.add(scanExp(tokens, begin, end));
										tempList.add(new NormalNode(")"));
									}
									
									if(this.distinctGatherField != null)
									{
										if(isCountIf || !functionName.equalsIgnoreCase("COUNT"))
										{
											MessageManager mm = ParseMessage.get();
											throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 非COUNT聚合函数的参数不支持DISTINCT");
										}
										
										String fieldName = paramNode.toExpression();
										if(fieldName == null)
										{
											MessageManager mm = ParseMessage.get();
											throw new RQException(mm.getMessage("syntax.error") + ":scanExp, COUNT(DISTINCT)的参数格式错误");
										}
										
										functionName = "icount";
										
										this.distinctGatherField = null;
									}
									break;
								} 
								else 
								{
									if(isCountIf)
									{
										if(tempList.size() > 0)
										{
											tempList.add(new NormalNode("||"));
										}
										tempList.add(new NormalNode("("));
										tempList.add(scanExp(tokens, begin, comma));
										tempList.add(new NormalNode(")"));
										begin = comma + 1;
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 某聚合函数的参数个数错误");
									}
								}
							}
						}
						if((isCountIf || isFirst || isLast) && paramNode == null && tempList.size() != 0)
						{
							paramNode = new ExpressionNode(tempList);
						}
						
						int size = paramNode.getNodeList().size();
						if (size == 0) 
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("function.paramError") + ":scanExp, 聚合函数的参数节点数不能为零");
						}
						
						expList.add(new GatherNode(functionName, paramNode));
					}
					else
					{
						ExpressionNode paramNode = null;
						int begin = i + 2;
						
						paramNode = scanExp(tokens, begin, end);
					
						expList.add(new FunctionNode(functionName, paramNode));
					}

					i = end;
				}
				else if (tokens[pos].getType() == Tokenizer.LEVELMARK) //F#L
				{
					String fieldName = tokens[i].getString();
					String levelName = tokens[pos].getLevelName();
					
					DimNode dimNode = new DimNode(levelName);
					dimNode.setField(fieldName);
					expList.add(dimNode);
					
					i = pos;
				}
				else if (tokens[pos].getType() == Tokenizer.OPERATOR) //F+G
				{
					int begin = i;
					String fieldName = tokens[begin].getString();
					
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					
					if(aliasList != null && aliasList.size() != 0)
					{
						if(aliasList.contains(fieldName.toLowerCase()))
						{
							//System.out.println("1----"+fieldName);
							expList.add(new NormalNode("'"+fieldName.toLowerCase()+"'"));
							continue;
						}
					}
					
					expList.add(new FieldNode(fieldName));
				}
				else if (tokens[pos].getType() == Tokenizer.COMMA) //F,
				{
					int begin = i;
					String fieldName = tokens[begin].getString();
					
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					
					if(aliasList != null && aliasList.size() != 0)
					{
						if(aliasList.contains(fieldName.toLowerCase()))
						{
							//System.out.println("2----"+fieldName);
							expList.add(new NormalNode("'"+fieldName.toLowerCase()+"'"));
							continue;
						}
					}
					//System.out.println("7----"+fieldName+",,,"+new FieldNode(fieldName).toExpression());
					
					expList.add(new FieldNode(fieldName));
				}
				else if (tokens[pos].getType() == Tokenizer.KEYWORD)//F asc/desc, F order by
				{
					int begin = i;
					String fieldName = tokens[begin].getString();
					
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					
					if(aliasList != null && aliasList.size() != 0)
					{
						if(aliasList.contains(fieldName.toLowerCase()))
						{
							//System.out.println("3----"+fieldName);
							expList.add(new NormalNode("'"+fieldName.toLowerCase()+"'"));
							continue;
						}
					}
					
					expList.add(new FieldNode(fieldName));
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 字段名后接了非法的字符");
				}
			} 
			else if (type == Tokenizer.LPAREN) 
			{
				int end = Tokenizer.scanParen(tokens, i, next);
				Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 1, end);
				boolean isSubQuery = false;
				if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
				{
					isSubQuery = true;
				}
				
				if(isSubQuery)
				{
					boolean needDelayed = false;
					for(int n = 0, len = subQueryTokens.length; n < len; n++)
					{
						if(n < len - 2 
						&& subQueryTokens[n].getString().equalsIgnoreCase(this.tableNode.getAlias())
						&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
						&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
						{
							String theFieldName = subQueryTokens[n + 2].getString();
							if(theFieldName.startsWith("\"") && theFieldName.endsWith("\"")
							&& theFieldName.substring(1, theFieldName.length() - 1).indexOf("\"") == -1)
							{
								theFieldName = theFieldName.substring(1, theFieldName.length() - 1);
							}
							new FieldNode(theFieldName).optimize();
							n += 2;
							
							needDelayed = true;
						}
					}
					
					if(needDelayed)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						if(i > 0 && tokens[i - 1].isKeyWord("IN"))
						{
							if(!this.subQueryOfInMap.containsKey(checkList.toString()))
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								
								ArrayList<Node> list = new ArrayList<Node>();
								list.add(new NormalNode("$" + uuid)); //非join传入的子查询字段因为是后添加的不算作选出字段
								ExpressionNode expNode = new ExpressionNode(list);
								expList.add(expNode);
								
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								this.subQueryOfInEntryList.add(subQueryMap.entrySet().iterator().next());
								
								this.subQueryOfInMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = this.subQueryOfInMap.get(checkList.toString());
								
								ArrayList<Node> list = new ArrayList<Node>();
								list.add(new NormalNode(dollar_uuid));//非join传入的子查询字段因为是后添加的不算作选出字段
								ExpressionNode expNode = new ExpressionNode(list);
								expList.add(expNode);
							}
						}
						else
						{
							if(!this.subQueryOfWhereMap.containsKey(checkList.toString()))
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								
								ArrayList<Node> list = new ArrayList<Node>();
								list.add(new NormalNode("$" + uuid));//非join传入的子查询字段因为是后添加的不算作选出字段
								ExpressionNode expNode = new ExpressionNode(list);
								expList.add(expNode);
								
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								this.subQueryOfWhereEntryList.add(subQueryMap.entrySet().iterator().next());
								
								this.subQueryOfWhereMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = this.subQueryOfWhereMap.get(checkList.toString());
								
								ArrayList<Node> list = new ArrayList<Node>();
								list.add(new NormalNode(dollar_uuid));//非join传入的子查询字段因为是后添加的不算作选出字段
								ExpressionNode expNode = new ExpressionNode(list);
								expList.add(expNode);
							}
						}
					}
					else
					{
						SimpleSQL lq = new SimpleSQL(this.ics, subQueryTokens, 0, subQueryTokens.length, this.parameterList, this.ctx, true);
						lq.setMemory(true);
						ICursor cursor = lq.query();
						DataStruct ds = lq.getDataStruct();
						if(ds.getFieldCount() == 1)
						{
							expList.add(new TableNode("", "", cursor, ds));
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 返回常数值的子查询只能为单列");
						}
					}
				}
				else
				{
					expList.add(new ParenNode(scanExp(subQueryTokens, 0, subQueryTokens.length)));//实际上不是子查询
				}
				
				i = end;
			}
			else if (token.isKeyWord("LEFT") && i+1 != next && tokens[i+1].getType() == Tokenizer.LPAREN) 
			{
				token.setType(Tokenizer.IDENT);
				i = i - 1;
			}
			else if (token.isKeyWord("RIGHT") && i+1 != next && tokens[i+1].getType() == Tokenizer.LPAREN) 
			{
				token.setType(Tokenizer.IDENT);
				i = i - 1;
			}
			else if (token.isKeyWord("FIRST") && i+1 != next && tokens[i+1].getType() == Tokenizer.LPAREN) 
			{
				token.setType(Tokenizer.IDENT);
				i = i - 1;
			}
			else if (token.isKeyWord("ORDER") && i+1 != next && tokens[i+1].isKeyWord("BY"))
			{
				expList.add(0, new NormalNode(","));
				int comma = Tokenizer.scanComma(tokens, i+2, next);
				if(comma != -1)
				{
					expList.add(0, new NormalNode("]"));
				}
				expList.addAll(0, scanExp(tokens, i+2, next).getNodeList());
				if(comma != -1)
				{
					expList.add(0, new NormalNode("["));
				}
				i = next;
			}
			else if (token.isKeyWord("IN")) 
			{
				int pos = i + 1;
				if (pos == next || tokens[pos].getType() != Tokenizer.LPAREN && tokens[pos].getType() != Tokenizer.IDENT) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, IN子句从句的格式错误");
				}
				
				ArrayList<Node> tempList = new ArrayList<Node>();
				for(int x = expList.size() - 1; x >= 0; x--)
				{
					if(expList.get(x) instanceof NormalNode)
					{
						NormalNode normal = (NormalNode)expList.get(x);
						if(normal.getValue().equalsIgnoreCase("AND") || normal.getValue().equalsIgnoreCase("&&") 
								|| normal.getValue().equalsIgnoreCase("OR") || normal.getValue().equalsIgnoreCase("||"))
						{
							break;
						}
					}
					tempList.add(0, expList.get(x));
					expList.remove(x);
				}
				ExpressionNode expNode = new ExpressionNode(tempList);

				if(tokens[pos].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, pos, next);
					ExpressionNode paramNode = scanExp(tokens, pos, end + 1);//必须包含左右括号以进入子查询解析
					expList.add(new InNode(expNode, paramNode, hasNot));
					i = end;
				}
				else if(tokens[pos].getType() == Tokenizer.IDENT)
				{
					ArrayList<Node> paramList = new ArrayList<Node>();
					String theFieldName = tokens[pos].getOriginString();
					if(theFieldName.startsWith("\"") && theFieldName.endsWith("\"")
					&& theFieldName.substring(1, theFieldName.length() - 1).indexOf("\"") == -1)
					{
						theFieldName = theFieldName.substring(1, theFieldName.length() - 1);
					}
					paramList.add(new FieldNode(theFieldName));//join传入的子查询字段因为已经存在所以算作选出字段
					ExpressionNode paramNode = new ExpressionNode(paramList);
					expList.add(new InNode(expNode, paramNode, hasNot));
					i = pos;
				}
				
				if(hasNot)
				{
					hasNot = false;
				}
			}
			else if (token.isKeyWord("LIKE")) 
			{
				int pos = i + 1;
				if (pos == next) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Like子句的格式错误");
				}
				else 
				{
//					if(tokens[pos].getType() != Tokenizer.STRING)
//					{
//						if(!tokens[pos].getString().startsWith("\"") || !tokens[pos].getString().endsWith("\"") 
//							|| tokens[pos].getString().substring(1, tokens[pos].getString().length()-1).indexOf("\"") != -1)
//						{
//							MessageManager mm = ParseMessage.get();
//							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Like子句的套式类型错误");
//						}
//					}
				}
				
//				String pattern = tokens[pos].getString().substring(1, tokens[pos].getString().length() - 1);//脱""或''以便于操作,最后再加上
				
				ArrayList<Node> tempList = new ArrayList<Node>();
				for(int x = expList.size() - 1; x >= 0; x--)
				{
					if(expList.get(x) instanceof NormalNode)
					{
						NormalNode normal = (NormalNode)expList.get(x);
						if(normal.getValue().equalsIgnoreCase("AND") || normal.getValue().equalsIgnoreCase("&&") 
								|| normal.getValue().equalsIgnoreCase("OR") || normal.getValue().equalsIgnoreCase("||"))
						{
							break;
						}
					}
					tempList.add(0, expList.get(x));
					expList.remove(x);
				}
				ExpressionNode expNode = new ExpressionNode(tempList);
				
				expList.add(new LikeNode(expNode, tokens[pos], hasNot,this));
				if(hasNot)
				{
					hasNot = false;
				}
				
				i = pos;
			}
			else if (token.isKeyWord("CASE")) 
			{
				int endPos = Tokenizer.scanCaseEnd(tokens, i, next);
				
				int elsePos = Tokenizer.scanCaseElse(tokens, i, endPos);
				
				List<Node> nodeList = new ArrayList<Node>();

				int pos = i + 1;
				boolean needOptimizeFilter = false;
				if(!tokens[pos].isKeyWord("WHEN"))
				{
					int whenPos = Tokenizer.scanCaseWhen(tokens, pos, elsePos == -1 ? endPos : elsePos);
					if(whenPos == -1)
					{
						throw new RQException("CASE语句缺少WHEN关键字");
					}
					ExpressionNode caseNode = scanExp(tokens, pos, whenPos);
					nodeList.add(caseNode);
					pos = whenPos;
				}
				else
				{
					nodeList.add(new NormalNode("true"));
					needOptimizeFilter = true;
				}
				
				while(pos < (elsePos == -1 ? endPos : elsePos))
				{
					int whenPos = Tokenizer.scanCaseWhen(tokens, pos, elsePos == -1 ? endPos : elsePos);
					if(whenPos == -1)
					{
						whenPos = elsePos == -1 ? endPos : elsePos;
					}
					
					int thenPos = Tokenizer.scanCaseThen(tokens, pos, whenPos);
					
					if(needOptimizeFilter)
					{
						Token[] whenTokens = Arrays.copyOfRange(tokens, pos + 1, thenPos);
						PerfectWhere pw = new PerfectWhere(whenTokens, this.parameterList);
						whenTokens = pw.getTokens(true);
						
						ExpressionNode whenNode = scanExp(whenTokens, 0, whenTokens.length);
						nodeList.add(whenNode);
					}
					else
					{
						ExpressionNode whenNode = scanExp(tokens, pos + 1, thenPos);
						nodeList.add(whenNode);
					}
					
					ExpressionNode thenNode = scanExp(tokens, thenPos+1, whenPos);
					nodeList.add(thenNode);
					
					pos = whenPos;
				}
				
				if(elsePos != -1)
				{
					ExpressionNode elseNode = scanExp(tokens, elsePos + 1, endPos);
					nodeList.add(elseNode);
				}
				
				expList.add(new CaseNode(nodeList));
				
				i = endPos;
			}
			else if (token.isKeyWord("BETWEEN")) 
			{
				ArrayList<Node> tempList = new ArrayList<Node>();
				for(int x = expList.size() - 1; x >= 0; x--)
				{
					if(expList.get(x) instanceof NormalNode)
					{
						NormalNode normal = (NormalNode)expList.get(x);
						if(normal.getValue().equalsIgnoreCase("AND") || normal.getValue().equalsIgnoreCase("&&") 
								|| normal.getValue().equalsIgnoreCase("OR") || normal.getValue().equalsIgnoreCase("||"))
						{
							break;
						}
					}
					tempList.add(0, expList.get(x));
					expList.remove(x);
				}
				ExpressionNode expNode = new ExpressionNode(tempList);
				
				int andPos = Tokenizer.scanKeyWord("AND", tokens, i + 1, next);
				if(andPos == -1)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Between后面缺少关键字and");
				}
				ExpressionNode floorValue = scanExp(tokens, i + 1, andPos);
				
				int endAnd = Tokenizer.scanKeyWord("AND", tokens, andPos + 1, next);
				int endOr = Tokenizer.scanKeyWord("OR", tokens, andPos + 1, next);
				int end = -1;
				if(endAnd == -1 || endOr == -1)
				{
					end = pos(endAnd, endOr, next);
				}
				else
				{
					end = endAnd < endOr ? endAnd : endOr;
				}
				ExpressionNode ceilValue = scanExp(tokens, andPos + 1, end);
				
				expList.add(new BetweenNode(expNode, floorValue, ceilValue, hasNot));
				if(hasNot)
				{
					hasNot = false;
				}
				
				i = end - 1;
			}
			else if (token.isKeyWord("EXISTS")) 
			{
				int pos = i + 1;
				if (pos + 1 >= next || tokens[pos].getType() != Tokenizer.LPAREN || !tokens[pos + 1].isKeyWord("SELECT")) 
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Exists子句从句的格式错误");
				}
				
				int end = Tokenizer.scanParen(tokens, pos, next);
				Token[] subQueryTokens = Arrays.copyOfRange(tokens, pos + 1, end);
				boolean needDelayed = false;
				for(int n = 0, len = subQueryTokens.length; n < len; n++)
				{
					if(n < len - 2 
					&& subQueryTokens[n].getString().equalsIgnoreCase(this.tableNode.getAlias())
					&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
					&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
					{
						String theFieldName = subQueryTokens[n + 2].getString();
						if(theFieldName.startsWith("\"") && theFieldName.endsWith("\"")
						&& theFieldName.substring(1, theFieldName.length() - 1).indexOf("\"") == -1)
						{
							theFieldName = theFieldName.substring(1, theFieldName.length() - 1);
						}
						new FieldNode(theFieldName).optimize();
						n += 2;
						
						needDelayed = true;
					}
				}
				
				if(needDelayed)
				{
					List<String> checkList = new ArrayList<String>();
					for(Token subQueryToken : subQueryTokens)
					{
						checkList.add(subQueryToken.getString().toUpperCase());
					}
					if(!this.subQueryOfExistsMap.containsKey(checkList.toString()))
					{
						String uuid = UUID.randomUUID().toString().replace("-", "_");
						
						Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
						subQueryMap.put("$" + uuid, subQueryTokens);
						this.subQueryOfExistsEntryList.add(subQueryMap.entrySet().iterator().next());
						
						expList.add(new NormalNode("$" + uuid));
						if(hasNot) //not exists : is null
						{
							expList.add(new NormalNode("=="));
						}
						else
						{
							expList.add(new NormalNode("!="));
						}
						expList.add(new NormalNode("null"));
						
						this.subQueryOfExistsMap.put(checkList.toString(), "$" + uuid);
					}
					else
					{
						String dollar_uuid = this.subQueryOfExistsMap.get(checkList.toString());

						expList.add(new NormalNode(dollar_uuid));
						if(hasNot) //not exists : is null
						{
							expList.add(new NormalNode("=="));
						}
						else
						{
							expList.add(new NormalNode("!="));
						}
						expList.add(new NormalNode("null"));
					}
				}
				else
				{
					SimpleSQL lq = new SimpleSQL(this.ics, subQueryTokens, 0, subQueryTokens.length, this.parameterList, this.ctx, true);
					lq.setMemory(true);
					ICursor cursor = lq.query();
					Sequence seq = null;
					if(cursor != null)
					{
						seq = cursor.peek(1);
					}
					if(seq == null)
					{
						if(hasNot)
						{
							expList.add(new NormalNode("1"));
							expList.add(new NormalNode("=="));
							expList.add(new NormalNode("1"));
						}
						else
						{
							expList.add(new NormalNode("1"));
							expList.add(new NormalNode("=="));
							expList.add(new NormalNode("0"));
						}
					}
					else
					{
						if(hasNot)
						{
							expList.add(new NormalNode("1"));
							expList.add(new NormalNode("=="));
							expList.add(new NormalNode("0"));
						}
						else
						{
							expList.add(new NormalNode("1"));
							expList.add(new NormalNode("=="));
							expList.add(new NormalNode("1"));
						}
					}
				}
				
				if(hasNot)
				{
					hasNot = false;
				}
				
				i = end;
			}
			else if (type == Tokenizer.PARAMMARK)
			{
				ParamNode paramNode = new ParamNode();
				String strIndex = tokens[i].getString().substring(1);
					
				if(strIndex.length() != 0)
				{
					int paramIndex = Integer.parseInt(strIndex);
					paramNode.setIndex(paramIndex);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 参数占位符解析错误");
				}
				expList.add(paramNode);
			}
			else
			{
				String value = token.getString();
				if (token.getType() == Tokenizer.OPERATOR)
				{
					if(value.equalsIgnoreCase("="))
					{
						if (i == start)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 表达式不能以'='号开头");
						}
						else if(tokens[i-1].getType() != Tokenizer.OPERATOR) 
						{
							if(hasNot)
							{
								value = "!=";
							}
							else
							{
								value = "==";
							}
						}
					}
					else if(value.equalsIgnoreCase(">"))
					{
						if (i + 1 == next)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 表达式不能以'>'号结尾");
						}
						else if(tokens[i + 1].getType() == Tokenizer.OPERATOR) 
						{
							if(tokens[i + 1].getString().equalsIgnoreCase("=") && (i + 1 < next))
							{
								if(hasNot)
								{
									value = "<";
									i = i + 1;
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 某表达式中使用了不支持的操作符");
							}
						}
						else
						{
							if(hasNot)
							{
								value = "<=";
							}
						}
					}
					else if(value.equalsIgnoreCase("<"))
					{
						if (i + 1 == next)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 表达式不能以'<'号结尾");
						}
						else if(tokens[i + 1].getType() == Tokenizer.OPERATOR) 
						{
							if(tokens[i + 1].getString().equalsIgnoreCase("=") && (i + 1 < next))
							{
								if(hasNot)
								{
									value = ">";
									i = i + 1;
								}
							}
							else if(tokens[i + 1].getString().equalsIgnoreCase(">") && (i + 1 < next))
							{
								if(hasNot)
								{
									value = "=";
									i = i + 1;
								}
								else
								{
									value = "!=";
									i = i + 1;
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 某表达式中使用了不支持的操作符");
							}
						}
						else
						{
							if(hasNot)
							{
								value = ">=";
							}
						}
					}
					else if(value.equalsIgnoreCase("!"))
					{
						if (i + 1 == next)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 表达式不能以'!'号结尾");
						}
						else if(tokens[i + 1].getType() == Tokenizer.OPERATOR) 
						{
							if(tokens[i + 1].getString().equalsIgnoreCase("=") && (i + 1 < next))
							{
								if(hasNot)
								{
									value = "==";
									i = i + 1;
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 某表达式中使用了不支持的操作符");
							}
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 表达式中'!'不能单独使用");
						}
					}
					
					if(hasNot)
					{
						hasNot = false;
					}
				}
				else if(token.getType() == Tokenizer.KEYWORD)
				{	
					if(value.equalsIgnoreCase("AND"))
					{
						value = "&&";
					}
					else if(value.equalsIgnoreCase("OR"))
					{
						value = "||";
					}
					else if(value.equalsIgnoreCase("NOT"))
					{
						hasNot = true;
						value = "";
					}
					else if(value.equalsIgnoreCase("IS"))
					{
						if (i + 1 == next)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is关键字用法错误");
						}
						else if(tokens[i + 1].getType() == Tokenizer.KEYWORD) 
						{
							if(tokens[i + 1].getString().equalsIgnoreCase("NOT"))
							{
								if (i + 2 == next)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is Not关键字用法错误");
								}
								
								if(tokens[i + 2].getType() == Tokenizer.KEYWORD)
								{
									if(tokens[i + 2].getString().equalsIgnoreCase("NULL"))
									{
										value = "!=";
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is Not关键字只能后接null");
									}
								}
								else
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is Not关键字只能后接null");
								}
								i = i + 1; //跳过not防止影响后面的条件
							}
							else if(tokens[i + 1].getString().equalsIgnoreCase("NULL"))
							{
								value = "==";
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is关键字只能后接null");
							}
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":scanExp, Is关键字用法错误");
						}
					}
					else if(value.equalsIgnoreCase("NULL"))
					{
						value = "null";
					}
					else if(value.equalsIgnoreCase("DISTINCT"))
					{
						this.distinctGatherField = "";
						value = "";
					}
					else if(value.equalsIgnoreCase("ASC"))
					{
						value = "";
					}
					else if(value.equalsIgnoreCase("DESC"))
					{
						value = "desc";
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanExp, 不支持的关键字用法:" + value);
					}
				}
				else if(token.getType() == Tokenizer.STRING)
				{
					value = value.trim();
					value = value.substring(1, value.length()-1);
					value = "\"" + value + "\"";
				}
				
				expList.add(new NormalNode(value));
			}
		}

		int size = expList.size();
		if (size == 0) 
		{
			expList.add(new NormalNode(""));
		}
		
		return new ExpressionNode(expList);
	}
	
	private void scanBy(Token []tokens, int start, int next) 
	{
		while (start < next) 
		{
			start++;
			if (start == next) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanBy, 起始位置超出结束位置");
			}
			
			int end = Tokenizer.scanComma(tokens, start, next);
			if (end < 0)
			{
				end = next;
			}
			
			int atPos = Tokenizer.scanKeyWord("AT", tokens, start, end);
			if (atPos < 0) 
			{
				ExpressionNode expNode = scanExp(tokens, start, end);
				if(expNode.getNodeList() == null || expNode.getNodeList().size() == 0)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanBy, 分组项不能为空值");
				}
				Node node = null;
				if(expNode.getNodeList().size() == 1)
				{
					if(expNode.getNodeList().get(0) instanceof NormalNode)
					{
						String value = ((NormalNode)expNode.getNodeList().get(0)).getValue();
						if(value != null && !value.trim().isEmpty() && Pattern.compile("^[\\d]+$").matcher(value).matches())
						{
							node = expNode.getNodeList().get(0);
						}
					}
				}
				if(node == null && expNode.hasField(false))
				{
					node = expNode;
				}
				if(node == null)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanBy, 分组项的类型不对");
				}
				this.groupList.add(node);
			} 
			else //not support 'AT'
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanBy, 不支持At语句");
			}
			start = end;
		}
		if(this.groupList == null || this.groupList.size() == 0)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanBy, 没有搜索到任何分组列");
		}
	}
	
	private void scanOrders(Token []tokens, int start, int next)
	{
		start++;
		if (start == next || !tokens[start].isKeyWord("BY")) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanOrders, Order子句格式错误");
		}

		while (start < next) 
		{
			start++;
			if (start == next) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanOrders, 起始位置超出结束位置");
			}
			
			int end = Tokenizer.scanComma(tokens, start, next);
			if (end < 0)
			{
				end = next;
			}
			
			ExpressionNode orderNode = scanExp(tokens, start, end);
			
			boolean desc = false;
			Node tail = orderNode.getNodeList().get(orderNode.getNodeList().size() - 1);
			if(tail instanceof NormalNode && ((NormalNode)tail).getValue().equals("desc"))
			{
				desc = true;
				orderNode.getNodeList().remove(tail);
			}
			
			Node node = null;
			if(orderNode.getNodeList().size() == 2 && orderNode.getNodeList().get(1) instanceof NormalNode
				&& ((NormalNode)orderNode.getNodeList().get(1)).getValue().isEmpty() || orderNode.getNodeList().size() == 1)
			{
				if(orderNode.getNodeList().get(0) instanceof NormalNode)
				{
					String value = ((NormalNode)orderNode.getNodeList().get(0)).getValue();
					if(value != null && !value.trim().isEmpty() && Pattern.compile("^[\\d]+$").matcher(value).matches())
					{
						node = orderNode.getNodeList().get(0);
					}
				}
			}
			if(node == null && orderNode.hasField(true))
			{
				node = orderNode;
			}
			if(node == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanOrders, 排序项的类型不对");
			}
			
			if(desc)
			{
				ArrayList<Node> newNodeList = new ArrayList<Node>();
				newNodeList.add(new NormalNode("-"));
				newNodeList.add(new ParenNode(orderNode));
				node = new ExpressionNode(newNodeList);
				desc = false;
			}
			
			this.sortList.add(node);
			start = end;
		}
		
		if (this.sortList == null || this.sortList.size() == 0) 
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanOrders, 排序字段不能为空值");
		}
	}
	
	public static List<String> fnames = Arrays.asList(new String[]{"_file","_ext","_date","_size"});
	private void execute()
	{
		if(this.tableNode != null)
		{
			if(this.columnList != null && this.columnList.size() != 0)
			{
				for(ExpressionNode expNode : this.columnList)
				{
					expNode.optimize();
				}
			}
			
			if(this.groupList != null && this.groupList.size() != 0)
			{
				List<Node> tempList = new ArrayList<Node>();
				for(int g=0, sz=this.groupList.size(); g<sz; g++)
				{
					Node grpNode = this.groupList.get(g);
					if(grpNode instanceof NormalNode)
					{
						int colNo = -1;
						try
						{
							colNo = Integer.parseInt(((NormalNode)grpNode).getValue());
							if(colNo <= 0 || colNo > this.columnList.size())
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, 分组项取的列号不对");
							}
						}
						catch(NumberFormatException ex)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":execute, 分组项的类型不对", ex);
						}
						grpNode = this.columnList.get(colNo - 1);
					}
					grpNode.optimize();
					tempList.add(grpNode);
				}
				this.groupList = tempList;
			}
			
			if(this.whereNode != null)
			{
				this.whereNode.optimize();
			}
			
			if(this.havingNode != null)
			{
				this.havingNode.optimize();
			}
			
			if(this.sortList != null && this.sortList.size() != 0)
			{
				for(int s=0, sz=this.sortList.size(); s<sz; s++)
				{
					Node srtNode = this.sortList.get(s);
					if(!(srtNode instanceof NormalNode))
					{
						srtNode.optimize();
					}
				}
			}
			
			String[] fds = new String[this.selectFieldList.size()];
			String[] bak = new String[this.selectFieldList.size()];
			Expression[] fxs = new Expression[this.selectFieldList.size()];
			
			this.selectFieldList.toArray(fds);
			
			String whereExp = null;
			if(this.whereNode != null)
			{
				this.whereNode.setFromWhere();
				whereExp = this.whereNode.toExpression();
				this.whereNode = null;
			}
			
			if(whereExp != null && whereExp.trim().equals("1==0"))
			{
				whereExp = "false";
			}
			else if(whereExp != null && whereExp.trim().equals("1==1"))
			{
				whereExp = null;
			}

			
			String qfnames = "";
			for(int a = 0; a < fds.length; a++)
			{
				if (SimpleSelect.fnames.indexOf(fds[a])>=0) {
					qfnames += "," + fds[a];
				}
			}
			if (qfnames.length()>0) {
				//this.tableNode.setFileAttrQuery(true);
				//fds = qfnames.substring(1).split(",");
			}

			
			Map<String, String> stdMap = new LinkedHashMap<String, String>();
			DataStruct gds = this.tableNode.dataStruct();
			
			if(gds != null && gds.getFieldCount() > 0)
			{
				if(fds.length == 0)
				{
					fds = new String[1];
					fds[0] = gds.getFieldName(0);
					bak = new String[1];
					//System.out.println("5----"+fds[0].toLowerCase());
					bak[0] = "'" + fds[0].toLowerCase() + "'";
					fxs = new Expression[1];
					fxs[0] = new Expression(String.format("#%d", 1));
				}
				else
				{
					String[] fns = gds.getFieldNames();
					Map<String, String> n2cMap = new LinkedHashMap<String, String>();
					Next:
					for(int a = 0; a < fds.length; a++)
					{
						//System.out.println("6----"+bak[a]);
						bak[a] = "'" + fds[a] + "'";
						// 20220715 xingjl add
						fxs[a] = new Expression(String.format("#%d", a+1));
						
						for(int b = 0; b < fns.length; b++)
						{
							// 20220715 xingjl remove
							//fxs[a] = new Expression(String.format("#%d", b+1));
							
							if(fds[a].equalsIgnoreCase(fns[b]))
							{
								if(whereExp != null && this.tableNode.getType() == TableNode.TYPE_GTB)
								{
									n2cMap.put(bak[a], fns[b]);
								}
								stdMap.put(fds[a], fns[b]);
								fds[a] = fns[b];
								continue Next;
							}
							else if(this.tableNode.getAlias() != null && !this.tableNode.getAlias().isEmpty())
							{
								if(fds[a].toLowerCase().startsWith(this.tableNode.getAlias().toLowerCase() + ".") && SimpleJoin.getRealFieldName(fds[a]).equalsIgnoreCase(fns[b]))
								{
									stdMap.put(fds[a], fns[b]);
									fds[a] = fns[b];
									continue Next;
								}
								else if(fns[b].toLowerCase().startsWith(this.tableNode.getAlias().toLowerCase() + ".") && SimpleJoin.getRealFieldName(fns[b]).equalsIgnoreCase(fds[a]))
								{
									stdMap.put(fds[a], fns[b]);
									fds[a] = fns[b];
									continue Next;
								}
							}
						}
						MessageManager mm = ParseMessage.get();
						throw new RQException("execute:" + fds[a] + mm.getMessage("field.notExist") + " in " + Arrays.asList(fns));
					}
					whereExp = ExpressionTranslator.translateExp(whereExp, n2cMap);
				}
			}

			//xingjl  remove 20220215
//			Set<String> fdsSet = new HashSet<String>(Arrays.asList(fds));
//			fds = new String[fdsSet.size()];
//			fdsSet.toArray(fds);

			this.tableNode.setAccessColumn(fds);
			
			boolean hasMasterFieldfromExists = false;
			if(whereExp != null && !this.subQueryOfExistsEntryList.isEmpty())
			{
				for(int m = 0; m < this.subQueryOfExistsEntryList.size(); m++)
				{
					String subQueryOfExistsIdent = this.subQueryOfExistsEntryList.get(m).getKey();
					if(whereExp.contains(subQueryOfExistsIdent))
					{
						hasMasterFieldfromExists = true;
						break;
					}
				}
			}
			
			boolean hasMasterFieldfromIn = false;
			if(whereExp != null && !this.subQueryOfInEntryList.isEmpty())
			{
				for(int m = 0; m < this.subQueryOfInEntryList.size(); m++)
				{
					String subQueryOfInIdent = this.subQueryOfInEntryList.get(m).getKey();
					if(whereExp.contains(subQueryOfInIdent))
					{
						hasMasterFieldfromIn = true;
						break;
					}
				}
			}
			
			boolean hasMasterFieldfromWhere = false;
			if(whereExp != null && !this.subQueryOfWhereEntryList.isEmpty())
			{
				for(int m = 0; m < this.subQueryOfWhereEntryList.size(); m++)
				{
					String subQueryOfWhereIdent = this.subQueryOfWhereEntryList.get(m).getKey();
					if(whereExp.contains(subQueryOfWhereIdent))
					{
						hasMasterFieldfromWhere = true;
						break;
					}
				}
			}
			
			Map<String, String> trMap = new LinkedHashMap<String, String>();
			for(int i = 0; i < fds.length; i++)
			{
				trMap.put(fds[i], fxs[i].toString());
				if(fds[i].startsWith("\"") && fds[i].endsWith("\"") && fds[i].substring(1, fds[i].length() - 1).indexOf("\"") == -1)
				{
					trMap.put(SimpleJoin.getRealFieldName(fds[i]), fxs[i].toString());
				}
				trMap.put("'"+fds[i]+"'", fxs[i].toString());
			}
			
			if(whereExp != null && !whereExp.equals("false") && this.tableNode.getType() == TableNode.TYPE_GTB 
			&& !hasMasterFieldfromExists && !hasMasterFieldfromIn && !hasMasterFieldfromWhere && this.topFilter == null)
			{
				whereExp = ExpressionTranslator.translateExp(whereExp, stdMap);
				whereExp = ExpressionTranslator.translateExp(whereExp, trMap);
				this.tableNode.setWhere(whereExp);
				whereExp = null;
			}
			
			if(whereExp != null && whereExp.equals("false"))
			{
				this.icur = null;
			}
			else
			{
				this.icur = this.tableNode.getCursor();
			}
			
			this.ds = new DataStruct(fds);
			
			ICursor cur = fillSubQueryField(this.ics, this.icur, this.subQueryOfExistsEntryList, this.parameterList, this.tableNode.getAlias(), SubQueryCursor.Exist_Type, this.ds);
			if(cur != null && cur instanceof SubQueryCursor && !cur.equals(this.icur))
			{
				this.ds = ((SubQueryCursor)cur).getTableDataStruct();
			}
			else if(cur != null)
			{
				this.ds = cur.getDataStruct();
			}
			this.icur = cur;
			
			cur = fillSubQueryField(this.ics, this.icur, this.subQueryOfInEntryList, this.parameterList, this.tableNode.getAlias(), SubQueryCursor.In_Type, this.ds);
			if(cur != null && cur instanceof SubQueryCursor && !cur.equals(this.icur))
			{
				this.ds = ((SubQueryCursor)cur).getTableDataStruct();
			}
			else if(cur != null)
			{
				this.ds = cur.getDataStruct();
			}
			this.icur = cur;
			
			cur = fillSubQueryField(this.ics, this.icur, this.subQueryOfWhereEntryList, this.parameterList, this.tableNode.getAlias(), SubQueryCursor.Where_Type, this.ds);
			if(cur != null && cur instanceof SubQueryCursor && !cur.equals(this.icur))
			{
				this.ds = ((SubQueryCursor)cur).getTableDataStruct();
			}
			else if(cur != null)
			{
				this.ds = cur.getDataStruct();
			}
			this.icur = cur;
			
			if(this.topFilter != null)
			{
				this.topFilter = ExpressionTranslator.translateExp(this.topFilter, stdMap);
				this.topFilter = ExpressionTranslator.translateExp(this.topFilter, trMap);
				Expression[] topExps = new Expression[]{new Expression(this.topFilter)};
				String[] topNames = new String[]{"_1"};
				if(this.icur != null)
				{	
					Table tab = this.icur.groups(null, null, topExps, topNames, null, ctx);
					if(tab == null || tab.length() != 1 || !(tab.get(1) instanceof BaseRecord)
					|| ((BaseRecord)tab.get(1)).getFieldCount() != 1 || !(((BaseRecord)tab.get(1)).getFieldValue(0) instanceof Sequence))
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":execute, top优化结果异常");
					}
					
					Table res = null;
					
					Sequence seq = (Sequence)((BaseRecord)tab.get(1)).getFieldValue(0);
					for(int i = 1; i <= seq.length(); i++)
					{
						Object obj = seq.get(i);
						if(!(obj instanceof BaseRecord))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":execute, top优化结果异常");
						}
						
						BaseRecord rec = (BaseRecord)obj;
						if(res == null)
						{
							res = new Table(rec.dataStruct());
						}
						res.add(rec);
					}
					
					this.icur = new MemoryCursor(res);
				}
			}
			
			if(whereExp != null && !whereExp.equals("false"))
			{
				whereExp = ExpressionTranslator.translateExp(whereExp, stdMap);
				whereExp = ExpressionTranslator.translateExp(whereExp, trMap);
				Expression flt = new Expression(whereExp);
				if(this.icur != null)
				{
					this.icur.addOperation(new Select(flt, null), ctx);
				}
				whereExp = null;
			}
			
			boolean gatherSign = false;
			boolean fieldNotGroupSign = false;
			ArrayList<String> nameList = new ArrayList<String>();
			ArrayList<String> functionList = new ArrayList<String>();
			ArrayList<String> gatherList = new ArrayList<String>();
			ArrayList<String> expressList = new ArrayList<String>();
			ArrayList<String> paramList = new ArrayList<String>();
			ArrayList<String> classifyList = new ArrayList<String>();
			ArrayList<String> numberList = new ArrayList<String>();
			
			if(this.groupList != null && this.groupList.size() != 0)
			{
				for(int g = 0, len = this.groupList.size(); g<len; g++)
				{
					Node groupNode = this.groupList.get(g);
					classifyList.add(groupNode.toExpression());
				}
			}
			
			if(this.columnList != null && this.columnList.size() != 0)
			{
				for(int c = 0, len = this.columnList.size(); c<len; c++)
				{
					ExpressionNode expNode = this.columnList.get(c);
					if(expNode.hasGather())
					{
						if(expNode.hasFieldNotGroup())
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":execute, 同一表达式中不能同时出现非分组字段和聚合函数");
						}
						gatherSign = true;
						expNode.collect();
					}
					else if(expNode.hasFieldNotGroup())
					{
						fieldNotGroupSign = true;
					}
					expressList.add(expNode.toExpression());
				}
			}
			
			if(gatherSign && fieldNotGroupSign)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":execute, 待查询字段中不能同时出现非分组字段和聚合函数");
			}
			
			if(gatherSign || this.groupList != null && this.groupList.size() != 0)
			{
				if(!this.gatherNodeList.isEmpty())
				{
					for(int i=0, size=this.gatherNodeList.size(); i<size; i++)
					{
						GatherNode gather = gatherNodeList.get(i);
						String function = gather.getName();
						Node param = gather.getParamNode();
						nameList.add(function.toLowerCase());
						paramList.add(param.toExpression());
						gatherList.add(gather.toExpression());
					}
				}
				else
				{
					for(int i=0, size=expressList.size(); i<size; i++)
					{
						nameList.add("");
						paramList.add(expressList.get(i));
						gatherList.add(expressList.get(i));
					}
				}
				
				for(int i = 0, size = classifyList.size() + paramList.size(); i < size; i++)
				{
					if(i < classifyList.size())
					{
						String classifyExp = classifyList.get(i);
						classifyExp = ExpressionTranslator.translateExp(classifyExp, stdMap);
						//2022/05/31 xingjl remove
						//classifyExp = ExpressionTranslator.translateExp(classifyExp, trMap);
						numberList.add(classifyExp);
					}
					else
					{
						String paramExp = paramList.get(i - classifyList.size());
						paramExp = ExpressionTranslator.translateExp(paramExp, stdMap);
						//2022/05/31 xingjl remove
						//paramExp = ExpressionTranslator.translateExp(paramExp, trMap);
						String functionName = nameList.get(i - classifyList.size());
						functionList.add(String.format("%s(%s)", functionName, paramExp));
					}
				}

				Expression[] funs = new Expression[functionList.size()];
				for(int k = 0, size = functionList.size(); k < size; k++)
				{
					funs[k] = new Expression(functionList.get(k));
				}
				String[] gathNames = new String[gatherList.size()];
				for(int k = 0, size = gatherList.size(); k < size; k++)
				{
					gathNames[k] = nameList.get(k) + "_" + (k + 1);
				}
				
				if(this.groupList == null || this.groupList.size() == 0)
				{
					if(funs.length == 1 
					&& funs[0].toString().equals("count(1)") 
					&& gathNames.length == 1
					&& gathNames[0].equals("count(1)"))
					{
						long count = ((this.icur == null) ? 0 : this.icur.skip());
						DataStruct ds = new DataStruct(gathNames);
						BaseRecord rd = new Record(ds);
						rd.set(0, count);
						Table tab = new Table(ds);
						tab.add(rd);
						this.icur = new MemoryCursor(tab);
					}
					else
					{
						if(this.icur != null)
						{
							Table tab = this.icur.groups(null, null, funs, gathNames, null, ctx);
							this.icur = new MemoryCursor(tab);
						}
						else
						{
							DataStruct ds = new DataStruct(gathNames);
							BaseRecord rd = new Record(ds);
							for(int i = 0; i < funs.length; i++)
							{
								if(funs[i].toString().startsWith("count(") && funs[i].toString().endsWith(")"))
								{
									rd.set(i, 0);
								}
								else
								{
									rd.set(i, null);
								}
							}
							Table tab = new Table(ds);
							tab.add(rd);
							this.icur = new MemoryCursor(tab);
						}
					}
				}
				else
				{
					Expression[] nums = new Expression[numberList.size()];
					for(int k = 0, size = numberList.size(); k < size; k++)
					{
						nums[k] = new Expression(numberList.get(k));
					}
					String[] clasNames = new String[classifyList.size()];
					for(int k = 0, size = classifyList.size(); k < size; k++)
					{
						clasNames[k] = "group" + "_" + (k + 1);
					}
					if(this.icur != null)
					{
						Table tab = this.icur.groups(nums, clasNames, funs, gathNames, null, ctx);
						this.icur = new MemoryCursor(tab);
						/*
						if(this.icur instanceof MemoryCursor || this.isMemory)
						{
							Table tab = CursorUtil.groups(this.icur, nums, clasNames, funs, gathNames, null, ctx);
							this.icur = new MemoryCursor(tab);
						}
						else
						{
							int capacity = EnvUtil.getCapacity(classifyList.size() + paramList.size());
							this.icur = CursorUtil.hashGroupx(this.icur, nums, clasNames, funs, gathNames, null, ctx, capacity);
						}
						*/
					}
					else
					{
						this.icur = new MemoryCursor(new Table());
					}
				}
				
				int sizeGroup = classifyList.size();
				if(this.havingNode != null)
				{
					this.havingNode.setFromHaving();
					String havExp = this.havingNode.toExpression();
					Map<String, String> n2cMap = new LinkedHashMap<String, String>();
					for(int c = 0, len = classifyList.size(); c < len; c++)
					{
						String grpExp = classifyList.get(c);
						n2cMap.put(grpExp, String.format("#%d", c+1));
					}
					for(int g = 0, len = gatherList.size(); g < len; g++)
					{
						String funExp = gatherList.get(g);
						n2cMap.put(funExp, String.format("#%d", sizeGroup+g+1));
					}
					havExp = ExpressionTranslator.translateExp(havExp, n2cMap);
					
					Expression flt = new Expression(havExp);
					if(this.icur != null)
					{
						this.icur.addOperation(new Select(flt, null), ctx);
					}
				}
				
				int sizeExpress = expressList.size();
				Expression[] exps = new Expression[sizeExpress];
				String[] names = new String[sizeExpress];
				this.aliasList.toArray(names);
				for(int i = 0; i < sizeExpress; i++)
				{
					String exp = expressList.get(i);
					Map<String, String> n2cMap = new LinkedHashMap<String, String>();
					for(int j=0, length=classifyList.size(); j<length; j++)
					{
						n2cMap.put(classifyList.get(j), String.format("#%d", j+1));
					}
					for(int j=0, length=gatherList.size(); j<length; j++)
					{
						n2cMap.put(gatherList.get(j), String.format("#%d", sizeGroup+j+1));
					}
					exp = ExpressionTranslator.translateExp(exp, n2cMap);
					exps[i] = new Expression(exp);
				}
				if(this.icur != null)
				{
					this.icur.addOperation(new New(exps, names, null), ctx);
				}
				
				if(this.hasDistinct)
				{
					Expression[] grps = new Expression[sizeExpress];
					for(int k = 0, size = grps.length; k < size; k++)
					{
						grps[k] = new Expression(String.format("#%d", k+1));
					}
					
					if(this.icur != null)
					{
						Table tab = this.icur.groups(grps, names, null, null, null, ctx);
						this.icur = new MemoryCursor(tab);
						/*
						if(this.icur instanceof MemoryCursor || this.isMemory)
						{
							Table tab = CursorUtil.groups(this.icur, grps, names, cnts, nms, null, ctx);
							this.icur = new MemoryCursor(tab);
						}
						else
						{
							int capacity = EnvUtil.getCapacity(grps.length + cnts.length);
							this.icur = CursorUtil.hashGroupx(this.icur, grps, names, cnts, nms, null, ctx, capacity);
						}
						*/
					}
				}
				
				if(this.sortList != null && this.sortList.size() != 0)
				{
					int limit = -1, offset = -1;
					if(this.topNumber >= 0 || this.limitNumber >= 0)
					{
						if(this.topNumber >= 0 && this.limitNumber >= 0)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Limit关键字不能同时使用");
						}
						else if(this.topNumber >= 0 && this.offsetNumber >= 0)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Offset关键字不能同时使用");
						}
						
						limit = this.topNumber >= 0 ? this.topNumber : this.limitNumber;
						offset = this.offsetNumber > 0 ? this.offsetNumber : 0;
						limit = limit + offset;
						if(limit <= ICursor.FETCHCOUNT)
						{
							this.topNumber = -1;
							this.limitNumber = -1;
							this.offsetNumber = -1;
						}
						else
						{
							limit = -1;
							offset = -1;
						}
					}
					
					Expression[] ordExps = new Expression[this.sortList.size()];
					for(int s = 0, size = ordExps.length; s < size; s++)
					{
						String ordExp = this.sortList.get(s).toExpression();
						if(this.sortList.get(s) instanceof NormalNode)
						{
							try
							{
								int colNo = Integer.parseInt(ordExp);
								if(colNo <= 0 || colNo > this.columnList.size())
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项取的列号不对");
								}
								ordExp = "#" + colNo;
							}
							catch(NumberFormatException ex)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项的类型不对", ex);
							}
						}
						else
						{
							Map<String, String> n2cMap = new LinkedHashMap<String, String>();
							for(int e = 0, len = expressList.size(); e < len; e++)
							{
								n2cMap.put(expressList.get(e), "#" + (e + 1));
							}
							for(int a = 0, len = this.aliasList.size(); a < len; a++)
							{
								String aliExp = "'" + this.aliasList.get(a) + "'";
								n2cMap.put(aliExp, "#" + (a + 1));
							}
							ordExp = ExpressionTranslator.translateExp(ordExp, n2cMap);
						}
						ordExps[s] = new Expression(ordExp);
					}
					if(limit >= 0)
					{
						if(ordExps.length > 1)
						{
							StringBuffer sb = new StringBuffer();
							sb.append("[");
							for(int ox = 0; ox < ordExps.length; ox++)
							{
								if(ox > 0)
								{
									sb.append(",");
								}
								sb.append(ordExps[ox].toString());
							}
							sb.append("]");
							
							if(this.icur != null)
							{
								Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, new Expression(sb.toString()), new Expression("~"), ctx);
								this.icur = new MemoryCursor(seq);
							}
						}
						else
						{
							if(this.icur != null)
							{
								Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, ordExps[0], new Expression("~"), ctx);
								this.icur = new MemoryCursor(seq);
							}
						}
						if(offset > 0)
						{
							if(this.icur != null)
							{
								this.icur.skip(offset);
							}
						}
					}
					else
					{
						if(this.icur != null)
						{
							if(this.icur instanceof MemoryCursor || this.isMemory)
							{
								this.icur = new MemoryCursor(this.icur.fetch().sort(ordExps, null, null, this.ctx));
							}
							else
							{
								int capacityEx = EnvUtil.getCapacity(ordExps.length);
								this.icur = CursorUtil.sortx(this.icur, ordExps, this.ctx, capacityEx, null);
							}
						}
					}
				}
			}
			else
			{
				if(this.havingNode != null)
				{
					this.havingNode.setFromHaving();
					String havingExp = this.havingNode.toExpression();
					havingExp = ExpressionTranslator.translateExp(havingExp, stdMap);
					havingExp = ExpressionTranslator.translateExp(havingExp, trMap);
					
					Expression flt = new Expression(havingExp);
					if(this.icur != null)
					{
						this.icur.addOperation(new Select(flt, null), ctx);
					}
				}
				
				cur = fillSubQueryField(this.ics, this.icur, this.subQueryOfSelectEntryList, this.parameterList, this.tableNode.getAlias(), SubQueryCursor.Select_Type, this.ds);
				if(cur != null && cur instanceof SubQueryCursor && !cur.equals(this.icur))
				{
					this.ds = ((SubQueryCursor)cur).getTableDataStruct();
				}
				else if(cur != null)
				{
					this.ds = cur.getDataStruct();
				}
				this.icur = cur;
				
				if(this.ds != null)
				{
					for(int i = trMap.size(); i < this.ds.getFieldCount(); i++)
					{
						trMap.put("'" + this.ds.getFieldName(i) + "'", this.ds.getFieldName(i));
					}
				}
				
				if(this.hasDistinct)
				{
					int sizeExpress = expressList.size();
					Expression[] exps = new Expression[sizeExpress];
					String[] names = new String[sizeExpress];
					this.aliasList.toArray(names);
					for(int i = 0; i < sizeExpress; i++)
					{
						String exp = expressList.get(i);
						exp = ExpressionTranslator.translateExp(exp, stdMap);
						exp = ExpressionTranslator.translateExp(exp, trMap);
						exps[i] = new Expression(exp);
					}
					if(this.icur != null)
					{
						this.icur.addOperation(new New(exps, names, null), ctx);
					}
					
					Expression[] grps = new Expression[sizeExpress];
					for(int k = 0; k < sizeExpress; k++)
					{
						grps[k] = new Expression(String.format("#%d", k + 1));
					}
					if(this.icur != null)
					{
						Table tab = this.icur.groups(grps, names, null, null, null, ctx);
						this.icur = new MemoryCursor(tab);
						/*
						if(this.icur instanceof MemoryCursor || this.isMemory)
						{
							Table tab = CursorUtil.groups(this.icur, grps, names, cnts, nms, null, ctx);
							this.icur = new MemoryCursor(tab);
						}
						else
						{
							int capacity = EnvUtil.getCapacity(grps.length + cnts.length);
							this.icur = CursorUtil.hashGroupx(this.icur, grps, names, cnts, nms, null, ctx, capacity);
						}
						*/
					}
					
					if(this.sortList != null && this.sortList.size() != 0)
					{
						int limit = -1, offset = -1;
						if(this.topNumber >= 0 || this.limitNumber >= 0)
						{
							if(this.topNumber >= 0 && this.limitNumber >= 0)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Limit关键字不能同时使用");
							}
							else if(this.topNumber >= 0 && this.offsetNumber >= 0)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Offset关键字不能同时使用");
							}
							limit = this.topNumber >= 0 ? this.topNumber : this.limitNumber;
							offset = this.offsetNumber > 0 ? this.offsetNumber : 0;
							limit = limit + offset;
							if(limit <= ICursor.FETCHCOUNT)
							{
								this.topNumber = -1;
								this.limitNumber = -1;
								this.offsetNumber = -1;
							}
							else
							{
								limit = -1;
								offset = -1;
							}
						}
						Expression[] ordExps = new Expression[this.sortList.size()];
						for(int s = 0, size = ordExps.length; s < size; s++)
						{
							String ordExp = this.sortList.get(s).toExpression();
							if(this.sortList.get(s) instanceof NormalNode)
							{
								try
								{
									int colNo = Integer.parseInt(ordExp);
									if(colNo <= 0 || colNo > this.columnList.size())
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项取的列号不对");
									}
									ordExp = "#" + colNo;
								}
								catch(NumberFormatException ex)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项的类型不对", ex);
								}
							}
							else
							{
								Map<String, String> n2cMap = new LinkedHashMap<String, String>();
								for(int e = 0, len = expressList.size(); e < len; e++)
								{
									String exp = expressList.get(e);
									exp = ExpressionTranslator.translateExp(exp, stdMap);
									n2cMap.put(exp, "#" + (e + 1));
								}
								for(int a = 0, len = this.aliasList.size(); a < len; a++)
								{
									String aliExp = "'"+this.aliasList.get(a)+"'";
									n2cMap.put(aliExp, "#" + (a + 1));
								}
								ordExp = ExpressionTranslator.translateExp(ordExp, stdMap);
								ordExp = ExpressionTranslator.translateExp(ordExp, n2cMap);
							}
							ordExps[s] = new Expression(ordExp);
						}
						if(limit >= 0)
						{
							if(ordExps.length > 1)
							{
								StringBuffer sb = new StringBuffer();
								sb.append("[");
								for(int ox = 0; ox < ordExps.length; ox++)
								{
									if(ox > 0)
									{
										sb.append(",");
									}
									sb.append(ordExps[ox].toString());
								}
								sb.append("]");
								if(this.icur != null)
								{
									Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, new Expression(sb.toString()), new Expression("~"), ctx);
									this.icur = new MemoryCursor(seq);
								}
							}
							else
							{
								if(this.icur != null)
								{
									Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, ordExps[0], new Expression("~"), ctx);
									this.icur = new MemoryCursor(seq);
								}
							}
							if(offset > 0)
							{
								if(this.icur != null)
								{
									this.icur.skip(offset);
								}
							}
						}
						else
						{
							if(this.icur != null)
							{
								if(this.icur instanceof MemoryCursor || this.isMemory)
								{
									this.icur = new MemoryCursor(this.icur.fetch().sort(ordExps, null, null, this.ctx));
								}
								else
								{
									int capacityEx = EnvUtil.getCapacity(ordExps.length);
									this.icur = CursorUtil.sortx(this.icur, ordExps, this.ctx, capacityEx, null);
								}
							}
						}
					}
				}
				else
				{
					if(this.sortList != null && this.sortList.size() != 0)
					{
						int limit = -1, offset = -1;
						if(this.topNumber >= 0 || this.limitNumber >= 0)
						{
							if(this.topNumber >= 0 && this.limitNumber >= 0)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Limit关键字不能同时使用");
							}
							else if(this.topNumber >= 0 && this.offsetNumber >= 0)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Offset关键字不能同时使用");
							}
							limit = this.topNumber >= 0 ? this.topNumber : this.limitNumber;
							offset = this.offsetNumber > 0 ? this.offsetNumber : 0;
							limit = limit + offset;
							if(limit <= ICursor.FETCHCOUNT)
							{
								this.topNumber = -1;
								this.limitNumber = -1;
								this.offsetNumber = -1;
							}
							else
							{
								limit = -1;
								offset = -1;
							}
						}
						
						Expression[] ordExps = new Expression[this.sortList.size()];
						for(int s = 0, size = ordExps.length; s < size; s++)
						{
							String ordExp = this.sortList.get(s).toExpression();
							if(this.sortList.get(s) instanceof NormalNode)
							{
								try
								{
									int colNo = Integer.parseInt(ordExp);
									if(colNo <= 0 || colNo > this.columnList.size())
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项取的列号不对");
									}
									ordExp = "#" + colNo;
								}
								catch(NumberFormatException ex)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":execute, 排序项的类型不对", ex);
								}
							}
							else
							{
								Map<String, String> n2cMap = new LinkedHashMap<String, String>();
								for(int a = 0, len = this.aliasList.size(); a < len; a++)
								{
									String aliExp = "'" + this.aliasList.get(a) + "'";
									String exp = expressList.get(a);
									exp = ExpressionTranslator.translateExp(exp, stdMap);
									String colExp = "(" + exp + ")";
									n2cMap.put(aliExp, colExp);
								}
								ordExp = ExpressionTranslator.translateExp(ordExp, stdMap);
								ordExp = ExpressionTranslator.translateExp(ordExp, n2cMap);
								ordExp = ExpressionTranslator.translateExp(ordExp, trMap);
							}
							ordExps[s] = new Expression(ordExp);
						}
						
						if(limit >= 0)
						{
							if(ordExps.length > 1)
							{
								StringBuffer sb = new StringBuffer();
								sb.append("[");
								for(int ox = 0; ox < ordExps.length; ox++)
								{
									if(ox > 0)
									{
										sb.append(",");
									}
									sb.append(ordExps[ox].toString());
								}
								sb.append("]");
								
								if(this.icur != null)
								{
									Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, new Expression(sb.toString()), new Expression("~"), ctx);
									this.icur = new MemoryCursor(seq);
								}
							}
							else
							{
								if(this.icur != null)
								{
									Sequence seq = (Sequence)CursorUtil.top(this.icur, limit, ordExps[0], new Expression("~"), ctx);
									this.icur = new MemoryCursor(seq);
								}
							}
							if(offset > 0)
							{
								if(this.icur != null)
								{
									this.icur.skip(offset);
								}
							}
						}
						else
						{
							if(this.icur != null)
							{
								if(this.icur instanceof MemoryCursor || this.isMemory)
								{
									this.icur = new MemoryCursor(this.icur.fetch().sort(ordExps, null, null, this.ctx));
								}
								else
								{
									int capacityEx = EnvUtil.getCapacity(ordExps.length);
									this.icur = CursorUtil.sortx(this.icur, ordExps, this.ctx, capacityEx, null);
								}
							}
						}
					}

					int sizeExpress = expressList.size();
					Expression[] exps = new Expression[sizeExpress];
					String[] names = new String[sizeExpress];
					this.aliasList.toArray(names);
					for(int i = 0; i < sizeExpress; i++)
					{
						String exp = expressList.get(i);
						exp = ExpressionTranslator.translateExp(exp, stdMap);
						exp = ExpressionTranslator.translateExp(exp, trMap);
						exps[i] = new Expression(exp);
					}
					if(this.icur != null)
					{
						this.icur.addOperation(new New(exps, names, null), ctx);
					}
				}
			}
			
			if(this.topNumber >= 0 && this.limitNumber >= 0)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Limit关键字不能同时使用");
			}
			else if(this.topNumber >= 0 && this.offsetNumber >= 0)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":execute, Top关键字与Offset关键字不能同时使用");
			}
			else if(this.topNumber >= 0)
			{	
				if(this.icur != null)
				{
					this.icur = new SubCursor(this.icur, this.topNumber);
				}
			}
			else if(this.limitNumber >= 0)
			{
				if(this.offsetNumber > 0)
				{
					if(this.icur != null)
					{
						this.icur.skip(this.offsetNumber);
					}
				}
				if(this.icur != null)
				{
					this.icur = new SubCursor(this.icur, this.limitNumber);
				}
			}
			else if(this.offsetNumber > 0)
			{
				if(this.icur != null)
				{
					this.icur.skip(this.offsetNumber);
				}
			}
			
			Expression[] colExps = new Expression[this.finalList.size()];
			for(int i=0, len=colExps.length; i<len; i++)
			{
				colExps[i] = new Expression(String.format("#%d", i+1));
			}
			String[] colNames = new String[this.finalList.size()];
			this.finalList.toArray(colNames);
			if(this.icur != null)
			{
				this.icur.addOperation(new New(colExps, colNames, null), this.ctx);
			}
			this.ds = new DataStruct(colNames);
		}
	}
	
	public DataStruct getDataStruct()
	{
		return this.ds;
	}
	
	private String[] getParams(String expression)
	{
		Token[] tokens = Tokenizer.parse(expression);
		int start = 0;
		int next = tokens.length;
		ArrayList<String> paramList = new ArrayList<String>();
		while(true)
		{
			StringBuffer sb = new StringBuffer();
			int comma = Tokenizer.scanComma(tokens, start, next);
			if(comma < 0)
			{
				for(int i = start; i < next; i++)
				{
					sb.append(tokens[i].getOriginString());
					sb.append(tokens[i].getSpaces());
				}
				if(sb.length() > 0)
				{
					paramList.add(sb.toString().trim());
				}
				break;
			}
			else
			{
				for(int i = start; i < comma; i++)
				{
					sb.append(tokens[i].getOriginString());
					sb.append(tokens[i].getSpaces());
				}
				if(sb.length() > 0)
				{
					paramList.add(sb.toString());
				}
				else
				{
					throw new RQException("函数参数不能为空");
				}
				start = comma + 1;
				if(start >= next)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("function.paramError") + ":getParams, 参数格式有误");
				}
			}
		}
		String[] params = new String[paramList.size()];
		paramList.toArray(params);
		return params;
	}
	
	private Token[] optimizeWhere(Token[] whereTokens, List<Object> paramList)
	{
		PerfectWhere pw = new PerfectWhere(whereTokens, paramList);
		String topFilter = pw.getTopFromTokens(null, null, this.tableNode.getName(), this.tableNode.getAlias());
		if(this.topFilter == null)
		{
			this.topFilter = topFilter;
		}
		else if(topFilter != null)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":optimizeWhere, WHERE子句被重复分析");
		}
		return pw.getTokens(true);
	}
	
	private String getSQLValue(Object value)
	{
		if(value == null)
		{
			return "null";
		}
		else if(value instanceof String)
		{
			return "'" + value + "'";
		}
		else if(value instanceof Boolean)
		{
			return value.toString();
		}
		else if(value instanceof Number)
		{
			return value.toString();
		}
		else if(value instanceof java.sql.Date)
		{
			return String.format("date(\"%s\",\"yyyy-MM-dd\")", new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date)value));
		}
		else if(value instanceof java.sql.Time)
		{
			return String.format("time(\"%s\",\"HH:mm:ss.SSS\")", new SimpleDateFormat("HH:mm:ss.SSS").format((java.sql.Time)value));
		}
		else if(value instanceof java.sql.Timestamp)
		{
			return String.format("timestamp(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((java.sql.Timestamp)value));
		}
		else if(value instanceof Date)
		{
			return String.format("timestamp(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date)value));
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("function.paramError") + ":getSQLValue, 不支持的数据类型");
		}
	}
	
	public String getProcValue(Object param)
	{
		if(param == null)
		{
			return "null";
		}
		else if(param instanceof String)
		{
			return String.format("\"%s\"", param.toString());
		}
		else if(param instanceof Boolean)
		{
			return param.toString();
		}
		else if(param instanceof Number)
		{
			return param.toString();
		}
		else if(param instanceof java.sql.Date)
		{
			return String.format("date(\"%s\",\"yyyy-MM-dd\")", new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date)param));
		}
		else if(param instanceof java.sql.Time)
		{
			return String.format("time(\"%s\",\"HH:mm:ss.SSS\")", new SimpleDateFormat("HH:mm:ss.SSS").format((java.sql.Time)param));
		}
		else if(param instanceof java.sql.Timestamp)
		{
			return String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((java.sql.Timestamp)param));
		}
		else if(param instanceof Date)
		{
			return String.format("datetime(\"%s\",\"yyyy-MM-dd HH:mm:ss.SSS\")", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date)param));
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("function.paramError") + ":getProcValue, 不支持的数据类型");
		}
	}
	
	private ICursor fillSubQueryField(ICellSet ics, ICursor icur, List<Map.Entry<String, Token[]>> subQueryEntryList, List<Object> paramList, String tableName, int type, DataStruct ds)
	{
		if(icur == null)
		{
			return null;
		}
		
		if(ds == null)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, 查询结果序列缺少数据结构");
		}
		
		if(subQueryEntryList.isEmpty())
		{
			icur.setDataStruct(ds);
			return icur;
		}
		
		String[] fieldNames = ds.getFieldNames();
		List<List<List<Token>>> subQueryListListList = new ArrayList<List<List<Token>>>();
		List<List<String>> fieldNameListList = new ArrayList<List<String>>();
		List<String> colNameList = new ArrayList<String>();
		
		for(Map.Entry<String, Token[]> subQueryEntry : subQueryEntryList)
		{
			Token[] subQueryTokens = subQueryEntry.getValue();
			boolean canUseJoin = false;
			if(type != SubQueryCursor.In_Type)
			{
				int fromPos = Tokenizer.scanKeyWord("FROM", subQueryTokens, 0, subQueryTokens.length);
				if(fromPos <= 0)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, FROM关键字缺失");
				}
				int wherePos = Tokenizer.scanKeyWord("WHERE", subQueryTokens, 0, subQueryTokens.length);
				if(wherePos >= 0 && wherePos < fromPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, WHERE关键字的位置不正确");
				}
				if(wherePos < 0)
				{
					break;
				}
				int minPos = wherePos;
				int groupPos = Tokenizer.scanKeyWord("GROUP", subQueryTokens, 0, subQueryTokens.length);
				if(groupPos >= 0 && groupPos < wherePos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, GROUP关键字的位置不正确");
				}
				minPos = pos(groupPos, minPos);
				int havingPos = Tokenizer.scanKeyWord("HAVING", subQueryTokens, 0, subQueryTokens.length);
				if(havingPos >= 0 && havingPos < groupPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, HAVING关键字的位置不正确");
				}
				minPos = pos(havingPos, minPos);
				int orderPos = Tokenizer.scanKeyWord("ORDER", subQueryTokens, 0, subQueryTokens.length);
				if(orderPos >= 0 && orderPos < minPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, ORDER关键字的位置不正确");
				}
				minPos = pos(orderPos , minPos);
				int limitPos = Tokenizer.scanKeyWord("LIMIT", subQueryTokens, 0, subQueryTokens.length);
				if(limitPos >= 0 && limitPos < minPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, LIMIT关键字的位置不正确");
				}
				minPos = pos(limitPos , minPos);
				int offsetPos = Tokenizer.scanKeyWord("OFFSET", subQueryTokens, 0, subQueryTokens.length);
				if(offsetPos >= 0 && offsetPos < minPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, OFFSET关键字的位置不正确");
				}
				int byStart = pos(groupPos, wherePos, fromPos);
				int byEnd = pos(havingPos, orderPos, limitPos, offsetPos, subQueryTokens.length);
				int byPos = Tokenizer.scanKeyWord("BY", subQueryTokens, byStart, byEnd);
				if(byPos < 0 && groupPos > 0)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, 关键字GROUP后面缺少关键字BY");
				}
				else if(havingPos > 0 && byPos > havingPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, 分组关键字BY位置应该在HAVING之前");
				}
				
				int whereEnd = pos(groupPos, byPos, havingPos, orderPos, limitPos, offsetPos, subQueryTokens.length);
				int intoPos = Tokenizer.scanKeyWord("INTO", subQueryTokens, 0, subQueryTokens.length);
				if(intoPos >= fromPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, INTO关键字的位置不正确");
				}
				int columnEnd = pos(intoPos, fromPos);
				int selectPos = Tokenizer.scanKeyWord("SELECT", subQueryTokens, 0, subQueryTokens.length);
				if(selectPos < 0)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, SELECT关键字缺失");
				}
				int columnStart = selectPos;
				int distinctPos = Tokenizer.scanKeyWord("DISTINCT", subQueryTokens, 0, subQueryTokens.length);
				if(distinctPos >= 0 && distinctPos < selectPos)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, DISTINCT关键字的位置不正确");
				}
				columnStart = pos(distinctPos, columnStart);
				int topPos = Tokenizer.scanKeyWord("TOP", subQueryTokens, 0, subQueryTokens.length);
				if(topPos >= 0 && topPos < columnStart)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, TOP关键字的位置不正确");
				}
				columnStart = pos(topPos, columnStart);
				if(columnStart == topPos)
				{
					columnStart += 2;
				}
				else
				{
					columnStart++;
				}
				Token[] whereTokens = Arrays.copyOfRange(subQueryTokens, wherePos + 1, whereEnd);
				PerfectWhere pw = new PerfectWhere(whereTokens, this.parameterList);
				Set<String> outerFieldSet = new LinkedHashSet<String>();
				Set<String> innerFieldSet = new LinkedHashSet<String>();
				Set<String> tableNames = new HashSet<String>();
				tableNames.add(tableName);
				Token[] newOnTokens = pw.getOnFromTokens(tableNames, outerFieldSet, innerFieldSet);
				if(newOnTokens != null && newOnTokens.length != 0)
				{
					canUseJoin = true;
				}
				if(canUseJoin)
				{
					whereTokens = pw.getTokens(true);
					
					Token[] columnTokens = Arrays.copyOfRange(subQueryTokens, columnStart, columnEnd);
					if(type == SubQueryCursor.Select_Type)
					{
						if(Tokenizer.scanComma(columnTokens, 0, columnTokens.length) != -1)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, 子查询中SELECT字段必须单列");
						}
						if(columnTokens[columnTokens.length - 1].getSpaces().isEmpty())
						{
							columnTokens[columnTokens.length - 1].addSpace();
						}
					}
					else if(type == SubQueryCursor.Exist_Type)
					{
						Token columnToken = new Token(Tokenizer.NUMBER, "1", -1, "1");
						columnToken.addSpace();
						columnTokens = new Token[]{columnToken};
					}
					
					String aliasName = subQueryEntry.getKey();
					Token aliasToken = new Token(Tokenizer.IDENT, aliasName, -1, aliasName);
					aliasToken.addSpace();
					
					String columnStr = "";
					for(Token columnToken : columnTokens)
					{
						columnStr += columnToken.getOriginString();
						columnStr += columnToken.getSpaces();
					}
					columnStr = columnStr.trim().toLowerCase();
					
					List<String> innerFieldList = new ArrayList<String>();
					int index = 2;
					Map<String, String> fn2cnMap = new LinkedHashMap<String, String>();
					for(String innerField : innerFieldSet)
					{
						if(!innerField.equals(columnStr))
						{
							innerFieldList.add(innerField);
							fn2cnMap.put(innerField, "#"+index);
							index++;
						}
						else
						{
							fn2cnMap.put(innerField, "#1");
						}
					}
					
					List<Token> newTokenList = new ArrayList<Token>();
					newTokenList.addAll(Arrays.asList(Arrays.copyOfRange(subQueryTokens, 0, columnStart)));
					newTokenList.addAll(Arrays.asList(columnTokens));
					newTokenList.add(aliasToken);
					
					index = 2;
					for(String innerField : innerFieldList)
					{
						Token commaToken = new Token(Tokenizer.COMMA, ",", -1, ",");
						commaToken.addSpace();
						newTokenList.add(commaToken);
						newTokenList.addAll(Arrays.asList(Tokenizer.parse(innerField + " " + "_" + index + " ")));
						index++;
					}
					
					newTokenList.addAll(Arrays.asList(Arrays.copyOfRange(subQueryTokens, columnEnd, wherePos + 1)));
					newTokenList.addAll(Arrays.asList(whereTokens));
					newTokenList.addAll(Arrays.asList(Arrays.copyOfRange(subQueryTokens, whereEnd, subQueryTokens.length)));
					Token[] newTokens = new Token[newTokenList.size()];
					newTokenList.toArray(newTokens);
					
					SimpleSQL lq = new SimpleSQL(this.ics, newTokens, 0, newTokens.length, this.parameterList, this.ctx, true);
					lq.setMemory(true);
					Object obj = lq.execute();
					Sequence subSeq = obj instanceof ICursor ? ((ICursor)obj).fetch() : (obj instanceof Sequence ? (Sequence)obj : null);
					
					String onFilter = "";
					for(Token newOnToken : newOnTokens)
					{
						onFilter += newOnToken.getOriginString();
						onFilter += newOnToken.getSpaces();
					}
					
					List<Expression> outerExpsList = new ArrayList<Expression>();
					List<Expression> innerExpsList = new ArrayList<Expression>();
					
					for(int m = 0, sz = outerFieldSet.size(); m < sz; m++)
					{
						String outerField = outerFieldSet.iterator().next();
						for(int k = 0, len = fieldNames.length; k < len; k++)
						{
							String fieldName = fieldNames[k];
							String bakOuterField = outerField;
							outerField = SimpleJoin.getRealFieldName(outerField);
							if(fieldName.equalsIgnoreCase(outerField))
							{
								fn2cnMap.put(bakOuterField, "#" + (k + 1));
							}
						}
					}
					
					String[] subFilters = onFilter.split("AND");
					for(String subFilter : subFilters)
					{
						String[] fltExps = subFilter.split("=");
						if(fltExps.length != 2)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+":fillSubQueryField, 等值布尔表达式语法错误");
						}
						
						if(outerFieldSet.contains(fltExps[0].trim().toLowerCase())) //外部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[0].trim().toLowerCase(), fn2cnMap);
							outerExpsList.add(new Expression(numberCode));
						}
						else if(innerFieldSet.contains(fltExps[0].trim().toLowerCase())) //内部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[0].trim().toLowerCase(), fn2cnMap);
							innerExpsList.add(new Expression(numberCode));
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+":fillSubQueryField, 未知的字段");
						}
						
						if(outerFieldSet.contains(fltExps[1].trim().toLowerCase())) //外部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[1].trim().toLowerCase(), fn2cnMap);
							outerExpsList.add(new Expression(numberCode));
						}
						else if(innerFieldSet.contains(fltExps[1].trim().toLowerCase())) //内部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[1].trim().toLowerCase(), fn2cnMap);
							innerExpsList.add(new Expression(numberCode));
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+":fillSubQueryField, 未知的字段");
						}
					}
					
					Expression[] outerExps = new Expression[outerExpsList.size()];
					outerExpsList.toArray(outerExps);
					Expression[] innerExps = new Expression[innerExpsList.size()];
					innerExpsList.toArray(innerExps);

					String opt = "";
					
					if(icur instanceof MemoryCursor)
					{
						Sequence res = new Join(null, new Expression[][]{outerExps}, new Sequence[]{subSeq}, new Expression[][]{innerExps}, new Expression[][]{new Expression[]{new Expression("#1")}}, new String[][]{new String[]{aliasName}}, opt).process(icur.fetch(), this.ctx);
						icur = new MemoryCursor(res);
					}
					else
					{
						icur.addOperation(new Join(null, new Expression[][]{outerExps}, new Sequence[]{subSeq}, new Expression[][]{innerExps}, new Expression[][]{new Expression[]{new Expression("#1")}}, new String[][]{new String[]{aliasName}}, opt), this.ctx);
					}
					
					String[] newFieldNames = new String[ds.getFieldCount() + 1];
					System.arraycopy(ds.getFieldNames(), 0, newFieldNames, 0, ds.getFieldCount());
					newFieldNames[ds.getFieldCount()] = aliasName;
					icur.setDataStruct(new DataStruct(newFieldNames));
				}
			}
			
			if(!canUseJoin)
			{
				List<List<Token>> subQueryListList = new ArrayList<List<Token>>();
				List<String> fieldNameList = new ArrayList<String>();
				List<Token> subQueryList = new ArrayList<Token>();
				for(int n = 0, len = subQueryTokens.length; n < len; n++)
				{
					String theFieldName = null;
					if(n < len - 2 
					&& subQueryTokens[n].getString().equalsIgnoreCase(tableName)
					&& subQueryTokens[n + 1].getType() == Tokenizer.DOT)
					{
						for(String fieldName : fieldNames)
						{
							if(subQueryTokens[n + 2].getString().equalsIgnoreCase(fieldName))
							{
								theFieldName = fieldName;
								break;
							}
						}
					}
					
					if(theFieldName != null)
					{
						subQueryListList.add(subQueryList);
						subQueryList  = new ArrayList<Token>();
						fieldNameList.add(theFieldName);
						n += 2;
					}
					else
					{
						subQueryList.add(subQueryTokens[n]);
					}
				}
				
				if(!subQueryList.isEmpty())
				{
					subQueryListList.add(subQueryList);
				}
				
				subQueryListListList.add(subQueryListList);
				fieldNameListList.add(fieldNameList);
				
				String aliasName = subQueryEntry.getKey();
				colNameList.add(aliasName);
			}
		}
		
		if(!colNameList.isEmpty())
		{
			icur = new SubQueryCursor(icur, type, ics, paramList, colNameList, subQueryListListList, fieldNameListList, this.ctx, ds);
		}
		
		return icur;
	}
	
	void setMemory(boolean isMemory)
	{
		this.isMemory = isMemory;
	}
	
	void setParallel(int parallelNumber)
	{
		this.parallelNumber = parallelNumber;
	}
	
	void setTopFilter(String topFilter)
	{
		this.topFilter = topFilter;
	}
}