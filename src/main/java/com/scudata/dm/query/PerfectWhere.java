package com.scudata.dm.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.expression.Expression;
import com.scudata.resources.ParseMessage;

public class PerfectWhere 
{
	private Node root = null;
	private boolean optimizeSign = false;
	
	abstract class Node 
	{
		protected Token[] tokens = null;
		protected List<Object> paramList = null;
		protected boolean canOptimize = false;
		protected Node left = null;
		protected Node right = null;
		protected Node parent = null;
		
		public Token[] getTokens()
		{
			return this.tokens;
		}
		
		public boolean getCanOptimize()
		{
			return this.canOptimize;
		}
		
		public void setLeft(Node node)
		{
			this.left = node;
			node.setParent(this);
		}
		
		public Node getLeft()
		{
			return this.left;
		}
		
		public void setRight(Node node)
		{
			this.right = node;
			node.setParent(this);
		}
		
		public Node getRight()
		{
			return this.right;
		}
		
		public void setParent(Node node)
		{
			this.parent = node;
		}
		
		public Node getParent()
		{
			return this.parent;
		}
		
		public boolean needParen()
		{
			return false;
		}
		
		public void optimize()
		{
		}
	}
	
	class AndNode extends Node
	{
		public AndNode(Token token)
		{
			this.tokens = new Token[]{token};
			this.canOptimize = false;
		}
		
		public Token[] getTokens()
		{
			if(this.left == null || this.right == null)
			{
				throw new RQException("Invalid OrNode that LeftNode or RightNode is Null");
			}
			List<Token> tokenList = new ArrayList<Token>();
			if(this.left.needParen())
			{
				Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
				tk.addSpace();
				tokenList.add(tk);
			}
			tokenList.addAll(Arrays.asList(this.left.getTokens()));
			if(this.left.needParen())
			{
				Token tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
				tk.addSpace();
				tokenList.add(tk);
			}
			
			tokenList.addAll(Arrays.asList(this.tokens));
			
			if(this.right.needParen())
			{
				Token tk = new Token(Tokenizer.LPAREN, "(", -1, "(");
				tk.addSpace();
				tokenList.add(tk);
			}
			tokenList.addAll(Arrays.asList(this.right.getTokens()));
			if(this.right.needParen())
			{
				Token tk = new Token(Tokenizer.RPAREN, ")", -1, ")");
				tk.addSpace();
				tokenList.add(tk);
			}
			Token[] tokens = new Token[tokenList.size()];
			tokenList.toArray(tokens);
			return tokens;
		}
		
		public void optimize()
		{
			this.left.optimize();
			
			if(optimizeSign)
			{
				return;
			}
			
			this.right.optimize();
		}
	}

	class OrNode extends Node
	{
		public OrNode(Token token)
		{
			this.tokens = new Token[]{token};
			this.canOptimize = false;
		}
		
		public Token[] getTokens()
		{
			if(this.left == null || this.right == null)
			{
				throw new RQException("Invalid OrNode that LeftNode or RightNode is Null");
			}
			List<Token> tokenList = new ArrayList<Token>();
			tokenList.addAll(Arrays.asList(this.left.getTokens()));
			tokenList.addAll(Arrays.asList(this.tokens));
			tokenList.addAll(Arrays.asList(this.right.getTokens()));
			Token[] tokens = new Token[tokenList.size()];
			tokenList.toArray(tokens);
			return tokens;
		}
		
		public boolean needParen()
		{
			return true;
		}
		
		public void optimize()
		{
			this.left.optimize();
			
			if(optimizeSign)
			{
				return;
			}

			this.right.optimize();
		}
	}

	class LeafNode extends Node
	{
		StringBuffer appendBuffer = null;
		public LeafNode(Token[] tokens, List<Object> paramList, boolean canOptimize, StringBuffer appendBuffer)
		{
			this.tokens = tokens;
			this.canOptimize = canOptimize;
			this.appendBuffer = appendBuffer;
		}
		
		public Token[] getTokens()
		{
			if(this.appendBuffer != null)
			{
				Token[] appends = Tokenizer.parse(this.appendBuffer.toString());
				Token[] totals = new Token[this.tokens.length + appends.length];
				System.arraycopy(this.tokens, 0, totals, 0, this.tokens.length);
				System.arraycopy(appends, 0, totals, this.tokens.length, appends.length);
				return totals;
			}
			else
			{
				Token[] totals = new Token[this.tokens.length];
				System.arraycopy(this.tokens, 0, totals, 0, this.tokens.length);
				return totals;
			}
		}
		
