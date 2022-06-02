package com.scudata.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ImportSpringBeanFactory implements ApplicationContextAware{
	
	public static BeanFactory beanFactory;

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ImportSpringBeanFactory.beanFactory = applicationContext.getAutowireCapableBeanFactory();
	}
	
}
