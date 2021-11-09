package com.raqsoft.lib.informix.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.raqsoft.cellset.INormalCell;
import com.raqsoft.common.Logger;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.SQLParser;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Expression;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;
import com.raqsoft.lib.informix.helper.Fragment;
import com.raqsoft.lib.informix.helper.IfxConn;
import com.raqsoft.lib.informix.helper.ImSQLParser;
import com.raqsoft.resources.EngineMessage;

public class ImQuery extends ImFunction {
	private String m_table=null;
	private String m_sql=null;
	private String m_sWhere=null;
	private int m_seg[] = new int[2];
	private Fragment m_frag = null;
	private Map<String,String> m_fieldMap = new HashMap<String,String>();
	
	public Node optimize(Context ctx) {
		super.optimize(ctx);
		
		return this;
	}

	public Object calculate(Context ctx) {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifx_client" + mm.getMessage("function.missingParam"));
		}
		
		option = this.getOption();	
		if (param.getType()==IParam.Semicolon){
			IParam sub = null;
			int nSize = param.getSubSize();
			for(int i=0; i<nSize; i++){
				sub = param.getSub(i);
				if (i==0){
					prevParse(ctx, sub);
				}else{// fragment start:end
					if (sub == null) break;
					postParse(ctx, sub);
				}				
			}
		}else{
			prevParse(ctx, param);
		}
		
