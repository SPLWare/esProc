package com.raqsoft.lib.informix.function.multiCursor;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.informix.lang.IfxToJavaType;
import com.raqsoft.common.Logger;
import com.raqsoft.dm.Table;
import com.raqsoft.lib.informix.helper.ImColumn;
import com.raqsoft.lib.informix.helper.ImTableInfo;

public class BPipeReader {
	public static int BUF_SIZE = 1024*1024*5;
	public boolean m_bOK=false;
	public int m_nCurrent;
	private int[] m_bufLen;
	private int[] m_offset;
	private ImColumn[] m_colums;
	
	private byte[] m_buf;
	private FileInputStream m_fis = null;
	private ImTableInfo m_tblInfo;

	private int  m_idx = 0;
	private static int  m_index = 0;
	private static long m_nTestDataSize[]=new long[10]; //总数据大小
	private static long m_startTime[]=new long[10];
	private static long m_endTime[]=new long[10];
	//private static Object m_lock = new Object();
	
	public BPipeReader(ImTableInfo tblInfo, String fileName){
		m_tblInfo = tblInfo;	

		try {
			m_nCurrent = 0;
			m_bufLen = new int[1];
			m_offset = new int[1];
			m_buf = new byte[BUF_SIZE];			
			m_idx =m_index;
			m_index++;
    		int nColSize = m_tblInfo.m_colNames.length;		
    		m_colums = new ImColumn[nColSize];
    		for(int nCol = 0; nCol<nColSize; nCol++){
    			m_colums[nCol] = m_tblInfo.m_colMap.get(nCol);
    		}
    		System.out.println("sPipeName1000 = " + fileName );
    		fileName = fileName.replace("\\", "/");
    		System.out.println("sPipeName2000 = " + fileName );
	    	m_fis = new FileInputStream(fileName);
	    	if (m_fis==null) return;	
	    	Date dStart = new Date();
			m_startTime[m_index] = dStart.getTime();
    	} catch (IOException e) {
    		Logger.error(e.getStackTrace());
		}
	}
	
	public void close() {
		try {
			if (m_fis!=null){
				m_fis.close();
				m_fis = null;
			}
		} catch (IOException e) {
			Logger.error(e.getStackTrace());
		}
	}
	
	public Object fetch(){	
		Table table = new Table(m_tblInfo.m_colNames);
		Map<Integer, ImColumn> map = m_tblInfo.getColumnInfo();
		int nColSize = map.size();
		Object[] objs = new Object[nColSize];
    	while( (m_bufLen[0]=ReadFile(m_fis, m_buf, BUF_SIZE-m_offset[0], m_offset[0]))>0){
	    	m_offset[0] = 0; //从头开始遍历
	    	while(true){
	    		if (m_bufLen[0]-m_offset[0]<m_tblInfo.getColSize()){
	    			// 剩余数据copy到头.
	    			System.arraycopy(m_buf,m_offset[0],m_buf,0, m_bufLen[0]-m_offset[0]);
	    			m_offset[0] = m_bufLen[0]-m_offset[0];
	    			break;
	    		}
	    		
	    		getValue(m_tblInfo,m_buf,m_bufLen, m_offset,nColSize, objs);
	    		table.newLast(objs);
	    		m_nCurrent++;
	 	        //System.out.println("idx=" + m_nCurrent + " val=" + objs);
	     	}		 	
    	}
    	
		return table;
	}
	
