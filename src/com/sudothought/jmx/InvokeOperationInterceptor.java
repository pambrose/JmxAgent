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

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * An interceptor that can be used to intercept an operation call on a (set of)
 * MBeans. Can only be used to intercept operations. get/set attribute calls
 * are not supported.
 * This class is intended to be subclassed in order to implement specific
 * behaviour. By default it acts as a pass-through.
 * Instances of this class (or of its subclasses) can then be added to
 * a {@link ForwardingInterceptor}.
 *
 * @author dfuchs
 */
public class InvokeOperationInterceptor {

    /**
     * The names of the MBeans on which the method call should be intercepted.
     * This can be a single MBean name, or a pattern. A null ObjectName is
     * equivalent to a wildcard.
     */
    public final ObjectName mbeanName;

    /**
     * The names of the method that is intercepted. This must be the exact
     * method name, or null for all methods.
     */
    public final String methodName;

    /**
     * Create a new InvokeOperationInterceptor.
     *
     * @param mbean  the MBean(s) on which the operations will be intercepted.
     * @param method the name of the operation to intercept (e.g. "stop"). Can
     *               only be used to intercept operations (MBeanServer.invoke).
     *               Intercepting get/set attribute calls is not supported.
     */
    public InvokeOperationInterceptor(ObjectName mbean, String method) {
        this.mbeanName = mbean;
        this.methodName = method;
    }

    /**
     * True if the given MBean is matched by this interceptor.
     *
     * @param name the candidate MBean.
     * @return true if the MBean is matched by this interceptor.
     */
    public boolean matches(ObjectName name) {
        if (mbeanName == null) {
            return true;
        }
        return mbeanName.apply(name);
    }

    /**
     * True if the given operation name is matched by this interceptor.
     *
     * @param name the candidate operation.
     * @return true if the operation name is matched by this interceptor.
     */
    public boolean matches(String method) {
        if (methodName == null) {
            return true;
        }
        return methodName.equals(method);
    }

    /**
     * Intercept the operation. By default acts as a pass through and
     * simply call {@code server.invoke(mbean, invoke, args, signature)}.
     * This behaviour can be changed by subclasses.
     *
     * @param server    the target server
     * @param mbean     the invoked mbean
     * @param invoke    the operation to invoke
     * @param args      the operation parameters
     * @param signature the operation signature
     * @return the operation result
     * @throws java.lang.Exception invoking the operation failed.
     */
    public Object intercept(MBeanServer server, ObjectName mbean,
                            String invoke, Object[] args, String[] signature) throws Exception {
        return server.invoke(mbean, invoke, args, signature);
    }

    /**
     * Return the ClassLoader that should be used to deserialize the operation
     * parameters for invoking the intercepted operation on the intercepted
     * MBeans.
     * By defaults calls {@code server.getClassLoaderFor(mbean)}.
     *
     * @param server the target server
     * @param mbean  the mbean on which the operation will be invoked.
     * @return
     * @throws javax.management.InstanceNotFoundException
     *
     */
    public ClassLoader getClassLoaderFor(MBeanServer server, ObjectName mbean)
            throws InstanceNotFoundException {
        return server.getClassLoaderFor(mbean);
    }
}
