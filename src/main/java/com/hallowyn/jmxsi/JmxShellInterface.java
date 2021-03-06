package com.hallowyn.jmxsi;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

// LATER add an option to disable CompositeData walk through
// LATER support for several command at once, using stdin instead of command line

public class JmxShellInterface {

	private enum EvaluateState { Text, SimpleVariable, CurlyBracesVariable };
	static final String VARIABLE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789";

	private JMXConnector jmxConnector = null;
	private MBeanServerConnection mbeanServer = null;
	
	/** Main method, see README file for usage. */
	public static void main(String[] args) throws Exception {
		String command = args.length >= 3 ? args[0] : "";
		if (command.equals("lsobj")) {
			JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
			jmxsi.lsobjCommand(args[2], args.length >= 4 ? args[3] : "%CanonicalName");
		} else if (command.equals("lsattr")) {
			JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
			jmxsi.lsattrCommand(args[2], args.length >= 4 ? args[3] : "*",
					args.length >= 5 ? args[4] : "%CompositeAttribute: %Type");
		} else if (command.equals("lsop")) {
			JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
			jmxsi.lsopCommand(args[2], args.length >= 4 ? args[3] : "%Operation: %Type");
		} else if (command.equals("get")) {
			if (args.length >= 4) {
				JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
				jmxsi.getCommand(args[2], args[3], args.length >= 5 ? args[4] : "%CompositeAttribute=%Value");
			} else {
				helpCommand();
			}
		} else if (command.equals("set")) {
			if (args.length >= 5) {
				JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
				jmxsi.setCommand(args[2], args[3], args[4], args.length >= 6 ? args[5] : "%CompositeAttribute=%Value");
			} else {
				helpCommand();
			}
		} else if (command.equals("invoke")) {
			if (args.length >= 4) {
				JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
				int p = 4;
				String outputformat = "%Result";
				if (args.length >= 6 && "-o".equals(args[4])) {
					outputformat = args[5];
					p = 6;
				}
				String[] params = new String[args.length-p];
				for (int i = p; i < args.length; ++i)
					params[i-p] = args[i];
				jmxsi.invokeCommand(args[2], args[3], params, outputformat);
			} else {
				helpCommand();
			}
		} else {
			helpCommand();
		}
	}

