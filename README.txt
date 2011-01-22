# To generate keystore and truststore files
keytool -genkey -keyalg RSA -keysize 1024 -dname "CN=com.sudothought.jmx" -keystore ./ssl/jmx-agent.jks -storepass secret

# To start test server:
java -cp ./JmxAgent.jar \
-Djmx.agent.port=3434 \
-javaagent:./JmxAgent.jar \
-Djavax.net.ssl.keyStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.keyStorePassword=secret \
-Djavax.net.ssl.trustStore=./ssl/jmx-agent.jks \
-Djavax.net.ssl.trustStorePassword=secret \
-Djava.rmi.server.hostname=foo.com \
com.sudothought.jmx.TestServer

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

# For more details, see:
http://download.oracle.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html#CreateKeystore

# Related URLs
http://blogs.sun.com/jmxetc/entry/jmx_connecting_through_firewalls_using
http://blogs.sun.com/jmxetc/entry/troubleshooting_connection_problems_in_jconsole
http://www.bserban.org/2009/10/creating-a-secure-jmx-agent-in-jdk-1-5/

** Credit to Daniel Fuchs for the Agent code.