package com.raqsoft.util;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.raqsoft.dm.Sequence;

/**
 * HTML工具类，用于读取HTML数据
 * @author RunQian
 *
 */
public class HTMLUtil {
	
	/**
	 * 取出HTML串中指定tag下的指定序号的文本，返回成序列
	 * @param html HTML内容
	 * @param tags 标签名数组
	 * @param seqs 标签下的文本序号
	 * @param subSeqs 子序号
	 * @return 序列
	 */
	public static Sequence htmlparse(String html, String []tags, int []seqs, int []subSeqs) {
		int len = tags.length;
		Sequence result = new Sequence(len);
		
		Document doc = Jsoup.parse(html);
		for (int i = 0; i < len; ++i) {
			// 根据tag名称取出相应的项
			Elements elements = doc.getElementsByTag(tags[i]);
			if (elements != null && elements.size() > seqs[i]) {
				// 根据序号取出文本
				Element element = elements.get(seqs[i]);
				if (tags[i].equals("table")) {
					// 表格生成序列的序列
					Elements rows = element.select("tr");
					int rowCount = rows == null ? 0 : rows.size();
					Sequence rowValues = new Sequence(rowCount);
					result.add(rowValues);
					
					for (int r = 0; r < rowCount; ++r) {
						Element row = rows.get(r);
						Elements cols = row.select("td");
						int colCount = cols == null ? 0 : cols.size();
						if (colCount == 0 && r == 0) {
							cols = row.select("th");
							colCount = cols == null ? 0 : cols.size();
						}
						
						// 每行读成一个序列
						Sequence colValues = new Sequence(colCount);
						rowValues.add(colValues);
						
						for (int c = 0; c < colCount; ++c) {
							String text = cols.get(c).text();
							if (text != null) {
								colValues.add(text.trim());
							} else {
								colValues.add(null);
							}
						}
					}
				} else {
					String text = null;
					//if (subSeqs[i] > 0) {
						if (subSeqs[i] < element.childNodeSize()) {
							Node node = element.childNode(subSeqs[i]);
							text = node.toString();
						}
					//} else {
					//	text = element.text();
					//}
					
					if (text != null) {
						result.add(text.trim());
					} else {
						result.add(null);
					}
				}
			} else {
				result.add(null);
			}
		}
		
		return result;
	}

	/**
	 * 取所有text节点的数据
	 * @param node 节点
	 * @param out 输出序列
	 */
	private static void getAllNodeText(Node node, Sequence out) {
		if (node instanceof TextNode) {
			String text = ((TextNode)node).text();
			if (text != null) {
				text = text.trim();
				if (text.length() > 0) {
					out.add(text.trim());
				}
			}
		} else {
			// 继续遍历子节点
			List<Node> childs = node.childNodes();
			if (childs != null) {
				for (Node child : childs) {
					getAllNodeText(child, out);
				}
			}
		}
	}
	
	/**
	 * 取所有text节点的数据，返回成序列
	 * @param html HTML内容
	 * @return 序列
	 */
	public static Sequence htmlparse(String html) {
		Sequence result = new Sequence();
		Document doc = Jsoup.parse(html);
		List<Node> childs = doc.childNodes();
		
		if (childs != null) {
			for (Node child : childs) {
				getAllNodeText(child, result);
			}
		}
				
		return result;
	}
}
