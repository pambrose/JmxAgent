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

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * This StoppableAgent will start a secure RMI Connector Server using only
 * port "example.rmi.agent.port".  It puts in place an MBeanServerForwarder
 * that will make it possible to stop the server remotely.
 *
 * @author dfuchs
 */
public class JmxStoppableAgent {

    private JmxStoppableAgent() {
    }

    public static void agentmain(String agentArgs) throws IOException, MalformedObjectNameException {

        final String[] args = agentArgs.split(";");
        final List<String> ssl = Arrays.asList(Attach.SSL_PROPERTIES);
        for (String s : args) {
            final int eg = s.indexOf("=");
            if (eg < 0) throw new IllegalArgumentException(s);
            final String pn = s.substring(0, eg);
            final String pv = s.substring(eg + 1);
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

    public static void premain(String agentArgs) throws IOException, MalformedObjectNameException {

        JmxAgent.startAgent(agentArgs, true);
    }
}