	public Table getData(int n){
		int nIndex = 0;
		Table table = new Table(m_tblInfo.m_colNames);
		Map<Integer, ImColumn> map = m_tblInfo.getColumnInfo();
		int nColSize = map.size();
		Object[] objs = new Object[nColSize];
		if (m_bufLen[0]>m_tblInfo.getColSize()){
	    	m_offset[0] = 0; //从头开始遍历
	    	while(true){
	    		if (m_bufLen[0]-m_offset[0]<m_tblInfo.getColSize()){
	    			// 剩余数据copy到头.
	    			int nLen = m_bufLen[0]-m_offset[0];
	    			if (nLen > 0){
		    			System.arraycopy(m_buf,m_offset[0],m_buf,0, nLen);
		    			m_offset[0] = nLen;
	    			}else{
	    				m_offset[0] = 0;
	    			}
					m_bufLen[0] = nLen;
	    			break;
	    		}
	    		
	    		getValue(m_tblInfo, m_buf,m_bufLen, m_offset, nColSize, objs);
	    		table.newLast(objs);
	    		m_nCurrent++;
	    		nIndex++;
	    		if (nIndex>=n) {
	    			// 剩余数据copy到头.
	    			int nLen = m_bufLen[0]-m_offset[0];
	    			if (nLen > 0){
		    			System.arraycopy(m_buf,m_offset[0],m_buf,0, nLen);
		    			m_offset[0] = nLen;
		    			m_bufLen[0] = nLen;
	    			}else{
	    				m_offset[0] = 0;
	    			}
	    			return table;
	    		}
	     	}	
		}
		
		// for test speed;
    	while( (m_bufLen[0]=ReadFile(m_fis, m_buf, BUF_SIZE-m_offset[0], m_offset[0]))>0  ){
    		//synchronized(m_lock)
    		{
    			m_nTestDataSize[m_idx] += m_bufLen[0];
    			Date dEnd = new Date();
	    		m_endTime[m_idx] = dEnd.getTime();
	    		long nDiff = (m_endTime[m_idx]-m_startTime[m_idx]);
	    		if (nDiff>30000){		    			
	    			m_startTime[m_idx] = m_endTime[m_idx];
	    			SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
	    			String sEndDate = df.format(dEnd);
	    			long nTotalM = m_nTestDataSize[m_idx]/(1024*1024);
	    			System.out.println("idx=" + m_idx//+ m_nTestCurrent + " readLen="+m_buf[nIndex].curLen 
	    					+ " diff="+ nDiff
	    					+ " dataLen=" + nTotalM
	    					+ " speed=" + (nTotalM*1000/nDiff) 
	    					+ " date=" + sEndDate);
	    					
	    			m_nTestDataSize[m_idx] = 0;
	    			//System.out.println("idx=" + nIndex +" nSize="+m_buf[nIndex].nSize + " total="+m_nTestCurrent);
	    		}
    		}
    		
    		m_bufLen[0] += m_offset[0];
	    	m_offset[0] = 0; //从头开始遍历
	    	while(true){
	    		if (m_bufLen[0]<=m_offset[0]){
	    			break;
	    		}else if (m_bufLen[0]>0 && m_bufLen[0]-m_offset[0]<m_tblInfo.getColSize()/2){
	    			// 剩余数据copy到头.
	    			int nLen = m_bufLen[0]-m_offset[0];
	    			if (nLen > 0){
		    			System.arraycopy(m_buf,m_offset[0],m_buf,0, nLen);
		    			m_offset[0] = nLen;
	    			}else{
	    				m_offset[0] = 0;
	    			}
	    			
	    			break;
	    		}
	    		
	    		getValue(m_tblInfo, m_buf,m_bufLen, m_offset, nColSize, objs);
	    		table.newLast(objs);
	    		m_nCurrent++;
	    		nIndex++;
//	    		if (m_nCurrent%10000==0){
//	 	       		System.out.println("thread_"+m_threadIndex+" idx=" + m_nCurrent + " val=" + objs[0]+ " val2=" + objs[1]);
//	    		}
	    		if (nIndex>=n) {
	    			// 剩余数据copy到头.
	    			int nLen = m_bufLen[0]-m_offset[0];
	    			if (nLen > 0){
		    			System.arraycopy(m_buf,m_offset[0],m_buf,0, nLen);
		    			m_offset[0] = nLen;
		    			
	    			}else{
	    				m_offset[0] = 0;
	    			}
					m_bufLen[0] = nLen;
	    			return table;
	    		}
	    		
//	    		if (m_nCurrent%10000==0){
//	 	       		System.out.println("idx=" + m_nCurrent + " val=" + objs[0]+ " val2=" + objs[1]);
//	    		}
	     	}		 	
    	}
    	
		return table;
	}

    // inputstrean, buffer, bufSize, offset
    public int ReadFile(FileInputStream inStream, byte[] bs, int bufSize, int offset){
    	int len=0; 
    	int oldLen = offset;
    	try{      	
    		//System.out.println("ReadFile_"+m_threadIndex+" start....bufSize=" + bufSize + " offset=" + offset);
        	while((len = inStream.read(bs, offset, bufSize))!=-1){//使用readLine方法，一次读一行                
        		bufSize -= len;
                offset+=len;
                if (bufSize<1024){
                	break;
                }
            }
        	
        }catch(Exception e){
            Logger.error(e.getStackTrace());
        }
    	len=offset-oldLen;
    	len = len<0 ? 0 : len;
        return len;
    }
    
