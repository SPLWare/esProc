package com.scudata.util;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import com.scudata.dm.Sequence;

/**
 * HTML工具类，用于读取HTML数据
 * @author RunQian
 *
 */
public class HTMLUtil {
	private static void getTable(Element element, boolean doParse, Sequence result) {
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
					text = text.trim();
					if (text.length() > 0) {
						if (doParse) {
							Object val = XMLUtil.parseText(text);
							colValues.add(val);
						} else {
							colValues.add(text);
						}
					} else {
						colValues.add(null);
					}
				} else {
					colValues.add(null);
				}
			}
		}
	}
	
	/**
	 * 取出HTML串中指定tag下的指定序号的文本，返回成序列
	 * @param html HTML内容
	 * @param tags 标签名数组
	 * @param seqs 标签下的文本序号
	 * @param subSeqs 子序号
	 * @param opt 0：保留null值（缺省会去掉），p：解析文本为相应类型的数值
	 * @return 序列
	 */
	public static Sequence htmlparse(String html, String []tags, int []seqs, int []subSeqs, String opt) {
		int len = tags.length;
		Sequence result = new Sequence(len);
		boolean containNull = false, doParse = false;
		if (opt != null) {
			if (opt.indexOf('0') != -1) containNull = true;
			if (opt.indexOf('p') != -1) doParse = true;
		}
		
		Document doc = Jsoup.parse(html);
		for (int i = 0; i < len; ++i) {
			// 根据tag名称取出相应的项
			Elements elements = doc.getElementsByTag(tags[i]);
			if (elements == null || elements.size() == 0) {
				if (containNull) {
					result.add(null);
				}
			} else if (seqs[i] == -1) {
				for (int j = 0, size = elements.size(); j < size; ++j) {
					Element element = elements.get(j);
					if (tags[i].equals("table")) {
						getTable(element, doParse, result);
					} else {
						String text = null;
						if (subSeqs[i] < element.childNodeSize()) {
							Node node = element.childNode(subSeqs[i]);
							text = node.toString();
						}
						
						if (text != null) {
							text = text.trim();
							if (text.length() > 0) {
								if (doParse) {
									Object val = XMLUtil.parseText(text);
									result.add(val);
								} else {
									result.add(text);
								}
							} else if (containNull) {
								result.add(null);
							}
						} else if (containNull) {
							result.add(null);
						}
					}
				}
			} else if (elements.size() > seqs[i]) {
				// 根据序号取出文本
				Element element = elements.get(seqs[i]);
				if (tags[i].equals("table")) {
					getTable(element, doParse, result);
				} else {
					String text = null;
					if (subSeqs[i] < element.childNodeSize()) {
						Node node = element.childNode(subSeqs[i]);
						text = node.toString();
					}
					
					if (text != null) {
						text = text.trim();
						if (text.length() > 0) {
							if (doParse) {
								Object val = XMLUtil.parseText(text);
								result.add(val);
							} else {
								result.add(text);
							}
						} else if (containNull) {
							result.add(null);
						}
					} else if (containNull) {
						result.add(null);
					}
				}
			} else {
				if (containNull) {
					result.add(null);
				}
			}
		}
		
		return result;
	}

	/**
	 * 取所有text节点的数据
	 * @param node 节点
	 * @param out 输出序列
	 */
	private static void getAllNodeText(Node node, Sequence out, boolean containNull, boolean doParse) {
		if (node instanceof TextNode) {
			String text = ((TextNode)node).text();
			if (text != null) {
				text = text.trim();
				if (text.length() > 0) {
					if (doParse) {
						Object val = XMLUtil.parseText(text);
						out.add(val);
					} else {
						out.add(text);
					}
				} else if (containNull) {
					out.add(null);
				}
			} else if (containNull) {
				out.add(null);
			}
		} else {
			// 继续遍历子节点
			List<Node> childs = node.childNodes();
			if (childs != null && childs.size() > 0) {
				for (Node child : childs) {
					getAllNodeText(child, out, containNull, doParse);
				}
			} else if (containNull && !node.nodeName().equals("head") && !node.nodeName().equals("body")) {
				out.add(null);
			}
		}
	}
	
	/**
	 * 取所有text节点的数据，返回成序列
	 * @param html HTML内容
	 * @param opt 0：保留null值（缺省会去掉），p：解析文本为相应类型的数值
	 * @return 序列
	 */
	public static Sequence htmlparse(String html, String opt) {
		Sequence result = new Sequence();
		Document doc = Jsoup.parse(html);
		List<Node> childs = doc.childNodes();
		boolean containNull = false, doParse = false;
		if (opt != null) {
			if (opt.indexOf('0') != -1) containNull = true;
			if (opt.indexOf('p') != -1) doParse = true;
		}
		
		if (childs != null) {
			for (Node child : childs) {
				getAllNodeText(child, result, containNull, doParse);
			}
		}
				
		return result;
	}
}
