package com.raqsoft.lib.kafka.function;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.raqsoft.common.Logger;
import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.Sequence;

import com.raqsoft.dm.cursor.ICursor;

public class ImCursor extends ICursor {
	private Context m_ctx;	
	private ImConnection m_conn = null;
	public  List<Object[]> m_buffer;
	private int m_nPart;   //分区数
	private int m_nTotal;  //记录总数
	private int m_current; //当前位置
	private boolean m_bFirst = true;
	private long m_nTimeout = 1000;
	
	public ImCursor(Context ctx, ImConnection conn, long nTimeout, int nPartSize) {
		this.m_ctx = ctx;
		this.m_conn = conn;
		this.m_nPart = nPartSize;
		m_nTimeout = nTimeout;
		ctx.addResource(this);	
		
		init();
	}
	
	private void init() {
		try {
			m_buffer = new ArrayList<Object[]>();	
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	public synchronized void close() {
		super.close();
		
		try {
			if (m_ctx != null) {
				m_ctx.removeResource(this);
				m_ctx = null;
			}
		} catch (Exception e) {
			Logger.error(e.getStackTrace());
		}
	}

	protected void finalize() throws Throwable {
		close();
	}
	
	private void getTotal(){
		if (m_bFirst){
			for(int i=0; i<3; i++) {
				if (m_nPart ==-1){
					m_buffer = ImFunction.getData(m_conn, m_nTimeout);
				}else{
					m_buffer = ImFunction.getClusterData(m_conn, m_nTimeout, m_nPart);
				}
	        	m_nTotal = m_buffer.size();
	        	if (m_nTotal>0){
		        	System.out.println("getTotal = " + m_nTotal );
		        	break;
		        }
		    }
			m_bFirst = false;
		}
	}
	
	protected Sequence get(int n) {	
		getTotal();
		List<Object[]> ls = getData(n);			
	
		if(ls.size()==0){
			return null;
		}else{			
			return ImFunction.toTable(m_conn.m_cols, ls);
		}
	}
	
	private List<Object[]> getData(int n) {
		List<Object[]> ls = new ArrayList<Object[]>();
		// 1。缓冲足够多
		if (n < m_buffer.size()) {
			m_current += n;
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext() && n > 0) {
				ls.add(iter.next());
				iter.remove();
				n--;
			}
			
		} else if (n == m_buffer.size()) { // 2。缓冲==n时
			ls.addAll(m_buffer);
			m_buffer.clear();
			m_current += n;
		} else if (n > m_buffer.size()) { // 3。缓冲<n时
			if(m_current >= m_nTotal) return ls;
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext()) {
				ls.add(iter.next());
			}
			n = n - m_buffer.size();
			m_current = m_buffer.size();
			m_buffer.clear();
			//有多少记录显示多少，若需要与n保持一致，参考elastic的实现.
			
			try {
				int nTotal = 0;
				while (m_current < m_nTotal) {
					if (m_nPart ==-1){
						m_buffer = ImFunction.getData(m_conn, m_nTimeout);
					}else{
						m_buffer = ImFunction.getClusterData(m_conn, m_nTimeout, m_nPart);
					}
					int len = m_buffer.size();
					if (len==0){
						break;
					}
					m_current += len;
					nTotal += len;
					//System.out.println("getlen = " + m_current);
					if (nTotal >= n) {
						break;
					}
				}
				// 数据补充到ls中.
				iter = m_buffer.iterator();
				while (iter.hasNext() && n > 0) {
					ls.add(iter.next());
					iter.remove();
					n--;
				}
			} catch (Exception e) {
				Logger.error(e.getStackTrace());
			}
		}

		return ls;
	}

	@Override
	protected long skipOver(long arg0) {
		getTotal();		
		long n = skipData(arg0);
				
		return n;
	}
	
	private long skipData(long n) {
		long org = n;
		if (m_buffer == null || n == 0) return 0;
		// 1。缓冲足够多
		if (n < m_buffer.size()) {
			m_current += n;
			Iterator<Object[]> iter = m_buffer.iterator();
			while (iter.hasNext() && n > 0) {
				iter.next();
				iter.remove();
				n--;
			}
		} else if (n == m_buffer.size()) { // 2。缓冲==n时
			m_current += n;
			n = 0;
			m_buffer.clear();
			
		} else if (n > m_buffer.size()) { // 3。缓冲<n时
			if(m_current >= m_nTotal) return 0;
			Iterator<Object[]> iter = null;
			n = n - m_buffer.size();
			m_current = m_buffer.size();
			m_buffer.clear();
			//有多少记录跳过多少，若需要与n保持一致，参考elastic的实现.
			
			try {
				int nTotal = 0;
				while (true) {
					if (m_nPart ==-1){
						m_buffer = ImFunction.getData(m_conn, m_nTimeout);
					}else{
						m_buffer = ImFunction.getClusterData(m_conn, m_nTimeout, m_nPart);
					}
					int len = m_buffer.size();
					if (len==0){
						break;
					}
					m_current += n;
					nTotal += len;
					//System.out.println("skip len = " + m_current);
					if (nTotal >= n) {
						break;
					}
				}
				// 数据补充到ls中.
				iter = m_buffer.iterator();
				while (iter.hasNext() && n > 0) {
					iter.next();
					iter.remove();
					n--;
				}
			} catch (Exception e) {
				Logger.error(e.getStackTrace());
			}
		}

		return org - n;
	}

}
