package com.raqsoft.dm.query.utils;

import java.util.Comparator;

import com.raqsoft.util.Variant;

public class SQLValueComprator implements Comparator<Object> 
{
	public int compare(Object ValueA, Object ValueB) 
	{
		return Variant.compare(ValueA, ValueB, true);
	}
}
