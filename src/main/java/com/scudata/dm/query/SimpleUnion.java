package com.scudata.dm.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.cursor.MergesCursor;
import com.scudata.dm.op.New;
import com.scudata.expression.Expression;
import com.scudata.resources.ParseMessage;

public class SimpleUnion 
{
	final public static int unionType = 0x01;
	final public static int exceptType = 0x02;
	final public static int intersectType = 0x04;
	final public static int allType = 0x08;
	
	private List<Object> parameterList;
	private Context ctx;
	private DataStruct ds;
	private ICursor icur;
	private Map<String, Object> withTableMap;
	private ICellSet ics;
	private boolean isMemory;
	
	public SimpleUnion(ICellSet ics, Context ctx)
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
		this.withTableMap = null;
	}
	
	public void setSQLParameters(List<Object> paramList)
	{
		this.parameterList = paramList;
	}
	
	public ICursor query(Token[] tokens, int start, int next)
	{
		start = scanWith(tokens, start, next);
		if(start < 0)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":query, 非法的查询语句");
		}
		
		tokens = Arrays.copyOfRange(tokens, start, next);
		start = 0;
		next = tokens.length;
		
		List<Integer> mergeList = new ArrayList<Integer>();
		List<Token> orderList = new ArrayList<Token>();
		
		List<Token[]> subTokenList = scanUnion(tokens, start, next, mergeList, orderList);
		if(subTokenList.size() == 1)
		{
			SimpleJoin join = new SimpleJoin(this.ics, this.ctx);
			join.setMemory(this.isMemory);
			join.setSQLParameters(this.parameterList);
			join.setWithTableMap(this.withTableMap);
			this.icur = join.query(tokens, start, next);
			this.ds = join.getDataStruct();
		}
		else
		{
			this.isMemory = true;
			
			DataStruct ds = null;
			Expression[] colExps = null;
			List<ICursor> subCurList = new ArrayList<ICursor>(subTokenList.size());
			
			for(int i=0, sz=subTokenList.size(); i<sz; i++)
			{
				ICursor icur = null;
				Token[] subTokens = subTokenList.get(i);
				String[] keyWords = new String[]{"UNION", "INTERSECT", "EXCEPT", "MINUS"};
				int keyPos = Tokenizer.scanKeyWords(keyWords, subTokens, 0, subTokens.length);;
				if(keyPos < 0)
				{
					SimpleJoin join = new SimpleJoin(this.ics, this.ctx);
					join.setMemory(this.isMemory);
					join.setSQLParameters(this.parameterList);
					join.setWithTableMap(this.withTableMap);
					icur = join.query(subTokens, 0, subTokens.length);
					if(i == 0)
					{
						ds = join.getDataStruct();
						colExps = new Expression[ds.getFieldCount()];
						for(int j=0, len=colExps.length; j<len; j++)
						{
							colExps[j] = new Expression(String.format("#%d", j+1));
						}
					}
				}
				else
				{
					SimpleSQL lq = new SimpleSQL(this.ics, subTokens, 0, subTokens.length, this.parameterList, this.ctx, false);
					lq.setMemory(this.isMemory);
					lq.setWithTableMap(this.withTableMap);
					icur = lq.query();
					if(i == 0)
					{
						ds = lq.getDataStruct();
						colExps = new Expression[ds.getFieldCount()];
						for(int j=0, len=colExps.length; j<len; j++)
						{
							colExps[j] = new Expression(String.format("#%d", j+1));
						}
					}
				}
				
				if(icur != null)
				{
					if(i != 0)
					{
						icur.addOperation(new New(colExps, ds.getFieldNames(), null), this.ctx);
					}
					subCurList.add(icur);
				}
				else
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, 做集合运算的第"+i+"个表为空表");
				}
			}
			
			this.ds = ds;
			if(subCurList.size() != 1 + mergeList.size())
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":query, 集合关键字与连接的子从句数目不匹配");
			}
			
			this.icur = subCurList.get(0);
			if(!mergeList.isEmpty())
			{
				Expression[] exps = new Expression[ds.getFieldCount()];
				for(int i=0; i<ds.getFieldCount(); i++)
				{
					exps[i] = new Expression("#"+(i+1));
				}
				
				for(int i=0, sz=mergeList.size(); i<sz; i++)
				{
					Sequence left = this.icur == null ? null : this.icur.fetch();
					Sequence right = subCurList.get(i + 1) == null ? null : subCurList.get(i + 1).fetch();
					int type = mergeList.get(i);
					if((type & allType) == 0)
					{
						if(i == 0)
						{
							/*
							//排序
							if(left instanceof MemoryCursor || this.isMemory)
							{
								left = new MemoryCursor(left.fetch().sort(exps, null, null, this.ctx));
							}
							else
							{
								left = CursorUtil.sortx(left, exps, this.ctx, EnvUtil.getCapacity(exps.length), null);
							}
							//去重
							left.addOperation(new Groups(exps, ds.getFieldNames(), new Expression[]{new Expression("count(1)")}, new String[]{"count(*)"}, "o", this.ctx), this.ctx);
							//去除多余项
							left.addOperation(new New(exps, ds.getFieldNames(), null), this.ctx);
							*/

							left = left == null ? null : left.groups(exps, ds.getFieldNames(), null, null, null, this.ctx);
						}
						/*
						//排序
						if(right instanceof MemoryCursor || this.isMemory)
						{
							right = new MemoryCursor(right.fetch().sort(exps, null, null, this.ctx));
						}
						else
						{
							right = CursorUtil.sortx(right, exps, this.ctx, EnvUtil.getCapacity(exps.length), null);
						}
						//去重
						right.addOperation(new Groups(exps, ds.getFieldNames(), new Expression[]{new Expression("count(1)")}, new String[]{"count(*)"}, "o", this.ctx), this.ctx);
						//去除多余项
						right.addOperation(new New(exps, ds.getFieldNames(), null), this.ctx);
						*/

						right = right == null ? null : right.groups(exps, ds.getFieldNames(), null, null, null, this.ctx);
					}
					
					String opt = null;
					switch(type)
					{
					case unionType | allType:
						break;
					case unionType:
						opt = "u";
						break;
					case exceptType:
						opt = "d";
						break;
					case intersectType:
						opt = "i";
						break;
					default:
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":query, 不支持的集合类型");
					}
					
					//ICursor[] curs = new ICursor[]{left, right};
					//this.icur = new MergesCursor(curs, exps, opt, this.ctx);
					
					this.icur = left == null ? (right == null ? null : new MemoryCursor(right)) : (right == null ? new MemoryCursor(left) : new MergesCursor(new ICursor[]{new MemoryCursor(left), new MemoryCursor(right)}, exps, opt, this.ctx));
				}
			}
			
			if(!orderList.isEmpty())
			{
				Expression[] exps = null;
				if(orderList.size() <= 2)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, ORDER子句中缺少排序字段");
				}
				else if(!orderList.get(0).isKeyWord("ORDER") || !orderList.get(1).isKeyWord("BY"))
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":query, ORDER子句的关键字异常");
				}
				else
				{ 
					orderList.remove(0);//remove "order"
					orderList.remove(0);//remove "by"
					Token[] orderTokens = new Token[orderList.size()];
					orderList.toArray(orderTokens);
					int begin = 0;
					int end = orderTokens.length;
					List<String> orderStrList = new ArrayList<String>();
					List<Boolean> descList = new ArrayList<Boolean>();
					int commaPos = -1;
					do
					{
						commaPos = Tokenizer.scanComma(orderTokens, begin, end);
						if(commaPos < 0)
						{
							StringBuffer sb = new StringBuffer();
							int desc = 0;
							for(int i=begin; i<end; i++)
							{
								if(orderTokens[i].getType() == Tokenizer.IDENT
								&& i + 2 < end
								&& orderTokens[i+1].getType() == Tokenizer.DOT
								&& orderTokens[i+2].getType() == Tokenizer.IDENT)
								{
									sb.append("\"");
									sb.append(orderTokens[i].getString());
									sb.append(orderTokens[i+1].getString());
									sb.append(orderTokens[i+2].getString());
									sb.append("\"");
									i = i + 2;
								}
								else if(orderTokens[i].isKeyWord("ASC"))
								{
									if(desc == 0)
									{
										desc = 1;
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 升降序关键字重复使用");
									}
								}
								else if(orderTokens[i].isKeyWord("DESC"))
								{
									if(desc == 0)
									{
										desc = -1;
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 升降序关键字重复使用");
									}
								}
								else
								{
									sb.append(orderTokens[i].getString());
								}
							}
							if(desc == -1)
							{
								descList.add(true);
							}
							else
							{
								descList.add(false);
							}
							orderStrList.add(sb.toString());
						}
						else
						{
							StringBuffer sb = new StringBuffer();
							int desc = 0;
							for(int i=begin; i<commaPos; i++)
							{
								if(orderTokens[i].getType() == Tokenizer.IDENT
								&& i + 2 < end
								&& orderTokens[i+1].getType() == Tokenizer.DOT
								&& orderTokens[i+2].getType() == Tokenizer.IDENT)
								{
									sb.append("\"");
									sb.append(orderTokens[i].getString());
									sb.append(orderTokens[i+1].getString());
									sb.append(orderTokens[i+2].getString());
									sb.append("\"");
									i = i + 2;
								}
								else if(orderTokens[i].isKeyWord("ASC"))
								{
									if(desc == 0)
									{
										desc = 1;
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 升降序关键字重复使用");
									}
								}
								else if(orderTokens[i].isKeyWord("DESC"))
								{
									if(desc == 0)
									{
										desc = -1;
									}
									else
									{
										MessageManager mm = ParseMessage.get();
										throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 升降序关键字重复使用");
									}
								}
								else
								{
									sb.append(orderTokens[i].getString());
								}
							}
							if(desc == -1)
							{
								descList.add(true);
							}
							else
							{
								descList.add(false);
							}
							orderStrList.add(sb.toString());
							begin = commaPos + 1;
						}
					}
					while(commaPos >= 0);
					exps = new Expression[orderStrList.size()];
					for(int i=0, sz=orderStrList.size(); i<sz; i++)
					{
						String orderStr = orderStrList.get(i);
						Boolean ifDesc = descList.get(i); 
						try
						{
							int k = Integer.parseInt(orderStr);
							if(k <= 0 || k > ds.getFieldCount())
							{
								MessageManager mm = ParseMessage.get();
								throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 排序字段序号超出范围");
							}
							exps[i] = new Expression(String.format("%s#%d%s", ifDesc?"-(":"", k, ifDesc?")":""));
						}
						catch(NumberFormatException nfe)
						{
							for(int j=0, len=ds.getFieldCount(); j<len; j++)
							{
								String fieldStr = ds.getFieldName(j);
								orderStr = orderStr.replaceAll("(?i)"+fieldStr, String.format("#%d", j+1));
								exps[i] = new Expression(ifDesc?String.format("-(%s)", orderStr):orderStr);
							}
						}
					}
				}
				
				/*
				if(this.icur instanceof MemoryCursor || this.isMemory)
				{
					this.icur = new MemoryCursor(this.icur.fetch().sort(exps, null, null, this.ctx));
				}
				else
				{
					this.icur = CursorUtil.sortx(this.icur, exps, this.ctx, EnvUtil.getCapacity(exps.length), null);
				}
				*/
				
				this.icur = new MemoryCursor(this.icur.fetch().sort(exps, null, null, this.ctx));
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
	
	private List<Token[]> scanUnion(Token []tokens, int start, int next, List<Integer> mergeList, List<Token> orderList) 
	{
		if(mergeList == null)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 参数错误:集合类型列表对象为空");
		}
		mergeList.clear();
		if(orderList == null)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 参数错误:排序字段列表对象为空");
		}
		orderList.clear();
		String[] keyWords = new String[]{"UNION", "INTERSECT", "EXCEPT", "MINUS"};
		List<Token[]> tokenList = new ArrayList<Token[]>();
		List<Token[]> resultList = new ArrayList<Token[]>();
		int keyPos = -1;
		int begin = start;
		do
		{
			keyPos = Tokenizer.scanKeyWords(keyWords, tokens, begin, next);
			if(keyPos < 0)
			{
				Token[] subTokens = Arrays.copyOfRange(tokens, begin, next);
				tokenList.add(subTokens);
			}
			else
			{
				Token[] subTokens = Arrays.copyOfRange(tokens, begin, keyPos);
				tokenList.add(subTokens);
				int mergeType = 0;
				if(tokens[keyPos].isKeyWord("UNION"))
				{
					mergeType = unionType;
				}
				else if(tokens[keyPos].isKeyWord("INTERSECT"))
				{
					mergeType = intersectType;
				}
				else if(tokens[keyPos].isKeyWord("EXCEPT"))
				{
					mergeType = exceptType;
				}
				else if(tokens[keyPos].isKeyWord("MINUS"))
				{
					mergeType = exceptType;
				}
				if(keyPos+1 == next)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, SQL语句长度错误");
				}
				if(tokens[keyPos+1].getType() == Tokenizer.KEYWORD && tokens[keyPos+1].isKeyWord("ALL"))
				{
					if(mergeType != unionType)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 不支持EXCEPT(MINUS)或INTERSECT与ALL关键字连用");
					}
					mergeType |= allType;
					begin = keyPos + 2;
				}
				else
				{
					begin = keyPos + 1;
				}
				if(begin == next)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, SQL语句长度错误");
				}
				mergeList.add(mergeType);
			}
		}
		while(keyPos >= 0);
		
		if(tokenList.size() > 0)
		{
			for(int i=0, sz=tokenList.size(); i<sz; i++)
			{
				Token[] subTokens = tokenList.get(i);
				int len = subTokens.length;
				int orderPos = Tokenizer.scanKeyWord("ORDER", subTokens, 0, len);
				if(orderPos >= 0)
				{
					if(i != sz-1)
					{
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("syntax.error") + ":scanUnion, 有集合关系的语句ORDER子句必须在最后且唯一");
					}
					else  // 否则作为全局的order记录
					{
						Token[] orderTokens = Arrays.copyOfRange(subTokens, orderPos, len);
						orderList.addAll(Arrays.asList(orderTokens));
						subTokens = Arrays.copyOfRange(subTokens, 0, orderPos);
						len = subTokens.length;
					}
				}
				while(subTokens[0].getType() == Tokenizer.LPAREN) //脱去无意义的括号
				{
					int end = Tokenizer.scanParen(subTokens, 0, len);
					if(end == len - 1)
					{
						subTokens = Arrays.copyOfRange(subTokens, 1, len-1);
						len = subTokens.length;
					}
					else
					{
						break;
					}
				}
				orderPos = Tokenizer.scanKeyWord("ORDER", subTokens, 0, len);
				if(orderPos >= 0)
				{
					int topPos = Tokenizer.scanKeyWord("TOP", subTokens, 0, len);
					int limitPos = Tokenizer.scanKeyWord("LIMIT", subTokens, 0, len);
					int offsetPos = Tokenizer.scanKeyWord("OFFSET", subTokens, 0, len);
					if(topPos < 0 && limitPos < 0 && offsetPos < 0) // 包含在括号里的没有top/limit/offset的无意义order将被优化掉
					{
						subTokens = Arrays.copyOfRange(subTokens, 0, orderPos);
						len = subTokens.length;
					}
				}
				while(subTokens[0].getType() == Tokenizer.LPAREN)//裁剪order后再次脱去无意义的括号
				{
					int end = Tokenizer.scanParen(subTokens, 0, len);
					if(end == len - 1)
					{
						subTokens = Arrays.copyOfRange(subTokens, 1, len-1);
						len = subTokens.length;
					}
					else
					{
						break;
					}
				}
				resultList.add(subTokens);
			}
		}
		
		return resultList;
	}
	
	private void addTable(Token[] tokens, int start, int next)
	{
		if (start+4 >= next)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句长度错误");
		}
		else if (tokens[start].getType() != Tokenizer.IDENT )
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句表别名错误");
		}
		else if (!tokens[start+1].isKeyWord("AS"))
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句AS关键字错误");
		}
		else if (tokens[start+2].getType() != Tokenizer.LPAREN || tokens[next-1].getType() != Tokenizer.RPAREN)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句左右括号错误");
		}
		
		String tableName = tokens[start].getString();
		String tableExpress = "";
		Token[] newTokens = new Token[next-start-4];
		for(int i=start+3, j=0; i<=next-2; i++, j++)
		{
			tableExpress += tokens[i].getOriginString();
			tableExpress += tokens[i].getSpaces();
			newTokens[j] = tokens[i];
		}
		
		tableExpress = tableExpress.trim();
		
		Object tableNode = null;	
		if(tableExpress.startsWith("\"") && tableExpress.endsWith("\"") //带""的默认是表文件名，不再支持
			&& tableExpress.substring(1, tableExpress.length()-1).indexOf("\"") == -1)
		{
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句中只能使用集算器脚本或子查询语句");
		}
		else //否则可能是集算器表达式、子查询语句或表名
		{
			try
			{
				Expression exp = new Expression(this.ics, this.ctx, tableExpress);
				exp.calculate(this.ctx);
				tableNode = exp;
			}
			catch(RQException rq)
			{
				String[] keywords = new String[]{"SELECT", "WITH", "UNION", "INTERSECT", "EXCEPT", "MINUS"};
				int keyPos = Tokenizer.scanKeyWords(keywords, newTokens, 0, newTokens.length);
				boolean isSubQuery = ((keyPos < 0) ? false : true);
				if(isSubQuery)
				{
					try
					{
						int intoPos = -1;
						int withPos = -1;
						int orderPos =  -1;
						int topPos = -1;
						int limitPos = -1;
						int offsetPos = -1;
						for(int i=0; i<newTokens.length; i++)
						{
							if(newTokens[i].isKeyWord("INTO"))
							{
								intoPos = i;
							}
							else if(newTokens[i].isKeyWord("WITH"))
							{
								withPos = i;
							}
							else if(newTokens[i].isKeyWord("ORDER"))
							{
								orderPos = i;
							}
							else if(newTokens[i].isKeyWord("TOP"))
							{
								topPos = i;
							}
							else if(newTokens[i].isKeyWord("LIMIT"))
							{
								limitPos = i;
							}
							else if(newTokens[i].isKeyWord("OFFSET"))
							{
								offsetPos = i;
							}
						}
						if(intoPos >= 0)
						{
							throw new RQException("INTO");
						}
						else if(withPos >= 0)
						{
							throw new RQException("WITH");
						}
						else if(orderPos >= 0)
						{
							if(topPos < 0 && limitPos < 0 && offsetPos < 0)
							{
								throw new RQException("ORDER");
							}
						}
						Map<String, Object> subWithTableMap = new HashMap<String, Object>();
						subWithTableMap.putAll(this.withTableMap);//生成新的Map以防止SimpleSQL回调自身造成死循环
						SimpleSQL lq = new SimpleSQL(this.ics, newTokens, 0, newTokens.length, this.parameterList, this.ctx, false);
						lq.setMemory(this.isMemory);
						lq.setWithTableMap(subWithTableMap);
						lq.query();
						tableNode = lq;
					}
					catch(RQException ex)
					{
						String msg = ex.getMessage();
						if(msg != null && msg.equalsIgnoreCase("INTO"))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句中不能使用INTO关键字");
						}
						else if(msg != null && msg.equalsIgnoreCase("WITH"))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句中不能嵌套WITH关键字");
						}
						else if(msg != null && msg.equalsIgnoreCase("ORDER"))
						{
							MessageManager mm = ParseMessage.get();
							throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句中不能使用ORDER关键字除非有TOP/LIMIT/OFFSET关键字");
						}

						isSubQuery = false;
					}
				}
				if(!isSubQuery) //不再支持表文件路径
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":addTable, WITH子句中只能使用集算器脚本或子查询语句");
				}
			}
		}
		this.withTableMap.put(tableName, tableNode);
	}
	
	private int scanWith(Token[] tokens, int start, int next)
	{
		int beginPos = start;
		int endPos = beginPos;
		int withPos = Tokenizer.scanKeyWord("WITH", tokens, start, next);
		if(withPos >= 0)
		{
			if(withPos != start)
			{
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + ":scanWith, WITH关键字位置错误");
			}
			beginPos = withPos+1;
			while(beginPos + 5 < next && tokens[beginPos].getType() == Tokenizer.IDENT 
					&& tokens[beginPos + 1].isKeyWord("AS") 
					&& tokens[beginPos + 2].getType() == Tokenizer.LPAREN)
			{
				int end = Tokenizer.scanParen(tokens, beginPos + 2, next);
				endPos = end + 1;
				if(endPos == next)
				{
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + ":scanWith, WITH子句缺少主语句");
				}
				else if(tokens[endPos].getType() != Tokenizer.COMMA)
				{
					break;
				}
				beginPos = endPos + 1;
			}
			beginPos = withPos+1;
			while(withPos >= 0)
			{
				if(this.withTableMap == null)
				{
					this.withTableMap = new HashMap<String, Object>();
				}
				int commaPos = Tokenizer.scanComma(tokens, beginPos, endPos);
				if(commaPos < 0)
				{
					addTable(tokens, beginPos, endPos);
					break;
				}
				else
				{
					addTable(tokens, beginPos, commaPos);
					beginPos = commaPos+1;
				}
			}
		}
		return endPos;
	}
	
	public void setWithTableMap(Map<String, Object> tableMap)
	{
		this.withTableMap = tableMap;
	}
	
	void setMemory(boolean isMemory)
	{
		this.isMemory = isMemory;
	}
}
