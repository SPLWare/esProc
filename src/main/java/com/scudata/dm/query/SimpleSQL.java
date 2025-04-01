package com.scudata.dm.query;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.scudata.app.common.AppUtil;
import com.scudata.app.config.ConfigUtil;
import com.scudata.cellset.ICellSet;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.BFileWriter;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.FileObject;
import com.scudata.dm.ObjectReader;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dm.op.New;
import com.scudata.dw.Cursor;
import com.scudata.expression.Expression;
import com.scudata.resources.EngineMessage;

//文件从esproc的主目录和搜索目录查找

public class SimpleSQL
{
	private Token[] sqlTokens;
	private SimpleUnion select;
	private ModifyHandler handler;
	private List<Object> paramValues;
	private DataStruct dataStruct;
	private int start;
	private int next;
	private Context ctx;
	private ICellSet ics;
	private int type;
	
	public static final int TYPE_SELECT = 0;
	public static final int TYPE_INSERT = 1;
	public static final int TYPE_UPDATE = 2;
	public static final int TYPE_DELETE = 3;
	public static final int TYPE_COMMIT = 4;//包括提交或回滚两种确认命令
	
	public SimpleSQL(String sql, List<Object> paramValues)
	{
		this(null, sql, paramValues, new Context());
	}
	
	public SimpleSQL(String sql, List<Object> paramValues, Context ctx)
	{
		this(null, sql, paramValues, ctx);
	}
	
	public SimpleSQL(ICellSet ics, String sql, List<Object> paramValues, Context ctx)
	{
		this(ics, Tokenizer.parse(sql), 0, -1, paramValues, ctx, true);
	}
	
	public static int getSQLType(Token[] tokens)
	{
		int type = TYPE_SELECT;
		if(tokens[0].isKeyWord("INSERT"))
		{
			type = TYPE_INSERT;
		}
		else if(tokens[0].isKeyWord("UPDATE"))
		{
			type = TYPE_UPDATE;
		}
		else if(tokens[0].isKeyWord("DELETE"))
		{
			type = TYPE_DELETE;
		}
		else if(tokens[0].isKeyWord("COMMIT"))
		{
			type = TYPE_COMMIT;
		}
		else if(tokens[0].isKeyWord("ROLLBACK"))
		{
			type = TYPE_COMMIT;
		}
		return type;
	}
	
	protected SimpleSQL(ICellSet ics, Token[] sqlTokens, int start, int next, List<Object> paramValues, Context ctx, boolean optimize)
	{
		this.ctx = ctx;
		this.ics = ics;
		this.type = getSQLType(sqlTokens);
		if(this.type == TYPE_SELECT)
		{
			this.select = new SimpleUnion(this.ics, this.ctx);
			sqlTokens = Arrays.copyOfRange(sqlTokens, start, ((next == -1) ? (sqlTokens == null ? 0 : sqlTokens.length) : next));
			start = 0;
			next = -1;
			int[] posBuf = new int[]{start, next};
			if(optimize)
			{
				sqlTokens = optimizeQuery(sqlTokens, posBuf);
				start = posBuf[0];
				next = posBuf[1];
			}
		}
		else
		{
//			this.commit = SimpleCommit.createObject();
//			if(this.type == TYPE_INSERT)
//			{
//				this.handler = new SimpleInsert(this.ics, this.ctx);
//			}
//			else if(this.type == TYPE_UPDATE)
//			{
//				this.handler = new SimpleUpdate(this.ics, this.ctx);
//			}
//			else if(this.type == TYPE_DELETE)
//			{
//				this.handler = new SimpleDelete(this.ics, this.ctx);
//			}
		}
		this.sqlTokens = sqlTokens;
		this.paramValues = paramValues;
		this.start = start;
		this.next = ((next == -1) ? (sqlTokens == null ? 0 : sqlTokens.length) : next);
	}
	
	//对外提供的API
	public Object execute()
	{
		if(this.type == TYPE_SELECT)
		{
			if(this.paramValues != null && !this.paramValues.isEmpty())
			{
				this.select.setSQLParameters(this.paramValues);
			}
			ICursor icur = this.select.query(this.sqlTokens, this.start, this.next);
			this.dataStruct = this.select.getDataStruct();
			//去除字段别名里的""
			String[] fieldNames = new String[this.dataStruct.getFieldCount()];
			Expression[] fieldExps = new Expression[this.dataStruct.getFieldCount()];
			int index = 0;
			if(this.dataStruct.getFieldNames() != null)
			{
				for(String fieldName : this.dataStruct.getFieldNames())
				{
					if(fieldName.startsWith("\"") && fieldName.endsWith("\"")
					&& fieldName.substring(1, fieldName.length() - 1).indexOf("\"") == -1)
					{
						fieldName = fieldName.substring(1, fieldName.length() - 1);
					}
					fieldNames[index++] = fieldName;
					fieldExps[index - 1] = new Expression("#" + index);
				}
			}
			if(icur != null)
			{
				icur.addOperation(new New(fieldExps, fieldNames, null), this.ctx);
			}
			this.dataStruct = new DataStruct(fieldNames);
			if(icur == null)
			{
				icur = new MemoryCursor(new Table(this.dataStruct));
			}
			icur.setDataStruct(this.dataStruct);
			//检查是否有CS功能点, 无CS功能点的轻装版返回序表对象
			//if (!Sequence.getFunctionPoint(5)) 
			//{
			//	return icur.fetch();
			//}
			return icur;
//		}
//		else if(this.type == TYPE_COMMIT)
//		{
//			long count = this.commit.execute(this.sqlTokens, this.start, this.next);
//			return count;
//		}
//		else
//		{
//			if(this.paramValues != null && !this.paramValues.isEmpty())
//			{
//				this.handler.setSQLParameters(this.paramValues);
//			}
//			long count = this.handler.execute(this.sqlTokens, this.start, this.next);
//			this.commit.addHandler(this.handler);
//			return count;
		}
		return null;
	}
	
