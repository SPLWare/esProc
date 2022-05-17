package com.scudata.server.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.ArgumentTokenizer;
import com.scudata.common.FileUtils;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.expression.mfn.sequence.Export;
import com.scudata.ide.spl.Esproc;
import com.scudata.parallel.Task;
import com.scudata.resources.ParallelMessage;
import com.scudata.server.IServer;
import com.scudata.thread.Job;
import com.scudata.util.CellSetUtil;
import com.scudata.util.DatabaseUtil;
import com.scudata.util.Variant;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Http服务处理器
 * 
 * 支持的url写法:
 * 1  http://..../splx1.splx()
 * 2  http://..../splx1.splx(arg1,arg2,...)
 * 3  http://localhost:.../splx1.splx(...)splx2.splx
 * HTTP服务的url串格式，分两种情况：
 * 1、只有一个splx文件。如果splx有参数，则在括号里依次写参数值，值间用逗号分隔；
 * 如果没有参数，则写个空括号。 计算结果按系统默认格式转化成字符串返回
 * 2、有两个splx，这种情况用于用户想要的返回串格式与系统默认的不同。
 * 		 第1个splx的写法与第1种情况相同。splx2是单参数的程序，参数值为splx1的返回值。
 * 用户在splx2中将splx1的返回值转换成自己需要格式的字符串，然后作为splx2的返回值
 *  
 * @author Joancy
 *
 */
public class SplxHttpHandler implements HttpHandler {
	private IServer server = null;
	static MessageManager mm = ParallelMessage.get();

	/**
	 * 设置服务器
	 * @param is 服务器
	 */
	public void setIServer(IServer is) {
		server = is;
	}

