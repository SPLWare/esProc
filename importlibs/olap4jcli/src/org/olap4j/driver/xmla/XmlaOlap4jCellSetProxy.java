package org.olap4j.driver.xmla;

import org.olap4j.*;
import org.olap4j.impl.Olap4jUtil;
import org.olap4j.metadata.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import com.scudata.common.Logger;

import static org.olap4j.driver.xmla.XmlaOlap4jUtil.*;

abstract class XmlaOlap4jCellSetProxy extends FactoryJdbc4Plus.AbstractCellSet {
    private static final boolean DEBUG = false;

    /**
     * Creates an XmlaOlap4jCellSet.
     *
     * @param olap4jStatement Statement
     */
    XmlaOlap4jCellSetProxy(XmlaOlap4jStatement olap4jStatement) throws OlapException
    {
    	super(olap4jStatement);
    }

    /**
     * Returns the error-handler.
     *
     * @return Error handler
     */
    private XmlaHelper getHelper() {
        return olap4jStatement.olap4jConnection.helper;
    }

    /**
     * Gets response from the XMLA request and populates cell set axes and cells
     * with it.
     *
     * @throws OlapException on error
     */
    void populate() throws OlapException {
        byte[] bytes = olap4jStatement.getBytes();

        Document doc;
        try {
            doc = parse(bytes);
        } catch (IOException e) {
            throw getHelper().createException("error creating CellSet", e);
        } catch (SAXException e) {
            throw getHelper().createException("error creating CellSet", e);
        }

        final Element envelope = doc.getDocumentElement();
        if (DEBUG) {
            System.out.println(XmlaOlap4jUtil.toString(doc, true));
        }
        assert envelope.getLocalName().equals("Envelope");
        assert envelope.getNamespaceURI().equals(SOAP_NS);
        Element body = findChild(envelope, SOAP_NS, "Body");
        Element fault = findChild(body, SOAP_NS, "Fault");
        if (fault != null) {
            throw getHelper().createException(
                "XMLA provider gave exception: "
                + XmlaOlap4jUtil.prettyPrint(fault));
        }
        Element executeResponse = findChild(body, XMLA_NS, "ExecuteResponse");
        Element returnElement = findChild(executeResponse, XMLA_NS, "return");       
        final Element root = findChild(returnElement, MDDATASET_NS, "root");
        XmlaOlap4jCellSetMetaData metaData = (XmlaOlap4jCellSetMetaData)this.getMetaData();

        if (olap4jStatement instanceof XmlaOlap4jPreparedStatement) {
            metaData = ((XmlaOlap4jPreparedStatement) olap4jStatement).cellSetMetaData;
        } else {
            //metaData = createMetaData(root);
            metaData = (XmlaOlap4jCellSetMetaData)loadClassPrivateMethod(this, XmlaOlap4jCellSet.class, "createMetaData",
            		new Class<?>[]{Element.class}, root);
        }

        final Element axesNode = findChild(root, MDDATASET_NS, "Axes");
        XmlaOlap4jCubeProxy cube = new XmlaOlap4jCubeProxy(metaData.cube.olap4jSchema,
        		metaData.cube.getName(),metaData.cube.getCaption(), metaData.cube.getDescription());
        //final MetadataReader metadataReader = metaData.cube.getMetadataReader();
        final MetadataReader metadataReader = cube.getMetadataReader();        

        final Map<String, XmlaOlap4jMember> memberMap = new HashMap<String, XmlaOlap4jMember>();
        List<String> uniqueNames = new ArrayList<String>();
        for (Element axisNode : findChildren(axesNode, MDDATASET_NS, "Axis")) {
            final Element tuplesNode = findChild(axisNode, MDDATASET_NS, "Tuples");
            for (Element tupleNode : findChildren(tuplesNode, MDDATASET_NS, "Tuple"))
            {
                for (Element memberNode : findChildren(tupleNode, MDDATASET_NS, "Member"))
                {
                    final String uname = stringElement(memberNode, "UName");
                    uniqueNames.add(uname);
                }
            }
        }

        // Fetch all members on all axes. Hopefully it can all be done in one
        // round trip, or they are in cache already.
        Database db = this.olap4jStatement.olap4jConnection.getOlapDatabase();
        String desc = db.getDescription();
        if (desc.contains("SAP")){
        	XmlaOlap4jConnectionProxy.dbType = XmlaOlap4jConnectionProxy.DatabaseType.SAP;
        }
        metadataReader.lookupMembersByUniqueName(uniqueNames, memberMap);
        XmlaOlap4jCellSetAxis filterAxis = (XmlaOlap4jCellSetAxis)this.getFilterAxis();
        // Second pass, populate the axis.
        final Map<Property, Object> propertyValues = new HashMap<Property, Object>();
        List<XmlaOlap4jCellSetAxis> axisListEx = new ArrayList<XmlaOlap4jCellSetAxis>();
        
        for (Element axisNode : findChildren(axesNode, MDDATASET_NS, "Axis")) {
            final String axisName = axisNode.getAttribute("name");
            final Axis axis = (Axis)loadClassPrivateMethod(this, XmlaOlap4jCellSet.class, "lookupAxis",
            		new Class<?>[]{String.class}, axisName);
            final ArrayList<Position> positions = new ArrayList<Position>();
            final XmlaOlap4jCellSetAxis cellSetAxis =
                new XmlaOlap4jCellSetAxis(
                    this, axis, Collections.unmodifiableList(positions));
            if (axis.isFilter()) {
                filterAxis = cellSetAxis;
            } else {
                axisListEx.add(cellSetAxis);
            }
            final Element tuplesNode = findChild(axisNode, MDDATASET_NS, "Tuples");
            for (Element tupleNode : findChildren(tuplesNode, MDDATASET_NS, "Tuple"))
            {
                final List<Member> members = new ArrayList<Member>();
                for (Element memberNode : findChildren(tupleNode, MDDATASET_NS, "Member"))
                {
                    //String hierarchyName = memberNode.getAttribute("Hierarchy");
                    final String uname = stringElement(memberNode, "UName");
                    XmlaOlap4jMemberBase member = memberMap.get(uname);
                    members.add(member);
                }
                positions.add(new XmlaOlap4jPosition(members, positions.size()));
            }
        }
        
        // If XMLA did not return a filter axis, it means that there was no
        // WHERE. This is equivalent to a slicer axis with one tuple that has
        // zero positions. (Versions of Mondrian before 3.4 do, in fact, return
        // a slicer axis with one empty position. This CellSet should behave the
        // same.)
        if (filterAxis == null) {
            filterAxis =
                new XmlaOlap4jCellSetAxis(
                    this,
                    Axis.FILTER,
                    Collections.<Position>singletonList(
                        new XmlaOlap4jPosition(
                            Collections.<Member>emptyList(), 0)));
        }

        Map<Integer, Cell> cellMapEx = new HashMap<Integer, Cell>();
        final Element cellDataNode = findChild(root, MDDATASET_NS, "CellData");
        for (Element cell : findChildren(cellDataNode, MDDATASET_NS, "Cell")) {
            propertyValues.clear();
            final int cellOrdinal = Integer.valueOf(cell.getAttribute("CellOrdinal"));
            final Object value= loadClassPrivateMethod(this, XmlaOlap4jCellSet.class, "getTypedValue",
            		new Class<?>[]{Element.class}, cell);
            final String formattedValue = stringElement(cell, "FmtValue");
            final String formatString = stringElement(cell, "FormatString");
            Olap4jUtil.discard(formatString);
            for (Element element : childElements(cell)) {
                String tag = element.getLocalName();
                final Property property =
                    metaData.propertiesByTag.get(tag);
                if (property != null) {
                    propertyValues.put(property, element.getTextContent());
                }
            }
            cellMapEx.put(
                cellOrdinal,
                new XmlaOlap4jCell(
                    this,
                    cellOrdinal,
                    value,
                    formattedValue,
                    propertyValues));            
        }
        try {
        	Class<?> o = this.getClass().getSuperclass().getSuperclass().getSuperclass();        	
        	setClassPrivateValue(this, o.getSuperclass(), "metaData", metaData);
        	setClassPrivateValue(this, o.getSuperclass(), "axisList", axisListEx);
        	setClassPrivateValue(this, o.getSuperclass(), "cellMap", cellMapEx);
        	setClassPrivateValue(this, o.getSuperclass(), "immutableAxisList", axisListEx);
        	metaData = null;
        	cellMapEx = null;
        	axisListEx = null;
	        
        }catch(Exception e){
        	Logger.error(e.getMessage());
        }
    }
    
