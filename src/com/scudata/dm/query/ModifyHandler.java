package com.scudata.dm.query;

import java.util.List;

import com.scudata.dm.Context;

public interface ModifyHandler 
{
	public void commit();
	public long execute(Token[] tokens, int start, int next);
	public void setSQLParameters(List<Object> paramList);
	public long getTotalCount();
	public Context getContext();
}
