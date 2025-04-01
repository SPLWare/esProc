package com.scudata.expression.fn;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Process;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.ObjectCache;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.JobSpace;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.Node;
import com.scudata.resources.EngineMessage;

/**
 * 调用系统命令，执行完毕后返回。例如打开bat和exe文件。
 * system(cmd) system(cmd1, cmd2, ...)
 * 目前只能执行一条cmd命令。如果想执行多条cmd命令，则把多条cmd命令写入bat文件中, 通过cmd命令调用bat文件。
 * @author runqian
 *
 */
public class SystemExec extends Function {
	static class Grabber extends Thread {
		StringBuffer buf;
		InputStream in;
		public Grabber(StringBuffer buf, InputStream in) {
			this.buf = buf;
			this.in = in;
		}

		public void run() {
			try {
				String enter = System.getProperty("line.separator", "\r\n");
				InputStreamReader isr = new InputStreamReader(in);
				BufferedReader br = new BufferedReader(isr);
				String line;
				while ((line = br.readLine()) != null) {
					if (buf != null) {
						buf.append(line);
						buf.append(enter);
					}
				}
			} catch (Exception e) {
			}
		}
	}

	public Node optimize(Context ctx) {
		param.optimize(ctx);
		return this;
	}

	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("system" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		JobSpace js = ctx.getJobSpace();
		if (js != null && js.getAppHome() != null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException(mm.getMessage("license.fpNotSupport") + "system");
		}

		String cmd = null;
		String []cmds = null;
		if (param.isLeaf()) {
			Object cmdObj = param.getLeafExpression().calculate(ctx);
			if (!(cmdObj instanceof String)) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("system" + mm.getMessage("function.paramTypeError"));
			}

			cmd = (String)cmdObj;
		} else {
			int count = param.getSubSize();
			cmds = new String[count];

			for (int i = 0; i < count; ++i) {
				IParam sub = param.getSub(i);
				if (sub == null || !sub.isLeaf()) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("system" + mm.getMessage("function.invalidParam"));
				}

				Object cmdObj = sub.getLeafExpression().calculate(ctx);
				if (!(cmdObj instanceof String)) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("system" + mm.getMessage("function.paramTypeError"));
				}

				cmds[i] = (String)cmdObj;
			}
		}

		try {
			Runtime runtime = Runtime.getRuntime();
			Process process;
			if (cmds == null) {
				process = runtime.exec(cmd);
			} else {
				process = runtime.exec(cmds);
			}

			StringBuffer errBuf = new StringBuffer(1024);
			StringBuffer outBuf = new StringBuffer(1024);
			Grabber g1 = new Grabber(errBuf, process.getErrorStream());
			Grabber g2 = new Grabber(outBuf, process.getInputStream());
			g1.start();
			g2.start();

			boolean isWait = true, isOut = false, isAll = false;
			if (option != null) {
				if (option.indexOf('p') != -1) {
					isWait = false;
				} else {
					if (option.indexOf('a') != -1) {
						isAll = true;
					} else if (option.indexOf('o') != -1) {
						isOut = true;
					}
				}
			}
			
			if (isWait) {
				int n = process.waitFor();
				g1.join();
				g2.join();
				
				if (isAll) {
					String error = null;
					if (errBuf.length() > 0) {
						error = errBuf.toString();
						Logger.info(error);
					}
					
					String out = null;
					if (outBuf.length() > 0) {
						out = outBuf.toString();
						Logger.info(out);
					}
					
					Sequence result = new Sequence(3);
					result.add(ObjectCache.getInteger(n));
					result.add(out);
					result.add(error);
					return result;
				} else if (isOut) {
					if (errBuf.length() > 0) {
						Logger.info(errBuf);
					}
					
					if (outBuf.length() > 0) {
						String out = outBuf.toString();
						Logger.info(out);
						return out;
					} else {
						return null;
					}
				} else {
					if (errBuf.length() > 0) {
						Logger.info(errBuf);
					}
					
					if (outBuf.length() > 0) {
						Logger.info(outBuf);
					}
					
					return ObjectCache.getInteger(n);
				}
			} else {
				return Boolean.TRUE;
			}
		} catch (Exception e) {
			throw new RQException(e);
		}
	}
}