    public static void setClassPrivateValue(Object clsObj, Class<?> cls, String privateVal, Object newValue) {
		try {
			Field listField = cls.getDeclaredField(privateVal);
			listField.setAccessible(true); // 绕过权限检测！
			listField.set(clsObj, newValue);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}
    
	public static Object loadClassPrivateMethod(Object objCls,Class<?> cls, String methodName, 
			Class<?>[] clazz, Object...arg) {
		Object ret = null;
		try {
			Method method = null;
			if (clazz.length==0){
				method = cls.getDeclaredMethod(methodName);
			}else if(clazz.length==1){
				method = cls.getDeclaredMethod(methodName, clazz[0]);
			}else if(clazz.length==2){
				method = cls.getDeclaredMethod(methodName, clazz[0], clazz[1]);
			}else if(clazz.length==3){
				method = cls.getDeclaredMethod(methodName, clazz[0], clazz[1], clazz[2]);
			}else if(clazz.length==4){
				method = cls.getDeclaredMethod(methodName, clazz[0], clazz[1], clazz[2], clazz[3]);
			}
			// 开启私有访问权限
			method.setAccessible(true);
			ret = method.invoke(objCls, arg);
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
		
		return ret;
	}
	
	// className: "com.my.classloader.OuterTest$staticInner"
	// Class<?>[]: Classes
	// arg: param...
	public static Object[] getStaticPrivateNestingClass(String className, Class<?>[] clss, Object... arg) {
		Object[] ret = new Object[2];
		try {
			Constructor<?> con = null;
			Class<?> staticClazz = Class.forName(className);
			if (clss.length == 0) {
				con = staticClazz.getDeclaredConstructor();
			} else if (clss.length == 1) {
				con = staticClazz.getDeclaredConstructor(clss[0]);
			} else if (clss.length == 2) {
				con = staticClazz.getDeclaredConstructor(clss[0], clss[1]);
			} else if (clss.length == 3) {
				con = staticClazz.getDeclaredConstructor(clss[0], clss[1], clss[3]);
			}

			con.setAccessible(true);
			ret[0] = staticClazz;
			if (arg == null) {
				ret[1] = con.newInstance();
			} else {
				ret[1] = con.newInstance(arg);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return ret;
	}

	public static Object[] getInnerPrivateNestingClass(Object objCls, Class<?> outerClaz, String className,
			Class<?>[] clss, Object... arg) {
		Object[] ret = new Object[2];
		try {
			Class<?> inClazz = Class.forName(className);
			Constructor<?> con = inClazz.getDeclaredConstructor(outerClaz);
			con.setAccessible(true);
			ret[0] = inClazz;
			if (arg == null || arg.length == 0) {
				ret[1] = con.newInstance(objCls);
			} else {
				ret[1] = con.newInstance(objCls, arg);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}

		return ret;
	}
}

// End XmlaOlap4jCellSetProxy.java

