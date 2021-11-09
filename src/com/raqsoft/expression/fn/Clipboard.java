package com.raqsoft.expression.fn;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.Reader;

import com.raqsoft.common.RQException;
import com.raqsoft.dm.Context;
import com.raqsoft.expression.Function;
import com.raqsoft.expression.IParam;
import com.raqsoft.expression.Node;

/**
 * 设置系统剪贴板内容，s省略则以串形式返回剪贴板内容
 * clipboard() clipboard(s)
 * 选项@e 如果发现当前系统剪贴板内容来自Excel，则取过来用并记录下来
 * @author RunQian
 *
 */
public class Clipboard extends Function {
	private static String m_lastBufExcelString = null;
	
	public Node optimize(Context ctx) {
		if (param != null) param.optimize(ctx);
		return this;
	}

	public Object calculate(Context ctx) {
		IParam param = this.param;
		String text = null;

		//优先于非Excel数据源
		if (this.option!=null && this.option.contains("e")){
			text = getClipboardExcelString();
			if (text!=null && !text.isEmpty()){
				return text;
			}
		}

		if (param == null) { //paster
			text = getClipboardString();
			return text;
		}
		
		boolean bLeft = param.isLeaf();
		if (bLeft){ //copy to clipboard
			Object o = param.getLeafExpression().calculate(ctx);
			if(o!=null) {
				setClipboardString(o.toString());
			}
			return true;
		}
		return null;
	}

    /**
     * 把文本设置到剪贴板（复制）
     */
    public void setClipboardString(String text) {
    	java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trans = new StringSelection(text);
        clipboard.setContents(trans, null);
    }
    
    /**
     * 从剪贴板中获取文本（粘贴）
     */
    public String getClipboardString() {
    	java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable trans = clipboard.getContents(null);
        if (trans != null) {
            if (trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String text = (String) trans.getTransferData(DataFlavor.stringFlavor);
                    if (text.endsWith("\n")){
						text = text.substring(0,text.length() - 1);
					}
                    return text;
                } catch (Exception e) {
                	throw new RQException(e.getMessage(), e);
                }
            }
        }

        return null;
    }

    /**
     * 从系统剪贴板中判断是否来自Excel的数据（粘贴）
     */
    
    private String getClipboardExcelString() {
    	java.awt.datatransfer.Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable tf = clip.getContents(null);
		DataFlavor dfs[] = tf.getTransferDataFlavors();
		if (dfs == null) {
			return null;
		}

		try {
			String text = null;
			for (int i = 0; i < dfs.length; i++) {
				if (dfs[i].getMimeType().indexOf("text/html") > -1) {
					Reader r = dfs[i].getReaderForText(tf);					
					String txt = getReaderContent(r).toString();
					String[] ss = txt.toLowerCase().split("\n");
					int nIdx = 0;
					for(String s:ss){
						if (s.contains("office:office") ||
							s.contains("office:excel") ||
							(s.contains("name=progid") && s.contains("excel")) ||
							(s.contains("name=generator") && s.contains("excel")) ){
							nIdx++;
						}
					}
					
					if (nIdx>=3){
						text = (String) tf.getTransferData(DataFlavor.stringFlavor);
						m_lastBufExcelString = null;
						m_lastBufExcelString = new String(text);		
						//System.out.println(txt);
						break;
					}else if(i>9){ //找不到用上次的.
						break;
					}					
				}
			}
			if (text == null){
				text = m_lastBufExcelString;
			}
			
			return text;
		} catch (Exception e) {
			throw new RQException(e.getMessage(), e);
		}
    }
    
    private StringBuilder getReaderContent(java.io.Reader r) throws Exception {
        char[] arr = new char[512];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = r.read(arr, 0, arr.length)) != -1) {
            buffer.append(arr, 0, numCharsRead);
            break;
        }
        r.close();
        return buffer;
    }
}