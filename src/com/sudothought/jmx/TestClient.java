package com.sudothought.jmx;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnector;
import javax.management.remote.rmi.RMIServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

public class TestClient {

    public static void main(String[] args) throws Exception {

        String jmxUrl = null;
        boolean useAgent = true;

        for (final String arg : args) {
            if (arg.startsWith("-url="))
                jmxUrl = arg.replace("-url=", "");

            if (arg.startsWith("-useagent="))
                useAgent = Boolean.valueOf(arg.replace("-useagent=", ""));
        }

        if (jmxUrl == null) {
            System.out.println("usage: java com.sudothought.jmx.TestClient "
                               + "-url:jmxurl -useagent:(true|false)");
            return;
        }

        final JMXServiceURL url = new JMXServiceURL(jmxUrl);

        if (!useAgent)
            System.out.println("Connecting to JMX with " + url);
        else
            System.out.println("Connecting to JmxAgent with " + url.getHost() + ":" + url.getPort());

        final JMXConnector jmxc = getJmxConnector(url, useAgent);
        final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        System.out.println("Bean count " + mbsc.getMBeanCount());
        System.out.println("Success!!");
    }

    public static JMXConnector getJmxConnector(final JMXServiceURL url,
                                               final boolean useAgent) throws IOException, NotBoundException {

        if (!useAgent) {
            return JMXConnectorFactory.connect(url);
        }
        else {
            final HashMap<String, Object> env = new HashMap<String, Object>();
            final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
            final Registry registry = LocateRegistry.getRegistry(url.getHost(), url.getPort(), csf);
            final String path = url.getURLPath();
            final String endpoint = path.contains("/") ? path.substring(path.lastIndexOf("/") + 1, path.length()) : "jmxrmi";
            final RMIServer stub = (RMIServer)registry.lookup(endpoint);
            final JMXConnector jmxc = new RMIConnector(stub, env);
            jmxc.connect(env);
            return jmxc;
        }
    }
}
