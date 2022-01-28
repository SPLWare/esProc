package com.scudata.dm.query;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.scudata.cellset.ICellSet;
import com.scudata.common.IOUtils;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.IMultipath;
import com.scudata.dm.cursor.JoinxCursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.SubCursor;
import com.scudata.dm.cursor.SyncCursor;
import com.scudata.dm.cursor.XJoinxCursor;
import com.scudata.dm.op.Join;
import com.scudata.dm.op.New;
import com.scudata.dm.op.Select;
import com.scudata.dm.query.utils.ExpressionTranslator;
import com.scudata.dw.ColumnGroupTable;
import com.scudata.dw.TableMetaData;
import com.scudata.excel.ExcelTool;
import com.scudata.expression.Expression;
import com.scudata.resources.ParseMessage;

public class SimpleJoin
{
	private List<Object> parameterList;
	private Context ctx;
	private DataStruct ds;
	private ICursor icur;
	private JoinNode rootNode;
	private Set<String> whereList;
	private Set<String> selectList;
	private List<String> recycleList;
	private String intoFileName;
	private Boolean singleTable;
	private Map<String, JoinTable> withTableMap;
	private ICellSet ics;
	private int parallelNumber;
	private List<Map.Entry<String, Token[]>> subQueryOfExistsEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfSelectEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfInEntryList;
	private List<Map.Entry<String, Token[]>> subQueryOfWhereEntryList;
	private Map<String, String> subQueryOfExistsMap;
	private Map<String, String> subQueryOfSelectMap;
	private Map<String, String> subQueryOfInMap;
	private Map<String, String> subQueryOfWhereMap;
	private boolean isMemory;
	private JoinTable fromTable;
	
	public static final int Memory_Join = 0;
	public static final int External_Join = 1;
	
	abstract class JoinNode 
	{
		protected JoinNode nodeParent = null;
		protected JoinNode nodeLeft = null;
		protected JoinNode nodeRight = null;
		protected Set<String> joinFilter = new HashSet<String>();
		protected Set<String> whereFilter = new HashSet<String>();
		protected Set<String> selectFields = null;
		protected String filterBuffer = null;
		protected Set<String> tableNameList = new HashSet<String>();
		protected ICursor nodeCursor = null;
		protected DataStruct nodeStruct = null;
		protected boolean isMemoryNode = false;
		protected int stamp = Memory_Join;
		protected boolean parallel = false;
		protected String tableFile = null;
		protected TableMetaData metaData = null;
		protected String topFilter = null;
		
		public JoinNode getLeft() 
		{
			return this.nodeLeft;
		}

		public JoinNode getRight() 
		{
			return this.nodeRight;
		}
		
		public JoinNode getParent() 
		{
			return this.nodeParent;
		}
		
		public void setLeft(JoinNode left) 
		{
			this.nodeLeft = left;
			addTableNames(left.getTableNames());
		}

		public void setRight(JoinNode right) 
		{
			this.nodeRight = right;
			addTableNames(right.getTableNames());
		}
		
		public void setParent(JoinNode parent) 
		{
			this.nodeParent = parent;
		}
		
		public String getTableFile()
		{
			return null;
		}
		
		public void setTableFile(String file)
		{
		}
		
		public TableMetaData getTableMetaData()
		{
			return null;
		}
		
		public void setTableMetaData(TableMetaData metaData)
		{
		}
		
		public boolean hasTableName(String table)
		{
			for(String tableName : this.tableNameList)
			{
				if(tableName.equalsIgnoreCase(table))
				{
					return true;
				}
			}
			return false;
		}
		
		public void addTableName(String table)
		{
			if(!hasTableName(table))
			{
				this.tableNameList.add(table);
			}
		}
		
		public Set<String> getTableNames()
		{
			return this.tableNameList;
		}
		
		public void addTableNames(Set<String> tableList)
		{
			this.tableNameList.addAll(tableList);
		}
		
		public String getFilter() 
		{
			String filter = "";
			for(String joinFilter : this.joinFilter)
			{
				if(!filter.isEmpty())
				{
					filter += " AND ";
				}
				filter += addSmartParen(joinFilter);
			}
			this.joinFilter.clear();
			return filter;
		}

		public void setFilter(String filter)
		{
			this.joinFilter.add(filter.trim());
		}
		
		public String getBuffer()
		{
			return this.filterBuffer;
		}
		
		public void setBuffer(String buffer)
		{
			this.filterBuffer = buffer;
		}
		
		public void moveBuffer(int sign)
		{
			if(sign == 1)
			{
				setWhere(this.filterBuffer);
				this.filterBuffer = null;
			}
			else if(sign == 0)
			{
				setFilter(this.filterBuffer);
				this.filterBuffer = null;
			}
			else if(sign == -1)
			{
				if(this.selectFields != null)
				{
					this.selectFields.add(this.filterBuffer.trim());
				}
				this.filterBuffer = null;
			}
		}
		
		public boolean analyzeBuffer(String filter)
		{
			if(filter == null || filter.trim().isEmpty())
			{
				this.filterBuffer = "1=1";
				return true;
			}
			
			String tableName = null;
			Token[] tokens = Tokenizer.parse(filter);
			String newName = null;
			StringBuffer sb = new StringBuffer();
			boolean inFlag = false;
			
			for(int i = 0, len = tokens.length; i < len; i++)
			{
				if(tokens[i].isKeyWord("NOT") && i + 3 < len && tokens[i + 1].isKeyWord("EXISTS") && tokens[i + 2].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 2, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 3, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						Set<String> tableNames = rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						Set<String> nameSet = new LinkedHashSet<String>(); 
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains= false;
							String tempName = null;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									tempName = name;
									break;
								}
							}
							
							if(n < l - 2 && contains
							&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
							&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
								n += 2;
								
								nameSet.add(tempName);
								needDelayed = true;
							}
						}
						
						if(needDelayed)
						{
							for(String name : nameSet)
							{
								if(!getTableNames().contains(name))
								{
									return false;
								}
							}
							
							List<String> checkList = new ArrayList<String>();
							for(Token subQueryToken : subQueryTokens)
							{
								checkList.add(subQueryToken.getString().toUpperCase());
							}
							
							if(!subQueryOfExistsMap.containsKey(checkList.toString()))
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfExistsEntryList.add(subQueryMap.entrySet().iterator().next());
								sb.append("$" + uuid + " IS NULL");
								sb.append(tokens[end].getSpaces());
								subQueryOfExistsMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfExistsMap.get(checkList.toString());
								sb.append(dollar_uuid + " IS NULL");
								sb.append(tokens[end].getSpaces());
							}
						}
						else
						{
							SimpleSQL lq = new SimpleSQL(ics, subQueryTokens, 0, subQueryTokens.length, parameterList, ctx, true);
							lq.setMemory(true);
							ICursor cursor = lq.query();
							Sequence seq = cursor.peek(1);
							if(seq == null)
							{
								sb.append("1=1");
								sb.append(tokens[end].getSpaces());
							}
							else
							{
								sb.append("1=0");
								sb.append(tokens[end].getSpaces());
							}
						}
						
						i = end;
						
