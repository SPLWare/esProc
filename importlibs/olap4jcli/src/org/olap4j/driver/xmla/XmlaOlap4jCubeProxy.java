package org.olap4j.driver.xmla;

import org.olap4j.OlapException;
import org.olap4j.metadata.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link Cube}
 * for XML/A providers.
 *
 * @author liaoxc
 * @since 2021.11.20
 */
class XmlaOlap4jCubeProxy extends XmlaOlap4jCube
{
    /**
     * Creates an XmlaOlap4jCube.
     *
     * @param olap4jSchema Schema
     * @param name Name
     * @param caption Caption
     * @param description Description
     * @throws org.olap4j.OlapException on error
     * @throws ClassNotFoundException 
     */
    XmlaOlap4jCubeProxy(
        XmlaOlap4jSchema olap4jSchema,
        String name,
        String caption,
        String description) throws OlapException
    {
    	super(olap4jSchema, name, caption, description);
        final Map<String, XmlaOlap4jMeasure> measuresMap =
            new HashMap<String, XmlaOlap4jMeasure>();
       
        // In case this is the dummy cube for shared dimensions stop here
        // to avoid unnecessary calls and errors with unique members
        if ("".equals(name)) {
            return;
        }
        MetadataReader metadataReaderEx = null;
        try{
        	
             Object[] cacheReader=XmlaOlap4jCellSetProxy.getStaticPrivateNestingClass(
             		"org.olap4j.driver.xmla.XmlaOlap4jCube$CachingMetadataReader", 
             		new Class<?>[]{MetadataReader.class, Map.class},
             		new RawMetadataReader(), measuresMap);
             metadataReaderEx = (MetadataReader)cacheReader[1];
        }catch(Exception e){
        	
        }
       
        final XmlaOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;

        final XmlaOlap4jConnection.Context context =
            new XmlaOlap4jConnection.Context(this, null, null, null);

        String[] restrictions = {
            "CATALOG_NAME", olap4jSchema.olap4jCatalog.getName(),
            "SCHEMA_NAME", olap4jSchema.getName(),
            "CUBE_NAME", getName()
        };

        // populate measures up front; a measure is needed in every query
        olap4jConnection.populateList(
            measures,
            context,
            XmlaOlap4jConnection.MetadataRequest.MDSCHEMA_MEASURES,
            new XmlaOlap4jConnection.MeasureHandler(),
            restrictions);
        for (XmlaOlap4jMeasure measure : measures) {
            measuresMap.put(measure.getUniqueName(), measure);
        }

        // populate named sets
        NamedList<XmlaOlap4jNamedSet> namedSetsEx = new DeferredNamedListImpl<XmlaOlap4jNamedSet>(
            XmlaOlap4jConnection.MetadataRequest.MDSCHEMA_SETS,
            context,
            new XmlaOlap4jConnection.NamedSetHandler(),
            restrictions);

        Class<?> o = this.getClass().getSuperclass();
        XmlaOlap4jCellSetProxy.setClassPrivateValue(this, o, "namedSets", namedSetsEx);
        XmlaOlap4jCellSetProxy.setClassPrivateValue(this, o, "metadataReader", metadataReaderEx);
    }
    
    /**
     * Implementation of MetadataReader that reads from the XMLA provider,
     * without caching.
     */
    private class RawMetadataReader implements MetadataReader {
    	private Object[] rawReader = null;
    	RawMetadataReader(){    		
            rawReader=XmlaOlap4jCellSetProxy.getInnerPrivateNestingClass(XmlaOlap4jCubeProxy.this, XmlaOlap4jCube.class,
            		"org.olap4j.driver.xmla.XmlaOlap4jCube$RawMetadataReader", new Class<?>[]{});
    	}
    	
