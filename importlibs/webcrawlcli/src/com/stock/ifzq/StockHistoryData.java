package com.stock.ifzq;

import java.util.List;

import com.web.StandPageItem;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.selector.Selectable;

public class StockHistoryData implements StandPageItem{

	@Override
	public void parse(Page page) {
		StringBuilder buf = new StringBuilder();
		 List<Selectable> nodes = page.getHtml().xpath("table/tbody/").nodes();
		 for(Selectable node:nodes){
			 //String day = node.xpath("//a/text()").get();
			 List<String> title = node.xpath("//a/text() | tr/td/text() ").all();
			 if (title.size()<5) continue;
			 String line = title.toString().replaceFirst(", , ", ", ");

			 //System.out.println("title: "+title);
			 buf.append(line+"\n");
		 }
		 page.putField("content", buf.toString());
	}

}
