//package com.scudata.ide.custom;
//
//import java.awt.Component;
//import java.util.List;
//
///**
// * 
// * EsProc Resource Tree Interface
// * 
// * This control will be placed on the left side of esProc.
// * 
// * The use of a custom resource tree in the IDE needs to implement this
// * interface. For example: public class MyResourceTree implements IResourceTree.
// * Configure the implementation class of IResourceTree in the CONFIG node in
// * systemconfig_zh.xml. Example: <CONFIG
// * IResourceTree="com.custom.MyResourceTree"/>
// */
//public interface IResourceTreeBase {
//	/**
//	 * Get esProc resource tree component
//	 * 
//	 * @return
//	 */
//	public Component getComponent();
//
//	/**
//	 * Refresh local resources based on esProc main path.
//	 * 
//	 * @param mainPath
//	 * 
//	 */
//	public void changeMainPath(String mainPath);
//
//}