        public XmlaOlap4jMember lookupMemberByUniqueName(
            String memberUniqueName)
            throws OlapException
        {
        	try{
	        	Method md = ((Class<?>)rawReader[0]).getDeclaredMethod("lookupMemberByUniqueName", String.class);
	        	return (XmlaOlap4jMember)md.invoke(rawReader[1], memberUniqueName);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
	    	return null;
        }

        public void lookupMembersByUniqueName(
            List<String> memberUniqueNames,
            Map<String, XmlaOlap4jMember> memberMap) throws OlapException
        {
            if (olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData
                    .olap4jConnection.getDatabase()
                    .indexOf("Provider=Mondrian") != -1)
            {
                mondrianMembersLookup(memberUniqueNames, memberMap);
            } else {
                genericMembersLookup(memberUniqueNames, memberMap);
            }
        }

        /**
         * Looks up members; optimized for Mondrian servers.
         *
         * @param memberUniqueNames A list of the members to lookup
         * @param memberMap Output map of members keyed by unique name
         * @throws OlapException Gets thrown for communication errors
         */
        private void mondrianMembersLookup(
            List<String> memberUniqueNames,
            Map<String, XmlaOlap4jMember> memberMap) throws OlapException
        {
        	try{
	        	Method md = ((Class<?>)rawReader[0]).getDeclaredMethod("mondrianMembersLookup", List.class, Map.class);
	        	md.invoke(rawReader[1], memberUniqueNames, memberMap);
	    	}catch(Exception e){
	    		e.printStackTrace();
	    	}
        }

        /**
         * Looks up members.
         *
         * @param memberUniqueNames A list of the members to lookup
         * @param memberMap Output map of members keyed by unique name
         * @throws OlapException Gets thrown for communication errors
         */
        private void genericMembersLookup(
                List<String> memberUniqueNames,
                Map<String, XmlaOlap4jMember> memberMap) throws OlapException
            {
                // Iterates through member names
            	final XmlaOlap4jConnection.Context context =
                        new XmlaOlap4jConnection.Context(
                            XmlaOlap4jCubeProxy.this, null, null, null);
            	
            	String pattern = "\\[(.*)\\]\\.\\[(.*)\\]\\.[&]?\\[(\\d+|All|\\(All\\))\\]"; //SSAS
            	if (XmlaOlap4jConnectionProxy.dbType == XmlaOlap4jConnectionProxy.DatabaseType.SAP){
            		pattern = "\\[(.*)]\\.\\[([\\S ]+)\\]";
            	}
            	String sKey = null;
            	Matcher m = null;
            	String val = null;
            	XmlaOlap4jLevel level = null;
            	Pattern r = Pattern.compile(pattern);
                for (String currentMemberName : memberUniqueNames) {
                    // Only lookup if it is not in the map yet                	
                    if (!memberMap.containsKey(currentMemberName)) {
                    	//System.out.println(currentMemberName);
                    	if (context.getCube(null).levelsByUname.size()>0){
	                    	m = r.matcher(currentMemberName);	                    	
	                    	if (m.find()){	                    		
	                    		if (XmlaOlap4jConnectionProxy.dbType == XmlaOlap4jConnectionProxy.DatabaseType.SAP){
//	                    			System.out.println(m.group(2));
//	                        		System.out.println(m.group(1));
	                        		
	                        		if (m.group(1).equalsIgnoreCase("Measures")){
	                        			val = m.group(2).toString();
		                    			level = context.getCube(null).levelsByUname.get("["+m.group(1)+"]");   
		                    		}else if (m.group(2).equalsIgnoreCase("All") || m.group(1).equalsIgnoreCase("All")){
		                    			;//skip 
		                    		}else{
		                    			val = m.group(2).toString();
		                    			level = context.getCube(null).levelsByUname.get("["+m.group(1)+"].[LEVEL01]");  
		                    		}
	                    		}else if (XmlaOlap4jConnectionProxy.dbType == XmlaOlap4jConnectionProxy.DatabaseType.SSAS){
//	                    			System.out.println(m.group(1));
//	                        		System.out.println(m.group(2));
//	                        		System.out.println(m.group(3));
	                        		
	                        		if (m.group(1).equalsIgnoreCase("Measures")){
		                    			level = context.getCube(null).levelsByUname.get("["+m.group(1)+"]");   
		                    		}else if (m.group(3).equalsIgnoreCase("All") || m.group(1).equalsIgnoreCase("All")){
		                    			sKey = "["+m.group(1)+"]."+"["+m.group(2)+"].[(All)]";
		                    			level = context.getCube(null).levelsByUname.get(sKey); 
		                    		}else{
		                    			sKey = "["+m.group(1)+"]."+"["+m.group(2)+"].["+m.group(2)+"]";
		                    			level = context.getCube(null).levelsByUname.get(sKey);  
		                    		}
	                        		val = m.group(3).toString();
	                    		}
	                    	}
                    	}
                    	
                    	if ( level!=null){                		
                    		XmlaOlap4jMember member = new XmlaOlap4jMember(
                    				level, currentMemberName, val,
                    				val, "", val, Member.Type.values()[0],
                    				0, 1, new HashMap<Property, Object>());
                    		memberMap.put(member.getUniqueName(), member);
                    	}else{
    	                    XmlaOlap4jMember member = this.lookupMemberByUniqueName(currentMemberName);
    	                    // Null members might mean calculated members
    	                    if (member != null) {
    	                        memberMap.put(member.getUniqueName(), member);	                        
    	                    }
                    	}
                    }
                }
            }

		public void lookupMemberRelatives(Set<Member.TreeOp> treeOps, String memberUniqueName,
				List<XmlaOlap4jMember> list) throws OlapException {
			try {
				Method md = ((Class<?>) rawReader[0]).getDeclaredMethod("lookupMemberRelatives", Set.class,
						String.class, List.class);
				md.invoke(rawReader[1], treeOps, memberUniqueName, list);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public List<XmlaOlap4jMember> getLevelMembers(XmlaOlap4jLevel level) throws OlapException {
			try {
				Method md = ((Class<?>) rawReader[0]).getDeclaredMethod("getLevelMembers", XmlaOlap4jLevel.class);
				return (List<XmlaOlap4jMember>) md.invoke(rawReader[1], level);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
 
}

// End XmlaOlap4jCube.java
