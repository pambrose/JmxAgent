JmxAgent
========

Attaching an non-stoppable agent on server launch
--------

Start a test server:
(The -Djava.rmi.server.hostname option is needed only with NAT)
    java -cp ./JmxAgent.jar \
        -javaagent:./JmxAgent.jar \
        -Djmx.agent.port=3434 \
        -Djava.rmi.server.hostname=foo.com \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestServer


Attaching a stoppable agent to a running server
--------
Start a test server
    java -cp ./JmxStoppableAgent.jar \
        -Djava.rmi.server.hostname=foo.com \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestServer

Get list of running JVMs
    java -cp ./JmxStoppableAgent.jar com.sudothought.jmx.Attach list

Attach agent to running server
    java  -cp ./JmxStoppableAgent.jar:/usr/lib/jvm/java-6-sun/lib/tools.jar \
        -Djmx.agent.port=3434 \
        -Djmx.agent.stopper=secret \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.Attach start PID

Check status of attached agent
    java  -cp ./JmxStoppableAgent.jar \
        -Djmx.agent.port=3434 \
        -Djmx.agent.stopper=secret \
        -Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.keyStorePassword=secret \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.Attach status

Detach agent from running server
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
Connect with a test client
    java -cp ./JmxAgent.jar \
        -Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
        -Djavax.net.ssl.trustStorePassword=secret \
        com.sudothought.jmx.TestClient \
        -url=service:jmx:rmi://foo.com:3434/jndi/rmi://foo.com:3434/jmxrmi

Connect with jconsole
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

** All credit goes to Daniel Fuchs (http://blogs.sun.com/jmxetc/page/About) for this code.