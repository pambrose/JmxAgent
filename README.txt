# To generate keystore and truststore files
keytool -genkey -keyalg RSA -keysize 1024 -dname "CN=com.sudothought.jmx" -keystore ./ssl/jmx-agent.jks -storepass secret

# For agent attached on server launch
=====================================
# To start test server:
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

# To connect with test client
java -cp ./JmxAgent.jar \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
com.sudothought.jmx.TestClient \
 -url:service:jmx:rmi://foo.com:3434/jndi/rmi://foo.com:3434/jmxrmi

# To connect with jconsole
jconsole \
-J-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-J-Djavax.net.ssl.trustStorePassword=secret \
foo.com:3434


# For start/stop agent
=====================================

# To start the test server
java -cp ./JmxStoppableAgent.jar \
-Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.keyStorePassword=secret \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
com.sudothought.jmx.TestServer

# To attach
java  -cp ./JmxStoppableAgent.jar \
-Djmx.agent.port=3434 \
-Djmx.agent.stopper=secret \
-Djava.rmi.server.hostname=foo.com \
-Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.keyStorePassword=secret \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
com.sudothought.jmx.Attach start PID

# To check status
java  -cp ./JmxStoppableAgent.jar \
-Djmx.agent.port=3434 \
-Djmx.agent.stopper=secret \
-Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.keyStorePassword=secret \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
com.sudothought.jmx.Attach status

# To detach
java  -cp ./JmxStoppableAgent.jar \
-Djmx.agent.port=3434 \
-Djmx.agent.stopper=secret \
-Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.keyStorePassword=secret \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
com.sudothought.jmx.Attach stop PID

# To connect with jconsole
jconsole \
-J-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-J-Djavax.net.ssl.trustStorePassword=secret \
foo.com:3434



# For more details, see:
http://download.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore

# Related URLs
http://blogs.sun.com/jmxetc/entry/jmx_connecting_through_firewalls_using
http://blogs.sun.com/jmxetc/entry/troubleshooting_connection_problems_in_jconsole
http://blogs.sun.com/jmxetc/entry/building_a_remotely_stoppable_connector
http://www.bserban.org/2009/10/creating-a-secure-jmx-agent-in-jdk-1-5/

** Credit goes to Daniel Fuchs for the Agent code.