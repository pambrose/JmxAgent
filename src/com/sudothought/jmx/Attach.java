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

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Create a secure connector that can be accessed remotely through the
 * attach API. The process in which the connector is created must be local,
 * but after the connector is created it will be remotely accessible.
 * {@code [start <pid>]}
 * <p/>
 * The created connector can be stopped later {@code [stop]}.
 * <p/>
 * You can query whether the connector is still running using {@code [status]}.
 * <p/>
 * Use {@code [help]} for more info.
 *
 * @author dfuchs
 */
public class Attach {

    public final static String SEND_SSL_PROPERTIES =
            Attach.class.getPackage().getName() + ".ssl.config.send";

    /**
     * Analyze the System properties to build a JMXServiceURL that can
     * be use to connect to the remote server. See StoppableAgent and Stopper.
     *
     * @return
     * @throws java.net.UnknownHostException
     * @throws java.net.MalformedURLException
     */
    public static JMXServiceURL getUrlForClient()
            throws UnknownHostException, MalformedURLException {
        final String hostname = InetAddress.getLocalHost().getHostName();
        final int port = StoppableAgent.getServerPort();
        JMXServiceURL url =
                new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" +
                                  hostname + ":" + port + "/jmxrmi");
        return url;
    }

    /**
     * Connects using SSL. Used by stop() and status().
     *
     * @param url the server to connect to.
     * @return A connected JMXConnector.
     * @throws java.io.IOException
     */
    public static JMXConnector connect(JMXServiceURL url) throws IOException {
        final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put("com.sun.jndi.rmi.factory.socket", csf);

        final JMXConnector c = JMXConnectorFactory.connect(url, env);
        return c;
    }

    final static String[] SSL_PROPERTIES = {
            "javax.net.ssl.keyStore", "javax.net.ssl.keyStorePassword",
            "javax.net.ssl.trustStore", "javax.net.ssl.trustStorePassword"
    };


    /**
     * Builds the agentArgs string to use when attaching to the target
     * process. Used by start()
     *
     * @return
     */
    public static String getAgentArgs() {
        final StringBuilder builder = new StringBuilder();
        builder.append(Stopper.STOPPER_PROPERTY).append("=");
        builder.append(System.getProperty(Stopper.STOPPER_PROPERTY,
                                          Stopper.STOPPER_NAME));
        builder.append(";");
        builder.append(StoppableAgent.PORT_PROPERTY).append("=");
        builder.append(System.getProperty(StoppableAgent.PORT_PROPERTY,
                                          StoppableAgent.DEFAULT_PORT));
        final String sendssl = System.getProperty(SEND_SSL_PROPERTIES, "false");
        if (Boolean.valueOf(sendssl).booleanValue()) {
            // send keystore etc... to attached process... This is
            // usually not a good idea...
            for (String s : SSL_PROPERTIES) {
                final String v = System.getProperty(s);
                if (v != null && v.length() > 0)
                    builder.append(";").append(s).append("=").append(v);
            }
        }
        return builder.toString();
    }

    /**
     * List the process to which it might be possible to attach.
     *
     * @return running process list.
     */
    public static List<String> list() {
        final List<String> vmlist = new ArrayList<String>();
        for (VirtualMachineDescriptor vd : VirtualMachine.list()) {
            vmlist.add(vd.id());
        }
        System.out.println("Running VMs are: " + vmlist);
        return vmlist;
    }


    /**
     * Attach to the given PID, and starts a secure StoppableAgent.
     *
     * @param pid the pid to attach to.
     * @return the JMXServiceURL to use in order to connect to the created
     *         connector server.
     * @throws java.lang.Exception
     */
    public static JMXServiceURL start(String pid) throws Exception {

        // attach to the target application
        final VirtualMachine vm;
        try {
            vm = VirtualMachine.attach(pid);
        }
        catch (Exception x) {
            final List<String> vmlist = new ArrayList<String>();
            for (VirtualMachineDescriptor vd : VirtualMachine.list()) {
                vmlist.add(vd.id());
            }
            System.out.println("Can't attach to PID.\n\tRunning VMs are: " +
                               vmlist);
            throw new IllegalArgumentException(pid, x);
        }

        final URL agent =
                StoppableAgent.class.getProtectionDomain().
                        getCodeSource().getLocation();
        System.out.println("loading " + agent.getFile());
        vm.loadAgent(agent.getFile(), getAgentArgs());

        final SslRMIClientSocketFactory csf = new SslRMIClientSocketFactory();
        final Map<String, Object> env = new HashMap<String, Object>();
        env.put("com.sun.jndi.rmi.factory.socket", csf);

        // Check that we can connect
        final JMXServiceURL url = getUrlForClient();
        final JMXConnector c = JMXConnectorFactory.connect(url, env);
        c.close();

        return url;
    }


    /**
     * Stops a connector server created by start(). It is important to
     * give the same set of System properties when invoking start and stop.
     *
     * @throws java.io.IOException
     */
    public static void stop() throws IOException {
        final JMXServiceURL url = getUrlForClient();
        final JMXConnector c = connect(url);
        try {
            Stopper.stopServer(c);
            System.out.println("Server stopped");
        }
        finally {
            c.close();
        }
    }

    /**
     * Prints crude help string.
     */
    public static void help() {
        System.out.println(SYNTAX);
        list();
    }

    /**
     * {@code start <pid>} starts a secure connector server in {@code <pid>}
     * by loading the StoppableAgent class through the
     * attach API. The target process must have a default
     * truststore and keystore configured - otherwise
     * starting the connector server will fail.
     * <p/>
     * {@code status} checks whether the started connector is still running.
     * <p/>
     * {@code list} lists running java processes
     * <p/>
     * {@code stop} stops the connector server created by start.
     * <p/>
     * For all the above methods, the following System properties are needed:
     * {@code
     * -Djavax.net.ssl.keyStore=<keystore>
     * -Djavax.net.ssl.keyStorePassword=<password>  (if necessary)
     * -Djavax.net.ssl.trustStore=<truststore>
     * -Djavax.net.ssl.trustStorePassword=<trustword> (if necessary)
     * -Dexample.rmi.agent.port=<port> (will use default if omitted)
     * -Dexample.rmi.agent.stopper=<secretname> (will use default if omitted)
     * }
     * <p/>
     * {@code help} prints a crude help message.
     *
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String args[])
            throws Exception {
        if (args.length < 1) syntaxError(args, 0);

        if ("start".equals(args[0]) && args.length != 2)
            syntaxError(args, 1);
        if ("start".equals(args[0])) {
            JMXServiceURL url = start(args[1]);
            System.out.println("Server started at: " + url);
            return;
        }
        if ("stop".equals(args[0])) {
            stop();
            return;
        }
        if ("list".equals(args[0])) {
            list();
            return;
        }
        if ("help".equals(args[0])) {
            help();
            return;
        }
        if ("status".equals(args[0])) {
            final JMXServiceURL url = getUrlForClient();
            try {
                final JMXConnector c = connect(url);
                final Integer i = c.getMBeanServerConnection().getMBeanCount();
                c.close();
                System.out.println("Server running with " + i + " MBeans");
                return;
            }
            catch (Exception x) {
                System.err.println("Can't connect to server at "
                                   + url + ": " + x);
                throw x;
            }
        }
        syntaxError(args, 0);
    }

    private final static String SYNTAX = Attach.class.getSimpleName() + " {start <pid> | stop | status | list | help}";

    private static void syntaxError(String[] args, int i) {
        String msg;
        if (args.length < 1) {
            msg = "missing command. Syntax is " + SYNTAX;
        }
        else if (args.length > i) {
            msg = "unrecognized option: " + args[i] + ". Syntax is " + SYNTAX;
        }
        else {
            msg = "missing argument. Syntax is " + SYNTAX;
        }
        System.err.println(msg);
        throw new IllegalArgumentException(msg);
    }
}
