package com.web;

import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.pipeline.Pipeline;

public interface StandPipeline extends Pipeline {
	public void setArgv(String argv);
	public void process(ResultItems paramResultItems, Task paramTask);
}
