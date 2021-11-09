package com.raqsoft.lib.ftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import com.raqsoft.common.Logger;

public class FileUtil {

	public static void write( String path, byte[] b ) throws Throwable {
		OutputStream os = null;
		try {
			os = getOutputStream( path );
			os.write( b );
			os.flush();
		}
		finally {
			try{ os.close(); }catch( Exception e ) {}
		}
	}


	public static void write( String path, String s, String charSet ) throws Throwable {
		write(path, s.getBytes(charSet));
	}

	public static OutputStream getOutputStream( String path ) throws Throwable {
		File f = new File( path );
		if( !f.exists() ) {
			f.getParentFile().mkdirs();
		}
		OutputStream os = null;
		try {
			os = new FileOutputStream( f );
			if ( os != null )return os;
		}
		catch ( Throwable e ) {
			e.printStackTrace();
		}
		return null;
	}


	public static byte[] getStreamBytes( InputStream is ) throws Exception {
		ArrayList al = new ArrayList();
		int totalBytes = 0;
		byte[] b = new byte[102400];
		int readBytes = 0;
		while ( ( readBytes = is.read( b ) ) > 0 ) {
			byte[] bb = new byte[readBytes];
			System.arraycopy( b, 0, bb, 0, readBytes );
			al.add( bb );
			totalBytes += readBytes;
		}
		b = new byte[totalBytes];
		int pos = 0;
		for ( int i = 0; i < al.size(); i++ ) {
			byte[] bb = ( byte[] ) al.get( i );
			System.arraycopy( bb, 0, b, pos, bb.length );
			pos += bb.length;
		}
		return b;
	}
	
	public static String readFile(File f, String charset) throws Exception {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			byte[] b = new byte[fis.available()];
			fis.read(b);
			return new String(b, charset);
		} catch (Exception e) {
			Logger.error(new String("read file error:[" + f.getPath() + "]"),e);
			e.printStackTrace();
		} finally {
			try {fis.close();} catch(Exception e) {};
		}
		return "";
	}


}