						continue;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, EXISTS关键字后面必须接子查询");
					}
				}
				else if(tokens[i].isKeyWord("EXISTS") && i + 2 < len && tokens[i + 1].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 1, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 2, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						Set<String> tableNames = rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						Set<String> nameSet = new LinkedHashSet<String>();
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains = false;
							String tempName = null;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									tempName = name;
									break;
								}
							}
							
							if(n < l - 2 && contains
							&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
							&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
								n += 2;
								
								nameSet.add(tempName);
								needDelayed = true;
							}
						}
						
						if(needDelayed)
						{
							for(String name : nameSet)
							{
								if(!getTableNames().contains(name))
								{
									return false;
								}
							}
							
							List<String> checkList = new ArrayList<String>();
							for(Token subQueryToken : subQueryTokens)
							{
								checkList.add(subQueryToken.getString().toUpperCase());
							}
							
							if(!subQueryOfExistsMap.containsKey(checkList.toString()))
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfExistsEntryList.add(subQueryMap.entrySet().iterator().next());
								sb.append("$" + uuid + " IS NOT NULL");
								sb.append(tokens[end].getSpaces());
								subQueryOfExistsMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfExistsMap.get(checkList.toString());
								sb.append(dollar_uuid + " IS NOT NULL");
								sb.append(tokens[end].getSpaces());
							}
						}
						else
						{
							SimpleSQL lq = new SimpleSQL(ics, subQueryTokens, 0, subQueryTokens.length, parameterList, ctx, true);
							lq.setMemory(true);
							ICursor cursor = lq.query();
							Sequence seq = cursor.peek(1);
							if(seq == null)
							{
								sb.append("1=0");
								sb.append(tokens[end].getSpaces());
							}
							else
							{
								sb.append("1=1");
								sb.append(tokens[end].getSpaces());
							}
						}
						
						i = end;
						
						continue;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, EXISTS关键字后面必须接子查询");
					}
				}
				
				newName = tokens[i].getOriginString();
				if(tokens[i].getType() == Tokenizer.IDENT && i + 2 < len && tokens[i + 1].getType() == Tokenizer.DOT)
				{
					if(tokens[i + 2].getType() == Tokenizer.IDENT && tokens[i+2].getString().equals("COUNT"))
					{
						tableName = tokens[i].getString();
						if(!hasTableName(tableName))
						{
							return false;
						}
						else
						{
							//统一起新的别名为：表名.列名，以区分不同表的同名字段
							newName = "\"" + tokens[i].getString()+ "." + "1" + "\"";
							i += 2;
						}
					}
					else if(tokens[i + 2].getString().equals("*"))
					{
						tableName = tokens[i].getString();
						if(!hasTableName(tableName))
						{
							return false;
						}
						else
						{
							//统一起新的别名为：表名.列名，以区分不同表的同名字段
							newName = "\"" + tokens[i].getString() + "." + tokens[i+2].getString() + "\"";
							i += 2;
						}
					}
					else if(tokens[i + 2].getType() == Tokenizer.IDENT)
					{
						tableName = tokens[i].getString();
						if(!hasTableName(tableName))
						{
							return false;
						}
						else
						{
							//顺便记录WHERE和ON子句中的原始字段名称到SELECT字段记录列表以备使用
							String oldName = tokens[i].getString() + tokens[i+1].getString() + tokens[i+2].getString();
							selectList.add(oldName);
							//统一起新的别名为：表名.列名，以区分不同表的同名字段
							newName = "\"" + tokens[i].getString()+"."+tokens[i+2].getString() + "\"";
							i += 2;
						}
					}
				}
				else if(tokens[i].getType() == Tokenizer.IDENT && tokens[i].getString().equals("COUNT"))
				{
					if(this instanceof JoinTable)
					{
						tableName = getTableNames().iterator().next();
						//统一起新的别名为：表名.列名，以区分不同表的同名字段
						newName = "\"" + tableName + "." + "1" + "\"";
					}
				}
				else if(tokens[i].getString().equals("*"))
				{
					if(this instanceof JoinTable)
					{
						tableName = getTableNames().iterator().next();
						//统一起新的别名为：表名.列名，以区分不同表的同名字段
						newName = "\"" + tableName + "." + tokens[i].getString() + "\"";
					}
				}
				else if(tokens[i].getType() == Tokenizer.LPAREN)
				{
					int finish = Tokenizer.scanParen(tokens, i, len);
					int begin = i;
					int end = finish;
					while(true)
					{
						begin++;
						end--;
						if(end > begin && tokens[begin].getType() == Tokenizer.LPAREN && tokens[end].getType() == Tokenizer.RPAREN)
						{
							int pos = Tokenizer.scanParen(tokens, begin, end + 1);
							if(pos == end)
							{
								continue;
							}
						}
						begin--;
						end++;
						break;
					}
					
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, begin + 1, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					ICursor icur = null;
					if(isSubQuery) //子查询
					{
						Set<String> tableNames = rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						Set<String> nameSet = new LinkedHashSet<String>();
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains = false;
							String tempName = null;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									tempName = name;
									break;
								}
							}
							
							if(n < l - 2 && contains
							&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
							&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
								n += 2;
								
								nameSet.add(tempName);
								needDelayed = true;
							}
						}
						
						if(inFlag)
						{
							if(needDelayed)
							{
								for(String name : nameSet)
								{
									if(!getTableNames().contains(name))
									{
										return false;
									}
								}
								
								List<String> checkList = new ArrayList<String>();
								for(Token subQueryToken : subQueryTokens)
								{
									checkList.add(subQueryToken.getString().toUpperCase());
								}
								
								if(!subQueryOfInMap.containsKey(checkList.toString()))
								{
									String uuid = UUID.randomUUID().toString().replace("-", "_");
									Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
									subQueryMap.put("$" + uuid, subQueryTokens);
									subQueryOfInEntryList.add(subQueryMap.entrySet().iterator().next());
									newName = "$" + uuid;
									subQueryOfInMap.put(checkList.toString(), "$" + uuid);
								}
								else
								{
									String dollar_uuid = subQueryOfInMap.get(checkList.toString());
									newName = dollar_uuid;
								}
							}
							else
							{
								SimpleSQL lq = new SimpleSQL(ics, subQueryTokens, 0, subQueryTokens.length, parameterList, ctx, true);
								lq.setMemory(true);
								icur = lq.query();
								Sequence seq = null;
								if(icur != null)
								{
									seq = icur.fetch();
								}
								
								if(seq == null || seq.length() == 0)
								{
									newName = "(1=0)";
								}
								else
								{
									if(!(seq.get(1) instanceof Record) || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, IN中子查询结果异常");
									}
									
									newName = "(";
									for(int m = 1, n = seq.length(); m <= n; m++)
									{
										if(m > 1)
										{
											newName += ",";
										}
										Object value = ((Record)seq.get(m)).getFieldValue(0);
										newName += getSQLValue(value);
									}
									newName += ")";
								}
								
								inFlag = false;
							}
						}
						else
						{
							if(needDelayed)
							{
								for(String name : nameSet)
								{
									if(!getTableNames().contains(name))
									{
										return false;
									}
								}
								
								List<String> checkList = new ArrayList<String>();
								for(Token subQueryToken : subQueryTokens)
								{
									checkList.add(subQueryToken.getString().toUpperCase());
								}
								
								if(!subQueryOfWhereMap.containsKey(checkList.toString()))
								{
									String uuid = UUID.randomUUID().toString().replace("-", "_");
									Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
									subQueryMap.put("$" + uuid, subQueryTokens);
									subQueryOfWhereEntryList.add(subQueryMap.entrySet().iterator().next());
									newName = "$" + uuid;
									subQueryOfWhereMap.put(checkList.toString(), "$" + uuid);
								}
								else
								{
									String dollar_uuid = subQueryOfWhereMap.get(checkList.toString());
									newName = dollar_uuid;
								}
							}
							else
							{
								SimpleSQL lq = new SimpleSQL(ics, subQueryTokens, 0, subQueryTokens.length, parameterList, ctx, true);
								lq.setMemory(true);
								icur = lq.query();
								Sequence seq = null;
								if(icur != null)
								{
									seq = icur.fetch();
									if(seq == null || seq.length() != 1 || !(seq.get(1) instanceof Record) || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, WHERE中子查询结果异常");
									}
								}
								newName = getSQLValue(((Record)seq.get(1)).getFieldValue(0));
							}
						}
						
						i = finish;
					}
				}
				else if(tokens[i].isKeyWord("IN"))
				{
					inFlag = true;
				}
				else if(inFlag)
				{
					inFlag = false;
				}
				
				sb.append(newName);
				sb.append(tokens[i].getSpaces());
			}
			
			this.filterBuffer = sb.toString();
			
			return true;
		}
		
		public String getWhere() 
		{
			String filter = "";
			for(String whereFilter : this.whereFilter)
			{
				if(!filter.isEmpty())
				{
					filter += " AND ";
				}
				filter += addSmartParen(whereFilter);
			}
			this.whereFilter.clear();
			return filter;
		}
		
		public void setWhere(String filter)
		{
			this.whereFilter.add(filter.trim());
		}
		
		public Set<String> getSelect() 
		{
			return this.selectFields;
		}
		
		public ICursor getCursor()
		{
			return this.nodeCursor;
		}
		
		public void setCursor(ICursor cursor)
		{
			this.nodeCursor = cursor;
		}
		
		public DataStruct getStruct()
		{
			return this.nodeStruct;
		}
		
		public void setStruct(DataStruct struct)
		{
			this.nodeStruct = struct;
		}
		
		public void executeJoin() 
		{
			if(this instanceof JoinTable)
			{
				boolean needRename = false;
				String tableName = this.getTableNames().iterator().next();
				String prefix = "\"" + tableName + ".";
				StringBuffer sb = new StringBuffer();
				int index = 0;
				for(String selectField : this.selectFields)
				{
					if(selectField != null)
					{
						selectField = selectField.trim();
					}
					if(index > 0)
					{
						sb.append(", ");
					}
					if(selectField.startsWith(prefix))
					{
						String fieldName = selectField.substring(prefix.length(), selectField.length() - 1); //注意要脱去""
						sb.append(fieldName);//原字段名
						sb.append(" ");
						if(fieldName.equals("1"))
						{
							sb.append(prefix + "COUNT\"");
						}
						else if(fieldName.equals("*"))
						{
							needRename = true;//过后补上新字段名
						}
						else
						{
							sb.append(selectField);//新字段名作列别名
						}
					}
					index++;
				}
				String topFilter = ((JoinTable)this).getTopFilter();
				if(this.nodeStruct == null && this.metaData == null)
				{
					if(this.tableFile == null || this.tableFile.isEmpty())
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, 无效的连接子表节点");
					}
					
					SimpleSelect ss = new SimpleSelect(ics, ctx);
					ss.setMemory(isMemory || this.isMemoryNode);
					if(this.parallel && parallelNumber > 1)
					{
						ss.setParallel(parallelNumber);
					}
					if(topFilter != null)
					{
						ss.setTopFilter(topFilter);
					}
					
					String sql = String.format("SELECT %s FROM %s %s", sb.toString(), this.getTableFile(), this.getTableNames().iterator().next());
					ICursor icur = ss.query(sql);
					if(icur != null)
					{
						String whereStr = getWhere();
						if(whereStr != null && !whereStr.trim().isEmpty())
						{
							whereStr = " WHERE " + whereStr;
						}
						
						DataStruct ds = ss.getDataStruct();
						for(String fieldName : ds.getFieldNames())
						{
							if(fieldName.startsWith(prefix))
							{
								whereStr = whereStr.replaceAll("(?i)" + fieldName, fieldName.substring(prefix.length(), fieldName.length() - 1));
							}
							else
							{
								whereStr = whereStr.replaceAll("(?i)" + prefix + fieldName + "\"", fieldName);
							}
						}
						whereStr = whereStr.trim();
						
						List<Map.Entry<String, Token[]>> newSubQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
						if(!whereStr.isEmpty() && !subQueryOfExistsEntryList.isEmpty())
						{
							for(Map.Entry<String, Token[]> subQueryOfExistsEntry : subQueryOfExistsEntryList)
							{
								String ident = subQueryOfExistsEntry.getKey();
								if(whereStr.contains(ident))
								{
									newSubQueryOfExistsEntryList.add(subQueryOfExistsEntry);
								}
							}
						}
						
						List<Map.Entry<String, Token[]>> newSubQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
						if(!whereStr.isEmpty() && !subQueryOfInEntryList.isEmpty())
						{
							for(Map.Entry<String, Token[]> subQueryOfExistsEntry : subQueryOfInEntryList)
							{
								String ident = subQueryOfExistsEntry.getKey();
								if(whereStr.contains(ident))
								{
									newSubQueryOfInEntryList.add(subQueryOfExistsEntry);
								}
							}
						}
						
						List<Map.Entry<String, Token[]>> newSubQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
						if(!whereStr.isEmpty() && !subQueryOfWhereEntryList.isEmpty())
						{
							for(Map.Entry<String, Token[]> subQueryOfExistsEntry : subQueryOfWhereEntryList)
							{
								String ident = subQueryOfExistsEntry.getKey();
								if(whereStr.contains(ident))
								{
									newSubQueryOfWhereEntryList.add(subQueryOfExistsEntry);
								}
							}
						}

						if(!newSubQueryOfExistsEntryList.isEmpty() || !newSubQueryOfInEntryList.isEmpty() || !newSubQueryOfWhereEntryList.isEmpty())
						{
							this.nodeCursor = icur;
							this.nodeStruct = ds;

							if(!newSubQueryOfExistsEntryList.isEmpty())
							{
								icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfExistsEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Exist_Type, this.nodeStruct);
								if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
								{
									this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
								}
								else if(icur != null)
								{
									this.nodeStruct = icur.getDataStruct();
								}
								this.nodeCursor = icur;
								
								if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
								{
									this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
								}
							}
							
							if(!newSubQueryOfInEntryList.isEmpty())
							{
								icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfInEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.In_Type, this.nodeStruct);
								if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
								{
									this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
								}
								else if(icur != null)
								{
									this.nodeStruct = icur.getDataStruct();
								}
								this.nodeCursor = icur;
								
								if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
								{
									this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
								}
							}
							
							if(!newSubQueryOfWhereEntryList.isEmpty())
							{
								icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfWhereEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Where_Type, this.nodeStruct);
								if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
								{
									this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
								}
								else if(icur != null)
								{
									this.nodeStruct = icur.getDataStruct();
								}
								this.nodeCursor = icur;
								
								if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
								{
									this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
								}
							}
							
							whereStr = SimpleSelect.scanExp(Tokenizer.parse(whereStr.replace("WHERE", "").trim()), parameterList);
							Map<String, String> transMap = new LinkedHashMap<String, String>();
							Next:
							for(int p = 0; p < this.nodeStruct.getFieldCount(); p++)
							{
								String colName = this.nodeStruct.getFieldName(p);
								for(Map.Entry<String, Token[]> newSubQueryOfExistsEntry : newSubQueryOfExistsEntryList)
								{
									String ident = newSubQueryOfExistsEntry.getKey();
									if(colName.equalsIgnoreCase(ident))
									{
										transMap.put("'" + colName + "'", "#" + (p + 1));
										continue Next;
									}
								}
								for(Map.Entry<String, Token[]> newSubQueryOfInEntry : newSubQueryOfInEntryList)
								{
									String ident = newSubQueryOfInEntry.getKey();
									if(colName.equalsIgnoreCase(ident))
									{
										transMap.put("'" + colName + "'", "#" + (p + 1));
										continue Next;
									}
								}
								for(Map.Entry<String, Token[]> newSubQueryOfWhereEntry : newSubQueryOfWhereEntryList)
								{
									String ident = newSubQueryOfWhereEntry.getKey();
									if(colName.equalsIgnoreCase(ident))
									{
										transMap.put("'" + colName + "'", "#" + (p + 1));
										continue Next;
									}
								}
								transMap.put("'" + getRealFieldName(colName) + "'", "#" + (p + 1));
							}
							whereStr = ExpressionTranslator.translateExp(whereStr, transMap);
							this.nodeCursor.addOperation(new Select(new Expression(whereStr), null), ctx);
						}
						else
						{
							sql = String.format("%s %s", sql, whereStr);
							ss = new SimpleSelect(ics, ctx);
							ss.setMemory(isMemory || this.isMemoryNode);
							if(this.parallel && parallelNumber > 1)
							{
								ss.setParallel(parallelNumber);
							}
							if(topFilter != null)
							{
								ss.setTopFilter(topFilter);
							}
							ss.setSQLParameters(parameterList);
							
							this.nodeCursor = ss.query(sql);
							this.nodeStruct = ss.getDataStruct();
							if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
							{
								this.nodeCursor = new MemoryCursor(this.nodeCursor.fetch());
							}
						}
					}
				}
				else
				{
					SimpleSelect ss = new SimpleSelect(ics, ctx);
					ss.setMemory(isMemory || this.isMemoryNode);
					String dql = String.format("SELECT %s FROM (?)", sb.toString());
					if(this.nodeStruct != null)
					{
						if(this.nodeCursor == null)
						{
							this.nodeCursor = new MemoryCursor(new Table(this.nodeStruct));
						}
						ss.setSQLParameters(this.nodeCursor);
						ss.setSQLParameters(this.nodeStruct);
					}
					else if(this.metaData != null)
					{
						ss.setSQLParameters(this.metaData);
					}
					if(topFilter != null)
					{
						ss.setTopFilter(topFilter);
					}
					this.nodeCursor = ss.query(dql);
					this.nodeStruct = ss.getDataStruct();
					if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
					{
						this.nodeCursor = new MemoryCursor(this.nodeCursor.fetch());
					}
				}
				
				if(needRename)//select * from T, 查询时没起新字段名此时补上
				{
					String[] fieldNames = this.nodeStruct.getFieldNames();
					Expression[] fieldExps = new Expression[fieldNames.length];
					for(int i=0, len=fieldNames.length; i<len; i++)
					{
						fieldExps[i] = new Expression(String.format("#%d", i+1));
						fieldNames[i] = tableName + "." + fieldNames[i];
					}
					if(this.nodeCursor != null)
					{
						this.nodeCursor.addOperation(new New(fieldExps, fieldNames, null), ctx);
						this.nodeStruct = new DataStruct(fieldNames);
					}
					needRename = false;
				}
				
				String whereStr = getWhere();
				if(whereStr != null && !whereStr.trim().isEmpty())
				{
					whereStr = " WHERE " + whereStr;
				}
				
				List<Map.Entry<String, Token[]>> newSubQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfExistsEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfExistsEntry : subQueryOfExistsEntryList)
					{
						String ident = subQueryOfExistsEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfExistsEntryList.add(subQueryOfExistsEntry);
						}
					}
				}
				
				List<Map.Entry<String, Token[]>> newSubQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfInEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfInEntry : subQueryOfInEntryList)
					{
						String ident = subQueryOfInEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfInEntryList.add(subQueryOfInEntry);
						}
					}
				}
				
				List<Map.Entry<String, Token[]>> newSubQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfWhereEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfWhereEntry : subQueryOfWhereEntryList)
					{
						String ident = subQueryOfWhereEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfWhereEntryList.add(subQueryOfWhereEntry);
						}
					}
				}

				if(!newSubQueryOfExistsEntryList.isEmpty() || !newSubQueryOfInEntryList.isEmpty() || !newSubQueryOfWhereEntryList.isEmpty())
				{
					if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
					{
						this.nodeCursor = new MemoryCursor(this.nodeCursor.fetch());
					}
					
					if(!newSubQueryOfExistsEntryList.isEmpty())
					{
						ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfExistsEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Exist_Type, this.nodeStruct);
						if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
						{
							this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
						}
						else if(icur != null)
						{
							this.nodeStruct = icur.getDataStruct();
						}
						this.nodeCursor = icur;
						
						if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
						{
							this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
						}
					}
					
					if(!newSubQueryOfInEntryList.isEmpty())
					{
						ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfInEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.In_Type, this.nodeStruct);
						if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
						{
							this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
						}
						else if(icur != null)
						{
							this.nodeStruct = icur.getDataStruct();
						}
						this.nodeCursor = icur;
						
						if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
						{
							this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
						}
					}
					
					if(!newSubQueryOfWhereEntryList.isEmpty())
					{
						ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfWhereEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Where_Type, this.nodeStruct);
						if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
						{
							this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
						}
						else if(icur != null)
						{
							this.nodeStruct = icur.getDataStruct();
						}
						this.nodeCursor = icur;
						
						if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
						{
							this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
						}
					}
					
					if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
					{
						this.nodeCursor = new MemoryCursor(this.nodeCursor == null ? null : this.nodeCursor.fetch());
					}
					
					whereStr = SimpleSelect.scanExp(Tokenizer.parse(whereStr.replace("WHERE", "").trim()), parameterList);
					Map<String, String> transMap = new LinkedHashMap<String, String>();
					Next:
					for(int p = 0; p < this.nodeStruct.getFieldCount(); p++)
					{
						String colName = this.nodeStruct.getFieldName(p);
						for(Map.Entry<String, Token[]> newSubQueryOfExistsEntry : newSubQueryOfExistsEntryList)
						{
							String ident = newSubQueryOfExistsEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						for(Map.Entry<String, Token[]> newSubQueryOfInEntry : newSubQueryOfInEntryList)
						{
							String ident = newSubQueryOfInEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						for(Map.Entry<String, Token[]> newSubQueryOfWhereEntry : newSubQueryOfWhereEntryList)
						{
							String ident = newSubQueryOfWhereEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						transMap.put("'" + colName + "'", "#" + (p + 1));
					}
					whereStr = ExpressionTranslator.translateExp(whereStr, transMap);
					this.nodeCursor.addOperation(new Select(new Expression(whereStr), null), ctx);
				}
				else
				{
					String sql = String.format("SELECT * FROM (?) %s", whereStr);
					SimpleSelect ss = new SimpleSelect(ics, ctx);
					ss.setMemory(isMemory || this.isMemoryNode);
					ss.setSQLParameters(this.nodeCursor);
					ss.setSQLParameters(this.nodeStruct);
					ss.setSQLParameters(parameterList);
					if(topFilter != null)
					{
						ss.setTopFilter(topFilter);
					}
					this.nodeCursor = ss.query(sql);
					this.nodeStruct = ss.getDataStruct();
					if(this.isMemoryNode && !(this.nodeCursor instanceof MemoryCursor))
					{
						this.nodeCursor = new MemoryCursor(this.nodeCursor.fetch());
					}
				}
			}
			else
			{
				JoinNode left = getLeft();
				JoinNode right = getRight();
				if(left == null || right == null)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":executeJoin, 某处存在异常的JOIN ON子句");
				}
				
				left.executeJoin();
				right.executeJoin();
				
				String option = null;
				if(this instanceof LeftJoin)
				{
					option = "1";
				}
				else if(this instanceof FullJoin)
				{
					option = "f";
				}
				
				ICursor lCur = left.getCursor();
				DataStruct lDst = left.getStruct();
				ICursor rCur = right.getCursor();
				DataStruct rDst = right.getStruct();
				
				String filter = getFilter();
				if(filter == null || filter.isEmpty())
				{
					filter = "(1=1)";
				}
				
				String whereStr = getWhere();

				List<Map.Entry<String, Token[]>> newSubQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfExistsEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfExistsEntry : subQueryOfExistsEntryList)
					{
						String ident = subQueryOfExistsEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfExistsEntryList.add(subQueryOfExistsEntry);
						}
					}
				}
				
				List<Map.Entry<String, Token[]>> newSubQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfInEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfInEntry : subQueryOfInEntryList)
					{
						String ident = subQueryOfInEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfInEntryList.add(subQueryOfInEntry);
						}
					}
				}
				
				List<Map.Entry<String, Token[]>> newSubQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				if(!whereStr.isEmpty() && !subQueryOfWhereEntryList.isEmpty())
				{
					for(Map.Entry<String, Token[]> subQueryOfWhereEntry : subQueryOfWhereEntryList)
					{
						String ident = subQueryOfWhereEntry.getKey();
						if(whereStr.contains(ident))
						{
							newSubQueryOfWhereEntryList.add(subQueryOfWhereEntry);
						}
					}
				}
				
				Token[] filterTokens = null;
				filterTokens = optimizeWhere(Tokenizer.parse(filter), parameterList);
				filter =  SimpleSelect.scanExp(filterTokens, parameterList);
				//System.out.println("SimpleJoin 1------------"+filter);
				
				Map<String, String> n2cMap = new LinkedHashMap<String, String>();
				for(int i = 0, len = lDst.getFieldNames().length; i < len; i++)
				{
					String fieldName = lDst.getFieldNames()[i];
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					n2cMap.put("'" + fieldName + "'", "L." + "#" + (i + 1));
				}
				for(int j = 0, len = rDst.getFieldNames().length; j < len; j++)
				{
					String fieldName = rDst.getFieldNames()[j];
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					n2cMap.put("'" + fieldName + "'", "~." + "#" + (j + 1));
				}
				
				Map<String, String> trMap = new LinkedHashMap<String, String>();
				trMap.put("L.#", "#");
				trMap.put("~.#", "#");
				
				filter = ExpressionTranslator.translateExp(filter, n2cMap);
				//xingjl 2020/07/23, correct : date('1980-07-19','yyyy-mm-dd')
				filter = filter.replaceAll("'", "\"");
				filter = filter.replaceAll("yyyy-mm-dd", "yyyy-MM-dd");
				//System.out.println("SimpleJoin 2------------"+filter);
				
				ICursor[] curs = new ICursor[]{lCur, rCur};
				DataStruct[] dss = new DataStruct[]{lDst, rDst};
				String[] names = new String[]{"L", "R"};
				Expression[] exps = new Expression[2];
				String[] fps = new String[]{null, null};
				
				exps[0] = new Expression("true");
				exps[1] = new Expression(filter);
				
				if(option != null && option.indexOf("f") != -1)
				{
					String[] subFilters = null;
					try
					{
						subFilters = splitAnd(filter);
					}
					catch(RQException ex)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, FULL JOIN仅支持AND连接", ex);
					}
					
					List<Expression> leftExpList = new ArrayList<Expression>();
					List<Expression> rightExpList = new ArrayList<Expression>();
					for(String subFilter : subFilters)
					{
						String[] subFilterItems = null;
						try
						{
							subFilterItems = splitEqual(subFilter);
						}
						catch(RQException ex)
						{
							subFilterItems = subFilter.split("!=");
							if(subFilterItems.length == 2 && subFilterItems[1].equals("null"))
							{
								continue;
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, FULL JOIN仅支持等值连接", ex);
							}
						}
						
						if(subFilterItems.length != 2)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, FULL JOIN仅支持等值连接");
						}
						
						if(subFilterItems[0].indexOf("~.") != -1 && subFilterItems[0].indexOf("L.") == -1)
						{
							rightExpList.add(new Expression(ExpressionTranslator.translateExp(subFilterItems[0], trMap)));
						}
						else if(subFilterItems[0].indexOf("~.") == -1 && subFilterItems[0].indexOf("L.") != -1)
						{
							leftExpList.add(new Expression(ExpressionTranslator.translateExp(subFilterItems[0], trMap)));
						}
						else if(subFilterItems[0].indexOf("~.") == -1 && subFilterItems[0].indexOf("L.") == -1)
						{
							;
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, FULL JOIN不支持两表字段在同一侧的等值连接");
						}
						
						if(subFilterItems[1].indexOf("~.") != -1 && subFilterItems[1].indexOf("L.") == -1)
						{
							rightExpList.add(new Expression(ExpressionTranslator.translateExp(subFilterItems[1], trMap)));
							if(rightExpList.size() > leftExpList.size())
							{
								leftExpList.add(new Expression(subFilterItems[0]));
							}
						}
						else if(subFilterItems[1].indexOf("~.") == -1 && subFilterItems[1].indexOf("L.") != -1)
						{
							leftExpList.add(new Expression(ExpressionTranslator.translateExp(subFilterItems[1], trMap)));
							if(leftExpList.size() > rightExpList.size())
							{
								rightExpList.add(new Expression(subFilterItems[0]));
							}
						}
						else if(subFilterItems[1].indexOf("~.") == -1 && subFilterItems[1].indexOf("L.") == -1)
						{
							if(leftExpList.size() > rightExpList.size())
							{
								rightExpList.add(new Expression(subFilterItems[1]));
							}
							else if(rightExpList.size() > leftExpList.size())
							{
								leftExpList.add(new Expression(subFilterItems[1]));
							}
							else
							{
								leftExpList.add(new Expression(subFilterItems[0]));
								rightExpList.add(new Expression(subFilterItems[1]));
							}
								
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, FULL JOIN不支持两表字段在同一侧的等值连接");
						}
					}
					
					Expression[][] expss = new Expression[2][];
					expss[0] = new Expression[leftExpList.size()];
					expss[1] = new Expression[rightExpList.size()];
					leftExpList.toArray(expss[0]);
					rightExpList.toArray(expss[1]);
					
					if(curs[0] instanceof MemoryCursor && (curs[1] instanceof MemoryCursor || this.stamp == Memory_Join))
					{
						this.nodeCursor = new SimpleJoinCursor(curs, expss, names, option, ctx, dss);
						this.nodeStruct = ((SimpleJoinCursor)this.nodeCursor).getTableDataStruct();
					}
					else
					{
						this.nodeCursor = new SimpleJoinxCursor(curs, expss, names, option, ctx, dss);
						this.nodeStruct = ((SimpleJoinxCursor)this.nodeCursor).getTableDataStruct();
					}
				}
				else
				{
					if(curs[0] instanceof MemoryCursor && (curs[1] instanceof MemoryCursor || this.stamp == Memory_Join))
					{
						this.nodeCursor = new SimpleXJoinCursor(curs, exps, names, fps, option, ctx, dss);
						this.nodeStruct = ((SimpleXJoinCursor)this.nodeCursor).getTableDataStruct();
					}
					else
					{
						if(curs[1] instanceof IMultipath)
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+"JoinNode.executeJoin, xjoinx功能对于关联维表不支持多路游标");
						}
						this.nodeCursor = new SimpleXJoinxCursor(curs, exps, names, fps, option, ctx, dss);
						this.nodeStruct = ((SimpleXJoinxCursor)this.nodeCursor).getTableDataStruct();
					}
				}
				
				if(!newSubQueryOfExistsEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfExistsEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Exist_Type, this.nodeStruct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
					{
						this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						this.nodeStruct = icur.getDataStruct();
					}
					this.nodeCursor = icur;
				}
				
				if(!newSubQueryOfInEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfInEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.In_Type, this.nodeStruct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
					{
						this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						this.nodeStruct = icur.getDataStruct();
					}
					this.nodeCursor = icur;
				}
				
				if(!newSubQueryOfWhereEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(ics, this.nodeCursor, newSubQueryOfWhereEntryList, parameterList, rootNode.getTableNames(), SubQueryCursor.Where_Type, this.nodeStruct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(this.nodeCursor))
					{
						this.nodeStruct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						this.nodeStruct = icur.getDataStruct();
					}
					this.nodeCursor = icur;
				}
				
				if(!whereStr.isEmpty())
				{
					whereStr = SimpleSelect.scanExp(Tokenizer.parse(whereStr), parameterList);
					Map<String, String> transMap = new LinkedHashMap<String, String>();
					Next:
					for(int p = 0; p < this.nodeStruct.getFieldCount(); p++)
					{
						String colName = this.nodeStruct.getFieldName(p);
						for(Map.Entry<String, Token[]> newSubQueryOfExistsEntry : newSubQueryOfExistsEntryList)
						{
							String ident = newSubQueryOfExistsEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						for(Map.Entry<String, Token[]> newSubQueryOfInEntry : newSubQueryOfInEntryList)
						{
							String ident = newSubQueryOfInEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						for(Map.Entry<String, Token[]> newSubQueryOfWhereEntry : newSubQueryOfWhereEntryList)
						{
							String ident = newSubQueryOfWhereEntry.getKey();
							if(colName.equalsIgnoreCase(ident))
							{
								transMap.put("'" + colName + "'", "#" + (p + 1));
								continue Next;
							}
						}
						transMap.put("'" + colName + "'", "#" + (p + 1));
					}
					whereStr = ExpressionTranslator.translateExp(whereStr, transMap);
					this.nodeCursor.addOperation(new Select(new Expression(whereStr), null), ctx);
				}
			}
		}

		public int getStamp()
		{
			return this.stamp;
		}
		
		public void setStamp(int stamp)
		{
			this.stamp = stamp;
		}
		
		public boolean getParallel()
		{
			return this.parallel;
		}
		
		public void setParallel(boolean parallel)
		{
			this.parallel = parallel;
		}
	}
	
	class JoinTable extends JoinNode
	{
		private JoinTable()
		{
			this.selectFields = new HashSet<String>();
		}
		
		public JoinTable(String file)
		{
			this();
			this.tableFile = file;
		}
		
		public JoinTable(TableMetaData metaData)
		{
			this();
			this.metaData = metaData;
		}
		
		public JoinTable(ICursor cursor, DataStruct struct)
		{
			this();
			this.nodeStruct = struct;
			this.nodeCursor = cursor;
		}
		
		public String getTableFile()
		{
			return this.tableFile;
		}
		
		public void setTableFile(String file)
		{
			this.tableFile = file;
		}
		
		public TableMetaData getTableMetaData()
		{
			return this.metaData;
		}
		
		public void setTableMetaData(TableMetaData metaData)
		{
			this.metaData = metaData;
		}
		
		public void setLeft(JoinNode left) 
		{
			this.nodeLeft = null;
		}

		public void setRight(JoinNode right) 
		{
			this.nodeRight = null;
		}
		
		public int getStamp()
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":JoinTable.getStamp, 表节点没有连接标签");
		}
		
		public void setStamp(int stamp)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":JoinTable.getStamp, 表节点不能设置连接标签");
		}
		
		public boolean isMemoryNode()
		{
			return this.isMemoryNode;
		}
		
		public void setMemoryNode(boolean isMemoryNode)
		{
			this.isMemoryNode = isMemoryNode;
		}
		
		public String getTopFilter()
		{
			return this.topFilter;
		}
		
		public void setTopFilter(String topFilter)
		{
			this.topFilter = topFilter;
		}
	}
	
	class LeftJoin extends JoinNode
	{
		public LeftJoin()
		{
		}
		
		public LeftJoin(JoinNode left, JoinNode right)
		{
			this.nodeLeft = left;
			this.nodeRight = right;
		}
	}
	
	class FullJoin extends JoinNode
	{
		public FullJoin()
		{
		}
		
		public FullJoin(JoinNode left, JoinNode right)
		{
			this.nodeLeft = left;
			this.nodeRight = right;
		}
	}
	
	class InnerJoin extends JoinNode
	{
		public InnerJoin()
		{
		}
		
		public InnerJoin(JoinNode left, JoinNode right)
		{
			this.nodeLeft = left;
			this.nodeRight = right;
		}
	}
	
	class SimpleXJoinxCursor extends XJoinxCursor
	{
		private DataStruct tabDs;
		private DataStruct ds1, ds2, ds;
		
		public SimpleXJoinxCursor(ICursor[] cursors, Expression[] exps, String[] names, String[] fltOpts, String opt, Context ctx, DataStruct[] dss) 
		{
			super(cursors, exps, names, fltOpts, opt, ctx);
			
			this.ds1 = dss[0];
			this.ds2 = dss[1];
			
			int l = (ds1 == null ? 0 : ds1.getFieldCount());
			int r = (ds2 == null ? 0 : ds2.getFieldCount());
			
			List<String> nameList = new ArrayList<String>();
			for(int i = 0; i< l + r; i++)
			{
				if(i < l)
				{
					nameList.add(ds1.getFieldName(i));
				}
				else
				{
					nameList.add(ds2.getFieldName(i - l));
				}
			}
			
			String[] colNames = new String[nameList.size()];
			nameList.toArray(colNames);
			
			this.tabDs = new DataStruct(colNames);
		}
		
		protected Sequence get(int n) 
		{
			if(this.tabDs == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 必须预先设置数据结构");
			}
			
			Sequence tab = super.get(n);
			if(tab != null)
			{
				Table res = new Table(this.tabDs);
				int l = this.ds1.getFieldCount();
				int r = this.ds2.getFieldCount();
				List<Object> valueList = new ArrayList<Object>(l+r);
				
				for(int i=1, x=tab.getMems().size(); i<=x; i++)
				{
					Record rd = new Record(this.tabDs);
					Object obj1 = tab.getMems().get(i);
					if(obj1 instanceof Record)
					{
						Record rd1 = (Record) obj1;
						for(int j=0, y=2; j<y; j++)
						{
							Object obj2 = rd1.getFieldValue(j);
							if(obj2 instanceof Record)
							{
								Record rd2 = (Record) obj2;
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = rd2.getFieldValue(k);
									valueList.add(obj);
								}
							}
							else if(obj2 == null) //left join or full join
							{
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = null;
									valueList.add(obj);
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 表中存在未知的数据类型II");
							}
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 表中存在未知的数据类型I");
					}
					
					for(int m=0, len=valueList.size(); m<len; m++)
					{
						rd.set(m, valueList.get(m));
					}
					
					valueList.clear();
					
					res.add(rd);
				}
				
				tab = res;
			}
			
			return tab;
		}
		
		public DataStruct getDataStruct()
		{
			return this.ds;
		}
		
		public void setDataStruct(DataStruct ds)
		{
			super.setDataStruct(ds);
			this.ds = ds;
		}
		
		public DataStruct getTableDataStruct()
		{
			return this.tabDs;
		}
	}
	
	class SimpleXJoinCursor extends MemoryCursor
	{
		private DataStruct tabDs;
		private DataStruct ds1, ds2, ds;
		
		public SimpleXJoinCursor(ICursor[] cursors, Expression[] exps, String[] names, String[] fltOpts, String opt,	Context ctx, DataStruct[] dss) 
		{
			super(Sequence.xjoin(new Sequence[]{cursors[0].fetch(), cursors[1].fetch()}, exps, fltOpts, names, opt, ctx));
			
			this.ds1 = dss[0];
			this.ds2 = dss[1];
			
			int l = (this.ds1 == null ? 0 : this.ds1.getFieldCount());
			int r = (this.ds2 == null ? 0 : this.ds2.getFieldCount());
			
			List<String> nameList = new ArrayList<String>();
			for(int i = 0; i< l + r; i++)
			{
				if(i < l)
				{
					nameList.add(this.ds1.getFieldName(i));
				}
				else
				{
					nameList.add(this.ds2.getFieldName(i - l));
				}
			}
			
			String[] colNames = new String[nameList.size()];
			nameList.toArray(colNames);
			
			this.tabDs = new DataStruct(colNames);
		}
		
		protected Sequence get(int n) 
		{
			if(this.tabDs == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinCursor.get, 必须预先设置数据结构");
			}
			
			Sequence tab = super.get(n);
			if(tab != null)
			{
				Table res = new Table(this.tabDs);
				int l = this.ds1.getFieldCount();
				int r = this.ds2.getFieldCount();
				List<Object> valueList = new ArrayList<Object>(l+r);
				
				for(int i=1, x=tab.getMems().size(); i<=x; i++)
				{
					Record rd = new Record(this.tabDs);
					Object obj1 = tab.getMems().get(i);
					if(obj1 instanceof Record)
					{
						Record rd1 = (Record) obj1;
						for(int j=0, y=2; j<y; j++)
						{
							Object obj2 = rd1.getFieldValue(j);
							if(obj2 instanceof Record)
							{
								Record rd2 = (Record) obj2;
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = rd2.getFieldValue(k);
									valueList.add(obj);
								}
							}
							else if(obj2 == null) //left join or full join
							{
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = null;
									valueList.add(obj);
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinCursor.get, 表中存在未知的数据类型II");
							}
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinCursor.get, 表中存在未知的数据类型I");
					}
					
					for(int m=0, len=valueList.size(); m<len; m++)
					{
						rd.set(m, valueList.get(m));
					}
					
					valueList.clear();
					
					res.add(rd);
				}
				
				tab = res;
			}
			
			return tab;
		}
		
		public DataStruct getDataStruct()
		{
			return this.ds;
		}
		
		public void setDataStruct(DataStruct ds)
		{
			super.setDataStruct(ds);
			this.ds = ds;
		}
		
		public DataStruct getTableDataStruct()
		{
			return this.tabDs;
		}
	}
	
	class SimpleJoinxCursor extends JoinxCursor
	{
		private DataStruct tabDs;
		private DataStruct ds1, ds2, ds;
		
		public SimpleJoinxCursor(ICursor[] cursors, Expression[][] expss, String[] names, String opt, Context ctx, DataStruct[] dss) 
		{
			super(cursors, expss, names, opt, ctx);
			
			this.ds1 = dss[0];
			this.ds2 = dss[1];
			
			int l = (ds1 == null ? 0 : ds1.getFieldCount());
			int r = (ds2 == null ? 0 : ds2.getFieldCount());
			
			List<String> nameList = new ArrayList<String>();
			for(int i = 0; i< l + r; i++)
			{
				if(i < l)
				{
					nameList.add(ds1.getFieldName(i));
				}
				else
				{
					nameList.add(ds2.getFieldName(i - l));
				}
			}
			
			String[] colNames = new String[nameList.size()];
			nameList.toArray(colNames);
			
			this.tabDs = new DataStruct(colNames);
		}
		
		protected Sequence get(int n) 
		{
			if(this.tabDs == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 必须预先设置数据结构");
			}
			
			Sequence tab = super.get(n);
			if(tab != null)
			{
				Table res = new Table(this.tabDs);
				int l = this.ds1.getFieldCount();
				int r = this.ds2.getFieldCount();
				List<Object> valueList = new ArrayList<Object>(l+r);
				
				for(int i=1, x=tab.getMems().size(); i<=x; i++)
				{
					Record rd = new Record(this.tabDs);
					Object obj1 = tab.getMems().get(i);
					if(obj1 instanceof Record)
					{
						Record rd1 = (Record) obj1;
						for(int j=0, y=2; j<y; j++)
						{
							Object obj2 = rd1.getFieldValue(j);
							if(obj2 instanceof Record)
							{
								Record rd2 = (Record) obj2;
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = rd2.getFieldValue(k);
									valueList.add(obj);
								}
							}
							else if(obj2 == null) //left join or full join
							{
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = null;
									valueList.add(obj);
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 表中存在未知的数据类型II");
							}
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":SimpleXJoinxCursor.get, 表中存在未知的数据类型I");
					}
					
					for(int m=0, len=valueList.size(); m<len; m++)
					{
						rd.set(m, valueList.get(m));
					}
					
					valueList.clear();
					
					res.add(rd);
				}
				
				tab = res;
			}
			
			return tab;
		}
		
		public DataStruct getDataStruct()
		{
			return this.ds;
		}
		
		public void setDataStruct(DataStruct ds)
		{
			super.setDataStruct(ds);
			this.ds = ds;
		}
		
		public DataStruct getTableDataStruct()
		{
			return this.tabDs;
		}
	}
	
	class SimpleJoinCursor extends MemoryCursor
	{
		private DataStruct tabDs;
		private DataStruct ds1, ds2, ds;
		
		public SimpleJoinCursor(ICursor[] cursors, Expression[][] expss, String[] names, String opt, Context ctx, DataStruct[] dss) 
		{
			super(Sequence.join(new Sequence[]{cursors[0].fetch(), cursors[1].fetch()}, expss, names, opt, ctx));
			
			this.ds1 = dss[0];
			this.ds2 = dss[1];
			
			int l = (this.ds1 == null ? 0 : this.ds1.getFieldCount());
			int r = (this.ds2 == null ? 0 : this.ds2.getFieldCount());
			
			List<String> nameList = new ArrayList<String>();
			for(int i = 0; i< l + r; i++)
			{
				if(i < l)
				{
					nameList.add(this.ds1.getFieldName(i));
				}
				else
				{
					nameList.add(this.ds2.getFieldName(i - l));
				}
			}
			
			String[] colNames = new String[nameList.size()];
			nameList.toArray(colNames);
			
			this.tabDs = new DataStruct(colNames);
		}
		
		protected Sequence get(int n) 
		{
			if(this.tabDs == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":SimpleJoinCursor.get, 必须预先设置数据结构");
			}
			
			Sequence tab = super.get(n);
			if(tab != null)
			{
				Table res = new Table(this.tabDs);
				int l = this.ds1.getFieldCount();
				int r = this.ds2.getFieldCount();
				List<Object> valueList = new ArrayList<Object>(l+r);
				
				for(int i=1, x=tab.getMems().size(); i<=x; i++)
				{
					Record rd = new Record(this.tabDs);
					Object obj1 = tab.getMems().get(i);
					if(obj1 instanceof Record)
					{
						Record rd1 = (Record) obj1;
						for(int j=0, y=2; j<y; j++)
						{
							Object obj2 = rd1.getFieldValue(j);
							if(obj2 instanceof Record)
							{
								Record rd2 = (Record) obj2;
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = rd2.getFieldValue(k);
									valueList.add(obj);
								}
							}
							else if(obj2 == null) //left join or full join
							{
								for(int k=0, z=(j==0?l:r); k<z; k++)
								{
									Object obj = null;
									valueList.add(obj);
								}
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":SimpleJoinCursor.get, 表中存在未知的数据类型II");
							}
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":SimpleJoinCursor.get, 表中存在未知的数据类型I");
					}
					
					for(int m=0, len=valueList.size(); m<len; m++)
					{
						rd.set(m, valueList.get(m));
					}
					
					valueList.clear();
					
					res.add(rd);
				}
				
				tab = res;
			}
			
			return tab;
		}
		
		public DataStruct getDataStruct()
		{
			return this.ds;
		}
		
		public void setDataStruct(DataStruct ds)
		{
			super.setDataStruct(ds);
			this.ds = ds;
		}
		
		public DataStruct getTableDataStruct()
		{
			return this.tabDs;
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
						if(!(obj instanceof Record))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, 查询结果序列必须由记录组成");
						}
						Record rec = (Record) obj;
						
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
								if(sq.length() != 1 || !(sq.get(1) instanceof Record) || sq.dataStruct() == null || sq.dataStruct().getFieldCount() != 1)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, SELECT/WHERE子句中子查询结果异常");
								}
								val = ((Record)sq.get(1)).getFieldValue(0);
							}
							else if(this.type == SubQueryCursor.In_Type)
							{
								Sequence v = new Sequence();
								for(int p = 1, q = sq.length(); p <= q; p++)
								{
									if(!(sq.get(1) instanceof Record) || sq.dataStruct() == null || sq.dataStruct().getFieldCount() != 1)
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":SubQueryCursor.get, IN子句中子查询结果异常");
									}
									v.add(((Record)sq.get(p)).getFieldValue(0));
								}
								val = v;
							}
						}
						
						Record newRec = new Record(struct);
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
	
	public SimpleJoin(ICellSet ics, Context ctx)
	{
		this.icur = null;
		this.ds = null;
		this.ctx = ctx;
		this.ics = ics;
		init();
	}
	
	private void init()
	{
		this.parameterList = null;
		if(this.ctx == null)
		{
			this.ctx = new Context();
		}
		this.whereList = new HashSet<String>();
		this.selectList = new HashSet<String>();
		this.recycleList = new ArrayList<String>();
		this.intoFileName = "";
		this.singleTable = false;
		this.withTableMap = new HashMap<String, JoinTable>();
		this.isMemory = false;
		this.parallelNumber = 1;
		this.subQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfSelectEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
		this.subQueryOfExistsMap = new HashMap<String, String>();
		this.subQueryOfSelectMap = new HashMap<String, String>();
		this.subQueryOfInMap = new HashMap<String, String>();
		this.subQueryOfWhereMap = new HashMap<String, String>();
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
	
	private int pos(int p1, int p2, int p3, int p4, int p5, int p6, int p7, int def) 
	{
		if (p1 > 0) return p1;
		if (p2 > 0) return p2;
		if (p3 > 0) return p3;
		if (p4 > 0) return p4;
		if (p5 > 0) return p5;
		if (p6 > 0) return p6;
		if (p7 > 0) return p7;
		return def;
	}
	
	public void setSQLParameters(List<Object> paramList)
	{
		this.parameterList = paramList;
	}
	
	private JoinTable scanTable(Token []tokens, int start, int next) 
	{
		String tableName = null;
		String aliasName = null;
		JoinTable node = null;
		if (start < next) 
		{
			int pos = start + 1;
			if(tokens[start].getType() == Tokenizer.KEYWORD)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanTable, 表名缺失或类型有误");
			}
			else if(tokens[start].getType() == Tokenizer.LPAREN)
			{
				tableName = "";
				int end = Tokenizer.scanParen(tokens, start, next);
				String[] keywords = new String[]{"SELECT", "WITH", "UNION", "INTERSECT", "EXCEPT", "MINUS"};
				int keyPos = Tokenizer.scanKeyWords(keywords, tokens, start+1, end);
				boolean isSubQuery = ((keyPos < 0) ? false : true);
				if(isSubQuery)
				{
					Token[] subTokens = Arrays.copyOfRange(tokens, pos, end);
					SimpleSQL lq = new SimpleSQL(this.ics, subTokens, 0, subTokens.length, parameterList, ctx, false);
					lq.setMemory(this.isMemory);
					ICursor icur = lq.query();
					DataStruct ds = lq.getDataStruct();
					node = new JoinTable(icur, ds);
					if(end+1 != next)
					{
						if(tokens[end+1].isKeyWord("AS"))
						{
							if(end+2 == next || tokens[end+2].getType() != Tokenizer.IDENT)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error"));
							}
							aliasName = tokens[end+2].getString();
						}
						else if(tokens[end+1].getType() == Tokenizer.IDENT)
						{
							aliasName = tokens[end+1].getString();
						}
					} 
				}
			}
			else
			{
				tableName = "";
				pos = start;
				while(pos < next)
				{
					tableName = tableName + tokens[pos].getOriginString();
					tableName = tableName + tokens[pos].getSpaces();
					pos++;
				}
				tableName = tableName.trim();
				if (next - 2 >= start && tokens[next - 1].getType() == Tokenizer.IDENT)
				{
					int splitPos = tableName.lastIndexOf(" ");
					if(splitPos != -1)
					{
						aliasName = tableName.substring(splitPos + 1);
						if(aliasName.equals(tokens[next - 1].getOriginString()))
						{
							tableName = tableName.substring(0, splitPos).trim();
						}
						else
						{
							aliasName = null;
						}
					}
					
					if(next - 3 >= start && tokens[next - 2].isKeyWord("AS"))
					{
						splitPos = tableName.lastIndexOf(" ");
						if(splitPos != -1)
						{
							String asKeyWord = tableName.substring(splitPos + 1);
							if(asKeyWord.equals(tokens[next - 2].getOriginString()))
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
			}
		}
		
		if(aliasName == null)
		{
			Token[] tableTokens = Tokenizer.parse(tableName);
			if(tableTokens.length == 1 && tableTokens[0].getType() == Tokenizer.IDENT)
			{
				aliasName = tableName;
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanTable, 每个拼接表都必须有一个标准的表名或别名");
			}
		}
		if(!tableName.isEmpty())
		{
			FileObject fileObject = new FileObject(tableName, null, "s", ctx);
			if(!fileObject.isExists()) //可能有密码，尝试去除
			{
				int index = tableName.lastIndexOf(":");
				if(index != -1)
				{
					fileObject = new FileObject(tableName.substring(0, index).trim(), null, "s", this.ctx);
				}
			}
			if(!fileObject.isExists())
			{
				node = this.withTableMap.get(tableName.toLowerCase());
				if(node == null)
				{
					Object obj = null;
					try
					{
						obj = new Expression(this.ics, this.ctx, tableName).calculate(this.ctx);
					}
					catch(Exception ex)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanTable, 异常的表名:"+tableName);
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
						node = new JoinTable(cursor, struct);
					}
					else if(obj instanceof Table)
					{
						ICursor cursor = new MemoryCursor((Table)obj);
						DataStruct struct = ((Table)obj).dataStruct();
						node = new JoinTable(cursor, struct);
					}
					else if(obj instanceof TableMetaData)
					{
						node = new JoinTable((TableMetaData)obj);
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanTable, 不支持的临时表数据类型");
					}
				}
			}
			else
			{
				node =  new JoinTable(tableName);
			}
		}
		node.addTableName(aliasName);
		return node;
	}
	
	private JoinNode scanNode(Token []tokens, int start, int next, boolean check) 
	{
		int count = 0;
		
		JoinTable table = null;
		JoinNode root = null;

		while (start < next) 
		{
			if (start == next) 
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanNode, 起始位置超出结束位置");
			}
			
			int end = Tokenizer.scanComma(tokens, start, next);
			if (end < 0)
			{
				end = next;
			}

			table = scanTable(tokens, start, end);
			start = end + 1;
			
			count++;
			if(count > 1)
			{
				if(check)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanJoin, FROM或JOIN关键字后的表的数量不正确");
				}
				
				JoinNode node = new InnerJoin();
				node.setStamp(Memory_Join);
				
				node.setLeft(root);
				root.setParent(node);
				
				node.setRight(table);
				table.setParent(node);
				table.setMemoryNode(true);
				
				root = node;
			}
			else
			{
				root = table;
			}
		}
		
		return root;
	}
	
	private JoinNode scanJoin(Token []tokens, int start, int next, boolean hide)
	{
		JoinNode root = null;
		JoinNode node = null;
		if(hide)
		{
			root = scanNode(tokens, start, next, false);
		}
		else
		{	
			int begin = start;
			int stamp = Memory_Join;
			int offset = 0;
			StringBuffer filter = null;
			for(int index = start; index <= next; index++)
			{
				if(index == next)
				{
					if(filter != null)
					{
						root.setFilter(filter.toString());
						filter = null;
					}
					break;
				}
				else if(tokens[index].getString().equals("{") && filter == null)
				{
					int end = Tokenizer.scanBrace(tokens, index, next);
					Token[] expTokens = Arrays.copyOfRange(tokens, index + 1, end);
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
						obj = new Expression(this.ics, this.ctx, expStr).calculate(this.ctx);
					}
					catch(Exception ex)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 异常的表名:"+expStr);
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
						node = new JoinTable(cursor, struct);
					}
					else if(obj instanceof Table)
					{
						ICursor cursor = new MemoryCursor((Table)obj);
						DataStruct struct = ((Table)obj).dataStruct();
						node = new JoinTable(cursor, struct);
					}
					else if(obj instanceof TableMetaData)
					{
						node = new JoinTable((TableMetaData)obj);
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 不支持的临时表数据类型");
					}
					if(end + 1 != next)
					{
						if(tokens[end + 1].isKeyWord("AS"))
						{
							if(end + 2 == next || tokens[end + 2].getType() != Tokenizer.IDENT)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error"));
							}
							node.addTableName(tokens[end + 2].getString());
						}
						else if(tokens[end + 1].getType() == Tokenizer.IDENT)
						{
							node.addTableName(tokens[end + 1].getString());
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error"));
					}
					if(root == null)
					{
						root = node;
					}
					index = end;
					begin = end + 1;
					continue;
				}
				else if(tokens[index].getType() == Tokenizer.LPAREN && filter == null)
				{
					int end = Tokenizer.scanParen(tokens, index, next);
					String[] keywords = new String[]{"SELECT", "WITH", "UNION", "INTERSECT", "EXCEPT", "MINUS"};
					int keyPos = Tokenizer.scanKeyWords(keywords, tokens, index + 1, end);
					boolean isSubQuery = ((keyPos < 0) ? false : true);
					if(isSubQuery)
					{
						Token[] subTokens = Arrays.copyOfRange(tokens, index + 1, end);
						SimpleSQL lq = new SimpleSQL(this.ics, subTokens, 0, subTokens.length, parameterList, ctx, false);
						lq.setMemory(this.isMemory);
						ICursor icur = lq.query();
						DataStruct ds = lq.getDataStruct();
						node = new JoinTable(icur, ds);
					}
					else
					{
						node = scanJoin(tokens, index + 1, end, false);
					}
					if(end + 1 != next)
					{
						if(tokens[end + 1].isKeyWord("AS"))
						{
							if(end + 2 == next || tokens[end + 2].getType() != Tokenizer.IDENT)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error"));
							}
							node.addTableName(tokens[end + 2].getString());
						}
						else if(tokens[end+1].getType() == Tokenizer.IDENT)
						{
							node.addTableName(tokens[end + 1].getString());
						}
					}
					if(root == null)
					{
						root = node;
					}
					index = end;
					begin = end + 1;
					continue;
				}
				else if(index + 5 < next
				&& tokens[index + 3].isKeyWord("EXTERNAL")
				&& tokens[index].equals("/") && tokens[index + 1].equals("*") && tokens[index + 2].equals("+") && tokens[index + 4].equals("*") && tokens[index + 5].equals("/"))
				{
					if(tokens[index + 3].isKeyWord("EXTERNAL")) // && Sequence.getFunctionPoint(15)
					{
						stamp = External_Join; //简单SQL的/*+EXTERNAL*/标签仅供内部测试
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error"));
					}
					offset = offset + 6;
					index = index + 5;
					continue;
				}
				else if(index + 1 < next && tokens[index].isKeyWord("LEFT") && tokens[index + 1].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new LeftJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 2;
					index++;
					
					node = null;
					continue;
				}
				else if(index + 2 < next && tokens[index].isKeyWord("LEFT") && tokens[index + 1].isKeyWord("OUTER") && tokens[index + 2].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new LeftJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 3;
					index += 2;
					
					node = null;
					continue;
				}
				else if(index + 1 < next && tokens[index].isKeyWord("FULL") && tokens[index + 1].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new FullJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 2;
					index++;
					
					node = null;
					continue;
				}
				else if(index + 2 < next && tokens[index].isKeyWord("FULL") && tokens[index + 1].isKeyWord("OUTER") && tokens[index + 2].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new FullJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 3;
					index += 2;
					
					node = null;
					continue;
				}
				else if(tokens[index].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new InnerJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 1;
					
					node = null;
					continue;
				}
				else if(index + 1 < next && tokens[index].isKeyWord("INNER") && tokens[index + 1].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new InnerJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setLeft(root);
					root = node;
					
					begin = index + 2;
					index++;
					
					node = null;
					continue;
				}
				else if(index + 1 < next && tokens[index].isKeyWord("RIGHT") && tokens[index + 1].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new LeftJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setRight(root);
					root = node;
					
					begin = index + 2;
					index++;
					
					node = null;
					continue;
				}
				else if(index + 2 < next && tokens[index].isKeyWord("RIGHT") && tokens[index + 1].isKeyWord("OUTER") && tokens[index + 2].isKeyWord("JOIN"))
				{
					if(root == null)
					{
						root = scanNode(tokens, begin, index - offset, true);
						offset = 0;
					}
					else
					{
						if(filter != null)
						{
							root.setFilter(filter.toString());
							filter = null;
						}
					}
					node = new LeftJoin();
					node.setStamp(stamp);
					stamp = Memory_Join;
					
					root.setParent(node);
					node.setRight(root);
					root = node;
					
					begin = index + 3;
					index += 2;
					
					node = null;
					continue;
				}
				else if(tokens[index].isKeyWord("ON"))
				{
					if(node == null)
					{
						node = scanNode(tokens, begin, index, true);
					}
					
					if(root != null)
					{
						if(node != null)
						{
							node.setParent(root);
							if(root.getRight() == null)
							{
								root.setRight(node);
							}
							else if(root.getLeft() == null)
							{
								root.setLeft(node);
							}
							else
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error"));
							}
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error"));
						}
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error"));
					}
					
					filter = new StringBuffer();
					node = null;
					continue;
				}
				
				if(filter != null)
				{
					filter.append(tokens[index].getOriginString());
					filter.append(tokens[index].getSpaces());
				}
			}
		}
		
		JoinNode point = root;
		while(point.getLeft() != null)
		{
			point = point.getLeft();
		}
		
		if(point instanceof JoinTable)
		{
			this.fromTable = (JoinTable)point;
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error"));
		}
		
		setMemoryNodes(root);
		setParallelNodes(root, null);

		return root;
	}
	
	private void setMemoryNodes(JoinNode node)
	{
		if(!(node instanceof JoinTable) && node.getStamp() == Memory_Join)
		{
			JoinNode left = node.getLeft();
			if(left instanceof JoinTable)
			{
				if(!left.equals(this.fromTable))
				{
					((JoinTable)left).setMemoryNode(true);
				}
				else
				{
					((JoinTable)left).setMemoryNode(false);
				}
			}
			else if(left != null)
			{
				setMemoryNodes(left);
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error"));
			}
			
			JoinNode right = node.getRight();
			if(right instanceof JoinTable)
			{
				if(!right.equals(this.fromTable))
				{
					((JoinTable) right).setMemoryNode(true);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error"));
				}
			}
			else if(right != null)
			{
				setMemoryNodes(right);
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error"));
			}
		}
	}
	
	private void setParallelNodes(JoinNode node, Integer isAllParallel)
	{
		if(isAllParallel == null)
		{
			isAllParallel = checkAllParallel(node);
		}
		
		if(node instanceof JoinTable)
		{
			if(isAllParallel == 1)
			{
				node.setParallel(true);
			}
			else if(isAllParallel == 0 && node.equals(this.fromTable))
			{
				node.setParallel(true);
			}
			else
			{
				node.setParallel(false);
			}
		}
		else if(node != null)
		{
			setParallelNodes(node.getLeft(), isAllParallel);
			setParallelNodes(node.getRight(), isAllParallel);
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error"));
		}
	}
	
	private int checkAllParallel(JoinNode node) //是否多线程取数由本程序决定
	{
		if(node instanceof JoinTable)
		{
			return 1;
		}
		else if(node != null)
		{
			if(node.getStamp() == External_Join)
			{
				return 0;
			}
			else
			{
				if(checkAllParallel(node.getLeft()) < 0 || checkAllParallel(node.getRight()) < 0)
				{
					return -1;
				}
				else
				{
					return checkAllParallel(node.getLeft()) * checkAllParallel(node.getRight());
				}
			}
		}
		else
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error"));
		}
	}
	
	private List<Token[]> scanFrom(Token []tokens, int start, int next, boolean hasJoin)
	{
		int fromPos = Tokenizer.scanKeyWord("FROM", tokens, start, next);
		if(fromPos < 0 || fromPos == start)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, FROM关键字缺失或位置不正确");
		}

		int intoPos = Tokenizer.scanKeyWord("INTO", tokens, start, next);
		if(intoPos >= fromPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, INTO关键字的位置不正确");
		}
		else if(intoPos >= 0)
		{
			ArrayList<Token> tempList = new ArrayList<Token>();
			for(int i=start; i<next; i++)
			{
				if(i > intoPos && i < fromPos)
				{
					this.intoFileName += tokens[i].getOriginString();
					this.intoFileName += tokens[i].getSpaces();
				}
				else if(i < intoPos || i >= fromPos)
				{
					tempList.add(tokens[i]);
				}
			}
			
			next = next - (fromPos - intoPos); 
			fromPos = intoPos;
			
			tokens = new Token[tempList.size()]; 
			tempList.toArray(tokens);
		}

		int minPos = fromPos;

		int wherePos = Tokenizer.scanKeyWord("WHERE", tokens, start, next);
		if(wherePos >= 0 && wherePos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, WHERE关键字的位置不正确");
		}
		
		minPos = pos(wherePos, minPos);
		
		int groupPos = Tokenizer.scanKeyWord("GROUP", tokens, start, next);
		if(groupPos >= 0 && groupPos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, GROUP关键字的位置不正确");
		}
		
		minPos = pos(groupPos , minPos);
		
		int havingPos = Tokenizer.scanKeyWord("HAVING", tokens, start, next);
		if(havingPos >= 0 && havingPos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, HAVING关键字的位置不正确");
		}
		
		minPos = pos(havingPos , minPos);

		int orderPos = Tokenizer.scanKeyWord("ORDER", tokens, start, next);
		if(orderPos >= 0 && orderPos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, ORDER关键字的位置不正确");
		}
		
		minPos = pos(orderPos , minPos);
		
		int limitPos = Tokenizer.scanKeyWord("LIMIT", tokens, start, next);
		if(limitPos >= 0 && limitPos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, LIMIT关键字的位置不正确");
		}
		
		minPos = pos(limitPos , minPos);
		
		int offsetPos = Tokenizer.scanKeyWord("OFFSET", tokens, start, next);
		if(offsetPos >= 0 && offsetPos < minPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, OFFSET关键字的位置不正确");
		}
		
		int byStart = pos(groupPos, wherePos, fromPos);
		int byEnd = pos(havingPos, orderPos, limitPos, offsetPos, next);
		
		int byPos = Tokenizer.scanKeyWord("BY", tokens, byStart, byEnd);
		if(byPos < 0 && groupPos > 0)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 关键字GROUP后面缺少关键字BY");
		}
		else if(havingPos > 0 && byPos > havingPos)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanFrom, 分组关键字BY位置应该在HAVING之前");
		}
		
		int fromEnd = pos(wherePos, groupPos, byPos, havingPos, orderPos, limitPos, offsetPos, next);
		
		if(!hasJoin) //不存在JOIN，检查是否是隐式内连接
		{
			int commaPos = Tokenizer.scanComma(tokens, fromPos, fromEnd);
			if(commaPos == -1) //单表
			{
				List<Token[]> resultList = new ArrayList<Token[]>();
				resultList.add(tokens);
				this.singleTable = true;
				return resultList;
			}
		}
		
		boolean hide = !checkJoinOn(tokens, fromPos+1, fromEnd);
		
		this.rootNode = scanJoin(tokens, fromPos+1, fromEnd, hide);
		
		if(wherePos >= 0)
		{
			int whereEnd = pos(groupPos, byPos, havingPos, orderPos, next);
			boolean fromBetween = false;
			StringBuffer sb = new StringBuffer();
			for(int i = wherePos; i < whereEnd; i++)
			{
				if(tokens[i].isKeyWord("WHERE"))
				{
					continue;
				}
				else if(tokens[i].getType() == Tokenizer.LPAREN)
				{
					int j = Tokenizer.scanParen(tokens, i, whereEnd);
					for(int k=i; k<=j; k++)
					{
						sb.append(tokens[k].getOriginString());
						sb.append(tokens[k].getSpaces());
					}
					i = j;
				}
				else if(tokens[i].isKeyWord("OR"))
				{
					this.whereList.clear();
					break;
				}
				else if(tokens[i].isKeyWord("BETWEEN"))
				{
					fromBetween = true;
					sb.append(tokens[i].getOriginString());
					sb.append(tokens[i].getSpaces());
				}
				else if(tokens[i].isKeyWord("AND"))
				{
					if(fromBetween)
					{
						fromBetween = false;
						sb.append(tokens[i].getOriginString());
						sb.append(tokens[i].getSpaces());
					}
					else
					{
						this.whereList.add(sb.toString());
						sb = new StringBuffer();
					}
				}
				else
				{
					sb.append(tokens[i].getOriginString());
					sb.append(tokens[i].getSpaces());
				}
				
				if(i == whereEnd-1)
				{
					this.whereList.add(sb.toString());
				}
			}
			
			//where子句是纯and连接，可以提前处理
			if(this.whereList.size() != 0)
			{
				fromEnd = whereEnd;
			}
		}
		
		//去除已分析完毕的into\from\join\on\where(全and连接时)的子句
		List<Token[]> tokensList = new ArrayList<Token[]>();
		List<Token> tokenList = new ArrayList<Token>();
		for(int i=0, len=tokens.length; i<len; i++)
		{
			if(i <= fromPos)
			{
				if(i == fromPos)
				{
					Token[] newTokens = new Token[tokenList.size()];
					tokenList.toArray(newTokens);
					tokensList.add(newTokens);
					tokenList.clear();
				}
				else
				{
					tokenList.add(tokens[i]);
				}
			}
			else if(i >= fromEnd)
			{
				if((tokens[i].isKeyWord("BY") && !tokens[i-1].isKeyWord("GROUP") && !tokens[i-1].isKeyWord("ORDER"))
				|| tokens[i].isKeyWord("GROUP") || tokens[i].isKeyWord("HAVING") || tokens[i].isKeyWord("ORDER"))
				{
					if(tokenList.size() != 0)
					{
						Token[] newTokens = new Token[tokenList.size()];
						tokenList.toArray(newTokens);
						tokensList.add(newTokens);
						tokenList.clear();
					}
				}
				
				tokenList.add(tokens[i]);
				
				if(i == len - 1)
				{
					Token[] newTokens = new Token[tokenList.size()];
					tokenList.toArray(newTokens);
					tokensList.add(newTokens);
					tokenList.clear();
				}
			}
		}

		return tokensList;
	}
	
	private int scanQuantifies(Token []tokens, int start, int next) //只跳过不执行功能
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
				t = tokens[c];
			}

			if(t.isKeyWord() && t.getString().equalsIgnoreCase("TOP")) 
			{
				c += 2;
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
	
	private void scanSelect(List<Token[]> tokensList)
	{
		for(Token[] tokens : tokensList)
		{
			boolean belongSelect = false;
			for(int i = 0, len = tokens.length; i < len; i++)
			{
				if(tokens[i].isKeyWord("SELECT"))
				{
					int start = i + 1;
					start = scanQuantifies(tokens, start, len);
					i = start;
					belongSelect = true;
				}
				
				if(tokens.length == 2 && tokens[i].getString().equalsIgnoreCase("*"))
				{
					selectList.clear();
					selectList.add("*");
					return;
				}
				else if(tokens[i].getType() == Tokenizer.IDENT)
				{
					if(i+1 != len && tokens[i].getString().equalsIgnoreCase("COUNT") && tokens[i+1].getType() == Tokenizer.LPAREN)
					{
						int end = Tokenizer.scanParen(tokens, i + 1, len);
						if(end == i + 3)
						{
							if(tokens[i + 2].getString().equals("*"))
							{
								selectList.add("COUNT");
								i = end;
							}
						}
						else if(end == i + 5)
						{
							if(tokens[i + 2].getType() == Tokenizer.IDENT && tokens[i + 3].getType() == Tokenizer.DOT && tokens[i + 4].getString().equals("*"))
							{
								String oldName = tokens[i+2].getString() + tokens[i+3].getString() + "COUNT";
								selectList.add(oldName);
								i = end;
							}
						}
					}
					else if(i + 1 != len && tokens[i + 1].getType() == Tokenizer.DOT)
					{
						if(i + 2 != len)
						{
							if(tokens[i + 2].getType() == Tokenizer.IDENT && Tokenizer.isGatherFunction(tokens[i + 2].getString()) && tokens[i + 3].getType() == Tokenizer.LPAREN)
							{
								if(i + 3 != len)
								{
									int end = Tokenizer.scanParen(tokens, i+3, len);
									if(end == i + 5)
									{
										String oldName = null;
										if(tokens[i + 2].getString().equalsIgnoreCase("COUNT") && tokens[i + 4].getString().equals("*"))
										{
											oldName = tokens[i].getString() + tokens[i+1].getString() + "COUNT";
										}
										else
										{
											oldName = tokens[i].getString() + tokens[i+1].getString() + tokens[i+4].getString();
										}
										selectList.add(oldName);
										i = end;
									}
									else if(end == i + 7)
									{
										if(tokens[i + 4].getType() == Tokenizer.IDENT && tokens[i + 5].getType() == Tokenizer.DOT)
										{
											if(!tokens[i].getString().equalsIgnoreCase(tokens[i + 4].getString()))
											{
												MessageManager mm = ParseMessage.get();
												throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, 聚合函数所属表必须与聚合字段所属表一致");
											}
											else
											{
												String oldName = null;
												if(tokens[i + 2].getString().equalsIgnoreCase("COUNT") && tokens[i + 6].getString().equals("*"))
												{
													oldName = tokens[i].getString() + tokens[i + 1].getString() + "COUNT";
												}
												else
												{
													oldName = tokens[i].getString() + tokens[i + 1].getString() + tokens[i + 6].getString();
												}
												selectList.add(oldName);
												i = end;
											}
										}
									}
								}
							}
							else if(tokens[i + 2].getString().equals("*"))
							{
								String tableName = tokens[i].getString();
								if(!this.rootNode.hasTableName(tableName))
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, SQL语句中存在未知的表");
								}
								else
								{
									List<String> deleteList = new ArrayList<String>();
									Iterator<String> iter = selectList.iterator();
									while(iter.hasNext())
									{
										String select = iter.next();
										if(select.toLowerCase().startsWith(tableName.toLowerCase() + "."))
										{
											deleteList.add(select);
										}
									}
									for(String delete : deleteList)
									{
										selectList.remove(delete);
									}
									selectList.add(tableName + "." + "*");
									i += 2;
								}
							}
							else if(tokens[i + 2].getType() == Tokenizer.IDENT)
							{
								String tableName = tokens[i].getString();
								if(!this.rootNode.hasTableName(tableName))
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, SQL语句中存在未知的表");
								}
								else
								{
									boolean selectTableAll = false;
									Iterator<String> iter = selectList.iterator();
									while(iter.hasNext())
									{
										String select = iter.next();
										if(select.equalsIgnoreCase(tokens[i].getString() + tokens[i+1].getString() + "*"))
										{
											selectTableAll = true;
										}
									}
									if(!selectTableAll)
									{
										//记录原始字段名称到SELECT字段记录列表以备使用
										String oldName = tokens[i].getString() + tokens[i+1].getString() + tokens[i+2].getString();
										selectList.add(oldName);
									}
									i += 2;
								}
							}
						}
					}
				}
				else if(i + 1 < len && tokens[i].isKeyWord("EXISTS") && tokens[i + 1].getType() == Tokenizer.LPAREN || i + 2 < len
				&& tokens[i].isKeyWord("NOT") && tokens[i + 1].isKeyWord("EXISTS") && tokens[i + 2].getType() == Tokenizer.LPAREN)
				{
					int pos = tokens[i].isKeyWord("EXISTS") ? i : i + 1;
					int end = Tokenizer.scanParen(tokens, pos + 1, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, pos + 2, end);
					
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						Set<String> tableNames = rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains= false;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									break;
								}
							}
							
							if(n < l - 2 && contains
							&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
							&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
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
							
							if(!subQueryOfExistsMap.containsKey(checkList.toString()))
							{
								subQueryOfExistsMap.put(checkList.toString(), null);
							}
						}
						
						i = end;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, EXISTS子句后面必须接子查询语句");
					}
				}
				else if(i + 1 < len && tokens[i].isKeyWord("IN") && tokens[i + 1].getType() == Tokenizer.LPAREN || i + 2 < len
				&& tokens[i].isKeyWord("NOT") && tokens[i + 1].isKeyWord("IN") && tokens[i + 2].getType() == Tokenizer.LPAREN)
				{
					int pos = tokens[i].isKeyWord("IN") ? i : i + 1;
					int end = Tokenizer.scanParen(tokens, pos + 1, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, pos + 2, end);
					
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						Set<String> tableNames = rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains= false;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									break;
								}
							}
							
							if(n < l - 2 && contains
							&& subQueryTokens[n + 1].getType() == Tokenizer.DOT
							&& subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
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
							
							if(!subQueryOfInMap.containsKey(checkList.toString()))
							{
								subQueryOfInMap.put(checkList.toString(), null);
							}
						}
						
						i = end;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanSelect, EXISTS子句后面必须接子查询语句");
					}		
				}
				else if(tokens[i].getType() == Tokenizer.LPAREN && i + 1 < len) //SELECT/WHERE
				{
					int end = Tokenizer.scanParen(tokens, i, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 1, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						Set<String> tableNames = this.rootNode.getTableNames();
						Set<String> containsList = new HashSet<String>();
						for(String select : this.selectList)
						{
							containsList.add(select.toLowerCase());
						}
						
						boolean needDelayed = false;
						for(int n = 0, l = subQueryTokens.length; n < l; n++)
						{
							boolean contains = false;
							for(String name : tableNames)
							{
								if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
								{
									contains = true;
									break;
								}
							}
							
							if(n < l - 2 && contains && subQueryTokens[n + 1].getType() == Tokenizer.DOT && subQueryTokens[n + 2].getType() == Tokenizer.IDENT)
							{
								String theFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
								if(!containsList.contains(theFieldName.toLowerCase()))
								{
									selectList.add(theFieldName);
									containsList.add(theFieldName.toLowerCase());
								}
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
							
							if(belongSelect)
							{
								if(!subQueryOfSelectMap.containsKey(checkList.toString()))
								{
									subQueryOfSelectMap.put(checkList.toString(), null);
								}
							}
							else
							{
								if(!subQueryOfWhereMap.containsKey(checkList.toString()))
								{
									subQueryOfWhereMap.put(checkList.toString(), null);
								}
							}
						}
						
						i = end;
					}
				}
			}
		}
	}
	
	private boolean checkJoinOn(Token []tokens, int start, int next)
	{
		boolean joinFlag = false;
		int couple = 0;
		
		for(int i=start; i<next; i++)
		{
			if(tokens[i].isKeyWord("JOIN"))
			{
				joinFlag = true;
				couple++;
			}
			else if(tokens[i].isKeyWord("ON"))
			{
				couple--;
			}
			else if(tokens[i].getType() == Tokenizer.LPAREN)
			{
				int end = Tokenizer.scanParen(tokens, i, next);
				
				joinFlag = joinFlag || checkJoinOn(tokens, i+1, end);
				
				i = end;
			}
			
			if(couple > 1 || couple < -1)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":checkJoinOn, 关键字JOIN与ON不匹配");
			}
		}
		
		return joinFlag;
	}
	
	private void filterCheck(JoinNode node)
	{
		if(node == null)
		{
			node = this.rootNode;
		}
		
		if(!(node instanceof JoinTable))
		{
			String filter = node.getFilter();
			if(!node.analyzeBuffer(filter))
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":filterBrowse, ON子句中存在未知的表");
			}
			node.moveBuffer(0);
			
			JoinNode left = node.getLeft();
			JoinNode right = node.getRight();
			if(left == null || right == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":filterCheck, 某处存在异常的JOIN ON子句");
			}
			
			filterCheck(left);
			filterCheck(right);
		}
	}
	
	private void filterBrowse(JoinNode node, String filter, List<String> recycle)
	{
		if(node == null)
		{
			node = this.rootNode;
			if(!node.analyzeBuffer(filter))
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":filterBrowse, WHERE子句中存在未知的表");
			}
		}
		
		if(node instanceof InnerJoin)//where在InnerJoin时能向左右节点试着向下传递
		{
			JoinNode left = node.getLeft();
			if(left != null)
			{
				if(left.analyzeBuffer(filter))//若where中的表全部在左节点中
				{
					filterBrowse(left, filter, recycle);//试着由左节点继续向下传递
					return;
				}
			}
			if(!(node.getRight() instanceof JoinTable))
			{
				JoinNode right = node.getRight();
				if(right != null)
				{
					if(right.analyzeBuffer(filter))//若where中的表全部在右节点中
					{
						filterBrowse(right, filter, recycle);//试着由右节点继续向下传递
						return;
					}
				}
			}
			String buffer = node.getBuffer();
			boolean contains = false;
			for(String ident : this.subQueryOfExistsMap.values())
			{
				if(contains)
				{
					break;
				}
				if(buffer.contains(ident))
				{
					contains = true;
				}
			}
			for(String ident : this.subQueryOfInMap.values())
			{
				if(contains)
				{
					break;
				}
				if(buffer.contains(ident))
				{
					contains = true;
				}
			}
			for(String ident : this.subQueryOfWhereMap.values())
			{
				if(contains)
				{
					break;
				}
				if(buffer.contains(ident))
				{
					contains = true;
				}
			}
			if(contains)
			{
				node.moveBuffer(1); //含主查询字段的归并于当前节点的where中
			}
			else
			{
				node.moveBuffer(0); //否则可以归并于on中
			}
		}
		else if(node instanceof LeftJoin)//where在LeftJoin时只能向左节点试着向下传递不能归并于当前节点的on中
		{
			JoinNode left = node.getLeft();
			if(left != null)
			{
				if(left.analyzeBuffer(filter))//若where中的表全部在左节点中
				{
					filterBrowse(left, filter, recycle);//试着由左节点继续向下传递
					return;
				}
			}
			node.moveBuffer(1); //否则归并于当前节点的where中
		}
		else if(node instanceof JoinTable)//where所在位置是表节点不能归并到on但可以提前过滤
		{
			PerfectWhere pw = new PerfectWhere(Tokenizer.parse(filter), this.parameterList);
			String topFilter = pw.getTopFromTokens(null, null, ((JoinTable)node).getTableFile(), ((JoinTable)node).getTableNames().iterator().next());
			if(topFilter == null)
			{
				node.moveBuffer(1);//归并于当前节点的where中
			}
			else
			{
				((JoinTable)node).setTopFilter(topFilter);
				Token[] filterTokens = pw.getTokens(true);
				filter = "";
				for(Token filterToken : filterTokens)
				{
					filter += filterToken.getOriginString();
					filter += filterToken.getSpaces();
				}
				node.setBuffer(filter);
				node.moveBuffer(1);//归并于当前节点的where中
			}
		}
		
		String where = node.getBuffer();
		if(where != null && !where.isEmpty())
		{
			recycle.add(where); //无法提前使用的where回收留到全部表拼完后用
			node.setBuffer(null);
		}
	}
	
	private void selectBrowse(JoinNode node, String field)
	{
		if(node == null)
		{
			node = this.rootNode;
			if(!node.analyzeBuffer(field))
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":selectBrowse, SELECT子句中存在未知的表");
			}
		}
		
		if(node instanceof JoinTable)
		{
			node.moveBuffer(-1);
		}
		else
		{
			JoinNode left = node.getLeft();
			JoinNode right = node.getRight();
			if(left == null || right == null)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":selectBrowse, 某处存在异常的JOIN ON子句");
			}
			
			if(left.analyzeBuffer(field))
			{
				selectBrowse(left, field);
			}

			if(right.analyzeBuffer(field))
			{
				selectBrowse(right, field);
			}
		}
	}
	
	private Token[] getNewTokens(List<Token[]> tokensList)
	{
		List<Token> tokenList = new ArrayList<Token>();
		int index = 0;
		for(int k = 0, sz = tokensList.size(); k < sz; k++)
		{
			Token[] tokens = tokensList.get(k);
			boolean belongSelect = false;
			for(int i = 0, len = tokens.length; i < len; i++)
			{
				if(tokens[i].isKeyWord("SELECT"))
				{
					belongSelect = true;
				}
				else if(tokens[i].isKeyWord("NOT") && i + 3 < len && tokens[i + 1].isKeyWord("EXISTS") && tokens[i + 2].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 2, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 3, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						boolean needDelayed = false;
						if(subQueryOfExistsMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						
						if(needDelayed)
						{
							if(subQueryOfExistsMap.get(checkList.toString()) == null)
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfExistsEntryList.add(subQueryMap.entrySet().iterator().next());
								
								Token tk = new Token(Tokenizer.IDENT, "$"+uuid, index++, "$"+uuid);
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IS", index++, "IS");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NULL", index++, "NULL");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
								
								subQueryOfExistsMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfExistsMap.get(checkList.toString());

								Token tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IS", index++, "IS");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NULL", index++, "NULL");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
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
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
							else
							{
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "0", index++, "0");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
						}
						
						i = end;
						
						continue;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, exists关键字后面必须接子查询");
					}
				}
				else if(tokens[i].isKeyWord("EXISTS") && i + 2 < len && tokens[i + 1].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 1, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 2, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						boolean needDelayed = false;
						if(subQueryOfExistsMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						
						if(needDelayed)
						{
							if(subQueryOfExistsMap.get(checkList.toString()) == null)
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfExistsEntryList.add(subQueryMap.entrySet().iterator().next());

								Token tk = new Token(Tokenizer.IDENT, "$"+uuid, index++, "$"+uuid);
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IS", index++, "IS");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NOT", index++, "NOT");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NULL", index++, "NULL");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
								
								subQueryOfExistsMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfExistsMap.get(checkList.toString());

								Token tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IS", index++, "IS");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NOT", index++, "NOT");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "NULL", index++, "NULL");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
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
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "0", index++, "0");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
							else
							{
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
						}
						
						i = end;
						
						continue;
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, exists关键字后面必须接子查询");
					}
				}
				else if(tokens[i].isKeyWord("NOT") && i + 3 < len && tokens[i + 1].isKeyWord("IN") && tokens[i + 2].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 2, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 3, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						boolean needDelayed = false;
						if(subQueryOfInMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						
						if(needDelayed)
						{
							if(subQueryOfInMap.get(checkList.toString()) == null)
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfInEntryList.add(subQueryMap.entrySet().iterator().next());
								
								Token tk = new Token(Tokenizer.KEYWORD, "NOT", index++, "NOT");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.IDENT, "$"+uuid, index++, "$"+uuid);
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
								
								subQueryOfInMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfInMap.get(checkList.toString());

								Token tk = new Token(Tokenizer.KEYWORD, "NOT", index++, "NOT");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
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
								seq = cursor.fetch();
							}
							
							if(seq == null || seq.length() == 0)
							{
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
							else
							{
								if(!(seq.get(1) instanceof Record) || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, IN中子查询结果异常");
								}
								
								Token tk = new Token(Tokenizer.KEYWORD, "NOT", index++, "NOT");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
								tk.addSpace();
								tokenList.add(tk);
								
								for(int p = 1, q = seq.length(); p <= q; p++)
								{
									if(p > 1)
									{
										tk = new Token(Tokenizer.COMMA, ",", index++, ",");
										tk.addSpace();
										tokenList.add(tk);
									}
									
									String valStr = getSQLValue(((Record)seq.get(p)).getFieldValue(0));
									Token[] valTokens = Tokenizer.parse(valStr + " ");
									tokenList.addAll(Arrays.asList(valTokens));
								}
								
								tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
						}
						
						i = end;
						
						continue;
					}
				}
				else if(tokens[i].isKeyWord("IN") && i + 2 < len && tokens[i + 1].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i + 1, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 2, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						boolean needDelayed = false;
						if(subQueryOfInMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						
						if(needDelayed)
						{
							if(subQueryOfInMap.get(checkList.toString()) == null)
							{
								String uuid = UUID.randomUUID().toString().replace("-", "_");
								Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
								subQueryMap.put("$" + uuid, subQueryTokens);
								subQueryOfInEntryList.add(subQueryMap.entrySet().iterator().next());

								Token tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.IDENT, "$" + uuid, index++, "$" + uuid);
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
								
								subQueryOfInMap.put(checkList.toString(), "$" + uuid);
							}
							else
							{
								String dollar_uuid = subQueryOfInMap.get(checkList.toString());

								Token tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
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
								seq = cursor.fetch();
							}
							
							if(seq == null || seq.length() == 0)
							{
								Token tk = new Token(Tokenizer.NUMBER, "1", index++, "1");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.OPERATOR, "=", index++, "=");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.NUMBER, "0", index++, "0");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
							else
							{
								if(!(seq.get(1) instanceof Record) || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, IN中子查询结果异常");
								}
								
								Token tk = new Token(Tokenizer.KEYWORD, "IN", index++, "IN");
								tk.addSpace();
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
								tk.addSpace();
								tokenList.add(tk);
								
								for(int p = 1, q = seq.length(); p <= q; p++)
								{
									if(p > 1)
									{
										tk = new Token(Tokenizer.COMMA, ",", index++, ",");
										tk.addSpace();
										tokenList.add(tk);
									}
									
									String valStr = getSQLValue(((Record)seq.get(p)).getFieldValue(0));
									Token[] valTokens = Tokenizer.parse(valStr + " ");
									tokenList.addAll(Arrays.asList(valTokens));
								}
								
								tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
							}
						}
						
						i = end;
						
						continue;
					}
				}
				else if(tokens[i].getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i, len);
					Token[] subQueryTokens = Arrays.copyOfRange(tokens, i + 1, end);
					boolean isSubQuery = false;
					if(Tokenizer.scanKeyWords(new String[]{"SELECT","UNION","INTERSECT","EXCEPT","MINUS"}, subQueryTokens, 0, subQueryTokens.length - 1) != -1)
					{
						isSubQuery = true;
					}
					
					if(isSubQuery)
					{
						List<String> checkList = new ArrayList<String>();
						for(Token subQueryToken : subQueryTokens)
						{
							checkList.add(subQueryToken.getString().toUpperCase());
						}
						
						boolean needDelayed = false;
						if(belongSelect && subQueryOfSelectMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						else if(!belongSelect && subQueryOfWhereMap.containsKey(checkList.toString()))
						{
							needDelayed = true;
						}
						
						if(needDelayed)
						{
							if(belongSelect)
							{
								if(subQueryOfSelectMap.get(checkList.toString()) == null)
								{
									String uuid = UUID.randomUUID().toString().replace("-", "_");
									Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
									subQueryMap.put("$" + uuid, subQueryTokens);
									this.subQueryOfSelectEntryList.add(subQueryMap.entrySet().iterator().next());
									
									Token tk = new Token(Tokenizer.IDENT, "$"+uuid, index++, "$"+uuid);
									tk.setSpaces(tokens[end].getSpaces());
									tokenList.add(tk);
									
									this.subQueryOfSelectMap.put(checkList.toString(), "$" + uuid);
								}
								else
								{
									String dollar_uuid = this.subQueryOfSelectMap.get(checkList.toString());

									Token tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
									tk.setSpaces(tokens[end].getSpaces());		
									tokenList.add(tk);
								}
							}
							else
							{
								if(subQueryOfWhereMap.get(checkList.toString()) == null)
								{
									String uuid = UUID.randomUUID().toString().replace("-", "_");
									Map<String, Token[]> subQueryMap = new HashMap<String, Token[]>();
									subQueryMap.put("$" + uuid, subQueryTokens);
									this.subQueryOfWhereEntryList.add(subQueryMap.entrySet().iterator().next());
									
									Token tk = new Token(Tokenizer.IDENT, "$"+uuid, index++, "$"+uuid);
									tk.setSpaces(tokens[end].getSpaces());
									tokenList.add(tk);
									
									this.subQueryOfWhereMap.put(checkList.toString(), "$" + uuid);
								}
								else
								{
									String dollar_uuid = this.subQueryOfWhereMap.get(checkList.toString());

									Token tk = new Token(Tokenizer.IDENT, dollar_uuid, index++, dollar_uuid);
									tk.setSpaces(tokens[end].getSpaces());		
									tokenList.add(tk);
								}
							}
						}
						else
						{
							SimpleSQL lq = new SimpleSQL(this.ics, subQueryTokens, 0, subQueryTokens.length, this.parameterList, this.ctx, true);
							lq.setMemory(this.isMemory);
							
							Sequence seq = null;
							ICursor cursor = lq.query();
							if(cursor != null)
							{
								seq = cursor.fetch(2);
							}
							if(seq == null || seq.length() != 1 || !(seq.get(1) instanceof Record) || seq.dataStruct() == null || seq.dataStruct().getFieldCount() != 1)
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, 在SELECT/WHERE子句中子查询结果异常");
							}
							
							Object val = ((Record)seq.get(1)).getFieldValue(0);
							Token[] valTokens = Tokenizer.parse(getSQLValue(val) + " ");
							tokenList.addAll(Arrays.asList(valTokens));
						}
						
						i = end;
						
						continue;
					}
				}
				else if(tokens[i].getType() == Tokenizer.IDENT)
				{
					if(i+1 != len && tokens[i].getString().equalsIgnoreCase("COUNT") && tokens[i+1].getType() == Tokenizer.LPAREN)
					{
						int end = Tokenizer.scanParen(tokens, i+1, len);
						if(end == i+5)
						{
							if(tokens[i+2].getType() == Tokenizer.IDENT && tokens[i+3].getType() == Tokenizer.DOT && tokens[i+4].getString().equals("*"))
							{
								Token tk = new Token(Tokenizer.IDENT, "COUNT", index++, "COUNT");
								tk.setSpaces(tokens[i].getSpaces());
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
								tk.setSpaces(tokens[i+1].getSpaces());
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.IDENT, tokens[i+2].getString() + ".COUNT", index++, tokens[i+2].getOriginString() + ".COUNT");
								tk.setSpaces(tokens[i+4].getSpaces());
								tokenList.add(tk);
								
								tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
								tk.setSpaces(tokens[end].getSpaces());
								tokenList.add(tk);
								
								i = end;
								continue;
							}
						}
					}
					else if(i + 1 != len && tokens[i+1].getType() == Tokenizer.DOT)
					{
						if(i + 2 != len)
						{
							if(tokens[i + 2].getType() == Tokenizer.IDENT && Tokenizer.isGatherFunction(tokens[i+2].getString()) && tokens[i+3].getType() == Tokenizer.LPAREN)
							{
								if(i + 3 != len)
								{
									int end = Tokenizer.scanParen(tokens, i+3, len);
									if(end == i + 5)
									{
										Token tk = new Token(Tokenizer.IDENT, tokens[i+2].getString(), index++, tokens[i+2].getOriginString());
										tk.setSpaces(tokens[i+2].getSpaces());
										tokenList.add(tk);
										
										tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
										tk.setSpaces(tokens[i+3].getSpaces());
										tokenList.add(tk);
										
										if(tokens[i+2].getString().equalsIgnoreCase("COUNT") && tokens[i+4].getString().equals("*"))
										{
											tk = new Token(Tokenizer.IDENT, tokens[i].getString() + ".COUNT", index++, tokens[i].getOriginString() + ".COUNT");
											tk.setSpaces(tokens[i+4].getSpaces());
											tokenList.add(tk);
										}
										else
										{
											tk = new Token(Tokenizer.IDENT, tokens[i].getString() + "." + tokens[i+4].getString(), index++, tokens[i].getOriginString() + "." + tokens[i+4].getOriginString());
											tk.setSpaces(tokens[i+4].getSpaces());
											tokenList.add(tk);
										}
										
										tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
										tk.setSpaces(tokens[end].getSpaces());
										tokenList.add(tk);
										
										i = end;
										continue;
									}
									else if(end == i+7)
									{
										if(tokens[i+4].getType() == Tokenizer.IDENT && tokens[i+5].getType() == Tokenizer.DOT)
										{
											if(!tokens[i].getString().equalsIgnoreCase(tokens[i+4].getString()))
											{
												MessageManager mm = ParseMessage.get();
												throw new RQException(mm.getMessage("syntax.error") + ":getNewTokens, 聚合函数所属表必须与聚合字段所属表一致");
											}
											else
											{
												Token tk = new Token(Tokenizer.IDENT, tokens[i+2].getString(), index++, tokens[i+2].getOriginString());
												tk.setSpaces(tokens[i+2].getSpaces());
												tokenList.add(tk);
												
												tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
												tk.setSpaces(tokens[i+3].getSpaces());
												tokenList.add(tk);
												
												if(tokens[i+2].getString().equalsIgnoreCase("COUNT") && tokens[i+6].getString().equals("*"))
												{
													tk = new Token(Tokenizer.IDENT, tokens[i+4].getString() + ".COUNT", index++, tokens[i+4].getOriginString() + ".COUNT");
													tk.setSpaces(tokens[i+6].getSpaces());
													tokenList.add(tk);
												}
												else
												{
													tk = new Token(Tokenizer.IDENT, tokens[i+4].getString() + "." + tokens[i+6].getString(), index++, tokens[i+4].getOriginString() + "." + tokens[i+6].getOriginString());
													tk.setSpaces(tokens[i+6].getSpaces());
													tokenList.add(tk);
												}
												
												tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
												tk.setSpaces(tokens[end].getSpaces());
												tokenList.add(tk);
												
												i = end;
												continue;
											}
										}
									}
								}
							}
							else if(tokens[i+2].getString().equals("*"))
							{
								String tableName = tokens[i].getString();
								DataStruct struct = this.rootNode.getStruct();
								if(struct != null)
								{
									String[] fieldNames = struct.getFieldNames();
									if(fieldNames != null)
									{
										boolean needComma = false;
										for(int j=0, l=fieldNames.length; j<l; j++)
										{
											String fieldName = fieldNames[j];
											if(fieldName.toLowerCase().startsWith(tableName.toLowerCase() + "."))
											{
												if(needComma)
												{
													Token tk = new Token(Tokenizer.COMMA, ",", index++, ",");
													tk.addSpace();
													tokenList.add(tk);
												}
												
												Token tk = new Token(Tokenizer.IDENT, fieldName, index++, fieldName);
												tk.addSpace();
												tokenList.add(tk);
												
												tk = new Token(Tokenizer.IDENT, fieldName, index++, fieldName);
												tk.addSpace();
												tokenList.add(tk);
												
												needComma = true;
												continue;
											}
										}
									}
								}
								else
								{
									Token tk = new Token(Tokenizer.KEYWORD, "NULL", index++, "NULL");
									tk.addSpace();
									tokenList.add(tk);
								}
								
								i += 2;
								continue;
							}
							else if(tokens[i+2].getType() == Tokenizer.IDENT)
							{
								Token tk = new Token(tokens[i].getType(), tokens[i].getString() + "." + tokens[i+2].getString(), index++, tokens[i].getOriginString() + "." + tokens[i+2].getOriginString());
								tk.setSpaces(tokens[i+2].getSpaces());
								tokenList.add(tk);
								
								i += 2;
								continue;
							}
						}
					}
				}
				
				Token tk = new Token(tokens[i].getType(),tokens[i].getString(),index++,tokens[i].getOriginString());
				tk.setSpaces(tokens[i].getSpaces());
				tokenList.add(tk);
			}
			
			if(k == 0)
			{
				Token tk = new Token(Tokenizer.KEYWORD, "FROM", index++, "FROM");
				tk.addSpace();
				tokenList.add(tk);
				
				tk = new Token(Tokenizer.LPAREN, "(", index++, "(");
				tk.addSpace();
				tokenList.add(tk);
				
				tk = new Token(Tokenizer.PARAMMARK, "?", index++, "?");
				tk.addSpace();
				tokenList.add(tk);
				
				tk = new Token(Tokenizer.RPAREN, ")", index++, ")");
				tk.addSpace();
				tokenList.add(tk);
				
				if(!this.recycleList.isEmpty())
				{
					tk = new Token(Tokenizer.KEYWORD, "WHERE", index++, "WHERE");
					tk.addSpace();
					tokenList.add(tk);
					
					boolean first = true;
					for(String recycle : this.recycleList)
					{
						if(first)
						{
							first = false;
						}
						else
						{
							tk = new Token(Tokenizer.KEYWORD, "AND", index++, "AND");
							tk.addSpace();
							tokenList.add(tk);
						}
						
						Token[] recycleTokens = Tokenizer.parse(recycle);
						for(Token token : recycleTokens)
						{
							tk = new Token(token.getType(), token.getString(), index++, token.getOriginString());
							tk.setSpaces(token.getSpaces());
							tokenList.add(tk);
						}
					}
				}
			}
		}
		
		Token[] newTokens = new Token[tokenList.size()];
		tokenList.toArray(newTokens);
		return newTokens;
	}
	
	public ICursor query(Token[] tokens, int start, int next)
	{
		tokens = scanParallel(Arrays.copyOfRange(tokens, start, next));
		
		start = 0;
		next = tokens.length;
		
		boolean hasJoin = true;
		int joinPos = Tokenizer.scanKeyWord("JOIN", tokens, start, next);
		if(joinPos == -1)
		{
			hasJoin = false;
		}
		
		List<Token[]> tokensList = scanFrom(tokens, start, next, hasJoin); //判断是否是多表查询，如果是则处理多表
		
		if(this.singleTable)
		{
			SimpleSelect sdql = new SimpleSelect(this.ics, this.ctx);
			sdql.setMemory(this.isMemory);
			if(this.parallelNumber > 1)
			{
				sdql.setParallel(this.parallelNumber);
			}
			sdql.setSQLParameters(this.parameterList);
			tokens = tokensList.get(0);
			for(String name : this.withTableMap.keySet())
			{
				JoinTable table = this.withTableMap.get(name);
				String tableFile = table.getTableFile();
				ICursor cursor = table.getCursor();
				DataStruct struct = table.getStruct();
				if(tableFile != null)
				{
					for(Token token : tokens)
					{
						if(token.getType() == Tokenizer.IDENT && token.getString().equalsIgnoreCase(name))
						{
							if(tableFile.startsWith("\"") && tableFile.endsWith("\"") && tableFile.substring(1, tableFile.length()-1).indexOf("\"") == -1)
							{
								tableFile = tableFile.substring(1, tableFile.length() - 1);
							}
							sdql.setTablePath(name, tableFile);
						}
					}
				}
				else
				{
					if(cursor != null)
					{
						List<Token> tmpList = new ArrayList<Token>();
						for(int i = 0; i < tokens.length; i++)
						{
							Token token = tokens[i];
							if(token.getType() == Tokenizer.IDENT && token.getString().equalsIgnoreCase(name))
							{
								if(tmpList.get(tmpList.size() - 1).isKeyWord("FROM"))
								{
									Token tk = new Token(Tokenizer.LPAREN, "(", tmpList.size(), "(");
									tk.addSpace();
									tmpList.add(tk);
									
									tk = new Token(Tokenizer.PARAMMARK, "?", tmpList.size(), "?");
									tk.addSpace();
									tmpList.add(tk);
									
									tk = new Token(Tokenizer.RPAREN, ")", tmpList.size(), ")");
									tk.addSpace();
									tmpList.add(tk);
									
									sdql.setSQLParameters(cursor);
									sdql.setSQLParameters(struct);
								}
								else if(i + 1 < tokens.length && tokens[i + 1].getType() == Tokenizer.DOT)
								{
									i++;
								}
								else
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":query, 表别名引用格式错误");
								}
							}
							else
							{
								Token tk = new Token(token.getType(), token.getString(), tmpList.size(), token.getOriginString());
								tk.setSpaces(token.getSpaces());
								tmpList.add(tk);
							}
						}
						tokens = new Token[tmpList.size()];
						tmpList.toArray(tokens);
					}
					else
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":query, 无效的表节点");
					}
				}
			}
			next = tokens.length;
			this.icur = sdql.query(tokens, start, next);
			this.ds = sdql.getDataStruct();
			if(this.ds != null)
			{
				boolean needRename = false;
				String[] names = new String[this.ds.getFieldCount()];
				Expression[] exps = new Expression[this.ds.getFieldCount()];
				for(int k = 0; k < this.ds.getFieldCount(); k++)
				{
					String fieldName = this.ds.getFieldName(k);
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"") && fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
						needRename = true;
					}
					Token[] fieldTokens = Tokenizer.parse(fieldName);
					if(fieldTokens.length == 3 && fieldTokens[0].getType() == Tokenizer.IDENT
					&& fieldTokens[1].getType() == Tokenizer.DOT && fieldTokens[2].getType() == Tokenizer.IDENT)
					{
						fieldName = fieldTokens[2].getOriginString();
						if(fieldName.startsWith("\"") && fieldName.endsWith("\"") && fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
						{
							fieldName = fieldName.substring(1, fieldName.length() - 1);
							needRename = true;
						}
					}
					names[k] = fieldName;
					exps[k] = new Expression("#" + (k + 1));
				}
				if(this.icur != null && needRename)
				{
					this.icur.addOperation(new New(exps, names, null), this.ctx);
					this.ds = new DataStruct(names);
				}
			}
		}
		else
		{
			filterCheck(null); //处理on子句
			if(this.whereList != null)
			{
				for(String where : this.whereList) //处理where子句
				{
					filterBrowse(null, where, this.recycleList);
				}
			}
			
			scanSelect(tokensList); //处理其他子句
			if(this.selectList != null)
			{
				for(String select:this.selectList) //确定select子句涉及到的字段
				{
					selectBrowse(null, select);
				}
			}
			if(this.rootNode != null)
			{
				//执行所有分表的拼接
				this.rootNode.executeJoin();
				
				//生成新的token数组,注意要在拼接之后，以处理像T.*这种特殊形式
				Token[] newTokens = getNewTokens(tokensList);
				
				//取原始游标和数据结构
				ICursor icursor = this.rootNode.getCursor();
				DataStruct struct = this.rootNode.getStruct();
				icursor.setDataStruct(struct);
				
				List<Map.Entry<String, Token[]>> newSubQueryOfExistsEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				for(Map.Entry<String, Token[]> subQueryOfExistsEntry : this.subQueryOfExistsEntryList)
				{
					String ident = subQueryOfExistsEntry.getKey();
					boolean contains = false;
					for(Token token : newTokens)
					{
						if(token.getString().equals(ident))
						{
							contains = true;
							break;
						}
					}
					if(contains)
					{
						newSubQueryOfExistsEntryList.add(subQueryOfExistsEntry);
					}
				}
				this.subQueryOfExistsEntryList = newSubQueryOfExistsEntryList;
				
				List<Map.Entry<String, Token[]>> newSubQueryOfInEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				for(Map.Entry<String, Token[]> subQueryOfInEntry : this.subQueryOfInEntryList)
				{
					String ident = subQueryOfInEntry.getKey();
					boolean contains = false;
					for(Token token : newTokens)
					{
						if(token.getString().equals(ident))
						{
							contains = true;
							break;
						}
					}
					if(contains)
					{
						newSubQueryOfInEntryList.add(subQueryOfInEntry);
					}
				}
				this.subQueryOfInEntryList = newSubQueryOfInEntryList;
				
				List<Map.Entry<String, Token[]>> newSubQueryOfWhereEntryList = new ArrayList<Map.Entry<String, Token[]>>();
				for(Map.Entry<String, Token[]> subQueryOfWhereEntry : this.subQueryOfWhereEntryList)
				{
					String ident = subQueryOfWhereEntry.getKey();
					boolean contains = false;
					for(Token token : newTokens)
					{
						if(token.getString().equals(ident))
						{
							contains = true;
							break;
						}
					}
					if(contains)
					{
						newSubQueryOfWhereEntryList.add(subQueryOfWhereEntry);
					}
				}
				this.subQueryOfWhereEntryList = newSubQueryOfWhereEntryList;
				
				if(!this.subQueryOfExistsEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(this.ics, icursor, this.subQueryOfExistsEntryList, this.parameterList, this.rootNode.getTableNames(), SubQueryCursor.Exist_Type, struct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(icursor))
					{
						struct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						struct = icur.getDataStruct();
					}
					icursor = icur;
				}
				
				if(!this.subQueryOfInEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(this.ics, icursor, this.subQueryOfInEntryList, this.parameterList, this.rootNode.getTableNames(), SubQueryCursor.In_Type, struct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(icursor))
					{
						struct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						struct = icur.getDataStruct();
					}
					icursor = icur;
				}
				
				if(!this.subQueryOfWhereEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(this.ics, icursor, this.subQueryOfWhereEntryList, this.parameterList, this.rootNode.getTableNames(), SubQueryCursor.Where_Type, struct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(icursor))
					{
						struct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						struct = icur.getDataStruct();
					}
					icursor = icur;
				}
				
				if(!this.subQueryOfSelectEntryList.isEmpty())
				{
					ICursor icur = fillSubQueryField(this.ics, icursor, this.subQueryOfSelectEntryList, this.parameterList, this.rootNode.getTableNames(), SubQueryCursor.Select_Type, struct);
					if(icur != null && icur instanceof SubQueryCursor && !icur.equals(icursor))
					{
						struct = ((SubQueryCursor)icur).getTableDataStruct();
					}
					else if(icur != null)
					{
						struct = icur.getDataStruct();
					}
					icursor = icur;
				}
				
				//查询最终选出列
				SimpleSelect sdql = new SimpleSelect(this.ics, this.ctx);
				sdql.setMemory(this.isMemory);
				if(this.parallelNumber > 1)
				{
					sdql.setParallel(this.parallelNumber);
				}
				sdql.setSQLParameters(icursor);
				sdql.setSQLParameters(struct);
				sdql.setSQLParameters(this.parameterList);
				this.icur = sdql.query(newTokens, 0, newTokens.length);
				this.ds = sdql.getDataStruct();
			}
		}
		
		if(!this.intoFileName.isEmpty())
		{
			if(this.withTableMap.containsKey(this.intoFileName.toLowerCase()))
			{
				String filePath = this.withTableMap.get(this.intoFileName.toLowerCase()).getTableFile();
				if(filePath != null && !filePath.isEmpty())
				{
					this.intoFileName = filePath;
				}
			}
			
			this.intoFileName = this.intoFileName.trim();
			
			if(this.intoFileName.startsWith("\"") && this.intoFileName.endsWith("\"")
			&& this.intoFileName.substring(1, this.intoFileName.length()-1).indexOf("\"") == -1)
			{
				this.intoFileName = this.intoFileName.substring(1, this.intoFileName.length()-1);
			}
			else if(this.intoFileName.startsWith("'") && this.intoFileName.endsWith("'")
			&& this.intoFileName.substring(1, this.intoFileName.length()-1).indexOf("'") == -1)
			{
				this.intoFileName = this.intoFileName.substring(1, this.intoFileName.length()-1);
			}
			
			this.intoFileName = this.intoFileName.trim();
			
			this.intoFileName = IOUtils.getPath(Env.getMainPath(), this.intoFileName);
			
			this.intoFileName = this.intoFileName.trim();
			
			File dir = new File(new File(this.intoFileName).getParent());
			if(!dir.exists())
			{
				try
				{
					boolean create = dir.mkdirs();
					if(!create)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":query, 执行into指令中创建文件路径失败");
					}
				}
				catch(Exception ex)
				{
					throw new RQException(ex.getMessage(), ex);
				}
			}
			
			String password = null;
			if(Pattern.compile("[^\\'\\\"\\f\\n\\r\\t\\v]+\\.[cC][tT][xX] *\\: *\\'[^\\'\\\"\\f\\n\\r\\t\\v]*\\'").matcher(this.intoFileName).matches()) //组表文件可以设置密码
			{
				Matcher matcher = Pattern.compile("[^\\'\\\"\\f\\n\\r\\t\\v]+\\.[cC][tT][xX](?= *\\: *\\'[^\\'\\\"\\f\\n\\r\\t\\v]*\\')").matcher(this.intoFileName);
				if(matcher.find())
				{
					String realName = matcher.group();
					password = this.intoFileName.substring(realName.length()).trim();
					if(!password.startsWith(":"))
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":query, into语句密码格式不对");
					}
					password = password.substring(1).trim();
					if(!password.startsWith("'") || !password.endsWith("'"))
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":query, into语句密码格式不对");
					}
					password = password.substring(1, password.length() - 1);
					this.intoFileName = realName;
				}
			}
			
			FileObject fo = new FileObject(this.intoFileName, null, null, ctx);
			
			String aopt = "";
			if(fo.isExists())
			{
				try {
					if(this.intoFileName.toLowerCase().endsWith(".btx") 
						|| this.intoFileName.toLowerCase().endsWith(".txt")
						|| this.intoFileName.toLowerCase().endsWith(".csv")) {
						String opt = this.intoFileName.toLowerCase().endsWith(".btx")?"b":(this.intoFileName.toLowerCase().endsWith(".txt")?"t":"tc");
						Sequence efo = fo.importSeries(opt);
						if (efo.getFirstRecordDataStruct().isCompatible(this.icur.getDataStruct())) aopt+="a";
						else {
							MessageManager mm = ParseMessage.get();
							throw  new RQException(mm.getMessage("syntax.error") + ":query, SELECT INTO file has existed， not compatible！");
						}
					} else if(this.intoFileName.toLowerCase().endsWith(".xls")	|| this.intoFileName.toLowerCase().endsWith(".xlsx")) {
						ExcelTool et = new ExcelTool(fo, true, this.intoFileName.toLowerCase().endsWith(".xlsx"), false, null);
						Table t = FileObject.import_x(et, "t");
						if (t.dataStruct().isCompatible(this.icur.getDataStruct())) aopt+="a";
						else {
							MessageManager mm = ParseMessage.get();
							throw  new RQException(mm.getMessage("syntax.error") + ":query, SELECT INTO file has existed， not compatible！");
						}
					}
				} catch (IOException e) {
					MessageManager mm = ParseMessage.get();
					throw  new RQException(mm.getMessage("syntax.error") + ":query, SELECT INTO file has existed， not compatible！");
				}
			}
			
			if(this.intoFileName.toLowerCase().endsWith(".btx"))
			{
				try 
				{
					fo.exportCursor(this.icur, null, null, "z"+aopt, null, ctx);
				}
				catch(Exception e)
				{
					if(fo.isExists())
					{
						fo.delete();
					}
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 将数据导入BTX文件失败", e);
				}
			}
			else if(this.intoFileName.toLowerCase().endsWith(".txt"))
			{
				try 
				{
					fo.exportCursor(this.icur, null, null, "t"+aopt, null, ctx);
				}
				catch(Exception e)
				{
					if(fo.isExists())
					{
						fo.delete();
					}
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 将数据导入TXT文件失败", e);
				}
			}
			else if(this.intoFileName.toLowerCase().endsWith(".csv"))
			{
				try 
				{
					fo.exportCursor(this.icur, null, null, "tc"+aopt, null, ctx);
				}
				catch(Exception e)
				{
					if(fo.isExists())
					{
						fo.delete();
					}
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 将数据导入CSV文件失败", e);
				}
			}
			else if(this.intoFileName.toLowerCase().endsWith(".xls"))
			{
				ExcelTool et = new ExcelTool(fo, true, false, false, null);
				try 
				{
					int maxCount = et.getMaxLineCount() - 1;
					this.icur = new SubCursor(this.icur, maxCount);
					FileObject.export_x(et, this.icur, null, null, true, ctx);
				} 
				catch (Exception e) 
				{
					if(fo.isExists())
					{
						fo.delete();
					}
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 游标导入xls文件失败", e);
				}
				finally
				{
					try 
					{
						et.close();
					} 
					catch (IOException e) 
					{
						if(fo.isExists())
						{
							fo.delete();
						}
						throw new RQException(e.getMessage(), e);
					}
				}	
			}
			else if(this.intoFileName.toLowerCase().endsWith(".xlsx"))
			{
				ExcelTool et = new ExcelTool(fo, true, true, false, null);
				try 
				{
					int maxCount = et.getMaxLineCount() - 1;
					this.icur = new SubCursor(this.icur, maxCount);
					FileObject.export_x(et, this.icur, null, null, true, ctx);
				} 
				catch (Exception e) 
				{
					if(fo.isExists())
					{
						fo.delete();
					}
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 游标导入xls文件失败", e);
				}
				finally
				{
					try 
					{
						et.close();
					} 
					catch (IOException e) 
					{
						if(fo.isExists())
						{
							fo.delete();
						}
						throw new RQException(e.getMessage(), e);
					}
				}	
			}
			else
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":query, 未知的文件类型");
			}
		}
		
		return this.icur;
	}
	
	public ICursor query(String dql)
	{
		Token[] tokens = Tokenizer.parse(dql);
		return query(tokens, 0, tokens.length);
	}
	
	public DataStruct getDataStruct()
	{
		return this.ds;
	}
	
	public void setWithTableMap(Map<String, Object> tableMap)
	{
		if(tableMap == null)
		{
			return;
		}
		for(Map.Entry<String, Object> entry : tableMap.entrySet())
		{
			String tableName = entry.getKey();
			Object tableObject = entry.getValue();
			JoinTable tableNode = null;
			if(tableObject instanceof String) //不再支持表文件路径
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":setWithTableMap, WITH子句中只能使用集算器脚本或子查询语句");
			}
			else if(tableObject instanceof Expression)
			{
				Object obj = ((Expression)tableObject).calculate(this.ctx);
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
					tableNode = new JoinTable(cursor, struct);
				}
				else if(obj instanceof Table)
				{
					ICursor cursor = new MemoryCursor((Table)obj);
					DataStruct struct = ((Table)obj).dataStruct();
					tableNode = new JoinTable(cursor, struct);
				}
				else if(obj instanceof TableMetaData)
				{
					tableNode = new JoinTable((TableMetaData)obj);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":setWithTableMap, 不支持的临时表数据类型");
				}
			}
			else if(tableObject instanceof SimpleSQL)
			{
				((SimpleSQL)tableObject).setMemory(this.isMemory);
				ICursor icur = ((SimpleSQL)tableObject).query();
				DataStruct ds = ((SimpleSQL)tableObject).getDataStruct();
				tableNode = new JoinTable(icur, ds);
			}
			tableNode.addTableName(tableName);
			this.withTableMap.put(tableName.toLowerCase(), tableNode);
		}
	}
	
	private Token[] optimizeWhere(Token[] whereTokens, List<Object> paramList)
	{
		PerfectWhere pw = new PerfectWhere(whereTokens, paramList);
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
	
	private ICursor fillSubQueryField(ICellSet ics, ICursor icur, List<Map.Entry<String, Token[]>> subQueryEntryList, List<Object> paramList, Set<String> tableNames, int type, DataStruct ds)
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
					
					for(String outerField : outerFieldSet)
					{
						for(int k = 0, len = fieldNames.length; k < len; k++)
						{
							String fieldName = fieldNames[k];
							if(fieldName.equalsIgnoreCase("\"" + outerField + "\"")	|| fieldName.equalsIgnoreCase(outerField))
							{
								fn2cnMap.put(outerField, "#" + (k + 1));
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
							String numberCode = ExpressionTranslator.translateExp(fltExps[0].toLowerCase(), fn2cnMap);
							outerExpsList.add(new Expression(numberCode));
						}
						else if(innerFieldSet.contains(fltExps[0].trim().toLowerCase())) //内部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[0].toLowerCase(), fn2cnMap);
							innerExpsList.add(new Expression(numberCode));
						}
						else
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error")+":fillSubQueryField, 未知的字段");
						}
						
						if(outerFieldSet.contains(fltExps[1].trim().toLowerCase())) //外部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[1].toLowerCase(), fn2cnMap);
							outerExpsList.add(new Expression(numberCode));
						}
						else if(innerFieldSet.contains(fltExps[1].trim().toLowerCase())) //内部字段
						{
							String numberCode = ExpressionTranslator.translateExp(fltExps[1].toLowerCase(), fn2cnMap);
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
					boolean contains = false;
					for(String name : tableNames)
					{
						if(name.equalsIgnoreCase(subQueryTokens[n].getString()))
						{
							contains = true;
							break;
						}
					}
					
					String theFieldName = null;
					if(n < len - 2 && contains
					&& subQueryTokens[n + 1].getType() == Tokenizer.DOT)
					{
						for(String fieldName : fieldNames)
						{
							String genericFieldName = subQueryTokens[n].getString() + subQueryTokens[n + 1].getString() + subQueryTokens[n + 2].getString();
							if(fieldName.startsWith("\"") && fieldName.endsWith("\""))
							{
								if(fieldName.substring(1, fieldName.length() - 1).indexOf("\"") != -1)
								{
									MessageManager mm = ParseMessage.get();
									throw new RQException(mm.getMessage("syntax.error") + ":fillSubQueryField, 异常的字段名");
								}
								genericFieldName = "\"" + genericFieldName + "\"";
							}
							if(genericFieldName.equalsIgnoreCase(fieldName))
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
	
	private Token[] scanParallel(Token[] tokens) // 处理/*+parallel*/
	{
		int select = Tokenizer.scanKeyWord("SELECT", tokens, 0, tokens.length);
		if(select == -1)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanOlap, 语句缺少select关键字");
		}
		int start = select + 1;
		
		List<Token> tokenList = new ArrayList<Token>();
		int index = 0;
		for(int i = 0, len = tokens.length; i < len; i++)
		{
			if(i + 5 < len 
			&& i == start
			&& tokens[i].equals("/") 
			&& tokens[i + 1].equals("*")
			&& tokens[i + 2].equals("+")
			&& tokens[i + 3].isKeyWord("PARALLEL")
			&& tokens[i + 4].equals("*")
			&& tokens[i + 5].equals("/"))
			{
				this.parallelNumber = Env.getParallelNum();
				i = i + 5;
				start = start + 6;
			}
			else if(i + 8 < len 
			&& i == start
			&& tokens[i].equals("/") 
			&& tokens[i + 1].equals("*")
			&& tokens[i + 2].equals("+")
			&& tokens[i + 3].isKeyWord("PARALLEL")
			&& tokens[i + 4].getType() == Tokenizer.LPAREN
			&& tokens[i + 5].getType() == Tokenizer.NUMBER
			&& tokens[i + 6].getType() == Tokenizer.RPAREN
			&& tokens[i + 7].equals("*")
			&& tokens[i + 8].equals("/"))
			{
				try
				{
					this.parallelNumber = Integer.parseInt(tokens[i + 5].getString());
					if(this.parallelNumber <= 0)
					{
						throw new RQException("并行数必须大于0");
					}
				}
				catch(Exception ex)
				{
					throw new RQException("并行数设置错误", ex);
				}
				i = i + 8;
				start = start + 9;
			}
			else
			{
				Token newToken = new Token(tokens[i].getType(), tokens[i].getString(), index++, tokens[i].getOriginString());
				newToken.setSpaces(tokens[i].getSpaces());
				tokenList.add(newToken);
			}
		}
		
		Token[] newTokens = new Token[tokenList.size()];
		tokenList.toArray(newTokens);
		return newTokens;
	}
	
	void setMemory(boolean isMemory)
	{
		this.isMemory = isMemory;
	}
	
	public static String[] splitEqual(String filter)
	{
		List<Integer> equalList = new ArrayList<Integer>();
		List<Integer> leftList = new ArrayList<Integer>();
		char a = '\0';
		for(int p = 0, l = filter.length(); p < l; p++)
		{
			char c = filter.charAt(p);
			if(c == '(')
			{
				leftList.add(p);
			}
			else if(c == ')')
			{
				if(leftList.isEmpty())
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 缺少匹配的左括号");
				}
				leftList.remove(leftList.size() - 1);
			}
			else if(leftList.isEmpty() && c == '=' && a == '=')
			{
				equalList.add(p - 1);
			}
			else if(leftList.isEmpty() && c == '=' && a == '!')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 等值连接不应出现不等值运算");
			}
			else if(leftList.isEmpty() && c == '=' && a == '>')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 等值连接不应出现不等值运算");
			}
			else if(leftList.isEmpty() && c == '=' && a == '<')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 等值连接不应出现不等值运算");
			}
			else if(leftList.isEmpty() && c == '>')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 等值连接不应出现不等值运算");
			}
			else if(leftList.isEmpty() && c == '<')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitEqual, 等值连接不应出现不等值运算");
			}
			a = c;
		}
		
		StringBuffer buf = new StringBuffer(filter);
		String split = "" + (char)18 + (char)19;
		for(int equal : equalList)
		{
			buf.replace(equal, equal + 2, split);
		}
		
		return buf.toString().split(split);
	}
	
	public static String[] splitAnd(String filter)
	{
		List<Integer> andList = new ArrayList<Integer>();
		List<Integer> leftList = new ArrayList<Integer>();
		char a = '\0';
		for(int p = 0, l = filter.length(); p < l; p++)
		{
			char c = filter.charAt(p);
			if(c == '(')
			{
				leftList.add(p);
			}
			else if(c == ')')
			{
				if(leftList.isEmpty())
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error")+"splitAnd, 缺少匹配的左括号");
				}
				leftList.remove(leftList.size() - 1);
			}
			else if(leftList.isEmpty() && c == '&' && a == '&')
			{
				andList.add(p - 1);
			}
			else if(leftList.isEmpty() && c == '|' && a == '|')
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error")+"splitAnd, AND连接不应出现OR运算");
			}
			a = c;
		}
		
		StringBuffer buf = new StringBuffer(filter);
		String split = "" + (char)18 + (char)19;
		for(int and : andList)
		{
			buf.replace(and, and + 2, split);
		}
		
		return buf.toString().split(split);
	}
	
	public static String getRealFieldName(String name)
	{
		if(name.startsWith("\"") && name.endsWith("\"") && name.substring(1, name.length() - 1).indexOf("\"") == -1)
		{
			name = name.substring(1, name.length() - 1);
		}
		
		String[] strs = name.split("\\.");
		if(strs.length == 2)
		{
			name = strs[1];
		}
		
		return name;
	}
	
	public static String addSmartParen(String exp)
	{
		boolean needParen = false;
		Token[] tokens = Tokenizer.parse(exp);
		for(int i = 0, len = tokens.length; i < len; i++)
		{
			if(tokens[i].getType() == Tokenizer.LPAREN)
			{
				i = Tokenizer.scanParen(tokens, i, len);
			}
			else if(tokens[i].isKeyWord("OR"))
			{
				needParen = true;
				break;
			}
		}
		return needParen ? "(" + exp + ")" : exp;
	}
}
