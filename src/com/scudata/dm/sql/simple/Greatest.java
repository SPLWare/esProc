package com.scudata.dm.sql.simple;

import java.util.TreeMap;

import com.scudata.dm.Context;
import com.scudata.dm.query.utils.SQLValueComprator;
import com.scudata.expression.Expression;

public class Greatest implements IFunction
{
	public String getFormula(String[] params)
	{
		TreeMap<Object, String> treeMap = new TreeMap<Object, String>(new SQLValueComprator());
		StringBuffer sb = new StringBuffer("[");
		boolean cannotSort = false;
		for(int i = 0; i < params.length; i++)
		{
			if(!cannotSort)
			{
				try
				{
					Object obj = new Expression(params[i]).calculate(new Context());
					if(obj == null)
					{
						return "null";
					}
					treeMap.put(obj, params[i]);
				}
				catch(Exception ex)
				{
					cannotSort = true;
				}
			}
			
			if(i > 0)
			{
				sb.append(",");
			}
			sb.append(params[i]);
		}
		sb.append("]");

		if(cannotSort)
		{
			return String.format("if(%s.contain(null),null,%s.max())", sb.toString(), sb.toString());
		}
		else
		{
			return treeMap.lastEntry().getValue();
		}
	}
}
