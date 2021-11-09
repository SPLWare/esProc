package com.raqsoft.dm.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.raqsoft.common.MD5;
import com.raqsoft.common.RQException;
import com.raqsoft.common.UUID;

public class PerfectSubquery //只针对from子句中的子查询的优化
{
	public static boolean optimizeSubquery(Token[] tokens, List<Token> sqlTokenList, boolean isSubquery, List<Boolean> canOptimizeList)
	{
		if(sqlTokenList == null || canOptimizeList == null)
		{
			throw new RQException("");
		}
		
		boolean hasOptimized = false;
		
		int unionPos = Tokenizer.scanKeyWords(new String[]{"UNION", "EXCEPT", "INTERSECT", "MINUS"}, tokens, 0, tokens.length);
		if(unionPos >= 0)
		{
			while(unionPos >= 0)
			{
				Token[] leftTokens = Arrays.copyOfRange(tokens, 0, unionPos);
				
				Token[] keyTokens = null;
				if(tokens[unionPos].isKeyWord("UNION"))
				{
					if(unionPos + 1 < tokens.length && tokens[unionPos + 1].isKeyWord("ALL"))
					{
						keyTokens = new Token[2];
						keyTokens[0] = tokens[unionPos];
						keyTokens[1] = tokens[unionPos + 1];
					}
					else
					{
						keyTokens = new Token[1];
						keyTokens[0] = tokens[unionPos];
					}
				}
				else
				{
					keyTokens = new Token[1];
					keyTokens[0] = tokens[unionPos];
				}
				
				Token[] rightTokens = null;
				if(keyTokens.length == 1)
				{
					rightTokens = Arrays.copyOfRange(tokens, unionPos + 1, tokens.length);
				}
				else
				{
					rightTokens = Arrays.copyOfRange(tokens, unionPos + 2, tokens.length);
				}

				while(leftTokens[0].getType() == Tokenizer.LPAREN)//最外侧脱括号
				{
					int parenPos = Tokenizer.scanParen(leftTokens, 0, leftTokens.length);
					if(parenPos == leftTokens.length - 1)
					{
						leftTokens = Arrays.copyOfRange(leftTokens, 1, parenPos);
					}
					else
					{
						break;
					}
				}
				
				Token[] bakTokens = SimpleSQL.copyTokens(leftTokens);//准备尝试优化
				List<Token> tokenList = new ArrayList<Token>();
				boolean leftOptimize = optimizeSubquery(leftTokens, tokenList, isSubquery, new ArrayList<Boolean>());
				if(!leftOptimize)
				{
					leftTokens = bakTokens;
				}
				else
				{
					leftTokens = new Token[tokenList.size()];
					tokenList.toArray(leftTokens);
				}
				
				Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
				tk.addSpace();
				sqlTokenList.add(tk);
				sqlTokenList.addAll(Arrays.asList(leftTokens));
				tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
				tk.addSpace();
				sqlTokenList.add(tk);
				sqlTokenList.addAll(Arrays.asList(keyTokens));
				
				hasOptimized = hasOptimized || leftOptimize;
				
				tokens = rightTokens;
				
				unionPos = Tokenizer.scanKeyWords(new String[]{"UNION", "EXCEPT", "INTERSECT", "MINUS"}, tokens, 0, tokens.length);
				if(unionPos < 0)
				{
					while(tokens[0].getType() == Tokenizer.LPAREN)//最外侧脱括号
					{
						int parenPos = Tokenizer.scanParen(tokens, 0, tokens.length);
						if(parenPos == tokens.length - 1)
						{
							tokens = Arrays.copyOfRange(tokens, 1, parenPos);
						}
						else
						{
							break;
						}
					}
					
					bakTokens = SimpleSQL.copyTokens(tokens);//准备尝试优化
					tokenList = new ArrayList<Token>();
					boolean rightOptimize = optimizeSubquery(tokens, tokenList, isSubquery, new ArrayList<Boolean>());
					if(!rightOptimize)
					{
						tokens = bakTokens;
					}
					else
					{
						tokens = new Token[tokenList.size()];
						tokenList.toArray(tokens);
					}
					
					tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
					tk.addSpace();
					sqlTokenList.add(tk);
					sqlTokenList.addAll(Arrays.asList(tokens));
					tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
					tk.addSpace();
					sqlTokenList.add(tk);
					
					hasOptimized = hasOptimized || rightOptimize;
				}
			}
		}
		else
		{
			List<List<Token[]>> tokensListList = new ArrayList<List<Token[]>>();
			hasOptimized = getSubquery(tokens, tokensListList, isSubquery, canOptimizeList);
			
			List<String> usedAliasList = new ArrayList<String>();
			if(tokensListList.size() > 1 && canOptimizeList.size() > 1)
			{
				for(List<Token[]> tokensList : tokensListList)
				{
					Token[] fromTokens = tokensList.get(2);
					String tableAlias = null;
					if(fromTokens.length >= 2)
					{
						if(fromTokens[fromTokens.length - 1].getType() == Tokenizer.IDENT 
						&& fromTokens[fromTokens.length - 2].getType() == Tokenizer.RPAREN)
						{
							tableAlias = fromTokens[fromTokens.length - 1].getString();
						}
						else if(fromTokens[fromTokens.length - 1].getType() == Tokenizer.IDENT 
						&& fromTokens[fromTokens.length - 2].getSpaces().length() >= 1
						&& fromTokens[fromTokens.length - 2].getType() == Tokenizer.IDENT)
						{
							tableAlias = fromTokens[fromTokens.length - 1].getString();
						}
					}
					if(tableAlias != null && tableAlias.isEmpty())
					{
						usedAliasList.add(tableAlias);
					}
				}
			}
			
			while(tokensListList.size() > 1 && canOptimizeList.size() > 1)
			{
				Boolean subCanOptimize = canOptimizeList.get(0);
				Boolean canOptimize = canOptimizeList.get(1);
				canOptimizeList.remove(subCanOptimize);
				
				List<Token[]> subTokensList = tokensListList.get(0);
				List<Token[]> tokensList = tokensListList.get(1);
				tokensListList.remove(subTokensList);
				tokensListList.remove(tokensList);

				Token[] selectTokens = tokensList.get(0);
				Token[] parallelTokens = tokensList.get(1);
				Token[] fromTokens = tokensList.get(2);
				Token[] whereTokens = tokensList.get(3);
				
				Token[] subSelectTokens = subTokensList.get(0);
				Token[] subParallelTokens = subTokensList.get(1);
				Token[] subFromTokens = subTokensList.get(2);
				Token[] subWhereTokens = subTokensList.get(3);
				
				if(subCanOptimize && canOptimize)
				{
					String tableAlias = null;
					String tableName = "";
					int pos = 0;
					
					while(pos < fromTokens.length)
					{
						tableName = tableName + fromTokens[pos].getOriginString();
						tableName = tableName + fromTokens[pos].getSpaces();
						pos++;
					}
					tableName = tableName.trim();
					
					if (fromTokens.length - 2 >= 0 && fromTokens[fromTokens.length - 1].getType() == Tokenizer.IDENT)
					{
						int splitPos = tableName.lastIndexOf(" ");
						if(splitPos != -1)
						{
							tableAlias = tableName.substring(splitPos + 1);
							if(tableAlias.equals(fromTokens[fromTokens.length - 1].getOriginString()))
							{
								tableName = tableName.substring(0, splitPos).trim();
							}
							else
							{
								tableAlias = null;
							}
						}
					}
					
					if(tableAlias == null)
					{
						Token tailToken = fromTokens[fromTokens.length - 1];
						if(tailToken.getSpaces().isEmpty())
						{
							tailToken.addSpace();
						}
						
						tableAlias = getNewAlias();
						
						List<Token> tmpTokenList = new ArrayList<Token>();
						tmpTokenList.addAll(Arrays.asList(fromTokens));
						
						Token aliasToken = new Token(Tokenizer.IDENT, tableAlias, -1, tableAlias);
						aliasToken.addSpace();
						tmpTokenList.add(aliasToken);
						
						fromTokens = new Token[tmpTokenList.size()];
						tmpTokenList.toArray(fromTokens);
					}
					
					String subTableAlias = null;
					String subTableName = "";
					pos = 0;
					
					while(pos < subFromTokens.length)
					{
						subTableName = subTableName + subFromTokens[pos].getOriginString();
						subTableName = subTableName + subFromTokens[pos].getSpaces();
						pos++;
					}
					subTableName = subTableName.trim();
					
					if (subFromTokens.length - 2 >= 0 && subFromTokens[subFromTokens.length - 1].getType() == Tokenizer.IDENT)
					{
						int splitPos = subTableName.lastIndexOf(" ");
						if(splitPos != -1)
						{
							subTableAlias = subTableName.substring(splitPos + 1);
							if(subTableAlias.equals(subFromTokens[subFromTokens.length - 1].getOriginString()))
							{
								subTableName = subTableName.substring(0, splitPos).trim();
							}
							else
							{
								subTableAlias = null;
							}
						}
					}
					
					if(subTableAlias == null)
					{
						Token tailToken = subFromTokens[subFromTokens.length - 1];
						if(tailToken.getSpaces().isEmpty())
						{
							tailToken.addSpace();
						}
						
						subTableAlias = getNewAlias();
						
						List<Token> tmpTokenList = new ArrayList<Token>();
						tmpTokenList.addAll(Arrays.asList(subFromTokens));
						
						Token aliasToken = new Token(Tokenizer.IDENT, subTableAlias, -1, subTableAlias);
						aliasToken.addSpace();
						tmpTokenList.add(aliasToken);
						
						subFromTokens = new Token[tmpTokenList.size()];
						tmpTokenList.toArray(subFromTokens);
					}
					
					selectTokens = regulateFieldTokens(selectTokens, tableAlias);
					whereTokens = regulateFieldTokens(whereTokens, tableAlias);
					subSelectTokens = regulateFieldTokens(subSelectTokens, subTableAlias);
					subWhereTokens = regulateFieldTokens(subWhereTokens, subTableAlias);
					
					Map<String, Token[]> subAliasColumnMap = new LinkedHashMap<String, Token[]>();
					if(subSelectTokens.length > 1)
					{
						int start = 1;
						int end = -1;
						do
						{
							end = Tokenizer.scanComma(subSelectTokens, start, subSelectTokens.length);
							if(end == -1)
							{
								end = subSelectTokens.length;
							}
							
							Token[] columnTokens = Arrays.copyOfRange(subSelectTokens, start, end);
							if(columnTokens.length < 2 || columnTokens[columnTokens.length - 1].getType() != Tokenizer.IDENT)
							{
								if(columnTokens.length == 0)
								{
									throw new RQException("子查询中列表达式不能为空");
								}
								else if(columnTokens.length == 1 && (columnTokens[0].getType() == Tokenizer.IDENT || columnTokens[0].getString().equals("*")))
								{
									Token aliasToken = columnTokens[columnTokens.length - 1];
									subAliasColumnMap.put(aliasToken.getString(), columnTokens);
								}
								else
								{
									throw new RQException("子查询中列表达式必须要有正确的别名");
								}
							}
							else
							{
								if(!columnTokens[columnTokens.length - 2].isKeyWord("AS") 
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.IDENT
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.NUMBER
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.RPAREN
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.STRING)
								{
									throw new RQException("子查询中列表达式的格式存在错误");
								}
										
								Token aliasToken = columnTokens[columnTokens.length - 1];
										
								columnTokens = Arrays.copyOfRange(columnTokens, 0, columnTokens.length - 1);
										
								if(columnTokens[columnTokens.length - 1].isKeyWord("AS"))
								{
									columnTokens = Arrays.copyOfRange(columnTokens, 0, columnTokens.length - 1);
								}
										
								subAliasColumnMap.put(aliasToken.getString(), columnTokens);
							}
							
							start = end + 1;
						}
						while(start < subSelectTokens.length);
					}
					
					fromTokens = subFromTokens;
					
					if(selectTokens.length == 2 && selectTokens[1].getString().equals("*"))
					{
						List<Token> tokenList = new ArrayList<Token>();
						tokenList.add(selectTokens[0]);
						for(Map.Entry<String, Token[]> subAliasColumnEntry : subAliasColumnMap.entrySet())
						{
							if(tokenList.size() > 1)
							{
								Token tk = new Token(Tokenizer.COMMA, ",", -1, ",");
								tk.addSpace();
								tokenList.add(tk);
							}

							Token[] columnTokens = subAliasColumnEntry.getValue();
							tokenList.addAll(Arrays.asList(columnTokens));
							
							String alias = subAliasColumnEntry.getKey();
							if(!alias.equals("*"))
							{
								Token tk = new Token(Tokenizer.IDENT, alias, -1, alias);
								tk.addSpace();
								tokenList.add(tk);
							}
						}
						
						selectTokens = new Token[tokenList.size()];
						tokenList.toArray(selectTokens);
					}
					else
					{
						Map<Token, Token[]> aliasColumnMap = new LinkedHashMap<Token, Token[]>();
						
						int start = 1;
						int end = -1;
						int index = 1;
						do
						{
							end = Tokenizer.scanComma(selectTokens, start, selectTokens.length);
							if(end == -1)
							{
								end = selectTokens.length;
							}
							
							Token[] columnTokens = Arrays.copyOfRange(selectTokens, start, end);
							if(columnTokens.length >= 2)
							{
								if(columnTokens[columnTokens.length - 1].getType() != Tokenizer.IDENT)
								{
									Token tk = new Token(Tokenizer.IDENT, "_"+index, -1, "_"+index++);
									tk.addSpace();
									aliasColumnMap.put(tk, columnTokens);
								}
								else if(!columnTokens[columnTokens.length - 2].isKeyWord("AS") 
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.IDENT
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.NUMBER
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.RPAREN
								&& columnTokens[columnTokens.length - 2].getType() != Tokenizer.STRING)
								{
									Token tk = new Token(Tokenizer.IDENT, "_"+index, -1, "_"+index++);
									tk.addSpace();
									aliasColumnMap.put(tk, columnTokens);
								}
								else
								{
									Token aliasToken = columnTokens[columnTokens.length - 1];
									columnTokens = Arrays.copyOfRange(columnTokens, 0, columnTokens.length - 1);		
									if(columnTokens[columnTokens.length - 1].isKeyWord("AS"))
									{
										columnTokens = Arrays.copyOfRange(columnTokens, 0, columnTokens.length - 1);
									}		
									aliasColumnMap.put(aliasToken, columnTokens);
								}
							}
							else
							{
								Token tk = new Token(Tokenizer.IDENT, "_"+index, -1, "_"+index++);
								tk.addSpace();
								aliasColumnMap.put(tk, columnTokens);
							}
							
							start = end + 1;
						}
						while(start < selectTokens.length);
						
						Map<Token, Token[]> newAliasColumnMap = new LinkedHashMap<Token, Token[]>();
						for(Map.Entry<Token, Token[]> aliasColumnEntry : aliasColumnMap.entrySet())
						{
							Token aliasToken = aliasColumnEntry.getKey();
							Token[] columnTokens = aliasColumnEntry.getValue();
							List<Token> columnTokenList = new ArrayList<Token>();
							for(int i = 0; i < columnTokens.length; i++)
							{
								Token columnToken = columnTokens[i];
								if(columnToken.getType() == Tokenizer.IDENT)
								{
									if(i < columnTokens.length - 2 
									&& columnTokens[i + 1].getType() == Tokenizer.DOT 
									&& columnTokens[i + 2].getType() == Tokenizer.IDENT
									&& columnToken.getString().equalsIgnoreCase(tableAlias)) // T.F
									{
										String subColumnAlias = columnTokens[i + 2].getString();
										for(String subAlias : subAliasColumnMap.keySet())
										{
											if(subAlias.equalsIgnoreCase(subColumnAlias))
											{
												subColumnAlias = subAlias;
												break;
											}
										}
										
										Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
										if(subColumnTokens == null)
										{
											columnTokenList.add(columnTokens[i + 2]);
										}
										else
										{
											if(subColumnTokens.length == 1)
											{
												columnTokenList.add(subColumnTokens[0]);
											}
											else if(subColumnTokens.length == 3 
											&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
											&& subColumnTokens[1].getType() == Tokenizer.DOT
											&& subColumnTokens[2].getType() == Tokenizer.IDENT)
											{
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
											&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
											{
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else
											{
												Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
												tk.addSpace();
												columnTokenList.add(tk);
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
												tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
												tk.addSpace();
												columnTokenList.add(tk);
											}
										}
										i = i + 2;
									}
									else if(i < columnTokens.length - 2 
									&& columnTokens[i + 1].getType() == Tokenizer.LPAREN) //fun()//非聚合普通函数表达式
									{
										for(int j = i; j <= i + 1; j++)
										{
											columnTokenList.add(columnTokens[j]);
										}
										i = i + 1;
									}
									else //F
									{
										String subColumnAlias = columnToken.getString();
										for(String subAlias : subAliasColumnMap.keySet())
										{
											if(subAlias.equalsIgnoreCase(subColumnAlias))
											{
												subColumnAlias = subAlias;
												break;
											}
										}
										
										Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
										if(subColumnTokens == null)
										{
											columnTokenList.add(columnToken);
										}
										else
										{
											if(subColumnTokens.length == 1)
											{
												columnTokenList.add(subColumnTokens[0]);
											}
											else if(subColumnTokens.length == 3 
											&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
											&& subColumnTokens[1].getType() == Tokenizer.DOT
											&& subColumnTokens[2].getType() == Tokenizer.IDENT)
											{
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
											&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
											{
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else
											{
												Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
												tk.addSpace();
												columnTokenList.add(tk);
												columnTokenList.addAll(Arrays.asList(subColumnTokens));
												tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
												tk.addSpace();
												columnTokenList.add(tk);
											}
										}
									}
								}
								else
								{
									columnTokenList.add(columnToken);
								}
							}
							
							columnTokens = new Token[columnTokenList.size()];
							columnTokenList.toArray(columnTokens);
							newAliasColumnMap.put(aliasToken, columnTokens);
						}
						
						aliasColumnMap = newAliasColumnMap;
						
						List<Token> tokenList = new ArrayList<Token>();
						tokenList.add(selectTokens[0]);
						
						for(Map.Entry<Token, Token[]> aliasColumnEntry : aliasColumnMap.entrySet())
						{
							if(tokenList.size() > 1)
							{
								Token tk = new Token(Tokenizer.COMMA, ",", -1, ",");
								tk.addSpace();
								tokenList.add(tk);
							}
							Token aliasToken = aliasColumnEntry.getKey();
							Token[] columnTokens = aliasColumnEntry.getValue();
							tokenList.addAll(Arrays.asList(columnTokens));
							tokenList.add(aliasToken);
						}
						
						selectTokens = new Token[tokenList.size()];
						tokenList.toArray(selectTokens);
					}
					
					if(whereTokens != null)
					{
						List<Token> whereTokenList = new ArrayList<Token>();
						for(int i = 0; i < whereTokens.length; i++)
						{
							Token whereToken = whereTokens[i];
							if(whereToken.getType() == Tokenizer.IDENT)
							{
								if(i < whereTokens.length - 2 
								&& whereTokens[i + 1].getType() == Tokenizer.DOT 
								&& whereTokens[i + 2].getType() == Tokenizer.IDENT
								&& whereToken.getString().equalsIgnoreCase(tableAlias)) // T.F
								{
									String subColumnAlias = whereTokens[i + 2].getString();
									for(String subAlias : subAliasColumnMap.keySet())
									{
										if(subAlias.equalsIgnoreCase(subColumnAlias))
										{
											subColumnAlias = subAlias;
											break;
										}
									}
									
									Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
									if(subColumnTokens == null)
									{
										whereTokenList.add(whereToken);
									}
									else
									{
										if(subColumnTokens.length == 1)
										{
											whereTokenList.add(subColumnTokens[0]);
										}
										else if(subColumnTokens.length == 3 
										&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
										&& subColumnTokens[1].getType() == Tokenizer.DOT
										&& subColumnTokens[2].getType() == Tokenizer.IDENT)
										{
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
										}
										else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
										&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
										{
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
										}
										else
										{
											Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
											tk.addSpace();
											whereTokenList.add(tk);
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
											tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
											tk.addSpace();
											whereTokenList.add(tk);
										}
									}
									i = i + 2;
								}
								else if(i < whereTokens.length - 2 
								&& whereTokens[i + 1].getType() == Tokenizer.LPAREN) //fun()//非聚合普通函数表达式
								{
									for(int j = i; j <= i + 1; j++)
									{
										whereTokenList.add(whereTokens[j]);
									}
									
									i = i + 1;
								}
								else //F
								{
									String subColumnAlias = whereToken.getString();
									for(String subAlias : subAliasColumnMap.keySet())
									{
										if(subAlias.equalsIgnoreCase(subColumnAlias))
										{
											subColumnAlias = subAlias;
											break;
										}
									}
									
									Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
									if(subColumnTokens == null)
									{
										whereTokenList.add(whereToken);
									}
									else
									{
										if(subColumnTokens.length == 1)
										{
											whereTokenList.add(subColumnTokens[0]);
										}
										else if(subColumnTokens.length == 3 
										&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
										&& subColumnTokens[1].getType() == Tokenizer.DOT
										&& subColumnTokens[2].getType() == Tokenizer.IDENT)
										{
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
										}
										else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
										&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
										{
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
										}
										else
										{
											Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
											tk.addSpace();
											whereTokenList.add(tk);
											whereTokenList.addAll(Arrays.asList(subColumnTokens));
											tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
											tk.addSpace();
											whereTokenList.add(tk);
										}
									}
								}
							}
							else
							{
								whereTokenList.add(whereToken);
							}
						}
						
						whereTokens = new Token[whereTokenList.size()];
						whereTokenList.toArray(whereTokens);
						
						if(subWhereTokens != null)
						{
							List<Token> tokenList = new ArrayList<Token>();
							Token tk = new Token(Tokenizer.KEYWORD, "WHERE", -1, "WHERE");
							tk.addSpace();
							tokenList.add(tk);
							tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
							tk.addSpace();
							tokenList.add(tk);
							tokenList.addAll(Arrays.asList(Arrays.copyOfRange(subWhereTokens, 1, subWhereTokens.length)));
							tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
							tk.addSpace();
							tokenList.add(tk);
							tk = new Token(Tokenizer.KEYWORD, "AND", -1, "AND");
							tk.addSpace();
							tokenList.add(tk);
							tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
							tk.addSpace();
							tokenList.add(tk);
							tokenList.addAll(Arrays.asList(Arrays.copyOfRange(whereTokens, 1, whereTokens.length)));
							tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
							tk.addSpace();
							tokenList.add(tk);
							whereTokens = new Token[tokenList.size()];
							tokenList.toArray(whereTokens);
						}
					}
					else
					{
						if(subWhereTokens != null)
						{
							whereTokens = subWhereTokens;
						}
					}
					
					if(subParallelTokens != null)
					{
						parallelTokens = subParallelTokens;
					}
					
					List<Token[]> newTokensList = new ArrayList<Token[]>();
					newTokensList.add(selectTokens);
					newTokensList.add(parallelTokens);
					newTokensList.add(fromTokens);
					newTokensList.add(whereTokens);
						
					if(tokensListList.isEmpty() && !isSubquery)
					{
						Token[] orderTokens = tokensList.get(4);
						if(orderTokens != null)
						{
							List<Token> orderTokenList = new ArrayList<Token>();
							for(int i = 0; i < orderTokens.length; i++)
							{
								Token orderToken = orderTokens[i];
								if(orderToken.getType() == Tokenizer.IDENT)
								{
									if(i < orderTokens.length - 2 
									&& orderTokens[i + 1].getType() == Tokenizer.DOT 
									&& orderTokens[i + 2].getType() == Tokenizer.IDENT
									&& orderToken.getString().equalsIgnoreCase(tableAlias)) // T.F
									{
										String subColumnAlias = orderTokens[i + 2].getString();
										for(String subAlias : subAliasColumnMap.keySet())
										{
											if(subAlias.equalsIgnoreCase(subColumnAlias))
											{
												subColumnAlias = subAlias;
												break;
											}
										}
										
										Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
										if(subColumnTokens == null)
										{
											orderTokenList.add(orderToken);
										}
										else
										{
											if(subColumnTokens.length == 1)
											{
												orderTokenList.add(subColumnTokens[0]);
											}
											else if(subColumnTokens.length == 3 
											&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
											&& subColumnTokens[1].getType() == Tokenizer.DOT
											&& subColumnTokens[2].getType() == Tokenizer.IDENT)
											{
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
											&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
											{
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else
											{
												Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
												tk.addSpace();
												orderTokenList.add(tk);
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
												tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
												tk.addSpace();
												orderTokenList.add(tk);
											}
										}
										i = i + 2;
									}
									else if(i < orderTokens.length - 2 
									&& orderTokens[i + 1].getType() == Tokenizer.LPAREN) //fun()//非聚合普通函数表达式
									{
										for(int j = i; j <= i + 1; j++)
										{
											orderTokenList.add(orderTokens[j]);
										}
										
										i = i + 1;
									}
									else //F
									{
										String subColumnAlias = orderToken.getString();
										for(String subAlias : subAliasColumnMap.keySet())
										{
											if(subAlias.equalsIgnoreCase(subColumnAlias))
											{
												subColumnAlias = subAlias;
												break;
											}
										}
										
										Token[] subColumnTokens = subAliasColumnMap.get(subColumnAlias);
										if(subColumnTokens == null)
										{
											orderTokenList.add(orderToken);
										}
										else
										{
											if(subColumnTokens.length == 1)
											{
												orderTokenList.add(subColumnTokens[0]);
											}
											else if(subColumnTokens.length == 3 
											&& subColumnTokens[0].getString().equalsIgnoreCase(subTableAlias)
											&& subColumnTokens[1].getType() == Tokenizer.DOT
											&& subColumnTokens[2].getType() == Tokenizer.IDENT)
											{
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else if(subColumnTokens[0].getType() == Tokenizer.LPAREN
											&& Tokenizer.scanParen(subColumnTokens, 0, subColumnTokens.length) == subColumnTokens.length - 1)
											{
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
											}
											else
											{
												Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
												tk.addSpace();
												orderTokenList.add(tk);
												orderTokenList.addAll(Arrays.asList(subColumnTokens));
												tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
												tk.addSpace();
												orderTokenList.add(tk);
											}
										}
									}
								}
								else
								{
									orderTokenList.add(orderToken);
								}
							}
							
							orderTokens = new Token[orderTokenList.size()];
							orderTokenList.toArray(orderTokens);
						}
						
						newTokensList.add(orderTokens);
						Token[] limitTokens = tokensList.get(5);
						newTokensList.add(limitTokens);
					}	
						
					tokensListList.add(0, newTokensList);
				}
				else if(subCanOptimize) //暂不优化，以后替换成深度优化
				{
					if(fromTokens.length > 3 && fromTokens[0].isKeyWord("FROM") && fromTokens[1].getType() == Tokenizer.LPAREN)
					{ 
						List<Token> fromTokenList = new ArrayList<Token>();
						fromTokenList.add(fromTokens[0]);
						fromTokenList.add(fromTokens[1]);
						int end = Tokenizer.scanParen(fromTokens, 1, fromTokens.length);
						fromTokenList.addAll(Arrays.asList(subSelectTokens));
						if(subParallelTokens != null)
						{
							fromTokenList.addAll(Arrays.asList(subParallelTokens));
						}
						fromTokenList.addAll(Arrays.asList(subFromTokens));
						if(subWhereTokens != null)
						{
							fromTokenList.addAll(Arrays.asList(subWhereTokens));
						}
						for(int i = end; i < fromTokens.length; i++)
						{
							fromTokenList.add(fromTokens[i]);
						}
						fromTokens = new Token[fromTokenList.size()];
						fromTokenList.toArray(fromTokens);
					}
					else
					{
						throw new RQException("");
					}
					
					List<Token[]> newTokensList = new ArrayList<Token[]>();
					newTokensList.add(selectTokens);
					newTokensList.add(parallelTokens);
					newTokensList.add(fromTokens);
					newTokensList.add(whereTokens);
					if(tokensListList.isEmpty() && !isSubquery)
					{
						Token[] orderTokens = tokensList.get(4);
						newTokensList.add(orderTokens);
						Token[] limitTokens = tokensList.get(5);
						newTokensList.add(limitTokens);
					}
					tokensListList.add(0, newTokensList);
				}
				else
				{
					if(fromTokens.length > 3 && fromTokens[0].isKeyWord("FROM") && fromTokens[1].getType() == Tokenizer.LPAREN)
					{ 
						List<Token> fromTokenList = new ArrayList<Token>();
						fromTokenList.add(fromTokens[0]);
						fromTokenList.add(fromTokens[1]);
						int end = Tokenizer.scanParen(fromTokens, 1, fromTokens.length);
						fromTokenList.addAll(Arrays.asList(subSelectTokens));
						fromTokenList.addAll(Arrays.asList(subFromTokens));
						for(int i = end; i < fromTokens.length; i++)
						{
							fromTokenList.add(fromTokens[i]);
						}
						fromTokens = new Token[fromTokenList.size()];
						fromTokenList.toArray(fromTokens);
					}
					else
					{
						throw new RQException("");
					}
					
					List<Token[]> newTokensList = new ArrayList<Token[]>();
					newTokensList.add(selectTokens);
					newTokensList.add(parallelTokens);
					newTokensList.add(fromTokens);
					newTokensList.add(whereTokens);
					if(tokensListList.isEmpty() && !isSubquery)
					{
						Token[] orderTokens = tokensList.get(4);
						newTokensList.add(orderTokens);
						Token[] limitTokens = tokensList.get(5);
						newTokensList.add(limitTokens);
					}
					tokensListList.add(0, newTokensList);
				}
				
				hasOptimized = hasOptimized || canOptimize;
			}
			
			List<Token[]> tokensList = tokensListList.get(0);
			
			Token[] selectTokens = tokensList.get(0);
			Token[] parallelTokens = tokensList.get(1);
			Token[] fromTokens = tokensList.get(2);
			Token[] whereTokens = tokensList.get(3);
			Token[] orderTokens = null;
			Token[] limitTokens = null;
			if(tokensList.size() > 4)
			{
				orderTokens = tokensList.get(4);
				limitTokens = tokensList.get(5);
			}
			
			sqlTokenList.add(selectTokens[0]);
			if(parallelTokens != null)
			{
				sqlTokenList.addAll(Arrays.asList(parallelTokens));
			}
			sqlTokenList.addAll(Arrays.asList(Arrays.copyOfRange(selectTokens, 1, selectTokens.length)));
			sqlTokenList.addAll(Arrays.asList(fromTokens));
			if(whereTokens != null)
			{
				Token whereToken = whereTokens[0];
				whereTokens = Arrays.copyOfRange(whereTokens, 1, whereTokens.length);
				whereTokens = new PerfectWhere(whereTokens, new ArrayList<Object>()).getTokens(true);
				Token[] tempTokens = new Token[whereTokens.length + 1];
				tempTokens[0] = whereToken;
				System.arraycopy(whereTokens, 0, tempTokens, 1, whereTokens.length);
				whereTokens = tempTokens;
				sqlTokenList.addAll(Arrays.asList(whereTokens));
			}
			
			if(orderTokens != null)
			{
				sqlTokenList.addAll(Arrays.asList(orderTokens));
			}
			
			if(limitTokens != null)
			{
				sqlTokenList.addAll(Arrays.asList(limitTokens));
			}
		}
		
		return hasOptimized;
	}
	
	private static boolean getSubquery(Token[] tokens, List<List<Token[]>> tokensListList, boolean isSubquery, List<Boolean> canOptimizeList)
	{
		if(tokensListList == null || canOptimizeList == null)
		{
			throw new RQException("");
		}
		
		boolean hasOptimized = false;
		boolean canOptimize = true;
		
		List<Token[]> tokensList = new ArrayList<Token[]>();
		
		Token[] selectTokens = null;
		Token[] parallelTokens = null;
		Token[] fromTokens = null;
		Token[] whereTokens = null;
		Token[] orderTokens = null;
		Token[] limitTokens = null;

		int len = tokens.length;
		
		int joinPos = Tokenizer.scanKeyWord("JOIN", tokens, 0, len);
		int havingPos = Tokenizer.scanKeyWord("HAVING", tokens, 0, len);
		int groupPos = Tokenizer.scanKeyWord("GROUP", tokens, 0, len);
		int distinctPos = Tokenizer.scanKeyWord("DISTINCT", tokens, 0, len);
		int intoPos = Tokenizer.scanKeyWord("INTO", tokens, 0, len);
		int withPos = Tokenizer.scanKeyWord("WITH", tokens, 0, len);
		int orderPos = Tokenizer.scanKeyWord("ORDER", tokens, 0, len);
		int topPos = Tokenizer.scanKeyWord("TOP", tokens, 0, len);
		int limitPos = Tokenizer.scanKeyWord("LIMIT", tokens, 0, len);
		int wherePos = Tokenizer.scanKeyWord("WHERE", tokens, 0, len);
		int fromPos = Tokenizer.scanKeyWord("FROM", tokens, 0, len);
		int selectPos = Tokenizer.scanKeyWord("SELECT", tokens, 0, len);
		int parallelPos = Tokenizer.scanKeyWord("PARALLEL", tokens, 0, len);

		if(isSubquery)
		{
			if(intoPos >= 0 || withPos >= 0)
			{
				throw new RQException("");
			}
			
			if(joinPos >= 0 || havingPos >= 0 || groupPos >= 0 || distinctPos >= 0 || topPos >= 0 || limitPos >= 0)
			{
				canOptimize = false;
			}
			else if(orderPos >= 0)
			{
				throw new RQException("");
			}
		}
		else
		{
			if(joinPos >= 0 || havingPos >= 0 || groupPos >= 0 || distinctPos >= 0 || intoPos >= 0)
			{
				canOptimize = false;
			}
			
			if(withPos >= 0)
			{
				canOptimize = false;
			}
		}
		
		for(int i = selectPos; i < fromPos; i++)
		{
			Token token = tokens[i];
			if(token.getType() == Tokenizer.IDENT && Tokenizer.isGatherFunction(token.getString()))
			{
				canOptimize = false;
			}
		}
		
		if(canOptimize)
		{
			if(!isSubquery)
			{
				if(topPos >= 0)
				{
					if(limitPos >= 0)
					{
						throw new RQException("");
					}
					
					limitTokens = new Token[2];
					selectTokens = new Token[len - 2];
					for(int i = 0, j = 0, k = 0; i < len; i++)
					{
						if(i >= topPos && i <= topPos + 1)
						{
							if(i == topPos)
							{
								Token tk = new Token(Tokenizer.KEYWORD, "LIMIT", -1, "LIMIT");
								tk.addSpace();
								limitTokens[j++] = tk;
							}
							else
							{
								limitTokens[j++] = tokens[i];
							}
						}
						else if(i < topPos || i > topPos + 1)
						{
							selectTokens[k++] = tokens[i];
						}
					}
					tokens = selectTokens;
				}
				else if(limitPos >= 0)
				{
					limitTokens = Arrays.copyOfRange(tokens, limitPos, len);
					tokens = Arrays.copyOfRange(tokens, 0, limitPos);
				}
				len = tokens.length;
				
				orderPos = Tokenizer.scanKeyWord("ORDER", tokens, 0, len);
				if(orderPos >= 0)
				{
					orderTokens = Arrays.copyOfRange(tokens, orderPos, len);
					tokens = Arrays.copyOfRange(tokens, 0, orderPos);
				}
				len = tokens.length;
			}
			
			if(wherePos >= 0)
			{
				whereTokens = Arrays.copyOfRange(tokens, wherePos, len);
				tokens = Arrays.copyOfRange(tokens, 0, wherePos);
			}
			len = tokens.length;
			
			if(fromPos >= 0)
			{
				fromTokens = Arrays.copyOfRange(tokens, fromPos, len);
				tokens = Arrays.copyOfRange(tokens, 0, fromPos);
			}
			else
			{
				throw new RQException("");
			}
			len = tokens.length;
			
			if(selectPos != 0)
			{
				throw new RQException("");
			}
			
			if(parallelPos >= 0)
			{
				if(parallelPos - 3 > selectPos && parallelPos + 5 < len
				&& tokens[parallelPos - 3].getString().equals("/")
				&& tokens[parallelPos - 2].getString().equals("*")
				&& tokens[parallelPos - 1].getString().equals("+")
				&& tokens[parallelPos + 1].getType() == Tokenizer.LPAREN
				&& tokens[parallelPos + 2].getType() == Tokenizer.NUMBER
				&& tokens[parallelPos + 3].getType() == Tokenizer.RPAREN
				&& tokens[parallelPos + 4].getString().equals("*")
				&& tokens[parallelPos + 5].getString().equals("/"))
				{
					parallelTokens = Arrays.copyOfRange(tokens, parallelPos - 3, parallelPos + 6);
					List<Token> tempTokenList = new ArrayList<Token>();
					for(int i = 0; i < len; i++)
					{
						if(i < parallelPos - 3 || i > parallelPos + 5)
						{
							tempTokenList.add(tokens[i]);
						}
					}
					selectTokens = new Token[tempTokenList.size()];
					tempTokenList.toArray(selectTokens);
				}
				else if(parallelPos - 3 > selectPos && parallelPos + 2 < len
				&& tokens[parallelPos - 3].getString().equals("/")
				&& tokens[parallelPos - 2].getString().equals("*")
				&& tokens[parallelPos - 1].getString().equals("+")
				&& tokens[parallelPos + 1].getString().equals("*")
				&& tokens[parallelPos + 2].getString().equals("/"))
				{
					parallelTokens = Arrays.copyOfRange(tokens, parallelPos - 3, parallelPos + 3);
					List<Token> tempTokenList = new ArrayList<Token>();
					for(int i = 0; i < len; i++)
					{
						if(i < parallelPos - 3 || i > parallelPos + 2)
						{
							tempTokenList.add(tokens[i]);
						}
					}
					selectTokens = new Token[tempTokenList.size()];
					tempTokenList.toArray(selectTokens);
				}
				else
				{
					throw new RQException("parallel子句格式错误");
				}
			}
			else
			{
				selectTokens = tokens;
			}
			
			if(selectTokens == null || fromTokens == null)
			{
				throw new RQException("");
			}
		}
		else
		{
			selectTokens = Arrays.copyOfRange(tokens, selectPos, fromPos);
			fromTokens = Arrays.copyOfRange(tokens, fromPos, len);
		}
		
		if(fromTokens.length > 2 && fromTokens[1].getType() == Tokenizer.LPAREN)
		{
			int subEnd = Tokenizer.scanParen(fromTokens, 1, fromTokens.length);
			int subUnionPos = Tokenizer.scanKeyWords(new String[]{"UNION", "EXCEPT", "INTERSECT", "MINUS"}, fromTokens, 2, subEnd);
			int subSelectPos = Tokenizer.scanKeyWord("SELECT", fromTokens, 2, subEnd);
			if(subSelectPos != -1 || subUnionPos != -1)
			{
				Token[] subTokens = Arrays.copyOfRange(fromTokens, 2, subEnd);
				List<Token> sqlTokenList = new ArrayList<Token>();
				if(subUnionPos != -1)
				{
					hasOptimized = optimizeSubquery(subTokens, sqlTokenList, true, new ArrayList<Boolean>());
					if(hasOptimized)
					{
						sqlTokenList.add(0, fromTokens[0]);
						sqlTokenList.add(1, fromTokens[1]);
						for(int i = subEnd; i < fromTokens.length; i++)
						{
							sqlTokenList.add(fromTokens[i]);
						}
						fromTokens = new Token[sqlTokenList.size()];
						sqlTokenList.toArray(fromTokens);
					}
				}
				else
				{
					hasOptimized = getSubquery(subTokens, tokensListList, true, canOptimizeList);
				}
			}
		}
		
		tokensList.add(selectTokens);
		tokensList.add(parallelTokens);
		tokensList.add(fromTokens);
		tokensList.add(whereTokens);
		if(!isSubquery)
		{
			tokensList.add(orderTokens);
			tokensList.add(limitTokens);
		}
		tokensListList.add(tokensList);
		canOptimizeList.add(canOptimize);
		
		return hasOptimized;
	}
	
	public static boolean optimizeCountAll(Token[] tokens, List<Token> tokenList)
	{
		if(tokenList == null || !tokenList.isEmpty())
		{
			return false;
		}
		
		int selectPos = Tokenizer.scanKeyWord("SELECT", tokens, 0, tokens.length);
		if(selectPos != 0)
		{
			return false;
		}
		
		int columnsPos = selectPos + 1;
		
		int fromPos = Tokenizer.scanKeyWord("FROM", tokens, columnsPos, tokens.length);
		if(fromPos > 0)
		{
			StringBuffer columnExp = new StringBuffer();
			for(int i = columnsPos; i < fromPos; i++)
			{
				columnExp.append(tokens[i].getString().toLowerCase());
			}
			
			if(!columnExp.toString().equals("count(*)"))
			{
				return false;
			}
		}
		else
		{
			throw new RQException("缺少FROM子句");
		}
		
		int parallelPos = Tokenizer.scanKeyWords(new String[]{"PARALLEL", "OLAP"}, tokens, columnsPos, fromPos);
		while(parallelPos > 0)
		{
			if(parallelPos - 3 > selectPos && parallelPos + 5 < fromPos
			&& tokens[parallelPos - 3].getString().equals("/")
			&& tokens[parallelPos - 2].getString().equals("*")
			&& tokens[parallelPos - 1].getString().equals("+")
			&& tokens[parallelPos].isKeyWord("PARALLEL")
			&& tokens[parallelPos + 1].getType() == Tokenizer.LPAREN
			&& tokens[parallelPos + 2].getType() == Tokenizer.NUMBER
			&& tokens[parallelPos + 3].getType() == Tokenizer.RPAREN
			&& tokens[parallelPos + 4].getString().equals("*")
			&& tokens[parallelPos + 5].getString().equals("/"))
			{
				columnsPos = parallelPos + 6;
			}
			else if(parallelPos - 3 > selectPos && parallelPos + 2 < fromPos
			&& tokens[parallelPos - 3].getString().equals("/")
			&& tokens[parallelPos - 2].getString().equals("*")
			&& tokens[parallelPos - 1].getString().equals("+")
			&& tokens[parallelPos].isKeyWord("PARALLEL")
			&& tokens[parallelPos + 1].getString().equals("*")
			&& tokens[parallelPos + 2].getString().equals("/"))
			{
				columnsPos = parallelPos + 3;
			}
			else if(parallelPos - 3 > selectPos && parallelPos + 2 < fromPos
			&& tokens[parallelPos - 3].getString().equals("/")
			&& tokens[parallelPos - 2].getString().equals("*")
			&& tokens[parallelPos - 1].getString().equals("+")
			&& tokens[parallelPos].isKeyWord("OLAP")
			&& tokens[parallelPos + 1].getString().equals("*")
			&& tokens[parallelPos + 2].getString().equals("/"))
			{
				columnsPos = parallelPos + 3;
			}
			else
			{
				throw new RQException("优化子句格式错误");
			}
			
			parallelPos = Tokenizer.scanKeyWords(new String[]{"PARALLEL", "OLAP"}, tokens, columnsPos, fromPos);
		}
		
		if(tokens[fromPos + 1].getType() == Tokenizer.LPAREN)
		{
			int end = Tokenizer.scanParen(tokens, fromPos + 1, tokens.length);
			Token[] subTokens = Arrays.copyOfRange(tokens, fromPos + 2, end);
			
			List<Token> resultList = new ArrayList<Token>();
			boolean canOptimize = checkSubquery(subTokens, resultList);
			if(!canOptimize)
			{
				return false;
			}
			
			if(end + 2 < tokens.length || end + 2 == tokens.length && tokens[end + 1].getType() != Tokenizer.IDENT)
			{
				return false;
			}
			
			
			for(int i = selectPos; i < tokens.length; i++)
			{
				if(i > fromPos + 1 && i < end)
				{
					tokenList.addAll(resultList);
					i = end - 1;
				}
				else
				{
					tokenList.add(tokens[i]);
				}
			}
		}
		else
		{
			return false;
		}
		
		return true;
	}
	
	private static boolean checkSubquery(Token[] tokens, List<Token> tokenList)
	{
		if(tokenList == null || !tokenList.isEmpty())
		{
			return false;
		}
		
		int unionPos = Tokenizer.scanKeyWords(new String[]{"UNION", "EXCEPT", "INTERSECT", "MINUS"}, tokens, 0, tokens.length);
		int groupPos = Tokenizer.scanKeyWord("GROUP", tokens, 0, tokens.length);
		int distinctPos = Tokenizer.scanKeyWord("DISTINCT", tokens, 0, tokens.length);
		int intoPos = Tokenizer.scanKeyWord("INTO", tokens, 0, tokens.length);
		int withPos = Tokenizer.scanKeyWord("WITH", tokens, 0, tokens.length);
		int orderPos = Tokenizer.scanKeyWord("ORDER", tokens, 0, tokens.length);

		int wherePos = Tokenizer.scanKeyWord("WHERE", tokens, 0, tokens.length);
		int fromPos = Tokenizer.scanKeyWord("FROM", tokens, 0, tokens.length);
		int selectPos = Tokenizer.scanKeyWord("SELECT", tokens, 0, tokens.length);
		int limitPos = Tokenizer.scanKeyWords(new String[]{"LIMIT", "OFFSET"}, tokens, 0, tokens.length);
		int topPos = Tokenizer.scanKeyWord("TOP", tokens, 0, tokens.length);
		int parallelPos = Tokenizer.scanKeyWord("PARALLEL", tokens, 0, tokens.length);
		int joinPos = Tokenizer.scanKeyWord("JOIN", tokens, 0, tokens.length);
		int havingPos = Tokenizer.scanKeyWord("HAVING", tokens, 0, tokens.length);

		if(intoPos >= 0 || withPos >= 0)
		{
			throw new RQException("");
		}
		
		if(topPos < 0 && limitPos < 0 && orderPos >= 0)
		{
			throw new RQException("");
		}
		
		if(unionPos >= 0 || groupPos >= 0 || distinctPos >= 0 || orderPos >= 0)
		{
			return false;
		}
		
		for(int i = selectPos; i < fromPos; i++)
		{
			Token token = tokens[i];
			if(token.getType() == Tokenizer.IDENT && Tokenizer.isGatherFunction(token.getString()))
			{
				return false;
			}
		}

		if(selectPos == 0)
		{
			tokenList.add(tokens[selectPos]);
		}
		else
		{
			throw new RQException("");
		}
		
		if(parallelPos > 0)
		{
			if(parallelPos != selectPos + 4 
			|| !tokens[selectPos+1].getString().equals("/")
			|| !tokens[selectPos+2].getString().equals("*")
			|| !tokens[selectPos+3].getString().equals("+"))
			{
				throw new RQException("");
			}
			else if(parallelPos + 5 < fromPos
			&& tokens[parallelPos+1].getString().equals("(")
			&& tokens[parallelPos+2].getType() == Tokenizer.NUMBER
			&& tokens[parallelPos+3].getString().equals(")")
			&& tokens[parallelPos+4].getString().equals("*")
			&& tokens[parallelPos+5].getString().equals("/"))
			{
				for(int i = selectPos + 1; i < parallelPos + 6; i++)
				{
					tokenList.add(tokens[i]);
				}
			}
			else if(parallelPos + 2 < fromPos
			&& tokens[parallelPos+1].getString().equals("*")
			&& tokens[parallelPos+2].getString().equals("/"))
			{
				for(int i = selectPos + 1; i < parallelPos + 3; i++)
				{
					tokenList.add(tokens[i]);
				}
			}
			else
			{
				throw new RQException("");
			}
		}
		
		if(topPos > 0)
		{
			for(int i = topPos; i < topPos + 2; i++)
			{
				tokenList.add(tokens[i]);
			}
		}
		
		Token tk = new Token(Tokenizer.NUMBER, "0", -1, "0");
		tk.addSpace();
		tokenList.add(tk);
		
		tk = new Token(Tokenizer.IDENT, "F", -1, "F");
		tk.addSpace();
		tokenList.add(tk);
		
		if(fromPos > 0)
		{
			for(int i = fromPos; i < tokens.length; i++)
			{
				tokenList.add(tokens[i]);
			}
		}
		else
		{
			throw new RQException("");
		}
		
		return true;
	}
	
	public static Token[] regulateFieldTokens(Token[] tokens, String tableAlias)
	{
		if(tokens != null && tokens.length > 0)
		{
			List<Token> tokenList = new ArrayList<Token>();
			if(tokens[0].isKeyWord("SELECT"))
			{
				tokenList.add(tokens[0]);
				
				int start = 1;
				int end = tokens.length;
				do
				{
					int comma = Tokenizer.scanComma(tokens, start, end);
					if(comma == -1)
					{
						comma = end;
					}
					
					Token[] tmpTokens = Arrays.copyOfRange(tokens, start, comma);
					
					StringBuffer buf = new StringBuffer();
					for(int pos = 0; pos < tmpTokens.length; pos++)
					{
						buf.append(tmpTokens[pos].getOriginString());
						buf.append(tmpTokens[pos].getSpaces());
					}
					String columnExp = buf.toString().trim();
					
					String aliasName = null;
					if (tmpTokens.length >= 2 && tmpTokens[tmpTokens.length - 1].getType() == Tokenizer.IDENT)
					{
						int splitPos = columnExp.lastIndexOf(" ");
						if(splitPos != -1)
						{
							aliasName = columnExp.substring(splitPos + 1);
							if(aliasName.equals(tmpTokens[tmpTokens.length - 1].getOriginString()))
							{
								columnExp = columnExp.substring(0, splitPos).trim();
							}
							else
							{
								aliasName = null;
							}
						}
						
						if(tmpTokens.length >= 3 && tmpTokens[tmpTokens.length - 2].isKeyWord("AS"))
						{
							splitPos = columnExp.lastIndexOf(" ");
							if(splitPos != -1)
							{
								String asKeyWord = columnExp.substring(splitPos + 1);
								if(asKeyWord.equals(tmpTokens[tmpTokens.length - 2].getOriginString()))
								{
									columnExp = columnExp.substring(0, splitPos).trim();
								}
							}
						}
					}
					
					tmpTokens = Tokenizer.parse(columnExp);
					
					for(int k = 0, n = tmpTokens.length; k < n; k++)
					{
						if(k + 2 < n && tmpTokens[k].getType() == Tokenizer.IDENT
						&& tmpTokens[k + 1].getType() == Tokenizer.LPAREN)
						{
							tokenList.add(tmpTokens[k]);
							tokenList.add(tmpTokens[k + 1]);
							k = k + 1;
						}
						else if(k + 2 < n && tmpTokens[k].getType() == Tokenizer.IDENT
						&& tmpTokens[k + 1].getType() == Tokenizer.DOT
						&& tmpTokens[k + 2].getType() == Tokenizer.IDENT)
						{
							tokenList.add(tmpTokens[k]);
							tokenList.add(tmpTokens[k + 1]);
							tokenList.add(tmpTokens[k + 2]);
							k = k + 2;
						}
						else if(k + 3 < n && tmpTokens[k].getType() == Tokenizer.LPAREN)
						{
							int t = Tokenizer.scanParen(tmpTokens, k, n);
							boolean sub = false;
							for(int m = k + 1; m < t; m++)
							{
								if(tmpTokens[m].isKeyWord("SELECT"))
								{
									sub = true;
									break;
								}
							}
							if(sub)
							{
								for(int m = k; m <= t; m++)
								{
									tokenList.add(tmpTokens[m]);
								}
								k = t;
							}
							else
							{
								tokenList.add(tmpTokens[k]);
							}
						}
						else if(tmpTokens[k].getType() == Tokenizer.IDENT)
						{
							tokenList.add(new Token(Tokenizer.IDENT, tableAlias, -1, tableAlias));
							tokenList.add(new Token(Tokenizer.DOT, ".", -1, "."));
							tokenList.add(tmpTokens[k]);
						}
						else
						{
							tokenList.add(tmpTokens[k]);
						}
					}
					
					if(aliasName != null)
					{
						Token tailToken = tokenList.get(tokenList.size() - 1);
						if(tailToken.getSpaces().isEmpty())
						{
							tailToken.addSpace();
						}
						
						tokenList.add(new Token(Tokenizer.IDENT, aliasName, -1, aliasName));
					}
					
					if(comma < end)
					{
						tokenList.add(new Token(Tokenizer.COMMA, ",", -1, ","));
					}
					
					Token tailToken = tokenList.get(tokenList.size() - 1);
					if(tailToken.getSpaces().isEmpty())
					{
						tailToken.addSpace();
					}
					
					start = comma + 1;
				}
				while(start < end);
			}
			else if(tokens[0].isKeyWord("WHERE"))
			{
				tokenList.add(tokens[0]);
				
				Token[] tmpTokens = Arrays.copyOfRange(tokens, 1, tokens.length);
				
				for(int k = 0, n = tmpTokens.length; k < n; k++)
				{
					if(k + 2 < n && tmpTokens[k].getType() == Tokenizer.IDENT
					&& tmpTokens[k + 1].getType() == Tokenizer.LPAREN)
					{
						tokenList.add(tmpTokens[k]);
						tokenList.add(tmpTokens[k + 1]);
						k = k + 1;
					}
					else if(k + 2 < n && tmpTokens[k].getType() == Tokenizer.IDENT
					&& tmpTokens[k + 1].getType() == Tokenizer.DOT
					&& tmpTokens[k + 2].getType() == Tokenizer.IDENT)
					{
						tokenList.add(tmpTokens[k]);
						tokenList.add(tmpTokens[k + 1]);
						tokenList.add(tmpTokens[k + 2]);
						k = k + 2;
					}
					else if(k + 3 < n && tmpTokens[k].getType() == Tokenizer.LPAREN)
					{
						int t = Tokenizer.scanParen(tmpTokens, k, n);
						boolean sub = false;
						for(int m = k + 1; m < t; m++)
						{
							if(tmpTokens[m].isKeyWord("SELECT"))
							{
								sub = true;
								break;
							}
						}
						if(sub)
						{
							for(int m = k; m <= t; m++)
							{
								tokenList.add(tmpTokens[m]);
							}
							k = t;
						}
						else
						{
							tokenList.add(tmpTokens[k]);
						}
					}
					else if(tmpTokens[k].getType() == Tokenizer.IDENT)
					{
						tokenList.add(new Token(Tokenizer.IDENT, tableAlias, -1, tableAlias));
						tokenList.add(new Token(Tokenizer.DOT, ".", -1, "."));
						tokenList.add(tmpTokens[k]);
					}
					else
					{
						tokenList.add(tmpTokens[k]);
					}
				}
				
				Token tailToken = tokenList.get(tokenList.size() - 1);
				if(tailToken.getSpaces().isEmpty())
				{
					tailToken.addSpace();
				}
			}
			else
			{
				throw new RQException("regulateFieldTokens函数：现只支持SELECT子句和WHERE子句");
			}
			
			tokens = new Token[tokenList.size()];
			tokenList.toArray(tokens);
		}
		return tokens;
	}
	
	public static String getNewAlias()
	{
		String index = new MD5().getMD5ofStr(UUID.randomUUID().toString().replace("-", "_"));
		String alias = "T_" + index;
		return alias;
	}
}