    /* TableInfo信息
	 * buf缓冲数据
     * bufLen 数据长度
     * offset 数据偏移位置
     * nColSize 列数
     * retObjs 返回数据
     */ 
    public void getValue(ImTableInfo tblInfo, byte[] buf,int[] bufLen,int[] offset, int nColSize, Object[] retObjs){
    	try {    	
    		ImColumn col = null;
    		for(int nCol = 0; nCol<nColSize; nCol++){
    			col = m_colums[nCol];
	        	switch(col.nType){
	        	case TYPE_BIGSERIAL:
	        	case TYPE_BIGINT:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaLongBigInt(buf,offset[0]);
	        		break;
	        	}
	        	case TYPE_BOOLEAN:{
	        		//0:点位，1:数据
	        		retObjs[nCol] = (buf[offset[0]+1]==1)?true:false;
	        		break;
	        	}
	        	
	        	case TYPE_SERIAL:
	        	case TYPE_INTEGER:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaInt(buf,offset[0]);
	        		if ((Integer)retObjs[nCol]==Integer.MIN_VALUE) retObjs[nCol] = null;
	        		break;
				} 
	        	case TYPE_MONEY:
	        	case TYPE_DECIMAL:{
	        		try {
	        			retObjs[nCol]= IfxToJavaType.IfxToJavaDecimal(buf,offset[0], col.nSize,col.nEndSize);
	        		}catch (Throwable t){
	        			retObjs[nCol] = null;
	        		}
	        		break;
	        	}
	        	case TYPE_NCHAR:
	        	case TYPE_CHAR:{
	        		String s = IfxToJavaType.IfxToJavaChar(buf,offset[0],(int)col.nSize,m_tblInfo.m_encode,false);
	        		if(s!=null) s = s.trim();
	        		retObjs[nCol] = s;	        		
	        		break;
	        	}
	        	case TYPE_LVARCHAR:{
	        		short len = ImTableInfo.bytes2short(buf, offset[0]+1);
	        		String s = IfxToJavaType.IfxToJavaChar(buf,offset[0]+3,len,null,false);
	        		if(s!=null) s = s.trim();
	        		retObjs[nCol] = s;	        
	        		break;
	        	}
	        		
	        	case TYPE_VARCHAR:{
	        		int nLen = (byte)buf[offset[0]] & 0xff;
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaChar(buf,offset[0]+1,nLen,m_tblInfo.m_encode,false);
	        		break;
	        	}
	        	
	        	case TYPE_LONG:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaDouble(buf,offset[0]);
	        		break;
	        	}	        		
	        		
	        	case TYPE_DOUBLE:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaDouble(buf,offset[0]);
	        		break;
	        	}
	        	
	        	case TYPE_FLOAT:{
	        		retObjs[nCol] = ImTableInfo.bytes2Double(buf,offset[0]);
	        		break;
	        	}
	        	
	        	case TYPE_INT8:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaLongInt(buf,offset[0]);
	        		if ((Long)retObjs[nCol]==Long.MIN_VALUE) retObjs[nCol] = null;
	        		break;
	        	}	        	
	        	case TYPE_DATE:{
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaDate(buf,offset[0],col.nSize, (short)0);
	        		break;
	        	}
	        	case TYPE_INTERVAL:
	        	case TYPE_DATETIME:{	        		
					retObjs[nCol] = IfxToJavaType.IfxToDateTimeUnloadString(buf,offset[0],col.nSize, col.nColLength);
					
	        		break;
	        	}
	        	
	        	case TYPE_SMALLFLOAT:{	 
	        		ImTableInfo.reverseOrder(buf,offset[0],4);
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaReal(buf,offset[0],col.nSize, (short)0);
	        		break;
	        	}
	        	
	        	case TYPE_SMALLINT:{	        		
	        		retObjs[nCol] = IfxToJavaType.IfxToJavaSmallInt(buf,offset[0],col.nSize, (short)0);
	        		if ((Short)retObjs[nCol]==Short.MIN_VALUE) retObjs[nCol] = null;
	        		break;
	        	}
	        	}//endSwitch
	        	offset[0] += col.nSize;
	        }
    	} catch (IOException e) {
    		Logger.error(e.getStackTrace());
		} 
    }
	
	public long skip(long n) throws IOException {

		return n;
	}
}