		public Token[] getOriginalTokens()
		{
			Token[] totals = new Token[this.tokens.length];
			System.arraycopy(this.tokens, 0, totals, 0, this.tokens.length);
			return totals;
		}
		
		public void setLeft(Node node)
		{
			throw new RQException("LeafNode Cannot Set LeftNode!");
		}
		
		public Node getLeft()
		{
			return null;
		}
		
		public void setRight(Node node)
		{
			throw new RQException("LeafNode Cannot Set RightNode!");
		}
		
		public Node getRight()
		{
			return null;
		}
		
		public void optimize()
		{
			if(optimizeSign)
			{
				return;
			}
			
			if(this.canOptimize)
			{
				try
				{
					String leafExp = SimpleSelect.scanExp(this.tokens, this.paramList);
					Object value = new Expression(leafExp).calculate(new Context());
					if(value instanceof Boolean)
					{
						Node replace = null;
						if(this.tokens.length == 3 
						&& this.tokens[0].isKeyWord("NULL")
						&& this.tokens[1].getString().equals("=")
						&& this.tokens[2].isKeyWord("NULL"))
						{
							replace = new FalseNode();
						}
						else if(this.tokens.length == 3 
						&& this.tokens[0].isKeyWord("NULL")
						&& this.tokens[1].isKeyWord("IS")
						&& this.tokens[2].isKeyWord("NULL"))
						{
							replace = new TrueNode();
						}
						else if(value.equals(Boolean.TRUE))
						{
							replace = new TrueNode();
						}
						else if(value.equals(Boolean.FALSE))
						{
							replace = new FalseNode();
						}
						
						Node parent = this.getParent();
						if(parent == null)
						{
							root = replace;
							root.setParent(null);
						}
						else
						{
							Node other = null;
							if(parent.getLeft().equals(this))
							{
								other = parent.getRight();
							}
							else if(parent.getRight().equals(this))
							{
								other = parent.getLeft();
							}
							
							Node choose = null;
							Node grand = parent.getParent();
							if((replace instanceof TrueNode) && (parent instanceof OrNode) || (replace instanceof FalseNode) && (parent instanceof AndNode))
							{
								choose = replace;
							}
							else if((replace instanceof TrueNode) && (parent instanceof AndNode) || (replace instanceof FalseNode) && (parent instanceof OrNode))
							{
								choose = other;
							}
							
							if(grand == null)
							{
								root = choose;
								root.setParent(null);
							}
							else
							{
								if(grand.getLeft().equals(parent))
								{
									grand.setLeft(choose);
								}
								else if(grand.getRight().equals(parent))
								{
									grand.setRight(choose);
								}
							}
						}
						optimizeSign = true;
					}
					else
					{
						this.canOptimize = false;
					}
				}
				catch(Throwable t)
				{
					this.canOptimize = false;
				}
			}
		}
	}
	
	class TrueNode extends LeafNode
	{
		public TrueNode() 
		{
			super(Tokenizer.parse("1=1"), null, true, null);
		}
		
		public void optimize()
		{
			if(optimizeSign)
			{
				return;
			}
			
			if(this.canOptimize)
			{
				Node replace = this;
				Node parent = this.getParent();
				if(parent == null)
				{
					return;
				}
				else
				{
					Node other = null;
					if(parent.getLeft().equals(this))
					{
						other = parent.getRight();
					}
					else if(parent.getRight().equals(this))
					{
						other = parent.getLeft();
					}
					
					Node choose = null;
					Node grand = parent.getParent();
					if((replace instanceof TrueNode) && (parent instanceof OrNode) || (replace instanceof FalseNode) && (parent instanceof AndNode))
					{
						choose = replace;
					}
					else if((replace instanceof TrueNode) && (parent instanceof AndNode) || (replace instanceof FalseNode) && (parent instanceof OrNode))
					{
						choose = other;
					}
					
					if(grand == null)
					{
						root = choose;
						root.setParent(null);
					}
					else
					{
						if(grand.getLeft().equals(parent))
						{
							grand.setLeft(choose);
						}
						else if(grand.getRight().equals(parent))
						{
							grand.setRight(choose);
						}
					}
				}
				optimizeSign = true;
			}
		}
	}
	
	class FalseNode extends LeafNode
	{
		public FalseNode() 
		{
			super(Tokenizer.parse("1=0"), null, true, null);
		}
		