	public JmxShellInterface(String url) throws Exception {
		jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(url));
		mbeanServer = jmxConnector.getMBeanServerConnection();

	}
	
	/** Handler for "help" command. */
	public static void helpCommand() throws Exception {
		try {
			// Print the output of README file untill "Examples" section
			InputStream in = JmxShellInterface.class.getResourceAsStream("/README.md");
			InputStreamReader r = new InputStreamReader(in, "UTF-8");
			LineNumberReader lnr = new LineNumberReader(r);
			boolean skip = false;
			while(lnr.ready()) {
				String line = lnr.readLine();
				if ("Examples".equals(line)) {
					skip = true;
				}
				if (!skip)
					System.out.println(line);
			}
		} catch(Exception e) {
			System.out.println("Syntax error.");
		}
		System.exit(1);
	}

	/** Handler for "lsobj" command. */
	public void lsobjCommand(String objectname, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			System.out.println(evaluateObject(o, outputformat));
		}
	}

	/** Handler for "lsattr" command. */
	public void lsattrCommand(String objectname, String attrname, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			MBeanAttributeInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getAttributes();
			AttributeList attrs = queryAttributes(o.getObjectName(), attrname);
			Map<String,String> context = new HashMap<String,String>();
			for (Object tmp : attrs) {
				Attribute attr = (Attribute)tmp;
				context.put("Attribute", attr.getName());
				context.put("CompositeAttribute", attr.getName());
				String type = "unknown";
				for (MBeanAttributeInfo info : infos) {
					if (info.getName().equals(attr.getName())) {
						type = info.getType();
						break;
					}
				}
				context.put("Type", type);
				System.out.println(evaluateObject(o, outputformat, context));
			}
		}
	}

	/** Handler for "lsop" command. */
	public void lsopCommand(String objectname, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			MBeanOperationInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getOperations();
			ArrayList<String> signature = new ArrayList<String>();
			Map<String,String> context = new HashMap<String,String>();
			for (MBeanOperationInfo info: infos) {
				MBeanParameterInfo[] paraminfos = info.getSignature();
				StringBuffer name = new StringBuffer();
				name.append(info.getName()).append('(');
				boolean first = true;
				signature.clear();
				for (MBeanParameterInfo paraminfo : paraminfos) {
					if (first)
						first = false;
					else
						name.append(',');
					name.append(paraminfo.getType());
					signature.add(paraminfo.getType());
				}
				name.append(')');
				context.put("Operation", name.toString());
				context.put("Type", info.getReturnType());
				System.out.println(evaluateObject(o, outputformat, context));
			}
		}
	}

	/** Handler for "get" command. */
	public void getCommand(String objectname, String attrname, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			MBeanAttributeInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getAttributes();
			AttributeList attrs = queryAttributes(o.getObjectName(), attrname);
			Map<String,String> context = new HashMap<String,String>();
			for (Object tmp : attrs) {
				Attribute attr = (Attribute)tmp;
				context.put("Attribute", attr.getName());
				context.put("CompositeAttribute", attr.getName());
				String type = "unknown";
				for (MBeanAttributeInfo info : infos) {
					if (info.getName().equals(attr.getName())) {
						type = info.getType();
						break;
					}
				}
				context.put("Type", type);
				if (type.equals("javax.management.openmbean.CompositeData")) {
					CompositeData cd = (CompositeData)attr.getValue();
					Set<String> keys = cd.getCompositeType().keySet();
					for (String key : keys) {
						context.put("CompositeAttribute", attr.getName()+"."+key);
						context.put("Value", cd.get(key) == null ? "" : cd.get(key).toString());
						System.out.println(evaluateObject(o, outputformat, context));
					}
				} else {
					context.put("Value", attr.getValue() == null ? "" : attr.getValue().toString());
					System.out.println(evaluateObject(o, outputformat, context));
				}
			}
		}
	}

	/** Handler for "set" command. */
	public void setCommand(String objectname, String attrname, String value, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			MBeanAttributeInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getAttributes();
			AttributeList attrs = queryAttributes(o.getObjectName(), attrname);
			Map<String,String> context = new HashMap<String,String>();
			for (Object tmp : attrs) {
				Attribute attr = (Attribute)tmp;
				context.put("Attribute", attr.getName());
				context.put("CompositeAttribute", attr.getName());
				context.put("Value", value);
				String type = "unknown";
				for (MBeanAttributeInfo info : infos) {
					if (info.getName().equals(attr.getName())) {
						type = info.getType();
						break;
					}
				}
				context.put("Type", type);
				if ("javax.management.openmbean.CompositeData".equals(type)) {
					throw new Exception("setting CompositeData attributes is not supported.");
				} else {
					mbeanServer.setAttribute(o.getObjectName(), new Attribute(attr.getName(), convertValueToObject(value, type)));
				}
				System.out.println(evaluateObject(o, outputformat, context));
			}
		}
	}

	/** Handler for "invoke" command. */
	public void invokeCommand(String objectname, String operationname, String[] params, String outputformat) throws Exception {
		List<ObjectInstance> objectinstances = queryObjects(objectname);
		if (objectinstances.size() == 0) {
			System.out.println("No such objects found.");
			System.exit(1);
		}
		for (ObjectInstance o : objectinstances) {
			MBeanOperationInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getOperations();
			MBeanOperationInfo operationinfo = null;
			ArrayList<String> signature = new ArrayList<String>();
			Map<String,String> context = new HashMap<String,String>();
			context.put("Operation", operationname);
			for (MBeanOperationInfo info: infos) {
				MBeanParameterInfo[] paraminfos = info.getSignature();
				StringBuffer name = new StringBuffer();
				name.append(info.getName()).append('(');
				boolean first = true;
				signature.clear();
				for (MBeanParameterInfo paraminfo : paraminfos) {
					if (first)
						first = false;
					else
						name.append(',');
					name.append(paraminfo.getType());
					signature.add(paraminfo.getType());
				}
				name.append(')');
				if (operationname.equals(name.toString())) {
					operationinfo = info;
					context.put("Type", info.getReturnType());
					break;
				}
			}
			if (operationinfo == null) {
				System.out.println("Operation \""+operationname+"\" not found on object \""
						+o.getObjectName().getCanonicalName()+"\".");
				continue;
			}
			if (signature.size() != params.length) {
				System.out.println("Operation \""+operationname+"\" has not a consistent number of params as compared to its signature.");
				continue;				
			}
			Object[] objects = new Object[params.length];
			for (int i = 0; i < params.length; ++i) {
				String type = operationinfo.getSignature()[i].getType();
				objects[i] = convertValueToObject(params[i], type);
			}
			Object result = mbeanServer.invoke(o.getObjectName(), operationinfo.getName(), objects, signature.toArray(new String[signature.size()]));
			context.put("Result", (result == null ? "null" : result.toString()));
			System.out.println(evaluateObject(o, outputformat, context));
		}
	}

	private final static Object convertValueToObject(String value, String type) {
		// LATER support more param types
		if ("long".equals(type))
			return new Long(value).longValue();
		else if ("int".equals(type))
			return new Integer(value).intValue();
		else if ("boolean".equals(type))
			return new Boolean(value).booleanValue();
		return value; // default to String
	}

	/** Get a sorted list of ObjectInstance given the objectname query string.
	 * @param objectname e.g. "java.lang:type=Memory" or "org.hornetq:module=Core,type=Acceptor,*"
	 */
	public List<ObjectInstance> queryObjects(String objectname) throws Exception {
		Set<ObjectInstance> set;
		if (objectname.equals("*") || objectname.isEmpty())
			set = mbeanServer.queryMBeans(null, null);
		else
			set = mbeanServer.queryMBeans(new ObjectName(objectname), null);
        List<ObjectInstance> list = new LinkedList<ObjectInstance>(set);
        Collections.sort(list, new Comparator<ObjectInstance>() {
        	@Override
        	public int compare(final ObjectInstance o1, final ObjectInstance o2) {
        		return o1.getObjectName().compareTo(o2.getObjectName());
        	}
        });
        return list;
	}
	
	/** Get a sorted list of Attributes given the objectname query string and an attrname string.
	 * @param objectname e.g. "java.lang:type=Memory" or "org.hornetq:module=Core,type=Acceptor,*"
	 * @param attrname e.g. "HeapMemoryUsage" or "HeapMemoryUsage,NonHeapMemoryUsage" or "*"
	 */
	public AttributeList queryAttributes(ObjectName objectname, String attrname) throws Exception {
		MBeanAttributeInfo[] infos = mbeanServer.getMBeanInfo(objectname).getAttributes();
		ArrayList<String> attrnames = new ArrayList<String>();
		if (attrname.equals("*")) {
			for (MBeanAttributeInfo info : infos) {
				attrnames.add(info.getName());
			}
		} else if (attrname.indexOf(',') >= 0) {
		 for (String token : attrname.split(","))
			 attrnames.add(token);
		} else {
			attrnames.add(attrname);
		}
		AttributeList list = mbeanServer.getAttributes(objectname, attrnames.toArray(new String[1]));
        Collections.sort(list, new Comparator<Object>() {
        	@Override
        	public int compare(final Object o1, final Object o2) {
        		return ((Attribute)o1).getName().compareTo(((Attribute)o2).getName());
        	}
        });
        return list;
	}

	/** Convenience method with empty context. */
	static public String evaluateObject(ObjectInstance o, String outputformat) {
		return evaluateObject(o, outputformat, null);
	}

	/** Evaluate an object following an outputformat with % variable patterns.
	 * @param o ObjectInstance being evaluated
	 * @param outputformat e.g. "%CanonicalName" "type=%type,name=%name,*" "%Result" "%{name}_%{type}"
	 * @param context variable,value map in addition to ObjectInstance properties (e.g. "type") and hard-wired variables (e.g. "%CanonicalName")
	 */
	static public String evaluateObject(ObjectInstance o, String outputformat, Map<String,String> context) {
		StringBuffer value = new StringBuffer();
		StringBuffer variable = new StringBuffer();
		EvaluateState state = EvaluateState.Text;
		for (int i = 0; i < outputformat.length(); ++i) {
			char c = outputformat.charAt(i);
			switch (state) {
			case Text:
				if (c == '%') {
					if (i < outputformat.length()+1) {
						if (outputformat.charAt(i+1) == '%') {
							value.append('%');
							++i;
						} else if (outputformat.charAt(i+1) == '{') {
							++i;
							state = EvaluateState.CurlyBracesVariable;
						} else {
							state = EvaluateState.SimpleVariable;
						}
					} else {
						// ignore % at end of string
					}
				} else {
					value.append(c);
				}
				break;
			case SimpleVariable:
				if (VARIABLE_CHARS.indexOf(c) >= 0) {
					variable.append(c);
				} else {
					value.append(evaluateObjectVariable(o, variable.toString(), context));
					variable.setLength(0);
					--i;
					state = EvaluateState.Text;
				}
				break;
			case CurlyBracesVariable:
				if (c == '}') {
					value.append(evaluateObjectVariable(o, variable.toString(), context));
					variable.setLength(0);
					state = EvaluateState.Text;
				} else {
					variable.append(c);
				}
				break;
			}
		}
		if (variable.length() > 0)
			value.append(evaluateObjectVariable(o, variable.toString(), context));
		return value.toString();
	}

	/** Method internally used by evaluateObject().
	 * Do not call from elsewhere. */
	static private final String evaluateObjectVariable(ObjectInstance o, String variablename, Map<String,String> context) {
		if (o == null)
			return "";
		if (context != null && context.containsKey(variablename)) {
			return context.get(variablename);
		} if (variablename.equals("CanonicalName")) {
			return o.getObjectName().getCanonicalName();
		} else if (variablename.equals("toString")) {
			return o.getObjectName().toString();
		} else if (variablename.equals("Domain")) {
			return o.getObjectName().getDomain();
		} else {
			return o.getObjectName().getKeyProperty(variablename);
		}
	}
}
