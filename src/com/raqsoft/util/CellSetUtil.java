package com.raqsoft.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.raqsoft.cellset.datamodel.CellSet;
import com.raqsoft.cellset.datamodel.PgmCellSet;
import com.raqsoft.cellset.datamodel.PgmNormalCell;
import com.raqsoft.common.ByteArrayInputRecord;
import com.raqsoft.common.ByteArrayOutputRecord;
import com.raqsoft.common.ByteMap;
import com.raqsoft.common.DES;
import com.raqsoft.common.IOUtils;
import com.raqsoft.common.MessageManager;
import com.raqsoft.common.RQException;
import com.raqsoft.common.Sentence;
import com.raqsoft.dm.Context;
import com.raqsoft.dm.LineImporter;
import com.raqsoft.dm.Param;
import com.raqsoft.dm.ParamList;
import com.raqsoft.dm.Sequence;
import com.raqsoft.expression.Expression;
import com.raqsoft.resources.EngineMessage;

/**
 * 程序网读写工具类
 * @author WangXiaoJun
 *
 */
public class CellSetUtil {
	private static final byte Type_PgmCellSet = 1;
	private static final String KEY = "rqqrrqqr"; // 加密密钥
	private static final byte ENCRYPTED = 0x01;

	/**
	 * 写程序网
	 * 网格类型 + 版本 + 密码长度 + 密码 + 属性长度 + 属性 + 是否加密 + 网长度 + 网内容
	 * @param out 输出流
	 * @param cs 程序网对象
	 * @throws Exception
	 */
	public static void writePgmCellSet(OutputStream out, PgmCellSet cs) throws Exception {
		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.write(Type_PgmCellSet); // 网格类型
		out.write(4); // 版本4：网格参数改成了value存真实值，editValue存输入值

		// 密码长度+密码
		ByteArrayOutputRecord bo = new ByteArrayOutputRecord();
		String psw = cs.getPasswordHash();
		bo.writeString(psw);
		int privilege = cs.getNullPasswordPrivilege();
		bo.writeInt(privilege);
		byte []pswBytes = bo.toByteArray();
		DES pswDes = new DES(KEY);
		pswBytes = pswDes.encrypt(pswBytes);
		IOUtils.writeInt(out, pswBytes.length);
		out.write(pswBytes);

		// 属性长度+属性
		ByteMap map = cs.getCustomPropMap();
		if (map == null || map.size() == 0) {
			IOUtils.writeInt(out, 0);
		} else {
			byte []mapBytes = map.serialize();
			IOUtils.writeInt(out, mapBytes.length);
			out.write(mapBytes);
		}

		byte[] csBytes = cs.serialize();
		if (psw == null || psw.length() == 0) {
			out.write(0); // 网格内容没有加密
		} else {
			out.write(ENCRYPTED); // 网格内容加密
			DES des = new DES(KEY);
			csBytes = des.encrypt(csBytes);
		}

		IOUtils.writeInt(out, csBytes.length);
		out.write(csBytes);

		out.write('R');
		out.write('Q');
		out.write('Q');
		out.write('R');
		out.flush();
	}