		public void optimize()
		{
			if(optimizeSign)
			{
				return;
			}
			
			if(this.canOptimize)
			{
				Node replace = this;
				Node parent = this.getParent();
				if(parent == null)
				{
					return;
				}
				else
				{
					Node other = null;
					if(parent.getLeft().equals(this))
					{
						other = parent.getRight();
					}
					else if(parent.getRight().equals(this))
					{
						other = parent.getLeft();
					}
					
					Node choose = null;
					Node grand = parent.getParent();
					if((replace instanceof TrueNode) && (parent instanceof OrNode) || (replace instanceof FalseNode) && (parent instanceof AndNode))
					{
						choose = replace;
					}
					else if((replace instanceof TrueNode) && (parent instanceof AndNode) || (replace instanceof FalseNode) && (parent instanceof OrNode))
					{
						choose = other;
					}
					
					if(grand == null)
					{
						root = choose;
						root.setParent(null);
					}
					else
					{
						if(grand.getLeft().equals(parent))
						{
							grand.setLeft(choose);
						}
						else if(grand.getRight().equals(parent))
						{
							grand.setRight(choose);
						}
					}
				}
				optimizeSign = true;
			}
		}
	}
	
	public PerfectWhere(Token[] whereTokens, List<Object> paramList)
	{
		this.root = buildTree(whereTokens, paramList);
	}
	
