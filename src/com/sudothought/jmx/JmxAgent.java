package com.sudothought.jmx;

import javax.management.MBeanServer;
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
import java.util.HashMap;

/**
 * This CustomAgent will start an RMI Connector Server using only
 * port "example.rmi.agent.port".
 *
 * @author dfuchs
 */
public class JmxAgent {

    private JmxAgent() {
    }

    public static void premain(String agentArgs) throws IOException {

        // Ensure cryptographically strong random number generator used
        // to choose the object number - see java.rmi.server.ObjID
        System.setProperty("java.rmi.server.randomIDs", "true");

        // Start an RMI registry on port specified by example.rmi.agent.port
        // (default 3000).
        //
        final int port = Integer.parseInt(System.getProperty("jmx.agent.port", "3412"));
        //System.out.println("Create RMI registry on port " + port);

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
        LocateRegistry.createRegistry(port, csf, ssf);

        // Retrieve the PlatformMBeanServer.
        //
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Environment map.
        // Specify the SSL Socket Factories:
        final HashMap<String, Object> env = new HashMap<String, Object>();
        env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, csf);
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
        //System.out.println("Create an RMI connector server");
        final String hostname = InetAddress.getLocalHost().getHostName();
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostname +
                                                    ":" + port + "/jndi/rmi://" + hostname + ":" + port + "/jmxrmi");

        //System.out.println("Creating jmx proxy with URL: " + url);

        // Now create the server from the JMXServiceURL
        //
        final JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        // Start the RMI connector server.
        System.out.println("RMI connector starting on port: " + port);
        cs.start();
        System.out.println("Proxy started at: " + cs.getAddress());

        // Start the CleanThread daemon...
        final Thread clean = new CleanThread(cs);
        clean.start();
    }

    /**
     * The CleanThread daemon thread will wait until all non-daemon threads
     * are terminated, excluding those non-daemon threads that are kept alive
     * by the presence of a started JMX RMI Connector Server. When no other
     * non-daemon threads remain, it stops the JMX RMI Connector Server,
     * allowing the application to terminate gracefully.
     */
    public static class CleanThread extends Thread {
        private final JMXConnectorServer cs;

        public CleanThread(JMXConnectorServer cs) {
            super("JMX Agent Cleaner");
            this.cs = cs;
            setDaemon(true);
        }

        public void run() {
            boolean loop = true;
            try {
                while (loop) {
                    final Thread[] all = new Thread[Thread.activeCount() + 100];
                    final int count = Thread.enumerate(all);
                    loop = false;
                    for (int i = 0; i < count; i++) {
                        final Thread t = all[i];
                        // daemon: skip it.
                        if (t.isDaemon()) continue;
                        // RMI Reaper: skip it.
                        if (t.getName().startsWith("RMI Reaper")) continue;
                        if (t.getName().startsWith("DestroyJavaVM")) continue;
                        // Non daemon, non RMI Reaper: join it, break the for
                        // loop, continue in the while loop (loop=true)
                        loop = true;
                        try {
                            // Found a non-daemon thread. Wait for it.
                            System.out.println("Waiting on thread " + t.getName() + " [id=" + t.getId() + "]");
                            t.join();
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        break;
                    }
                }
                // We went through a whole for-loop without finding any thread
                // to join. We can close cs.
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            finally {
                try {
                    // if we reach here it means the only non-daemon threads
                    // that remain are reaper threads - or that we got an
                    // unexpected exception/error.
                    //
                    cs.stop();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
