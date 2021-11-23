package com.scudata.common;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
	
	public final static String FORMAT_GIF = "gif";
	public final static String FORMAT_JPG = "jpg";
	public final static String FORMAT_PNG = "png";
	public final static String FORMAT_PDF = "pdf";
	public final static String FORMAT_XLS = "xls";
	public final static String FORMAT_XLSX = "xlsx";
	public final static String FORMAT_DOC = "doc";
	public final static String FORMAT_DOCX = "docx";
	public final static String FORMAT_XLS_DOC = "doc/xls";
	public final static String FORMAT_XLSX_DOCX = "docx/xlsx";
	public final static String FORMAT_TXT = "txt";

	private final static byte[] JPGID = {
			(byte)0xFF, (byte)0xD8};
	private final static byte[] PNGID = {
			(byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47};
	private final static byte[] PDFID = {
			(byte)0x25, (byte)0x50, (byte)0x44, (byte)0x46, (byte)0x2D,
			(byte)0x31, (byte)0x2E};
	private final static byte[] XLS_DOCID = {
			(byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0};
	private final static byte[] XLSX_DOCXID = {
			(byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04};
	/*private final static byte[] XLSX_DOCXID = {
			(byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04, (byte)0x14,
			(byte)0x00, (byte)0x06, (byte)0x00, (byte)0x08, (byte)0x00,
			(byte)0x00, (byte)0x00, (byte)0x21, (byte)0x00};*/
	private final static byte[] DOCID512 = {
			(byte)0xEC, (byte)0xA5, (byte)0xC1, (byte)0x00};
	private final static byte[] RTFID = {
			(byte)0x7B, (byte)0x5C, (byte)0x72, (byte)0x74, (byte)0x66};
	
	/* 根据文件内容检测图片格式，支持gif、jpg、png，不认识时返回null　*/
	public static String getPicFormat(byte[] data) {
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
        	//GIF图片
        	return FileUtils.FORMAT_GIF;
        }
        else if (check(data, JPGID)) {
        	return FileUtils.FORMAT_JPG;
        }
        else if (data[1] == PNGID[1] && data[2] == PNGID[2] &&
          data[3] == PNGID[3]) {
        	return FileUtils.FORMAT_PNG;
        }
		return null;
	}
	
	private static boolean check(byte[] data, byte[] id) {
		int len = id.length;
		if (data.length < len) return false;
		for (int i=0; i<len; i++) {
			if (data[i] != id[i]) return false;
		}
		return true;
	}
	
	private static String getDocxType(byte[] data) {
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
		try {
			while(true){
				ZipEntry ze = zis.getNextEntry();
				if(ze==null) break;
				if(ze.getName().startsWith("word")){
					return FileUtils.FORMAT_DOCX;
				}
				else if (ze.getName().contains("xl")){
					return FileUtils.FORMAT_XLSX;
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				zis.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

    private static String getDocType(byte[] fdata)
    {
        int bigBlockSize = 512;
        if(fdata[30] == 12)
            bigBlockSize = 4096;
        int propertyStart = getInt(fdata, 48);
        
        int propertyCount = bigBlockSize / 128;
        int offset = bigBlockSize * (propertyStart+1);
        for(int i = 0; i < propertyCount; i++){
            if(fdata[offset + 66] == 2){
                short nameSize = getShort(fdata, offset + 64);
                int nameLen = nameSize / 2 - 1;
                char buf[] = new char[nameLen];
                for(int k = 0; k < nameLen; k++)
                    buf[k] = (char)getShort(fdata, offset + k * 2);

                String name = new String(buf);
                if(name.equalsIgnoreCase("Workbook"))
                    return FileUtils.FORMAT_XLS;
                if(name.equalsIgnoreCase("WordDocument"))
                    return FileUtils.FORMAT_DOC;;
                if(name.equalsIgnoreCase("PowerPoint Document"))
                    return "ppt";
            }
            offset += 128;
        }
        return null;
    }
	
	/* 根据文件内容检测文件格式，支持gif、jpg、png、pdf、xls、xlsx、doc、docx、txt，不认识时返回null */
	public static String getFileFormat(byte[] data) {
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
        	//GIF图片
        	return FileUtils.FORMAT_GIF;
        }
        else if (check(data, JPGID)) {
        	return FileUtils.FORMAT_JPG;
        }
        else if (data[1] == PNGID[1] && data[2] == PNGID[2] &&
          data[3] == PNGID[3]) {
        	return FileUtils.FORMAT_PNG;
        }
        else if (check(data, RTFID)) {
        	return FileUtils.FORMAT_DOC;
        }
        else if (check(data, PDFID)) {
        	return FileUtils.FORMAT_PDF;
        }
        else if (check(data, XLS_DOCID)) {
        	return getDocType(data);
        }
        else if (check(data, XLSX_DOCXID)) {
        	return getDocxType(data);
        }
		return FORMAT_TXT;
	}

    private static long getNumber(byte data[], int offset, int size)
    {
        long result = 0L;
        for(int j = (offset + size) - 1; j >= offset; j--)
        {
            result <<= 8;
            result |= 0xff & data[j];
        }

        return result;
    }

    private static int getInt(byte data[], int offset)
    {
        return (int)getNumber(data, offset, 4);
    }

    private static short getShort(byte data[], int offset)
    {
        return (short)getNumber(data, offset, 2);
    }
	
	public static void main(String[] arg) {
		//String filepath = "D:/files/DummyData.xlsx";
		String filepath = "D:/files/test.docx";
		//String filepath = "D:/files/case(B).doc";
		//String filepath = "D:/files/3.xls";
		try {
			com.scudata.dm.FileObject fo = new com.scudata.dm.FileObject(filepath);
			byte[] bytes = (byte[]) fo.read(0, -1, "b");
			String type = FileUtils.getFileFormat(bytes);
			System.out.println(type);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