	//仅供简单SQL内部调用以实现嵌套查询
	protected ICursor query()
	{
		if(this.paramValues != null && !this.paramValues.isEmpty())
		{
			this.select.setSQLParameters(this.paramValues);
		}
		ICursor icur = this.select.query(this.sqlTokens, this.start, this.next);
		this.dataStruct = this.select.getDataStruct();
		if(icur == null)
		{
			icur = new MemoryCursor(new Table(this.dataStruct));
		}
		icur.setDataStruct(this.dataStruct);
		//处理完毕
		return icur;
	}
	
	public DataStruct getDataStruct()
	{
		return this.dataStruct;
	}
	
	protected void setWithTableMap(Map<String, Object> tableMap)
	{
		this.select.setWithTableMap(tableMap);
	}
	
	public static Token[] copyTokens(Token[] tokens)
	{
		Token[] cloneTokens = new Token[tokens.length];
		for(int j = 0; j < tokens.length; j++)
		{
			Token token = tokens[j];
			cloneTokens[j] = new Token(token.getType(), token.getString(), token.getPos(), token.getOriginString());
			cloneTokens[j].setSpaces(token.getSpaces());
		}
		return cloneTokens;
	}
	
	void setMemory(boolean isMemory)
	{
		this.select.setMemory(isMemory);
	}
	
	public static int checkParallel(FileObject file)//检查集文件的类型以判断是否可以并行读取即是否由@z生成
	{
		InputStream is = file.getInputStream();
		ObjectReader in = new ObjectReader(is);
		int type = -1;
		try 
		{
			if (in.read() != 'r' || in.read() != 'q' || in.read() != 't' || in.read() != 'b' || in.read() != 'x') 
			{
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
			type = in.read();
			if(type != BFileWriter.TYPE_NORMAL && type != BFileWriter.TYPE_BLOCK && type != BFileWriter.TYPE_GROUP)
			{
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("license.fileFormatError"));
			}
		} 
		catch (Exception e) 
		{
			throw new RQException(e.getMessage(), e);
		}
		finally
		{
			try 
			{
				in.close();
			} 
			catch (Exception e)
			{
				throw new RQException(e.getMessage(), e);
			}
		}
		return type;
	}
	
	public static Token[] optimizeQuery(Token[] sqlTokens, int[] posBuf)//针对北京银行工程中count(*)和子查询做特殊优化
	{
		if(posBuf == null || posBuf.length != 2)
		{
			throw new RQException("参数个数错误");
		}
		
		Token[] bakTokens = copyTokens(sqlTokens);
		List<Token> sqlTokenList = new ArrayList<Token>();
		boolean canOptimize = PerfectSubquery.optimizeSubquery(sqlTokens, sqlTokenList, false, new ArrayList<Boolean>());
		if(!canOptimize)
		{
			sqlTokens = bakTokens;
			bakTokens = copyTokens(sqlTokens);
			sqlTokenList.clear();
		}
		else
		{
			sqlTokens = new Token[sqlTokenList.size()];
			sqlTokenList.toArray(sqlTokens);
			sqlTokenList.clear();
			posBuf[0] = 0;
			posBuf[1] = -1;
		}
		
		bakTokens = copyTokens(sqlTokens);
		sqlTokenList = new ArrayList<Token>();
		canOptimize = PerfectSubquery.optimizeCountAll(sqlTokens, sqlTokenList);
		if(!canOptimize)
		{
			sqlTokens = bakTokens;
			bakTokens = copyTokens(sqlTokens);
			sqlTokenList.clear();
		}
		else
		{
			sqlTokens = new Token[sqlTokenList.size()];
			sqlTokenList.toArray(sqlTokens);
			sqlTokenList.clear();
			posBuf[0] = 0;
			posBuf[1] = -1;
		}
		
		Token lastToken = null;
		for(Token sqlToken : sqlTokens)
		{
			if(lastToken != null && lastToken.getSpaces().isEmpty()
			&& (lastToken.getType() == Tokenizer.IDENT || lastToken.getType() == Tokenizer.KEYWORD  || lastToken.getType() == Tokenizer.NUMBER || lastToken.getType() == Tokenizer.STRING)
			&& (sqlToken.getType() == Tokenizer.IDENT || sqlToken.getType() == Tokenizer.KEYWORD  || sqlToken.getType() == Tokenizer.NUMBER || sqlToken.getType() == Tokenizer.STRING))
			{
				lastToken.addSpace();
			}
			lastToken = sqlToken;
		}

		return sqlTokens;
	}
	
	public static void main(String args[]){
		try {
			ConfigUtil.load("d:\\esProcData\\raqsoftConfig.xml");
			Context ctx = new Context();
//			Object o = AppUtil.executeSql("select * from D:/esProcData/员工.ctx where 1 = 2", null, ctx, false);
			//			Object o = AppUtil.executeSql("select top 1 * from D:/esProcData/员工.ctx", null, ctx, false);
//			if (o != null) {
//				if (o instanceof com.raqsoft.dm.cursor.SubCursor) {
//					System.out.println(((com.raqsoft.dm.cursor.SubCursor)o).fetch());
//				} else if (o instanceof com.raqsoft.dw.Cursor) {
//					System.out.println(((com.raqsoft.dw.Cursor)o).fetch());
//				}
//			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
