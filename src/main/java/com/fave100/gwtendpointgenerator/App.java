package com.fave100.gwtendpointgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class App 
{	
	private static String ENTITY_PACKAGE = "com.fave100.client.generated.entities";
	private static String SERVICE_PACKAGE = "com.fave100.client.generated.services";
	
	private static int indentCount = 0;
	private static StringBuilder sb = new StringBuilder();
	private static String folderPath = "C:\\Users\\yissachar.radcliffe\\dev\\EclipseWorkspace\\fave100\\src\\com\\fave100\\client\\generated\\";
	private static String servicePath = "";
	private static String entitiesPath = "";
	private static String apiName = "";
	private static String version = "";
	
    public static void main( String[] args ) throws IOException
    {   
        // Delete old generated files
        deleteFiles(new File(folderPath));
        
        // TODO: Remove all the harcoding paths
        // Run AppEngine Endpoints tool to get the latest discovery doc
        String discoveryDocFolder = "C:\\Users\\yissachar.radcliffe\\dev\\EclipseWorkspace\\fave100\\war\\WEB-INF";
        
        List<String> endpointsToolArgs = new ArrayList<>();
        endpointsToolArgs.add("C:/Users/yissachar.radcliffe/dev/lib/java/appengine-java-sdk-1.8.9/bin/endpoints.cmd");
        endpointsToolArgs.add("get-discovery-doc");
        endpointsToolArgs.add("--output=\""+discoveryDocFolder+"\"");
        endpointsToolArgs.add("--war=C:\\Users\\yissachar.radcliffe\\dev\\EclipseWorkspace\\fave100\\war\\");
        endpointsToolArgs.add("com.fave100.server.domain.favelist.FaveListApi");
        endpointsToolArgs.add("com.fave100.server.domain.SongApi");
        endpointsToolArgs.add("com.fave100.server.domain.appuser.AppUserApi");
        endpointsToolArgs.add("com.fave100.server.domain.WhylineApi");
        
        try {
        	ProcessBuilder pb = new ProcessBuilder(endpointsToolArgs);
        	pb.redirectErrorStream(true);        	
            Process p = pb.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = input.readLine()) != null)
        	   System.out.println(line);

            input.close();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        BufferedReader in = new BufferedReader(new FileReader(new File(discoveryDocFolder + "\\fave100-v1-rest.discovery")));

        String inputLine = "";
        String temp = null;
        while ((temp = in.readLine()) != null)
            inputLine += temp;
        in.close();
        
        Object obj = JSONValue.parse(inputLine);
        JSONObject json = (JSONObject)obj;
        
        apiName = (String)json.get("name");
        version = (String)json.get("version");
        
        servicePath = folderPath + "\\services\\";
        entitiesPath = folderPath + "\\entities\\";
        
    	new File(servicePath).mkdirs();
    	new File(entitiesPath).mkdirs();
    	
        JSONObject schemas = (JSONObject)json.get("schemas");
        for(Object schemaObj : schemas.keySet()) {
        	String schemaName = (String)schemaObj;
        	JSONObject schema = (JSONObject)schemas.get(schemaName);
        	writeClass(schema);        	
        }
        
        writeServices((JSONObject)json.get("resources"));        	
        
    }
    
    public static void deleteFiles(File dir) {
	    for (File file : dir.listFiles()) {
	    	if(file.isDirectory())
	    		deleteFiles(file);
	        file.delete();
	    }
    }
        
    public static void writeClass(JSONObject schema) {
    	String className = getClassName((String)schema.get("id"));
    			
    	sb.append(getWarningComment());
    	
    	sb.append("package ");
    	sb.append(ENTITY_PACKAGE);
    	sb.append(";");
    	sb.append("\n\nimport java.util.List;");    	
    	sb.append("\n\npublic class ");
    	sb.append(className);
    	sb.append(" {\n\n");
    	
    	JSONObject properties = (JSONObject)schema.get("properties");
    	
    	if(properties != null) {
	    	// Print property fields
	    	for(Object propObj : properties.keySet()) {
	    		String propName = (String)propObj;
	    		JSONObject propJson = (JSONObject)properties.get(propName);
	    		indent();
	        	sb.append("private ");
	        	if(propJson.get("type") != null) {
	        		sb.append(convertPropertyToType(propJson));
	        	} else {
	        		sb.append(getClassName((String)(propJson.get("$ref"))));
	        	}
	        	sb.append(" ");
	        	sb.append(propName);
	        	sb.append(";\n");
	        	outdent();
	    	}
	    	
	    	sb.append("\n");
    	
	    	// Print getters and setters
	    	for(Object propObj : properties.keySet()) {
	    		String propName = (String)propObj;
	    		JSONObject propJson = (JSONObject)properties.get(propName);
	    		
	    		// Getter
	    		String returnType = "";
	    		if(propJson.get("type") != null) {
	    			returnType = convertPropertyToType(propJson);
	        	} else {
	        		returnType = getClassName((String)(propJson.get("$ref")));
	        	}
	    		indent();
	        	sb.append("public ");
	        	sb.append(returnType);
	        	sb.append(returnType.equals("boolean") && !propName.equals("value") ? " is" : " get");
	        	sb.append(ucFirst(propName));
	        	sb.append("() {\n");
	        		indent();
	        		sb.append("return this.");
	        		sb.append(propName);
	            	sb.append(";\n");
	            	sb.append("    }\n\n");
	            	outdent();
	        	outdent();
	        	        	
	        	// Setter
	        	indent();
	        	sb.append("public ");
	        	sb.append("void");
	        	sb.append(" set");
	        	sb.append(ucFirst(propName));
	        	sb.append("(");
	        	if(propJson.get("type") != null) {
	        		sb.append(convertPropertyToType(propJson));
	        	} else {
	        		sb.append(getClassName((String)(propJson.get("$ref"))));
	        	}
	        	sb.append(" ");
	        	sb.append(propName);
	        	sb.append(") {\n");
	        		indent();
	        		sb.append("this.");
	        		sb.append(propName);
	        		sb.append(" = ");
	        		sb.append(propName);
	            	sb.append(";\n");
	            	sb.append("    }\n\n");
	            	outdent();
	        	outdent();
	    	}	    	
    	}
    	
    	sb.append("\n}");
    	
    	PrintWriter writer;
		try {
			writer = new PrintWriter(new File(entitiesPath + className + ".java"), "UTF-8");
	    	writer.print(sb.toString());
	    	writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sb = new StringBuilder();
    }
    
    public static void writeServices(JSONObject resources) {
    	
    	for(Object resourceObj : resources.keySet()) {
        	String resourceName = (String)resourceObj;
        	String serviceName = ucFirst(resourceName) + "Service";
        	JSONObject resource = (JSONObject)resources.get(resourceName);
	    	
        	JSONObject methods = ((JSONObject)resource.get("methods"));
        	
        	sb.append(getWarningComment());
        	
        	sb.append("package ");
        	sb.append(SERVICE_PACKAGE);
        	sb.append(";");
        	
        	sb.append("\n\nimport javax.ws.rs.Path;\n");
        
        	// Import proper jackson annotations
        	Set<String> importedHttpMethods = new HashSet<>();
        	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	
            	String httpMethod = (String)method.get("httpMethod");
            	if (!importedHttpMethods.contains(httpMethod)) {
            		sb.append("import javax.ws.rs.");
                	sb.append(httpMethod);
                	sb.append(";\n");
                	importedHttpMethods.add(httpMethod);
				}
            	
        	}

	    	sb.append("import javax.ws.rs.QueryParam;\n");
	    	sb.append("import com.gwtplatform.dispatch.rest.shared.RestAction;\n");
	    	sb.append("import com.gwtplatform.dispatch.rest.shared.RestService;\n");
	    	
	    	// Import all needed entities;
	    	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	JSONObject response = (JSONObject)method.get("response");
            	
            	if(response != null) {
	            	String responseType = getClassName((String)(response.get("$ref")));
	    	    	sb.append("import ");
	    	    	sb.append(ENTITY_PACKAGE);
	    	    	sb.append(".");
	    	    	sb.append(responseType);
	    	    	sb.append(";\n");
            	}
	    	}
	    	
	    	// Path anno
        	sb.append("\n@Path(\"/");
        	sb.append(apiName);        	
        	sb.append("/");
        	sb.append(version);        	
        	sb.append("/");
        	sb.append("\")");
        	
        	// Interface
        	sb.append("\npublic interface ");
        	sb.append(serviceName);
        	sb.append(" extends RestService {\n\n");
        	
        	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	String responseType = getClassName((String)((JSONObject)method.get("response")).get("$ref"));            	
            	
            	indent();
            	sb.append("@");
            	sb.append(method.get("httpMethod"));
            	
            	sb.append("\n@Path(\"");
            	sb.append(method.get("path"));
            	sb.append("\")\npublic RestAction<");
            	sb.append(responseType);
            	sb.append("> ");
            	sb.append(methodName);
            	sb.append("(");

            	// Add Query params
            	JSONObject params = (JSONObject)method.get("parameters");
            	
            	if(params != null) {
	            	int i = 1;
	            	int length = params.size();
	            	for(Object paramObj : params.keySet()) {
	                	String paramName = (String)paramObj;                	
	                	JSONObject param = (JSONObject)params.get(paramName);
	                	                	
	                	// Add @QueryParam anno if needed 
	                	String location = (String)param.get("location");
	                	if(location != null && location.equals("query")) {
	                		sb.append("@QueryParam(\"");
	                		sb.append(paramName);
	                		sb.append("\") ");
	                	}
	                	
	                	sb.append(convertPropertyToType(param));
	                	sb.append(" ");
	                	sb.append(paramName);
	                	
	                	if(length > 1 && i != length)
	                		sb.append(", ");
	                	
	                	i++;
	            	}
            	}
            	
            	// Add Request Params
            	JSONObject request = (JSONObject)method.get("request");
            	if(request != null) {
            		sb.append(getClassName((String)request.get("$ref")));
            		sb.append(" ");
            		sb.append(request.get("parameterName"));
            	}
            	
            	sb.append(");\n");
            	
        	}
        	
        	sb.append("}");
	    	
	    	PrintWriter writer;
			try {
				writer = new PrintWriter(new File(servicePath + serviceName + ".java"), "UTF-8");
		    	writer.print(sb.toString());
		    	writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			sb = new StringBuilder();
    	}
    }
    
    public static String getWarningComment() {
    	return "/*\n"
    			+ "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
    			+ "*\n"
    			+ "* WARNING: THIS IS A GENERATED FILE. ANY CHANGES YOU\n"
    			+ "* MAKE WILL BE LOST THE NEXT TIME THIS FILE IS GENERATED\n"
    			+ "*\n"
    			+ "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
    			+ "*/\n\n"; 
    }
    
    public static String getClassName(String type) {
    	if(!type.endsWith("Collection")) 
    		type += "Dto";
    	
    	return type;
    }
    
    public static String convertPropertyToType(JSONObject property) {
    	String type = convertType((String)property.get("type"));
    	
    	JSONObject itemObj = (JSONObject)property.get("items");
    	if(itemObj != null) {
    		String itemType = (String)itemObj.get("$ref");
    		
    		if(itemType == null) {
    			// Raw type, don't try to turn it into DTO
    			itemType = (String)itemObj.get("type");    			
    			type += "<" + convertType(itemType) + ">";
    		} else {
    			// DTO ref
    			type += "<" + getClassName(convertType(itemType)) + ">";
    		}
    	}
    	
    	return type;
    }
    
    public static String convertType(String type) {
    	switch (type) {
		case "string":
			return "String";
		
		case "array":
			return "List";
		
		case "integer":
			return "int";

		default:
			return type;
		}
    }
    
    public static void indent() {
    	indentCount++;
    	for(int i = 0; i < indentCount * 4; i++) {
    		sb.append(" ");
    	}
    }
    
    public static void outdent() {
    	indentCount--;
    }
    
    private static String ucFirst(String string) {
    	return Character.toString(string.charAt(0)).toUpperCase() + string.substring(1);
    }
}