	private Node buildTree(Token[] whereTokens, List<Object> paramList)
	{
		List<Token> tokenList = new ArrayList<Token>();
		StringBuffer appendBuffer = new StringBuffer();
		boolean canOptimize = true;
		Node root = null;
		for(int i = 0, len = whereTokens.length; i < len; i++)
		{
			Token token = whereTokens[i];
			if(token.getType() == Tokenizer.KEYWORD)
			{
				if(token.isKeyWord("OR"))
				{
					Token[] tokens = new Token[tokenList.size()];
					tokenList.toArray(tokens);
					Boolean needAppend = false;
					Token lastToken = null;
					for(int j = 0, size = tokens.length; j < size; j++)
					{
						Token subToken = tokens[j];
						if(subToken.getString().equals("<") && (j + 1 >= size || !tokens[j + 1].getString().equals(">"))
						|| subToken.getString().equals(">") && (j - 1 < 0 || !tokens[j - 1].getString().equals("<"))
						|| subToken.getString().equals("<") && (j + 1 < size && tokens[j + 1].getString().equals(">")) 
						|| subToken.getString().equals("!") && (j + 1 < size && tokens[j + 1].getString().equals("=")) 
						|| subToken.getString().equals("=") && (j - 1 < 0 || !tokens[j - 1].getString().equals("!"))
						|| subToken.isKeyWord("IN"))
						{
							needAppend = true;
							break;
						}
						lastToken = subToken;
					}
					if(!needAppend)
					{
						appendBuffer = null;
					}
					LeafNode leaf = new LeafNode(tokens, paramList, canOptimize, appendBuffer);
					appendBuffer = new StringBuffer();
					tokenList.clear();
					canOptimize = true;
					
					OrNode or = new OrNode(token);
					if(root != null)
					{
						if(root instanceof OrNode)
						{
							Node right = root.getRight();
							if(right != null)
							{
								right.setRight(leaf);
							}
							else
							{
								root.setRight(leaf);
							}
						}
						else if(root instanceof AndNode)
						{
							root.setRight(leaf);
						}
						or.setLeft(root);
						root = or;
					}
					else
					{
						or.setLeft(leaf);
						root = or;	
					}
				}
				else if(token.isKeyWord("AND"))
				{
					Token[] tokens = new Token[tokenList.size()];
					tokenList.toArray(tokens);
					Boolean needAppend = false;
					Token lastToken = null;
					for(int j = 0, size = tokens.length; j < size; j++)
					{
						Token subToken = tokens[j];
						if(subToken.getString().equals("<") && (j + 1 >= size || !tokens[j + 1].getString().equals(">"))
						|| subToken.getString().equals(">") && (j - 1 < 0 || !tokens[j - 1].getString().equals("<"))
						|| subToken.getString().equals("<") && (j + 1 < size && tokens[j + 1].getString().equals(">")) 
						|| subToken.getString().equals("!") && (j + 1 < size && tokens[j + 1].getString().equals("=")) 
						|| subToken.getString().equals("=") && (j - 1 < 0 || !tokens[j - 1].getString().equals("!"))
						|| subToken.isKeyWord("IN"))
						{
							needAppend = true;
							break;
						}
						lastToken = subToken;
					}
					if(!needAppend)
					{
						appendBuffer = null;
					}
					LeafNode leaf = new LeafNode(tokens, paramList, canOptimize, appendBuffer);
					appendBuffer = new StringBuffer();
					tokenList.clear();
					canOptimize = true;
					
					AndNode and = new AndNode(token);
					if(root != null)
					{
						if(root instanceof OrNode)
						{
							Node right = root.getRight();
							if(right != null)
							{
								right.setRight(leaf);
								and.setLeft(right);
								root.setRight(and);
							}
							else
							{
								and.setLeft(leaf);
								root.setRight(and);
							}
						}
						else if(root instanceof AndNode)
						{
							root.setRight(leaf);
							and.setLeft(root);
							root = and;
						}
					}
					else
					{
						and.setLeft(leaf);
						root = and;
					}
				}
				else
				{
					tokenList.add(token);
				}
			}
			else if(token.getType() == Tokenizer.IDENT)
			{
				if(i < len - 2  
				&& whereTokens[i + 1].getType() == Tokenizer.DOT 
				&& whereTokens[i + 2].getType() == Tokenizer.IDENT) // T.F
				{
					appendBuffer.append(" AND ");
					for(int j = i; j <= i + 2; j++)
					{
						tokenList.add(whereTokens[j]);
						appendBuffer.append(whereTokens[j].getString());
					}
					appendBuffer.append(" IS NOT NULL");
					i = i + 2;
					canOptimize = false;
				}
				else if(i < len - 2 
				&& whereTokens[i + 1].getType() == Tokenizer.LPAREN) //fun()
				{
					int end = Tokenizer.scanParen(whereTokens, i + 1, whereTokens.length);//函数表达式
					appendBuffer.append(" AND ");
					for(int j = i; j <= end; j++)
					{
						tokenList.add(whereTokens[j]);
						appendBuffer.append(whereTokens[j].getString());
					}
					appendBuffer.append(" IS NOT NULL");
					i = end;
					canOptimize = false;
				}		
				else //F
				{
					appendBuffer.append(" AND ");
					tokenList.add(token);
					appendBuffer.append(token.getString());
					appendBuffer.append(" IS NOT NULL");
					canOptimize = false;
				}
			}
			else if(token.getType() == Tokenizer.LPAREN)
			{
				int end = Tokenizer.scanParen(whereTokens, i, whereTokens.length);//括号表达式
				
				int queryPos = Tokenizer.scanKeyWords(new String[]{"UNION","MINUS","EXCEPT","INTERSECT","SELECT"}, whereTokens, i + 1, end);
				
				if(queryPos != -1)
				{
					for(int j = i; j <= end; j++)
					{
						tokenList.add(whereTokens[j]);
					}
					i = end;
					canOptimize = false;
				}
				else if(tokenList.isEmpty() 
				&& (end + 1 < len && (whereTokens[end + 1].isKeyWord("AND") || whereTokens[end + 1].isKeyWord("OR")) || end + 1 == len))
				{
					Token[] subTokens = Arrays.copyOfRange(whereTokens, i + 1, end);
					Node node = buildTree(subTokens, paramList);
					
					if(end + 1 < len && (whereTokens[end + 1].isKeyWord("AND") || whereTokens[end + 1].isKeyWord("OR")))
					{
						if(whereTokens[end + 1].isKeyWord("OR"))
						{
							OrNode or = new OrNode(whereTokens[end + 1]);
							if(root != null)
							{
								if(root instanceof OrNode)
								{
									Node right = root.getRight();
									if(right != null)
									{
										right.setRight(node);
									}
									else
									{
										root.setRight(node);
									}
								}
								else if(root instanceof AndNode)
								{
									root.setRight(node);
								}
								or.setLeft(root);
								root = or;
							}
							else
							{
								or.setLeft(node);
								root = or;	
							}
						}
						else if(whereTokens[end + 1].isKeyWord("AND"))
						{
							AndNode and = new AndNode(whereTokens[end + 1]);
							if(root != null)
							{
								if(root instanceof OrNode)
								{
									Node right = root.getRight();
									if(right != null)
									{
										right.setRight(node);
										and.setLeft(right);
										root.setRight(and);
									}
									else
									{
										and.setLeft(node);
										root.setRight(and);
									}
								}
								else if(root instanceof AndNode)
								{
									root.setRight(node);
									and.setLeft(root);
									root = and;
								}
							}
							else
							{
								and.setLeft(node);
								root = and;
							}
						}
					}
					else if(end + 1 == len)
					{
						if(root != null)
						{
							if(root instanceof OrNode)
							{
								Node right = root.getRight();
								if(right != null)
								{
									right.setRight(node);
								}
								else
								{
									root.setRight(node);
								}
							}
							else if(root instanceof AndNode)
							{
								root.setRight(node);
							}
						}
						else
						{
							root = node;
						}
					}
					
					i = end + 1;
				}
				else
				{
					tokenList.add(token);
				}
			}
			else
			{
				tokenList.add(token);
			}
		}
		
		if(!tokenList.isEmpty())
		{
			Token[] tokens = new Token[tokenList.size()];
			tokenList.toArray(tokens);
			
			Boolean needAppend = false;
			Token lastToken = null;
			for(int i = 0, len = tokens.length; i < len; i++)
			{
				Token subToken = tokens[i];
				if(subToken.getType() == Tokenizer.LPAREN)
				{
					int end = Tokenizer.scanParen(tokens, i, len);
					int pos = Tokenizer.scanKeyWords(new String[]{"UNION","MINUS","EXCEPT","INTERSECT","SELECT"}, tokens, i+1, end);
					if(pos != -1)
					{
						i = end;
					}
				}
				else if(subToken.getString().equals("<") && (i + 1 >= len || !tokens[i + 1].getString().equals(">"))
				|| subToken.getString().equals(">") && (i - 1 < 0 || !tokens[i - 1].getString().equals("<"))
				|| subToken.getString().equals("<") && (i + 1 < len && tokens[i + 1].getString().equals(">"))
				|| subToken.getString().equals("!") && (i + 1 < len && tokens[i + 1].getString().equals("="))
				|| subToken.getString().equals("=") && (i - 1 < 0 || !tokens[i - 1].getString().equals("!"))
				|| subToken.isKeyWord("IN"))
				{
					needAppend = true;
					break;
				}
				lastToken = subToken;
			}
			
			if(!needAppend)
			{
				appendBuffer = null;
			}
			
			LeafNode leaf = new LeafNode(tokens, paramList, canOptimize, appendBuffer);
			if(root != null)
			{
				if(root instanceof OrNode)
				{
					Node right = root.getRight();
					if(right != null)
					{
						right.setRight(leaf);
					}
					else
					{
						root.setRight(leaf);
					}
				}
				else if(root instanceof AndNode)
				{
					root.setRight(leaf);
				}
			}
			else
			{
				root = leaf;	
			}
		}
		
		return root;
	}
	
