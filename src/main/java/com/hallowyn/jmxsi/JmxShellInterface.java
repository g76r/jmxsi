package com.hallowyn.jmxsi;

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
	
	/**
	 * jmxsi <command> [params...]
	 * 
	 * commands:
	 * - help
	 * - lsobj <url> <objectname> [outputformat]
	 * - lsattr // FIXME 
	 * - get <url> <objectname> <attrname> [outputformat] // FIXME what if composite
	 * - set <url> <objectname> <attrname> <value> // FIXME what if composite
	 * - 
	 * 
	 * params
	 * - url e.g. "service:jmx:rmi:///jndi/rmi://localhost:42/jmxrmi"
	 * - objectname e.g. "org.hornetq:module=Core,type=Acceptor,*"
	 * - outputformat e.g. "%Domain:type=%type,*", "%name", default: "%CanonicalName" for list and "%CompositeAttribute=%Value" for get // FIXME
	 * - attributename
	 * - value
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String command = args.length >= 3 ? args[0] : "";
		//System.out.println("command: "+command+" args.length: "+args.length);
		if (command.equals("lsobj")) {
			JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
			jmxsi.lsobjCommand(args[2], args.length >= 4 ? args[3] : "%CanonicalName");
		} else if (command.equals("get")) {
			if (args.length >= 4) {
				JmxShellInterface jmxsi = new JmxShellInterface(args[1]);
				jmxsi.getCommand(args[2], args[3], args.length >= 5 ? args[4] : "%CompositeAttribute=%Value");
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
	
	public static void helpCommand() {
		System.out.println("help"); // FIXME		
		System.exit(1);
	}

	public void lsobjCommand(String objectname, String outputformat) throws Exception {
		for (ObjectInstance o : queryObjects(objectname)) {
			String output = evaluateObject(o, outputformat);
			System.out.println(output);
		}
	}

	public void getCommand(String objectname, String attrname, String outputformat) throws Exception {
		//System.out.println("getCommand");
		for (ObjectInstance o : queryObjects(objectname)) {
			MBeanAttributeInfo[] infos = mbeanServer.getMBeanInfo(o.getObjectName()).getAttributes();
			ArrayList<String> attrnames = new ArrayList<String>();
			if (attrname.equals("*")) {
				for (MBeanAttributeInfo info : infos) {
					attrnames.add(info.getName());
				}
			} else {
				attrnames.add(attrname);
			}
			AttributeList attrs = mbeanServer.getAttributes(o.getObjectName(), attrnames.toArray(new String[1]));
			Map<String,String> context = new HashMap<String,String>();
			for (Object tmp : attrs) {
				Attribute attr = (Attribute)tmp;
				context.put("Attribute", attr.getName()); // FIXME
				context.put("CompositeAttribute", attr.getName()); // FIXME
				String type = "unknown";
				for (MBeanAttributeInfo info : infos) {
					if (info.getName().equals(attr.getName())) {
						type = info.getType();
						break;
					}
				}
				//System.out.println("class: "+attr.getClass().getName());
				//System.out.println("type: "+type);
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

	public List<ObjectInstance> queryObjects(String objectname) throws Exception {
        Set<ObjectInstance> set = mbeanServer.queryMBeans(new ObjectName(objectname), null);
        List<ObjectInstance> list = new LinkedList<ObjectInstance>(set);
        Collections.sort(list, new Comparator<ObjectInstance>() {
        	@Override
        	public int compare(final ObjectInstance o1, final ObjectInstance o2) {
        		return o1.getObjectName().compareTo(o2.getObjectName());
        	}
        });
        return list;
	}

	static public String evaluateObject(ObjectInstance o, String outputformat) {
		return evaluateObject(o, outputformat, null);
	}

	static public String evaluateObject(ObjectInstance o, String outputformat, Map<String,String> context) {
		//System.out.println("evaluateObject: "+o.toString()+" "+outputformat);
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
	
	static private final String evaluateObjectVariable(ObjectInstance o, String variablename, Map<String,String> context) {
		// FIXME
		if (o == null)
			return "";
		/*System.out.println("evaluateObjectVariable: "+o.toString()+" "+variablename);
		System.out.println("keys: "+o.getObjectName().getKeyPropertyListString());
		System.out.println("canoncial keys: "+o.getObjectName().getCanonicalKeyPropertyListString());
		System.out.println("domain: "+o.getObjectName().getDomain());
		System.out.println("object.toString: "+o.toString());
		System.out.println("object.name.toString: "+o.getObjectName().toString());*/
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
		//return new String();
	}
}