		if (m_frag==null){
			m_frag = m_ifxConn.getFragment(m_table);
			if (m_frag!=null){
				m_frag.setSegment(0, 0);
			}
		}
		return doQuery(null);		
	}
	
	private void prevParse(Context ctx, IParam sub){
		Object o;
		String field =null;
		String fieldAlias=null;
		ArrayList<Expression> lsExp = new ArrayList<Expression>();
		
		for(int j=0; j<sub.getSubSize(); j++){
			if (j==0){ // 1. ifxConn;
				o = sub.getSub(j).getLeafExpression().calculate(ctx);
				if (o instanceof IfxConn){
					m_ifxConn = (IfxConn)o;
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("ifx_cursor" + mm.getMessage("function.invalidParam"));
				}
			}else if(j==1){ //2. table:where or sql
				if (sub.getSub(j)==null) continue;
				sub.getSub(j).getAllLeafExpression(lsExp);
				Expression e = lsExp.get(0);
				if(e.isConstExpression()){
					m_sql = ((Expression) e).getIdentifierName();					
				}else{
					INormalCell cell = e.getHome().getSourceCell();
					if (cell==null) continue;
					m_sql = cell.getValue().toString();
				}
				m_sql = m_sql.replaceAll("\"", "");
				
				String sql = m_sql.toLowerCase();			
				if (sql.indexOf("select ")>=0){
					SQLParser parse = new SQLParser(m_sql);
					m_table = parse.getClause(SQLParser.KEY_FROM);
					String a[] = m_table.split(" ");
					m_table = a[0];
				}else{
					m_table = m_sql;
				}
				if (lsExp.size()==2){
					m_sWhere = ((Expression) lsExp.get(1)).getIdentifierName();
					m_sWhere = m_sWhere.replaceAll("\"", "");
				}						
			}else{ //3. fields
				if (sub.getSub(j)==null) continue;
				lsExp.clear();
				sub.getSub(j).getAllLeafExpression(lsExp);
				field = ((Expression) lsExp.get(0)).getIdentifierName();
				field = field.replaceAll("\"", "");
				if (lsExp.size()==2){
					fieldAlias = ((Expression) lsExp.get(1)).getIdentifierName();
					fieldAlias = fieldAlias.replaceAll("\"", "");
				}else{
					fieldAlias = null;
				}
				m_fieldMap.put(field, fieldAlias);
			}
		}
		
		if (m_ifxConn==null){
			MessageManager mm = EngineMessage.get();
			throw new RQException("ifx_cursor " + mm.getMessage(" ifxConn is null"));
		}
	}
	
	private void postParse(Context ctx, IParam sub){
		ArrayList<Expression> lsExp = new ArrayList<Expression>();
		sub.getAllLeafExpression(lsExp);
		
		String s = null;
		Expression e = lsExp.get(0);
		if(e.isConstExpression()){ //1.for k1:k2
			s = ((Expression) lsExp.get(0)).getIdentifierName();
			m_seg[0] = Integer.parseInt(s);
			if (m_seg[0]<0) m_seg[0] = 1;
			if (lsExp.size()==2){
				e = lsExp.get(1);
				if (e!=null){
					s = e.getIdentifierName();
					m_seg[1] = Integer.parseInt(s);
					if (m_seg[1]<0) m_seg[1] = 0;
				}
			}
			
			m_frag = m_ifxConn.getFragment(m_table);
			if (m_frag!=null){
				if (m_seg[0]>m_frag.getPartitionCount()) m_seg[0]=m_frag.getPartitionCount();
				if (m_seg[1]>m_frag.getPartitionCount()) m_seg[1]=m_frag.getPartitionCount();
				m_frag.setSegment(m_seg[0], m_seg[1]);
			}
		}else{ //2. for ifx_cursor:f
			INormalCell cell = e.getHome().getSourceCell();
			ImCursor cursor = (ImCursor)cell.getValue();
			String cursorTable = cursor.getTableName();
			if (cursorTable!=null){
				//System.out.println("cursor = " + cursorTable);
				m_frag = new Fragment();
				Fragment frag = m_ifxConn.getFragment(cursorTable);
				m_frag = frag;
				if (lsExp.size()==2 && m_frag!=null){
					e = lsExp.get(1);
					if (e!=null){
						s = e.getIdentifierName().replaceAll("\"", "");
						m_frag.setFieldName(s);
					}
				}
			}
		}		
	}
	
	public Object doQuery( Object[] objs){
		try {
			String sql = "";
			if (m_sql!=null && m_sql.indexOf("select")!=-1){
				sql = m_sql;
				//SQLParser parser = new SQLParser (sql);
				//m_table = parser.getClause(SQLParser.KEY_FROM);
			}else {
				sql = "select * from "+m_table;
				if (m_fieldMap.size()>0){			
					String key, val;
					String fields = "kkk";
					Set<Entry<String, String>> set = m_fieldMap.entrySet();
					Iterator<Entry<String, String>> iterator = set.iterator();
					while (iterator.hasNext())
					{
						Map.Entry<String, String> mapentry = iterator.next();
						key = (String)mapentry.getKey();	
						val = (String)mapentry.getValue();	
						if (val==null){
							fields += ","+key;
						}else{
							fields += ","+key+" as " + val;
						}
					}
	
					fields=fields.replaceFirst("kkk,", "");
					sql = "select " + fields + " from " + m_table;
				}
				
				if (m_sWhere != null){
					sql += " where "+m_sWhere;
				}
			}
			
			boolean bMultiPipe = false;
			Fragment.ORDER_TYPE nOrderby = Fragment.ORDER_TYPE.ORDER_FORCE;
			if (option!=null){
				if (this.option.indexOf('o')>-1){
					nOrderby = Fragment.ORDER_TYPE.ORDER_NO;
				}else if (this.option.indexOf('f')>-1){
					nOrderby = Fragment.ORDER_TYPE.ORDER_FORCE;
				}
				if (this.option.indexOf('m')>-1){
					bMultiPipe = true;
				}
			}
			//System.out.println("cursor sql = " + sql);
			if (m_frag!=null){
				m_frag.setOrderby(nOrderby);
				ImSQLParser parser = new ImSQLParser(m_frag, sql);
				if (parser.isOk()){
					String newSql = parser.getSql();	
					if (bMultiPipe){
						return new ImMultiCursor(m_ctx, m_ifxConn, newSql, m_frag, nOrderby );
					}else{
						return new ImCursor(m_ctx, m_ifxConn, newSql, m_frag, nOrderby );
					}
				}else{
					MessageManager mm = EngineMessage.get();
					throw new RQException("ifx_cursor " + mm.getMessage("group by, "+parser.getGroupString()+" is not segment column"));
				}
			}else{	
				if (bMultiPipe){
					return new ImMultiCursor(m_ctx, m_ifxConn, sql, null, nOrderby );
				}else{
					return new ImCursor(m_ctx, m_ifxConn, sql, null, nOrderby);
				}
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
		
		return null;
	}

}

