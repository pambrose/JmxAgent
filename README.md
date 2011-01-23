JmxAgent
========

Attaching an agent on server launch
--------

To start test server:
    java -cp ./JmxAgent.jar \
        -javaagent:./JmxAgent.jar \
        -Djmx.agent.port=3434 \
        -Djava.rmi.server.hostname=foo.com \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestServer

# The -Djava.rmi.server.hostname option is needed only with NAT

Attaching an agent to a running server
--------
To start the test server
    java -cp ./JmxStoppableAgent.jar \
        -Djava.rmi.server.hostname=foo.com \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestServer

To get list of running VMs
    java  -cp ./JmxStoppableAgent.jar com.sudothought.jmx.Attach list

To attach agent to running server
    java  -cp ./JmxStoppableAgent.jar:/usr/lib/jvm/java-6-sun/lib/tools.jar \
        -Djmx.agent.port=3434 \
        -Djmx.agent.stopper=secret \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.Attach start PID

To check status of attached agent
    java  -cp ./JmxStoppableAgent.jar \
        -Djmx.agent.port=3434 \
        -Djmx.agent.stopper=secret \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.Attach status

To detach agent from running server
    java  -cp ./JmxStoppableAgent.jar \
        -Djmx.agent.port=3434 \
        -Djmx.agent.stopper=secret \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.Attach stop PID


Connecting a client
--------
To connect with test client
    java -cp ./JmxAgent.jar \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestClient \
        -url:service:jmx:rmi://foo.com:3434/jndi/rmi://foo.com:3434/jmxrmi

To connect with jconsole
    jconsole \
        -J-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -J-Djavax.net.ssl.trustStorePassword=secret \
        foo.com:3434

To generate keystore and truststore files
    keytool -genkey -keyalg RSA -keysize 1024 -dname "CN=com.sudothought.jmx" -keystore ./ssl/jmx-agent.jks -storepass secret

For more details on generating the keystore and truststore files, see:
http://download.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore

# Related URLs
http://blogs.sun.com/jmxetc/entry/jmx_connecting_through_firewalls_using
http://blogs.sun.com/jmxetc/entry/troubleshooting_connection_problems_in_jconsole
http://blogs.sun.com/jmxetc/entry/building_a_remotely_stoppable_connector
http://www.bserban.org/2009/10/creating-a-secure-jmx-agent-in-jdk-1-5/

** Credit goes to Daniel Fuchs for this code.