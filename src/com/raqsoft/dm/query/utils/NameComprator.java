package com.raqsoft.dm.query.utils;

import java.util.Comparator;

import com.raqsoft.common.RQException;

public class NameComprator implements Comparator<String>
{
	public int compare(String NameA, String NameB) 
	{
		if(NameA == null || NameB == null)
		{
			throw new RQException("字符串为空值无法比较大小");
		}
		//长的排在前面，短的排在后面
        if(NameA.length() < NameB.length())
        {
        	return 1;
        }
        else if(NameA.length() > NameB.length())
        {
        	return -1;
        }
        else
        {
        	return 0;
        }
	}
}