	private void optimizeTree(Node root)
	{
		do
		{
			this.optimizeSign = false;
			this.root.optimize();			
		}
		while(this.optimizeSign);
	}
	
	public Token[] getTokens(boolean optimize)
	{
		if(optimize)
		{
			optimizeTree(this.root);
		}
		return this.root.getTokens();
	}
	
	private Token[] getOnFromTokens(Node node, List<LeafNode> nodeList, Set<String> tableAliasSet, Set<String> outerFieldSet, Set<String> innerFieldSet)
	{
		if(node == null)
		{
			node = this.root;
		}
		if(node.equals(this.root))
		{
			nodeList = new ArrayList<LeafNode>();
			optimizeTree(node);
		}
		if(node instanceof AndNode)
		{
			if(node.getLeft() == null || node.getRight() == null)
			{
				throw new RQException("WHERE子句语法错误");
			}
			getOnFromTokens(node.getLeft(), nodeList, tableAliasSet, outerFieldSet, innerFieldSet);
			getOnFromTokens(node.getRight(), nodeList, tableAliasSet, outerFieldSet, innerFieldSet);
		}
		else if(node instanceof OrNode)
		{
			if(node.getLeft() == null || node.getRight() == null)
			{
				throw new RQException("WHERE子句语法错误");
			}
			getOnFromTokens(node.getLeft(), null, tableAliasSet, null, null);
			getOnFromTokens(node.getRight(), null, tableAliasSet, null, null);
		}
		else if(node instanceof LeafNode)
		{
			LeafNode leaf = (LeafNode)node;
			Token[] tokens = leaf.getOriginalTokens();
			Boolean hasOuterField = false;
			Boolean hasOnlyEqual = null;
			Boolean hasOnlyField = true;
			for(int n = 0, len = tokens.length; n < len; n++)
			{
				if(tokens[n].getType() == Tokenizer.IDENT 
				&& n < len - 2 
				&& tokens[n + 1].getType() == Tokenizer.DOT
				&& tokens[n + 2].getType() == Tokenizer.IDENT)
				{
					Boolean contain = false;
					for(String tableAlias : tableAliasSet)
					{
						if(tableAlias.equalsIgnoreCase(tokens[n].getOriginString()))
						{
							contain = true;
						}
					}
					if(contain)
					{
						hasOuterField = true;
						if(outerFieldSet != null)
						{
							outerFieldSet.add((tokens[n].getString() + "." + tokens[n + 2].getString()).toLowerCase());
						}
					}
					else
					{
						if(innerFieldSet != null)
						{
							innerFieldSet.add((tokens[n].getString() + "." + tokens[n + 2].getString()).toLowerCase());
						}
					}
					n = n + 2;
				}
				else if(tokens[n].getType() == Tokenizer.IDENT
				&& n < len - 2 
				&& tokens[n + 1].getType() == Tokenizer.LPAREN)
				{
					hasOnlyField = false;
					n = Tokenizer.scanParen(tokens, n + 1, len);
				}
				else if(tokens[n].getType() == Tokenizer.IDENT)
				{
					if(innerFieldSet != null)
					{
						innerFieldSet.add(tokens[n].getString().toLowerCase());
					}
				}
				else if(tokens[n].getString().equals(">") 
				&& n < len - 1
				&& tokens[n + 1].getString().equals("="))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
					n = n + 1;
				}
				else if(tokens[n].getString().equals("<") 
				&& n < len - 1
				&& tokens[n + 1].getString().equals("="))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
					n = n + 1;
				}
				else if(tokens[n].getString().equals("!") 
				&& n < len - 1
				&& tokens[n + 1].getString().equals("="))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
					n = n + 1;
				}
				else if(tokens[n].getString().equals("<") 
				&& n < len - 1
				&& tokens[n + 1].getString().equals(">"))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
					n = n + 1;
				}
				else if(tokens[n].getString().equals(">"))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
				}
				else if(tokens[n].getString().equals("<"))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = false;
				}
				else if(tokens[n].getString().equals("="))
				{
					if(hasOnlyEqual != null)
					{
						throw new RQException("WHERE子句中某布尔表达式错误");
					}
					hasOnlyEqual = true;
				}
				else
				{
					hasOnlyField = false;
				}
			}
			if(hasOnlyEqual == null)
			{
				hasOnlyEqual = false;
			}
			if(hasOuterField)
			{
				if(!hasOnlyEqual || !hasOnlyField || nodeList == null)
				{
					throw new RQException("false");
				}
				else
				{
					nodeList.add(leaf);
				}
			}
		}
		if(node.equals(this.root))
		{
			List<Token> tokenList = new ArrayList<Token>();
			for(LeafNode leaf : nodeList)
			{
				if(!tokenList.isEmpty())
				{
					if(tokenList.get(tokenList.size() - 1).getSpaces().isEmpty())
					{
						tokenList.get(tokenList.size() - 1).addSpace();
					}
					Token andToken = new Token(Tokenizer.KEYWORD, "AND", -1, "AND");
					andToken.addSpace();
					tokenList.add(andToken);
				}
				tokenList.addAll(Arrays.asList(leaf.getTokens()));
				
				if(leaf.equals(this.root))
				{
					this.root = new TrueNode();
				}
				else
				{
					TrueNode replace = new TrueNode();
					Node parent = leaf.getParent();
					if(leaf.equals(parent.getLeft()))
					{
						parent.setLeft(replace);
					}
					else if(leaf.equals(parent.getRight()))
					{
						parent.setRight(replace);
					}
					replace.setParent(parent);
				}
			}
			Token[] result = new Token[tokenList.size()];
			tokenList.toArray(result);
			return result;
		}
		return null;
	}
	
	public Token[] getOnFromTokens(Set<String> tableAliasSet, Set<String> outerFieldSet, Set<String> innerFieldSet)
	{
		Token[] result = null;
		try
		{
			result = getOnFromTokens(null, null, tableAliasSet, outerFieldSet, innerFieldSet);
		}
		catch(RQException ex)
		{
			if(ex.getMessage().equals("false"))
			{
				return null;
			}
			else
			{
				throw ex;
			}
		}
		return result;
	}
	
	public String getTopFromTokens(Node node, Map<LeafNode, String> nodeFieldMap, String tablePath, String tableAlias)
	{
		if(tablePath == null || tablePath.isEmpty())
		{
			return null;
		}
		
		if(node == null)
		{
			node = this.root;
		}
		
		if(node.equals(this.root))
		{
			nodeFieldMap = new HashMap<LeafNode, String>();
			optimizeTree(node);
		}
		
		if(node instanceof AndNode)
		{
			if(node.getLeft() == null || node.getRight() == null)
			{
				throw new RQException("WHERE子句语法错误");
			}
			getTopFromTokens(node.getLeft(), nodeFieldMap, tablePath, tableAlias);
			getTopFromTokens(node.getRight(), nodeFieldMap, tablePath, tableAlias);
		}
		else if(node instanceof OrNode)
		{
			return null;
		}
		else if(node instanceof LeafNode)
		{
			Token[] tokens = ((LeafNode)node).getOriginalTokens();
			
			Token equalToken = null;
			int equalPos = 0;
			for(int i = 0;  i < tokens.length; i++)
			{
				Token token = tokens[i];
				if(token.getString().equals("="))
				{
					if(equalToken == null)
					{
						equalToken = token;
						equalPos = i;
					}
					else
					{
						throw new RQException("异常的布尔表达式");
					}
				}
				else if(token.getType() == Tokenizer.LPAREN)
				{
					i = Tokenizer.scanParen(tokens, i, tokens.length);
				}
			}
			
			if(equalToken == null)
			{
				return null;
			}
			
			Token[] leftTokens = Arrays.copyOfRange(tokens, 0, equalPos);
			Token[] rightTokens = Arrays.copyOfRange(tokens, equalPos + 1, tokens.length);
			
			Token[] fieldTokens = null;
			
			if(leftTokens.length == 3 && tableAlias != null
			&& leftTokens[0].getType() == Tokenizer.IDENT 
			&& leftTokens[0].getString().equalsIgnoreCase(tableAlias)
			&& leftTokens[1].getType() == Tokenizer.DOT 
			&& leftTokens[2].getType() == Tokenizer.IDENT)
			{
				fieldTokens = leftTokens;
			}
			else if(leftTokens.length == 1 
			&& leftTokens[0].getType() == Tokenizer.IDENT)
			{
				fieldTokens = leftTokens;
			}
			
			if(rightTokens.length == 3 && tableAlias != null
			&& rightTokens[0].getType() == Tokenizer.IDENT 
			&& rightTokens[0].getString().equalsIgnoreCase(tableAlias)
			&& rightTokens[1].getType() == Tokenizer.DOT 
			&& rightTokens[2].getType() == Tokenizer.IDENT)
			{
				if(fieldTokens != null)
				{
					return null;
				}
				
				fieldTokens = rightTokens;
			}
			else if(rightTokens.length == 1 
			&& rightTokens[0].getType() == Tokenizer.IDENT)
			{
				if(fieldTokens != null)
				{
					return null;
				}
				
				fieldTokens = rightTokens;
			}
			
			Token[] subQueryTokens = null;
			
			if(leftTokens.length > 4 && leftTokens[0].getType() == Tokenizer.LPAREN
			&& leftTokens[leftTokens.length - 1].getType() == Tokenizer.RPAREN
			&& Tokenizer.scanKeyWord("SELECT", leftTokens, 1, leftTokens.length - 1) != -1)
			{
				subQueryTokens = Arrays.copyOfRange(leftTokens, 1, leftTokens.length - 1);
			}
			
			if(rightTokens.length > 4 && rightTokens[0].getType() == Tokenizer.LPAREN
			&& rightTokens[rightTokens.length - 1].getType() == Tokenizer.RPAREN
			&& Tokenizer.scanKeyWord("SELECT", rightTokens, 1, rightTokens.length - 1) != -1)
			{
				if(subQueryTokens != null)
				{
					return null;
				}
				
				subQueryTokens = Arrays.copyOfRange(rightTokens, 1, rightTokens.length - 1);	
			}
			
			if(fieldTokens == null || subQueryTokens == null)
			{
				return null;
			}
			
			int selectPos = -1;
			int fromPos = -1;
			for(int n = 0, len = subQueryTokens.length; n < len; n++)
			{
				if(subQueryTokens[n].isKeyWord())
				{
					if(subQueryTokens[n].isKeyWord("SELECT"))
					{
						selectPos = n;
					}
					else if(subQueryTokens[n].isKeyWord("FROM"))
					{
						fromPos = n;
					}
					else
					{
						return null;
					}
				}
			}
			
			if(selectPos < 0 && fromPos < 0)
			{
				return null;
			}

			Token[] selectTokens = Arrays.copyOfRange(subQueryTokens, selectPos + 1, fromPos);
			Token[] fromTokens = Arrays.copyOfRange(subQueryTokens, fromPos + 1, subQueryTokens.length);
			
			if(selectTokens.length <= 3 || Tokenizer.scanComma(selectTokens, 0, selectTokens.length) != -1
			|| selectTokens[0].getType() != Tokenizer.IDENT || selectTokens[1].getType() != Tokenizer.LPAREN
			|| !selectTokens[0].getString().equalsIgnoreCase("max") && !selectTokens[0].getString().equalsIgnoreCase("min"))
			{
				return null;
			}
			
			int functionType = selectTokens[0].getString().equalsIgnoreCase("max") ? -1 : (selectTokens[0].getString().equalsIgnoreCase("min") ? 1 : 0);
			if(functionType == 0)
			{
				return null;
			}
			
			String tableName = "";
			for(Token fromToken : fromTokens)
			{
				tableName = tableName + fromToken.getOriginString();
				tableName = tableName + fromToken.getSpaces();
			}
			tableName = tableName.trim();
			
			String aliasName = null;
			if (fromTokens.length - 2 >= 0 && fromTokens[fromTokens.length - 1].getType() == Tokenizer.IDENT)
			{
				int splitPos = tableName.lastIndexOf(" ");
				if(splitPos != -1)
				{
					aliasName = tableName.substring(splitPos + 1);
					if(aliasName.equals(fromTokens[fromTokens.length - 1].getOriginString()))
					{
						tableName = tableName.substring(0, splitPos).trim();
					}
					else
					{
						aliasName = null;
					}
				}
				
				if(fromTokens.length - 3 >= 0 && fromTokens[fromTokens.length - 2].isKeyWord("AS"))
				{
					splitPos = tableName.lastIndexOf(" ");
					if(splitPos != -1)
					{
						String asKeyWord = tableName.substring(splitPos + 1);
						if(asKeyWord.equals(fromTokens[fromTokens.length - 2].getOriginString()))
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
			
			FileObject file1 = new FileObject(tablePath, null, "s", null);
			FileObject file2 = new FileObject(tableName, null, "s", null);
			
			if(!file2.isExists() || !file2.getLocalFile().getFile().getAbsolutePath().equals(file1.getLocalFile().getFile().getAbsolutePath()))
			{
				return null;
			}
			
			String fieldName = null;
			int rightParen = Tokenizer.scanParen(selectTokens, 1, selectTokens.length);
			Token[] paramTokens = Arrays.copyOfRange(selectTokens, 2, rightParen);
			if(paramTokens.length == 3 && aliasName != null 
			&& paramTokens[0].getString().equalsIgnoreCase(aliasName) && paramTokens[0].getType() == Tokenizer.IDENT 
			&& paramTokens[1].getType() == Tokenizer.DOT && paramTokens[2].getType() == Tokenizer.IDENT)
			{
				fieldName = paramTokens[2].getString();
			}
			else if(paramTokens.length == 1 && paramTokens[0].getType() == Tokenizer.IDENT)
			{
				fieldName = paramTokens[0].getString();
			}
			else
			{
				return null;
			}
			
			if(fieldTokens.length == 3)
			{
				if(!fieldTokens[2].getString().equalsIgnoreCase(fieldName))
				{
					return null;
				}
			}
			else if(fieldTokens.length == 1)
			{
				if(!fieldTokens[0].getString().equalsIgnoreCase(fieldName))
				{
					return null;
				}
			}
			
			nodeFieldMap.put((LeafNode)node, String.format("top(%d;%s)", functionType, fieldName));
		}
		
		if(node.equals(this.root))
		{
			if(nodeFieldMap.size() != 1)
			{
				return null;
			}
			
			LeafNode leaf = nodeFieldMap.keySet().iterator().next();
			if(leaf.equals(this.root))
			{
				this.root = new TrueNode();
			}
			else
			{
				TrueNode replace = new TrueNode();
				Node parent = leaf.getParent();
				if(leaf.equals(parent.getLeft()))
				{
					parent.setLeft(replace);
				}
				else if(leaf.equals(parent.getRight()))
				{
					parent.setRight(replace);
				}
				replace.setParent(parent);
			}
			
			return nodeFieldMap.get(leaf);
		}
		
		return null;
	}
}