	/**
	 * 处理Http请求
	 */
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			if (HttpContext.dfxHome == null) {
				String mainPath = Env.getMainPath();
				if (mainPath == null || mainPath.trim().length() == 0) {
					throw new Exception(
							mm.getMessage("DfxHttpHandler.emptyMain"));
				}
				mainPath = StringUtils.replace(mainPath, "\\", "/");
				HttpContext.dfxHome = mainPath.trim();
			}
			URI uri = httpExchange.getRequestURI();
			String path = uri.getPath();
			if (path.equals("/shutdown")) {
				server.shutDown();
				return;
			}
			// 浏览器头一次访问时，会发送这么一个请求，忽略它
			if (path.equals("/favicon.ico")) {
				return;
			}
			new Thread(new HandlerThread(httpExchange)).start();
		} catch (Throwable t) {
			String result;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				t.printStackTrace(new PrintStream(baos));
			} finally {
				baos.close();
			}
			result = "error:" + baos.toString();
			result = StringUtils.replace( result, "\n", "<br>" );
			Logger.severe(t);
			try {
				byte[] bytes = result.getBytes("UTF-8");
				httpExchange.getResponseHeaders().add( "Content-Type", "text/html;charset=UTF-8" );
				httpExchange.sendResponseHeaders( 500, bytes.length );
				OutputStream os = httpExchange.getResponseBody();
				os.write(bytes);
				os.close();
			}
			catch( Throwable th ){
				Logger.severe(th);
			}
			finally {
				try {
					httpExchange.close();
				}
				catch( Throwable th){}
			}
			//SplxServerInIDE dsi = (SplxServerInIDE)server;
			//dsi.shutDown();     //为什么要shutdown呢
		}
		
	}

	private Object obj2String(Object obj) {
		if (obj == null)
			return "";
		if( obj instanceof String || obj instanceof byte[] )
			return obj;
		String result = "";
		if (obj instanceof Table) { // Table
			Table t = (Table) obj;
			result = Export.export(t, null, null, null, "t", new Context());
		} else if (obj instanceof Sequence) {// 排列，序列
			result = ((Sequence) obj).toExportString();
		} else if (obj instanceof Record) {// 记录
			result = ((Record) obj).toString("tq");
		} else {
			result = obj.toString();
		}
		return result;
	}

	class HandlerThread extends Job{
		HttpExchange httpExchange;
		
		public HandlerThread(HttpExchange httpExchange){
			this.httpExchange = httpExchange;
		}
		
		public void run() {
			Object result = "";
			String contentType = "text/html;charset=UTF-8";
			String headers = null;
			int status = 200;     //请求响应码
			Context ctx1 = null;
			try {
				URI uri = httpExchange.getRequestURI();
				String path = uri.getPath().trim();

				if (path.equals("/")) {
					String url = SplxServerInIDE.getInstance().getContext().getDefaultUrl();
					result = mm.getMessage("DfxHttpHandler.demo", url);
				} else {
					String ext = null;
					int extpos = path.lastIndexOf( "." );
					if( extpos > 0 ) {
						ext = path.substring( extpos + 1 );
						ext = ext.toLowerCase();
					}
					if( ext != null ) {
						if( "png".equals( ext ) || "jpg".equals( ext ) || "gif".equals( ext ) || "jpeg".equals( ext ) ) {
							contentType = "image";
						}
						else {
							ext = null;
						}
					}
					if( ext != null ) {   //访问图片文件
						String fileName = path.substring( 1 );
						FileObject fo = new FileObject(fileName, "sp");
						InputStream is = null;
						try {
							is = fo.getInputStream();
							result = getStreamBytes( is );
						}
						catch( Throwable th ) {
							status = 404;
						}
						finally {
							try{ is.close(); }catch( Throwable t ){}
						}
					}
					else {    //没有扩展名的，都当成splx
						if( path.endsWith( ".splx" ) || path.endsWith( ".spl" ) || path.endsWith( ".dfx" ) ) {  //以这些结尾时，自动给添加上()
							path += "()";
						}
						String fileName = "";
						String splx2 = "";
						String params = "";
						int pos = path.indexOf( "(" );
						if( pos > 0 ) { 
							if( path.indexOf(".splx") < 0 && path.indexOf(".spl") < 0 && path.indexOf(".dfx") < 0 )
								throw new Exception(
										mm.getMessage("DfxHttpHandler.errorurl"));
							path = path.trim();
							pos = path.lastIndexOf(")");
							if (pos < 0)
								throw new Exception(mm.getMessage(
										"DfxHttpHandler.erroruri", path));
							String path1 = path.substring(0, pos);
							if (pos < path.length() - 1)
								splx2 = path.substring(pos + 1).trim();
							int start = path1.indexOf("(");
							if (start < 0)
								throw new Exception(mm.getMessage(
										"DfxHttpHandler.erroruri", path1));
							params = path1.substring(start + 1).trim();
							fileName = path1.substring(1, start).trim();
						}
						else {
							//path中没有()，说明是sap的restful格式的url: http://..../sapPath/dfx/argvalue1/argvalue2/...
							//其中argvalue是参数名与值合在一起书写，参数名全是字母，参数值全是数字开头
							//没有dfx2
							path = path.trim();
							ArrayList<String> saps = SplxServerInIDE.getInstance().getContext().getSapPath();
							String prefix = "";
							for( int k = 0; k < saps.size(); k++ ) {
								String sap = saps.get( k );
								if( !sap.startsWith( "/" ) ) sap = "/" + sap;
								if( path.startsWith( sap ) ) {
									if( sap.length() > prefix.length() ) prefix = sap;
								}
							}
							path = path.substring( prefix.length() );
							pos = path.indexOf( "/", 1 );
							String path1 = path;
							String args = "";
							if( pos > 0 ) {
								path1 = path.substring( 0, pos );
								args = path.substring( pos + 1 );
							}
							path1 = prefix + path1;
							fileName = path1.substring(1).trim();
							if( !fileName.toLowerCase().endsWith( ".splx" ) ) fileName += ".splx";
							if( args.length() > 0 ) {
								ArgumentTokenizer at = new ArgumentTokenizer( args, '/' );
								while( at.hasMoreTokens() ) {
									String tmp = at.nextToken().trim();
									while( tmp.length() > 0 && !Character.isDigit( tmp.charAt( 0 ) ) ) {
										tmp = tmp.substring( 1 );
									}
									if( params.length() > 0 ) params += ",";
									params += tmp;
								}
							}
						}
						PgmCellSet pcs1 = null;
						try {
							FileObject fo = new FileObject(fileName, "s");
							pcs1 = fo.readPgmCellSet();
						}
						catch( Throwable th ) {
							status = 404;
							th.printStackTrace();
						}
						if( status == 200 ) {
							ParamList list = pcs1.getParamList();
							ArgumentTokenizer at = new ArgumentTokenizer(params, ',');
							boolean hasPost = false;
							ctx1 = Esproc.prepareEnv();
							if( list != null ) {
								for( int i = 0; i < list.count(); i++ ) {
									Param p = (Param) list.get(i);
									Object value = p.getValue();
									String pName = p.getName();
									if( "argpost".equals( pName ) ) hasPost = true;
									//if( value == null ) continue;
									ctx1.setParamValue( pName, value );
								}
							}
							for (int i = 0; at.hasMoreTokens() && list != null; i++) {
								String pvalue = at.nextToken();
								//if (pvalue == null || pvalue.trim().length() == 0) continue;
								Param p = (Param) list.get(i);
								ctx1.setParamValue(p.getName(), Variant.parse(pvalue));
							}
							//读取post内容
							if( hasPost ) {
								InputStream reqis = null;
								try {
									reqis = httpExchange.getRequestBody();
									BufferedReader br = new BufferedReader( new InputStreamReader( reqis ) );
									StringBuffer sb = new StringBuffer();
									String line = null;
									while( ( line = br.readLine() ) != null ) {
										sb.append( line ).append( "\n" );
									}
									ctx1.setParamValue( "argpost", sb.toString() );
									//Logger.info( "设置argpost参数为：" + sb.toString() );
								}
								finally {
									try{ if( reqis != null ) reqis.close(); }catch(Throwable t){}
								}
							}
							//post end
							SplxServerInIDE server = SplxServerInIDE.instance;
							if (server != null) {
								RaqsoftConfig rc = server.getRaqsoftConfig();
								if (rc != null) {
									List<String> dbs = rc.getAutoConnectList();
									DatabaseUtil.connectAutoDBs(ctx1, dbs);
								}
							}
		
							try {
								pcs1.reset();
								pcs1.setContext(ctx1);
								pcs1.calculateResult();
							}
							finally {
								JobSpaceManager.closeSpace( ctx1.getJobSpace().getID() );
							}
							Object obj1 = pcs1.nextResult();
							if (splx2.length() == 0) { // 说明只有一个splx
								result = obj2String(obj1);
							} else {
								if (!splx2.startsWith("/"))
									splx2 = "/" + splx2;
								PgmCellSet pcs2 = CellSetUtil
										.readPgmCellSet(HttpContext.dfxHome + splx2);
								ParamList list2 = pcs2.getParamList();
								Context ctx2 = new Context();
								ctx2.setParamValue(((Param) list2.get(0)).getName(), obj1);
								pcs2.reset();
								pcs2.setContext(ctx2);
								pcs2.calculateResult();
								Object obj2 = pcs2.nextResult();
								result = obj2String(obj2);
							}
							Object hs = pcs1.nextResult();   //返回的第二个结果表示responseHeaders
							if( hs != null ) {
								headers = (String)obj2String( hs );
							}
						}
					}
				}
			}catch (Throwable t) {
				status = 500;
				result = "error:";
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					t.printStackTrace(new PrintStream(baos));
				} finally {
					try {
						baos.close();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				result = "error:" + baos.toString();
				result = StringUtils.replace( (String)result, "\n", "<br>" );
//				t.printStackTrace();
				Logger.severe(t);
			} finally {
				DatabaseUtil.closeAutoDBs(ctx1);
			}
			
			try{
				String encoding = "UTF-8"; 
				Headers hs = httpExchange.getResponseHeaders();
				boolean hasContentType = false;
				if( headers != null && headers.trim().length() > 0 ) {
					JSONObject jo = new JSONObject( headers );
					Iterator<String> it = jo.keys();
					while( it.hasNext() ) {
						String key = it.next();
						if( key.toLowerCase().equals( "content-type" ) ) {
							hasContentType = true;
							String ct = jo.getString(key);
							int pos = ct.indexOf( "charset=" );
							if( pos >= 0 ) encoding = ct.substring( pos + 8 ).trim();
						}
						if( key.toLowerCase().equals( "status" ) ) {
							status = jo.getInt( key );
						}
						else hs.add( key, jo.getString( key ) );
					}
				}
				byte[] bytes = null;
				if( result instanceof String ) {
					bytes = ((String)result).getBytes(encoding);
				}
				else {
					bytes = (byte[])result;
					String type = FileUtils.getFileFormat( bytes );
					if( FileUtils.FORMAT_GIF.equals( type )	|| FileUtils.FORMAT_JPG.equals( type ) || FileUtils.FORMAT_PNG.equals( type ) ) {
						contentType = "image";
					}
				}
				if( !hasContentType ) {
					hs.add( "Content-Type", contentType );
				}
				if( status == 404 ) {
					httpExchange.sendResponseHeaders( 404, -1 );
				}
				else {
					httpExchange.sendResponseHeaders( status, bytes.length );
					OutputStream os = httpExchange.getResponseBody();
					os.write(bytes);
					os.close();
				}
			}
			catch(Throwable x){
				Logger.severe( x );
			}
			finally {
				try {
					httpExchange.close();
				}
				catch( Throwable th){}
			}
		}
		
		public byte[] getStreamBytes(InputStream is) throws Exception {
			ArrayList al = new ArrayList();
			int totalBytes = 0;
			byte[] b = new byte[102400];
			int readBytes = 0;
			while ((readBytes = is.read(b)) > 0) {
				byte[] bb = new byte[readBytes];
				System.arraycopy(b, 0, bb, 0, readBytes);
				al.add(bb);
				totalBytes += readBytes;
			}
			b = new byte[totalBytes];
			int pos = 0;
			for (int i = 0; i < al.size(); i++) {
				byte[] bb = (byte[]) al.get(i);
				System.arraycopy(bb, 0, b, pos, bb.length);
				pos += bb.length;
			}
			return b;
		}
		
	}

}

