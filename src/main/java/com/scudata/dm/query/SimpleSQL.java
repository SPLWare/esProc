package com.scudata.dm.query;

import java.util.ArrayList;
import java.util.List;

import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.resources.ParseMessage;

public class SimpleSQL {
	private static final String PARAMNAME_PREFIX = "sql_temp_param_"; // 变量名前缀
	
	private ICellSet cs;
	private Context ctx;
	private Token[] tokens;
	//private List<Object> paramValues;
	
	private ArrayList<WithItem> withItems = new ArrayList<WithItem>();
	private QueryBody query;
	private int subTableSeq = 0; // 用于生成子查询序表的变量名
	
	public SimpleSQL(String sql, List<Object> paramValues) {
		this(null, sql, paramValues, null);
	}
	
	public SimpleSQL(String sql, List<Object> paramValues, Context ctx) {
		this(null, sql, paramValues, ctx);
	}
	
	public SimpleSQL(ICellSet cs, String sql, List<Object> paramValues, Context ctx) {
		this.cs = cs;
		this.tokens = Tokenizer.parse(sql);
		
		if (ctx == null) {
			this.ctx = new Context();
		} else {
			this.ctx = new Context(ctx);
		}
		
		if (paramValues != null && paramValues.size() > 0) {
			// 产生临时上下文，把参数产生成变量
			ParamList paramList = this.ctx.getParamList();
			long now = System.currentTimeMillis();
			String prefix = PARAMNAME_PREFIX + now + "_";
			int seq = 1;
			
			for (Object val : paramValues) {
				Param param = new Param(prefix + seq, Param.VAR, val);
				paramList.add(param);
				seq++;
			}

			// 把?n修改为变量名
			for (Token token : tokens) {
				if (token.getType() == Tokenizer.PARAMMARK) {
					seq = Integer.parseInt(token.getString());
					if (seq > paramList.count()) {
						MessageManager mm = ParseMessage.get();
						throw new RQException(mm.getMessage("function.paramError") + token.getPos());
					}
					
					Param param = paramList.get(seq - 1);
					token.setString(param.getName());
				}
			}
		}
	}
	
	public WithItem getWithItem(String tableName) {
		for (WithItem with : withItems) {
			if (with.equals(tableName)) {
				return with;
			}
		}
		
		return null;
	}
		
	public ICellSet getCellSet() {
		return cs;
	}

	public Context getContext() {
		return ctx;
	}

	/**
	 * 扫描with语句，返回下一个
	 * @param tokens
	 * @param start
	 * @param next
	 * @return
	 */
	private int scanWith(Token []tokens, int start, int next) {
		// WITH [RECURSIVE] with_query_name1 [(column_name[, ...])] AS (...),with_query_name1 [(column_name[, ...])] AS (...),...
		start++; // 跳过with
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		if (tokens[start].isKeyWord("RECURSIVE")) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
		}
		
		while (start < next) {
			start = scanWithQuery(tokens, start, next);
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			} else if (tokens[start].isComma()) {
				start++;
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				}
			} else {
				break;
			}
		}
		
		return start;
	}

	// with_query_name1 [(column_name[, ...])] AS (...)
	private int scanWithQuery(Token []tokens, int start, int next) {
		if (tokens[start].getType() != Tokenizer.IDENT) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
		}
		
		String name = tokens[start].getOriginString();
		ArrayList<String> columnNames = null;
		start++;
		
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		}
		
		if (tokens[start].getType() == Tokenizer.LPAREN) {
			start++;
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
			
			columnNames = new ArrayList<String>();
			while (start < next) {
				if (tokens[start].getType() != Tokenizer.IDENT) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
				}
				
				columnNames.add(tokens[start].getOriginString());
				start++;
				
				if (start == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
				} else if (tokens[start].isComma()) {
					start++;
				} else if (tokens[start].getType() == Tokenizer.RPAREN) {
					start++;
					break;
				} else {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
				}
			}
			
			if (start == next) {
				MessageManager mm = ParseMessage.get();
				throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
			}
		}
		
		if (!tokens[start].isKeyWord("AS")) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
		}
		
		start++;
		if (start == next) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start - 1].getPos());
		} else if (tokens[start].getType() != Tokenizer.LPAREN) {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
		}
		
		int end = Tokenizer.scanParen(tokens, start, next);
		start++;
		
		if (tokens[start].isKeyWord("SELECT")) {
			QueryBody query = scanQuery(tokens, start, end);
			WithItem withItem = new WithItem(name, columnNames, query);
			withItems.add(withItem);
			return end + 1;
		} else {
			MessageManager mm = ParseMessage.get();
			throw new RQException(mm.getMessage("syntax.error") + tokens[start].getPos());
		}
	}

	private QueryBody scanQuery(Token []tokens, int start, int next) {
		SetOperation operation = null;
		for (int i = start; i < next;) {
			Token token = tokens[i];
			if (token.getType() == Tokenizer.LPAREN) {
				i = Tokenizer.scanParen(tokens, i, next) + 1;
			} else if (token.isKeyWord("UNION")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
				
				SetOperation.Type type = SetOperation.Type.UNION;
				if (tokens[i].isKeyWord("ALL")) {
					type = SetOperation.Type.UNIONALL;
					i++;
				}
				
				if (operation == null) {
					operation = new SetOperation(type);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(type);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else if (token.isKeyWord("INTERSECT")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
								
				if (operation == null) {
					operation = new SetOperation(SetOperation.Type.INTERSECT);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(SetOperation.Type.INTERSECT);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else if (token.isKeyWord("MINUS")) {
				Select select = new Select(this, tokens, start, i);
				
				i++;
				if (i == next) {
					MessageManager mm = ParseMessage.get();
					throw new RQException(mm.getMessage("syntax.error") + tokens[i - 1].getPos());
				}
								
				if (operation == null) {
					operation = new SetOperation(SetOperation.Type.MINUS);
					operation.setLeft(select);
				} else {
					operation.setRight(select);
					SetOperation newOperation = new SetOperation(SetOperation.Type.MINUS);
					newOperation.setLeft(operation);
					operation = newOperation;
				}
				
				start = i;
			} else {
				i++;
			}
		}
		
		if (operation == null) {
			return new Select(this, tokens, start, next);
		} else {
			Select select = new Select(this, tokens, start, next);
			operation.setRight(select);
			return operation;
		}
	}
	
	private void analyseSQL() {
		int start = 0;
		int next = tokens.length;
		if (tokens[start].isKeyWord("WITH")) {
			start = scanWith(tokens, start, next);
		}
		
		query = scanQuery(tokens, start, next);
	}
	
	String getNextTableParamName() {
		++subTableSeq;
		return "subsql_table_param_name" + subTableSeq;
	}
	
	/**
	 * 执行查询返回游标或序表
	 * @return
	 */
	public Object execute() {
		analyseSQL();
		return query.getData();
	}
	
	/**
	 * 执行查询返回游标
	 * @return
	 */
	public ICursor query() {
		Object obj = execute();
		if (obj instanceof ICursor) {
			return (ICursor)obj;
		} else if (obj instanceof Sequence) {
			return new MemoryCursor((Sequence)obj);
		} else {
			return null;
		}
	}
	
	/**
	 * 取查询的结果集数据结构
	 * @return
	 */
	public DataStruct getDataStruct() {
		return query.getDataStruct();
	}
}
