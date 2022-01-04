package com.scudata.dm.sql.simple;

import com.scudata.common.RQException;

public class Coalesce implements IFunction
{
	public String getFormula(String[] params) 
	{
		StringBuffer sb = new StringBuffer();
		for(int i = 0, len = params.length; i < len; i++)
		{
			if(params[i].isEmpty())
			{
				throw new RQException("Coalesce函数参数不能为空");
			}
			if(i > 0)
			{
				sb.append(",");
			}
			sb.append(params[i]);
		}
		return String.format("ifn(%s)", sb.toString());
	}
}
