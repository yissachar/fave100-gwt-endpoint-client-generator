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
	private static String folderPath = "C:\\Users\\yissachar.radcliffe\\dev\\EclipseWorkspace\\fave100\\src\\com\\fave100\\client\\generated\\";
	private static String servicePath = "";
	private static String entitiesPath = "";
	private static String apiName = "";
	private static String version = "";
	private static List<String> services = new ArrayList<>();
	
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
        	writeEntity(schema);        	
        }
        
        writeServices((JSONObject)json.get("resources"));        	
        
        writeServiceFactory();
        
    }
    
    public static void deleteFiles(File dir) {
	    for (File file : dir.listFiles()) {
	    	if(file.isDirectory())
	    		deleteFiles(file);
	        file.delete();
	    }
    }
        
    public static void writeEntity(JSONObject schema) throws FileNotFoundException, UnsupportedEncodingException {
    	
    	FileBuilder fb = new FileBuilder();
    	
    	String className = getClassName((String)schema.get("id"));
    			
    	fb.append(getWarningComment());
    	
    	fb.append("package ");
    	fb.append(ENTITY_PACKAGE);
    	fb.append(";");
    	fb.append("\n\nimport java.util.List;");    	
    	fb.append("\n\npublic class ");
    	fb.append(className);
    	fb.append(" {\n\n");
    	
    	JSONObject properties = (JSONObject)schema.get("properties");
    	
    	if(properties != null) {
	    	// Print property fields
	    	for(Object propObj : properties.keySet()) {
	    		String propName = (String)propObj;
	    		JSONObject propJson = (JSONObject)properties.get(propName);
	    		fb.indent();
	        	fb.append("private ");
	        	if(propJson.get("type") != null) {
	        		fb.append(convertPropertyToType(propJson));
	        	} else {
	        		fb.append(getClassName((String)(propJson.get("$ref"))));
	        	}
	        	fb.append(" ");
	        	fb.append(propName);
	        	fb.append(";\n");
	        	fb.outdent();
	    	}
	    	
	    	fb.append("\n");
    	
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
	    		fb.indent();
	        	fb.append("public ");
	        	fb.append(returnType);
	        	fb.append(returnType.equals("boolean") && !propName.equals("value") ? " is" : " get");
	        	fb.append(ucFirst(propName));
	        	fb.append("() {\n");
	        		fb.indent();
	        		fb.append("return this.");
	        		fb.append(propName);
	            	fb.append(";\n");
	            	fb.append("    }\n\n");
	            	fb.outdent();
	            fb.outdent();
	        	        	
	        	// Setter
	            fb.indent();
	        	fb.append("public ");
	        	fb.append("void");
	        	fb.append(" set");
	        	fb.append(ucFirst(propName));
	        	fb.append("(");
	        	if(propJson.get("type") != null) {
	        		fb.append(convertPropertyToType(propJson));
	        	} else {
	        		fb.append(getClassName((String)(propJson.get("$ref"))));
	        	}
	        	fb.append(" ");
	        	fb.append(propName);
	        	fb.append(") {\n");
	        		fb.indent();
	        		fb.append("this.");
	        		fb.append(propName);
	        		fb.append(" = ");
	        		fb.append(propName);
	            	fb.append(";\n");
	            	fb.append("    }\n\n");
	            	fb.outdent();
	            fb.outdent();
	    	}	    	
    	}
    	
    	fb.append("}");
    	
    	fb.save(entitiesPath + className + ".java");
    }
    
    public static void writeServices(JSONObject resources) throws FileNotFoundException, UnsupportedEncodingException {
    	
    	for(Object resourceObj : resources.keySet()) {
    		FileBuilder fb = new FileBuilder();
    		
        	String resourceName = (String)resourceObj;
        	String serviceName = ucFirst(resourceName) + "Service";
        	services.add(serviceName);
        	JSONObject resource = (JSONObject)resources.get(resourceName);
	    	
        	JSONObject methods = ((JSONObject)resource.get("methods"));
        	
        	fb.append(getWarningComment());
        	
        	fb.append("package ");
        	fb.append(SERVICE_PACKAGE);
        	fb.append(";");
        	
        	fb.append("\n\nimport javax.ws.rs.Path;\n");
        
        	// Import proper jackson annotations
        	Set<String> importedHttpMethods = new HashSet<>();
        	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	
            	String httpMethod = (String)method.get("httpMethod");
            	if (!importedHttpMethods.contains(httpMethod)) {
            		fb.append("import javax.ws.rs.");
                	fb.append(httpMethod);
                	fb.append(";\n");
                	importedHttpMethods.add(httpMethod);
				}
            	
        	}

	    	fb.append("import javax.ws.rs.QueryParam;\n");
	    	fb.append("import com.gwtplatform.dispatch.rest.shared.RestAction;\n");
	    	fb.append("import com.gwtplatform.dispatch.rest.shared.RestService;\n");
	    	
	    	// Import all needed entities;
	    	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	JSONObject response = (JSONObject)method.get("response");
            	
            	if(response != null) {
	            	String responseType = getClassName((String)(response.get("$ref")));
	    	    	fb.append("import ");
	    	    	fb.append(ENTITY_PACKAGE);
	    	    	fb.append(".");
	    	    	fb.append(responseType);
	    	    	fb.append(";\n");
            	}
	    	}
	    	
	    	// Path anno
        	fb.append("\n@Path(\"/");
        	fb.append(apiName);        	
        	fb.append("/");
        	fb.append(version);        	
        	fb.append("/");
        	fb.append("\")");
        	
        	// Interface
        	fb.append("\npublic interface ");
        	fb.append(serviceName);
        	fb.append(" extends RestService {\n\n");
        	
        	for(Object methodObj : methods.keySet()) {
            	String methodName = (String)methodObj;
            	JSONObject method = (JSONObject)methods.get(methodName);
            	JSONObject responseObj = (JSONObject)method.get("response");
            	
            	String responseType;
            	if(responseObj != null) {
                	responseType = getClassName((String)responseObj.get("$ref"));	
            	} else {
            		responseType = "Void";
            	}
            	
            	fb.indent();
            	fb.append("@");
            	fb.append(method.get("httpMethod"));
            	
            	fb.append("\n");
            	fb.applyIndent();
            	fb.append("@Path(\"");
            	fb.append(method.get("path"));
            	fb.append("\")\n");
            	fb.applyIndent();
            	fb.append("public RestAction<");
            	fb.append(responseType);
            	fb.append("> ");
            	fb.append(methodName);
            	fb.append("(");

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
	                		fb.append("@QueryParam(\"");
	                		fb.append(paramName);
	                		fb.append("\") ");
	                	}
	                	
	                	fb.append(convertPropertyToType(param));
	                	fb.append(" ");
	                	fb.append(paramName);
	                	
	                	if(length > 1 && i != length)
	                		fb.append(", ");
	                	
	                	i++;
	            	}
            	}
            	
            	// Add Request Params
            	JSONObject request = (JSONObject)method.get("request");
            	if(request != null) {
            		fb.append(getClassName((String)request.get("$ref")));
            		fb.append(" ");
            		fb.append(request.get("parameterName"));
            	}
            	
            	fb.append(");\n\n");
            	fb.outdent();
            	
        	}
        	
        	fb.append("}");
	    	
        	fb.save(servicePath + serviceName + ".java");
    	}
    }
    
    public static void writeServiceFactory() throws FileNotFoundException, UnsupportedEncodingException {
    	FileBuilder fb = new FileBuilder();
    	
    	fb.append("package com.fave100.client.generated.services;\n\n");
    	fb.append("import com.google.inject.Inject;\n\n");
    	fb.append("public class RestServiceFactory {\n\n");
    	
    	// Variable declarations
    	fb.indent();
    	for(String service : services) {
    		fb.append("private ");
    		fb.append(service);
    		fb.append(" _");
    		fb.append(lcFirst(service));
    		fb.append(";\n");
    		fb.applyIndent();
    	}
    	
    	fb.append("\n");
    	fb.applyIndent();
    	fb.append("@Inject\n");
    	fb.applyIndent();
    	fb.append("public RestServiceFactory(");
    	
    	// Constructor params
    	int i = 1;
    	for(String service : services) {
    		fb.append(service);
    		fb.append(" ");
    		fb.append(lcFirst(service));
    		
    		if(services.size() > 1 && i != services.size())
    			fb.append(", ");
    		
    		i++;
    	}
    	
    	fb.append(") {\n");
    	
    	// Field initialization    	
    	fb.indent();
    	for(String service : services) {
    		fb.append(" _");
    		fb.append(lcFirst(service));
    		fb.append(" = ");
    		fb.append(lcFirst(service));
    		fb.append(";\n");
    		fb.applyIndent();
    	}
    	fb.outdent();
    	fb.append("}\n\n");
    	
    	// Getters
    	for(String service : services) {
        	fb.applyIndent();
    		fb.append("public ");
    		fb.append(service);    		
    		fb.append(" get");
    		fb.append(service);
    		fb.append("() {\n");
    		fb.indent();
    		fb.append("return _");
    		fb.append(lcFirst(service));
    		fb.append(";\n");
    		fb.outdent();
    		fb.applyIndent();
    		fb.append("}\n\n");
    	}

		fb.outdent();
    	fb.append("}");
    	
    	fb.save(servicePath + "RestServiceFactory.java");
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
    
    private static String ucFirst(String string) {
    	return Character.toString(string.charAt(0)).toUpperCase() + string.substring(1);
    }
    
    private static String lcFirst(String string) {
    	return Character.toString(string.charAt(0)).toLowerCase() + string.substring(1);
    }
}
