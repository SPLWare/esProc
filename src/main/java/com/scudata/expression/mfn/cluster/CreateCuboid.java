package com.scudata.expression.mfn.cluster;

import java.io.File;
import java.util.List;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.FileObject;
import com.scudata.dm.Sequence;
import com.scudata.dm.cursor.ICursor;
import com.scudata.dm.cursor.MemoryCursor;
import com.scudata.dw.Cuboid;
import com.scudata.expression.ClusterTableMetaDataFunction;
import com.scudata.expression.Expression;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamInfo2;
import com.scudata.parallel.ClusterCursor;
import com.scudata.resources.EngineMessage;

public class CreateCuboid  extends ClusterTableMetaDataFunction {

	public Object calculate(Context ctx) {
		String C;
		if (param == null) {			
			List<String> files = Cuboid.getCuboids(table); 
			if (files != null) {
				for (String f : files) {
					FileObject fo = new FileObject(f);
					if (fo.isExists())
					{
						fo.delete();
					}
				}
			}
			return Boolean.TRUE;
		} else if (param.isLeaf()) {
			C = (String) param.getLeafExpression().getIdentifierName();
			//delete
			FileObject fo = new FileObject(table.getClusterFile().getFileName() + Cuboid.CUBE_PREFIX + C);
			if (fo.isExists())
			{
				fo.delete();
			}
			return Boolean.TRUE;
		}
		
		IParam sub0;
		IParam sub1 = null;
		if (param.getType() == IParam.Semicolon) {
			int size = param.getSubSize();
			if (size > 2) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cuboid" + mm.getMessage("function.invalidParam"));
			}
			
			sub0 = param.getSub(0);
			if (sub0 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("cuboid" + mm.getMessage("function.invalidParam"));
			}
			
			sub1 = param.getSub(1);
		} else {
			sub0 = param;
		}
		
		C = (String) sub0.getSub(0).getLeafExpression().getIdentifierName();
		sub0 = sub0.create(1, sub0.getSubSize());
		
		Expression []exps;
		String []_names = null;
		Expression []newExps = null;
		String []_newNames = null;
		ParamInfo2 pi0 = ParamInfo2.parse(sub0, "cuboid", true, false);
		exps = pi0.getExpressions1();
		_names = new String[exps.length];
		int i = 0;
		for(Expression e : exps) {
			String s = e.getIdentifierName();
			_names[i++] = s;
		}
		
		ParamInfo2 pi1 = null;
		if (sub1 != null) {
			pi1 = ParamInfo2.parse(sub1, "cuboid", true, false);
			newExps = pi1.getExpressions1();
			_newNames = new String[newExps.length];
			i = 0;
			for(Expression e : newExps) {
				String s = e.getIdentifierName();
				_newNames[i++] = s;
			}
		}
		
		//为cuboid命名
		FileObject fo = new FileObject(table.getClusterFile().getFileName() + Cuboid.CUBE_PREFIX + C);
		if (fo.isExists())
		{
			fo.delete();
		}
		
		String []expNames = null;
		String []names = null;
		String []newExpNames = null;
		String []newNames = null;
		
		if (sub0 != null) {
			pi0 = ParamInfo2.parse(sub0, "cuboid", true, false);
			names = pi0.getExpressionStrs2();
			expNames = pi0.getExpressionStrs1();
		}
		
		pi1 = null;
		if (sub1 != null) {
			pi1 = ParamInfo2.parse(sub1, "cuboid", true, false);
			newExpNames = pi1.getExpressionStrs1();
			newNames = pi1.getExpressionStrs2();
		}
		int fcount = expNames == null ? 0 : expNames.length;
		if (newExps != null) fcount += newExps.length;
		ClusterCursor cc = table.cursor(null, null, null, null, null, null, 0, null, ctx);
		ICursor cursor = new MemoryCursor((Sequence) cc.groups(exps, names, newExps, newNames, null, ctx));
		
		//保存到另一个组表
		File file = fo.getLocalFile().file();
		String colNames[] = new String[fcount];
		int sbytes[] = new int[fcount];
		i = 0;
		for(String n : _names) {
			colNames[i++] = "#" + n;
		}
		
		for(String s : _newNames) {
			colNames[i++] = s;
		}
		Cuboid table = null;
		try {
			table = new Cuboid(file, colNames, sbytes, ctx, "cuboid", "cuboid",
					expNames, newExpNames);
			table.save();
			table.close();
			table = new Cuboid(file, ctx);//重新打开
			table.checkPassword("cuboid");
			table.append(cursor);
			table.writeHeader();
			table.close();
		} catch (Exception e) {
			if (table != null) table.close();
			file.delete();
			throw new RQException(e.getMessage(), e);
		}
		return table;
	}

}
