/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.sudothought.jmx;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This StoppableAgent will start a secure RMI Connector Server using only
 * port "example.rmi.agent.port".  It puts in place an MBeanServerForwarder
 * that will make it possible to stop the server remotely.
 *
 * @author dfuchs
 */
public class StoppableAgent {
    public static final String PORT_PROPERTY = "example.rmi.agent.port";
    public static final String DEFAULT_PORT  = "3412";

    private StoppableAgent() {
    }

    public static int getServerPort() {
        final int port = Integer.parseInt(
                System.getProperty(PORT_PROPERTY, DEFAULT_PORT));
        return port;
    }

    public static void agentmain(String agentArgs)
            throws IOException, MalformedObjectNameException {
        String[] args = agentArgs.split(";");
        final List<String> ssl = Arrays.asList(Attach.SSL_PROPERTIES);
        for (String s : args) {
            final int eg = s.indexOf("=");
            if (eg < 0) throw new IllegalArgumentException(s);
            String pn = s.substring(0, eg);
            String pv = s.substring(eg + 1);
            if (!ssl.contains(pn)) {
                System.setProperty(pn, pv);
            }
            else {
                // if it's an SSL property - only set it if it's not already
                // set...
                final String v = System.getProperty(pn);
                if (v == null || v.length() == 0)
                    System.setProperty(pn, pv);
            }
        }
        premain(agentArgs);
    }

    public static void premain(String agentArgs)
            throws IOException, MalformedObjectNameException {

        // Ensure cryptographically strong random number generator used
        // to choose the object number - see java.rmi.server.ObjID
        //
        System.setProperty("java.rmi.server.randomIDs", "true");

        // Start an RMI registry on port specified by example.rmi.agent.port
        // (default 3412).
        //
        final int port = getServerPort();

        System.out.println("Create RMI registry on port " + port);

        // We create a couple of SslRMIClientSocketFactory and
        // SslRMIServerSocketFactory. We will use the same factories to export
        // the RMI Registry and the JMX RMI Connector Server objects. This
        // will allow us to use the same port for all the exported objects.
        // If we didn't use the same factories everywhere, we would have to
        // use at least two ports, because two different RMI Socket Factories
        // cannot share the same port.
        //
        final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        final SslRMIServerSocketFactory ssf = new SslRMIServerSocketFactory();

        // Create the RMI Registry using the SSL socket factories above.
        // In order to use a single port, we must use these factories
        // everywhere, or nowhere. Since we want to use them in the JMX
        // RMI Connector server, we must also use them in the RMI Registry.
        // Otherwise, we wouldn't be able to use a single port.
        //
        final Registry registry = LocateRegistry.createRegistry(port, csf, ssf);

        // Retrieve the PlatformMBeanServer.
        //
        System.out.println("Get the platform's MBean server");
        final MBeanServer mbs =
                ManagementFactory.getPlatformMBeanServer();

        // Environment map.
        //
        System.out.println("Initialize the environment map");
        HashMap<String, Object> env = new HashMap<String, Object>();

        // Now specify the SSL Socket Factories - use the same factories
        // everywhere!
        //
        // For the client side (remote)
        //
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);

        // For the server side (local)
        //
        env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, ssf);

        // For binding the JMX RMI Connector Server with the registry
        // created above:
        //
        env.put("com.sun.jndi.rmi.factory.socket", csf);

        // Create an RMI connector server.
        //
        // As specified in the JMXServiceURL the RMIServer stub will be
        // registered in the RMI registry running in the local host on
        // port 3000 with the name "jmxrmi". This is the same name the
        // out-of-the-box management agent uses to register the RMIServer
        // stub too.
        //
        // The port specified in "service:jmx:rmi://"+hostname+":"+port
        // is the second port, where RMI connection objects will be exported.
        // Here we use the same port as that we choose for the RMI registry.
        // The port for the RMI registry is specified in the second part
        // of the URL, in "rmi://"+hostname+":"+port
        //
        System.out.println("Create an RMI connector server");
        final String hostname = InetAddress.getLocalHost().getHostName();
        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi://" + hostname +
                                  ":" + port + "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi");

        System.out.println("creating server with URL: " + url);

        // Now create the server from the JMXServiceURL
        //
        final JMXConnectorServer cs =
                JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        cs.setMBeanServerForwarder(Stopper.createForwarderFor(cs, registry));
        System.out.println("Stopper ready for: " +
                           Stopper.getDefaultStopperName());

        // Start the RMI connector server.
        //
        System.out.println("Start the RMI connector server on port " + port);
        cs.start();
        System.out.println("Server started at: " + cs.getAddress());
    }
}