	/**
	 * 返回网格文件是否加密了
	 * @param fileName String 文件路径名
	 * @return boolean
	 */
	public static boolean isEncrypted(String fileName) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			return isEncrypted(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 返回网格文件是否加密了
	 * @param is InputStream 输入流
	 * @return boolean
	 */
	public static boolean isEncrypted(InputStream is) {
		try {
			int c1 = is.read();
			int c2 = is.read();
			int c3 = is.read();
			int c4 = is.read();

			if (c1 != 'R' || c2 != 'Q' || c3 != 'Q' || c4 != 'R') {
				return false;
			}

			int type = is.read(); // 网格类型
			if (type != Type_PgmCellSet) {
				return false;
			}

			int ver = is.read(); // 版本
			if (ver < 3) {
				return false;
			} else {
				int pswLen = IOUtils.readInt(is);
				byte []pswBytes = new byte[pswLen];
				IOUtils.readFully(is, pswBytes);
				DES pswDes = new DES(KEY);
				pswBytes = pswDes.decrypt(pswBytes);

				ByteArrayInputRecord bi = new ByteArrayInputRecord(pswBytes);
				String psw = bi.readString();
				return psw != null && psw.length() > 0;
			}
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * 读取网格的自定义属性映射
	 * @param fileName String
	 * @return ByteMap
	 */
	public static ByteMap readCsCustomPropMap(String fileName) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(fileName);
			return readCsCustomPropMap(fis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 读取网格的自定义属性映射
	 * @param is InputStream
	 * @return ByteMap
	 */
	public static ByteMap readCsCustomPropMap(InputStream is) {
		try {
			int c1 = is.read();
			int c2 = is.read();
			int c3 = is.read();
			int c4 = is.read();

			if (c1 != 'R' || c2 != 'Q' || c3 != 'Q' || c4 != 'R') {
				return null;
			}

			int type = is.read(); // 网格类型
			int ver = is.read(); // 版本
			if (ver > 1 || type != Type_PgmCellSet) {
				int pswLen = IOUtils.readInt(is);
				if (pswLen > 0) is.skip(pswLen);
			}

			int mapLen = IOUtils.readInt(is);
			if (mapLen > 0) {
				byte []mapBytes = new byte[mapLen];
				IOUtils.readFully(is, mapBytes);
				ByteMap map = new ByteMap();
				map.fillRecord(mapBytes);
				return map;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 写程序网
	 * @param fileName 要写入程序网的文件名
	 * @param cs 程序网对象
	 * @throws Exception
	 */
	public static void writePgmCellSet(String fileName, PgmCellSet cs) throws Exception {
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(fileName));
			writePgmCellSet(bos, cs);
		} finally {
			if(bos != null) bos.close();
		}
	}

	/**
	 * 读程序网
	 * @param is InputStream 输入流
	 * @throws Exception
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(InputStream is) throws Exception {
		return readPgmCellSet(is, null);
	}

	private static void changeOldVersionParam(PgmCellSet pcs) {
		// 版本小于4的网格里的参数值存的是编辑值
		ParamList paramList = pcs.getParamList();
		if (paramList == null) {
			return;
		}
		
		for (int i = 0, size = paramList.count(); i < size; ++i) {
			Param param = paramList.get(i);
			Object value = param.getValue();
			if (value instanceof String) {
				String old = (String)value;
				value = Variant.parse(old);
				param.setValue(value);
				
				if (value instanceof String) {
					int match = Sentence.scanQuotation(old, 0);
					if (match == old.length() -1) {
						param.setEditValue('\'' + (String)value);
					} else if (old.charAt(0) == '\'') {
						param.setEditValue('\'' + old);
					} else {
						param.setEditValue(old);
					}
				} else {
					param.setEditValue(old);
				}
			}
		}
	}
	
	/**
	 * 读程序网
	 * @param is 输入流
	 * @param psw 密码，没有密码则为空
	 * @return PgmCellSet
	 * @throws Exception
	 */
	public static PgmCellSet readPgmCellSet(InputStream is, String psw) throws Exception {
		if (is.read() != 'R' || is.read() != 'Q' || is.read() != 'Q' || is.read() != 'R') {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		int type = is.read(); // 网格类型
		if (type != Type_PgmCellSet) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fileFormatError"));
		}

		PgmCellSet cs = new PgmCellSet();
		int ver = is.read(); // 版本
		
		if (ver == 1) {
			int mapLen = IOUtils.readInt(is);
			if (mapLen > 0) is.skip(mapLen);

			int csLen = IOUtils.readInt(is);
			byte []csBytes = new byte[csLen];
			IOUtils.readFully(is, csBytes);

			is.read(); // R
			is.read(); // Q
			is.read(); // Q
			is.read(); // R

			// 先把签名读出来，序列化是需要检查是否签名了
			cs.fillRecord(csBytes);
			changeOldVersionParam(cs);
			return cs;
		}

		int pswLen = IOUtils.readInt(is);
		byte []pswBytes = new byte[pswLen];
		IOUtils.readFully(is, pswBytes);

		if (ver > 2) {
			DES pswDes = new DES(KEY);
			pswBytes = pswDes.decrypt(pswBytes);

			ByteArrayInputRecord bi = new ByteArrayInputRecord(pswBytes);
			String pswHash = bi.readString();
			int nullPswPrivilege = bi.readInt();
			PgmCellSet.getPrivilege(pswHash, psw, nullPswPrivilege);
			//if (privilege == PgmCellSet.PRIVILEGE_NULL) {
			//	MessageManager mm = EngineMessage.get();
			//	throw new RQException(mm.getMessage("cellset.pswError"));
			//}
		}

		int mapLen = IOUtils.readInt(is);
		if (mapLen > 0) {
			is.skip(mapLen);
		}

		// 是否有加密权限
		int isEncrypted = is.read() & ENCRYPTED;
		int csLen = IOUtils.readInt(is);
		byte []csBytes = new byte[csLen];
		IOUtils.readFully(is, csBytes);

		is.read(); // R
		is.read(); // Q
		is.read(); // Q
		is.read(); // R

		if (isEncrypted == ENCRYPTED) { // 加密程序网
			DES des = new DES(KEY);
			csBytes = des.decrypt(csBytes);
		}

		cs.fillRecord(csBytes);
		cs.setCurrentPassword(psw);
		
		if (ver < 4) {
			// 版本小于4的网格里的参数值存的是编辑值
			changeOldVersionParam(cs);
		}
		
		return cs;
	}

	/**
	 * 读程序网
	 * @param fileName 程序网文件名
	 * @throws Exception
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(String fileName) throws Exception {
		return readPgmCellSet(fileName, null);
	}

	/**
	 * 读加了密的程序网
	 * @param fileName 程序网文件名
	 * @throws Exception
	 * @param psw String 密码
	 * @return PgmCellSet
	 */
	public static PgmCellSet readPgmCellSet(String fileName, String psw) throws Exception {
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(fileName));
			return readPgmCellSet(bis, psw);
		}
		finally {
			if(bis != null) bis.close();
		}
	}
	
	/**
	 * @param cellSet CellSet
	 * @param args String[]
	 * Esproc 为dos命令输入方式，值通常为串数组,需要先计算再put到context。
	 */
	public static void putArgStringValue(CellSet cellSet,String[] args) {
		if( args==null ) {
			putArgValue(cellSet, null);
		} else {
			Object[] vals = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				vals[i] = PgmNormalCell.parseConstValue(args[i]);
			}
			
			putArgValue(cellSet, vals);
		}
	}

	/**
	 * 把args的值依cellSet中参数次序依次设置
	 * @param cellSet CellSet，要设置的网格对象
	 * @param args Object[]，用户输入的串类型的参数值，方法为Esproc以及dataHub中计算网格前准备，xq
	 * DataHub为JDBC调用方式，值都是算好的Object数组。
	 */
	public static void putArgValue(CellSet cellSet,Object[] args) {
		ParamList params = cellSet.getParamList();
		if (params == null || params.count() == 0) {
			return;
		}
		
		Context context = cellSet.getContext();
		int c = 0;
		for (int i = 0; i < params.count(); i++) {
			Param p = params.get(i);
			if (p.getKind() != Param.VAR){//Param.ARG) {
				continue;
			}
			
			String paraName = p.getName();
			if (args != null && c < args.length) {//paras.isUserChangeable() &&
				context.setParamValue(paraName, args[c], Param.VAR);// Param.ARG);
				c++;
			} else{
				context.setParamValue(paraName, p.getValue(), Param.VAR);//, Param.ARG);
			}
		}
	}
	
	/**
	 * 执行单表达式，不生成网格
	 * @param src 表达式
	 * @param args 参数值构成的序列，用argi引用
	 * @param ctx
	 * @return
	 */
	public static Object execute1(String src, Sequence args, Context ctx) {
		// 语句中的参数，固定以"arg"开头
		if (args != null && args.length() > 0) {
			for (int i = 1; i <= args.length(); ++i) {
				ctx.setParamValue("arg" + i, args.get(i));
			}
		}
		
		Expression exp = new Expression(ctx, src);
		return exp.calculate(ctx);
	}

	/**
	 * 执行表达式串，列用tab分隔，行用回车分隔
	 * @param src
	 * @param args 参数值构成的序列，用argi引用
	 * @param ctx
	 * @return
	 */
	public static Object execute(String src, Sequence args, Context ctx) {
		PgmCellSet pcs = toPgmCellSet(src);
		
		// 语句中的参数，固定以"arg"开头
		if (args != null && args.length() > 0) {
			for (int i = 1; i <= args.length(); ++i) {
				ctx.setParamValue("arg" + i, args.get(i));
			}
		}
		
		pcs.setContext(ctx);
		pcs.calculateResult();
		
		if (pcs.hasNextResult()) {
			return pcs.nextResult();
		} else {
			int colCount = pcs.getColCount();
			for (int r = pcs.getRowCount(); r > 0; --r) {
				for (int c = colCount; c > 0; --c) {
					PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
					if (cell.isCalculableCell() || cell.isCalculableBlock()) {
						return cell.getValue();
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * 单元格表达式二维数组生成程序网
	 * @param expStrs 单元格表达式组成的行列二维数组
	 * @return 程序网
	 */
	public static PgmCellSet toPgmCellSet(String[][]expStrs) {
		if (expStrs == null || expStrs.length == 0) {
			return null;
		}
		
		int rowCount = expStrs.length;
		int colCount = 0;
		
		for (int r = 0; r < rowCount; ++r) {
			if (expStrs[r] != null && expStrs[r].length > colCount) {
				colCount = expStrs[r].length;
			}
		}
		
		if (colCount == 0) {
			return null;
		}
		
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);
		for (int r = 1; r <= rowCount; ++r) {
			String []row = expStrs[r - 1];
			int count = row != null ? row.length : 0;
			for (int c = 1; c <= count; ++c) {
				PgmNormalCell cell = pcs.getPgmNormalCell(r, c);
				cell.setExpString(row[c - 1]);
			}
		}
		
		return pcs;
	}
	
	/**
	 * 分隔字符串，填到程序网的格子里
	 * @param src 字符串，列用tab分隔，行用回车分隔
	 * @return
	 */
	public static PgmCellSet toPgmCellSet(String src) {
		if (src == null || src.length() == 0) return null;
		
		char []buffer = src.toCharArray();
		int len = buffer.length;
		int index = 0;
		
		// 开头的n行是参数
		// #var1=xxx
		// #var2=xxx
		ParamList paramList = new ParamList();
		while (index < len && buffer[index] == '#') {
			String strParam = null;
			for(int i = ++index; i < len; ++i) {
				if (buffer[i] == '\n') {
					if (buffer[i - 1] == '\r') {
						strParam = new String(buffer, index, i - index - 1);
					} else {
						strParam = new String(buffer, index, i - index);
					}
					
					index = i + 1;
					break;
				}
			}
			
			if (strParam == null) {
				strParam = new String(buffer, index, len - index);
				index = len;
			}
			
			int s = strParam.indexOf('=');
			String paramName;
			Object paramValue = null;
			if (s != -1) {
				paramName = strParam.substring(0, s);
				paramValue = Variant.parse(strParam.substring(s + 1), false);
			} else {
				paramName = strParam;
			}
			
			paramList.add(paramName, Param.VAR, paramValue);
		}
		
		final char colSeparator = '\t';
		ArrayList<String> line = new ArrayList<String>();
		int rowCount = 10;
		int colCount = 1;
		PgmCellSet pcs = new PgmCellSet(rowCount, colCount);
		int curRow = 1;

		if (paramList.count() > 0) {
			pcs.setParamList(paramList);
		}
		
		while (index != -1) {
			index = LineImporter.readLine(buffer, index, colSeparator, line);
			int curColCount = line.size();
			if (curColCount > colCount) {
				pcs.addCol(curColCount - colCount);
				colCount = curColCount;
			}

			if (curRow > rowCount) {
				rowCount += 10;
				pcs.addRow(10);
			}
			
			for (int f = 0; f < curColCount; ++f) {
				String exp = line.get(f);
				if (exp != null && exp.length() > 0) {
					PgmNormalCell cell = pcs.getPgmNormalCell(curRow, f + 1);
					cell.setExpString(exp);
				}
			}
			
			curRow++;
			line.clear();
		}
		
		pcs.removeRow(curRow, rowCount - curRow + 1);
		return pcs;
	}
	
	/**
	 * 把程序网转为字符串,开头的n行是参数
	 * #var1=***
	 * #var2=***
	 * @param cs 程序网
	 * @return String
	 */
	public static String toString(PgmCellSet cs) {
		StringBuffer sb = new StringBuffer(1024);
		ParamList paramList = cs.getParamList();
		int paramCount = paramList == null ? 0 : paramList.count();
		boolean isFirstLine = true;
		for (int i = 0; i < paramCount; ++i) {
			Param param = paramList.get(i);
			if (isFirstLine) {
				isFirstLine = false;
			} else {
				sb.append('\n');
			}
			
			sb.append('#');
			sb.append(param.getName());
			sb.append('=');
			sb.append(Variant.toString(param.getValue()));
		}
		
		int rowCount = cs.getRowCount();
		int colCount = cs.getColCount();
		for (int r = 1; r <= rowCount; ++r) {
			if (isFirstLine) {
				isFirstLine = false;
			} else {
				sb.append('\n');
			}
			
			for (int c = 1; c <= colCount; ++c) {
				if (c > 1) {
					sb.append('\t');
				}
				
				String exp = cs.getPgmNormalCell(r, c).getExpString();
				if (exp != null) {
					sb.append(exp);
				}
			}
		}
		
		return sb.toString();
	}
}
