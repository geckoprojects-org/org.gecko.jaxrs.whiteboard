/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.eclipselabs.osgi.jaxrs.helper;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Helper class for JaxRs related stuff
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public class JaxRsHelper {

	/**
	 * Returns a servlet registration path from the given application. For that, the {@link ApplicationPath} annotation
	 * will be read. If present the value is taken and transformed into a valid Servlet spec format with
	 * leading '/' and trailing /* to make the resources work.
	 * If no application instance id given the default value /* is returned.
	 * @param application the JaxRs application instance
	 * @return the application path
	 */
	public static String getServletPath(Application application) {
		if (application != null) {
			ApplicationPath applicationPathAnnotation = application.getClass().getAnnotation(ApplicationPath.class);
			if (applicationPathAnnotation != null) {
				String applicationPath = applicationPathAnnotation.value();
				return toServletPath(applicationPath);
			}
		}
		return toServletPath(null);
	}

	/**
	 * Returns a servlet registration path from the given path. If the path value is <code>null</code>,
	 * the default /* is returned. If present the value is taken and transformed into a valid Servlet spec format with
	 * leading '/' and trailing /* to make the resources work.
	 * If no application instance id given the default value /* is returned.
	 * @param application the JaxRs application instance
	 * @return the application path
	 */
	public static String toServletPath(String path) {
		return "/" + toApplicationPath(path);
	}
	
	/**
	 * Returns a servlet registration path from the given path. If the path value is <code>null</code>,
	 * the default /* is returned. If present the value is taken and transformed into a valid Servlet spec format with
	 * leading '/' and trailing /* to make the resources work.
	 * If no application instance id given the default value /* is returned.
	 * @param application the JaxRs application instance
	 * @return the application path
	 */
	public static String toApplicationPath(String path) {
		String applicationPath = "*";
		if (path == null || path.isEmpty() || path.equals("/")) {
			return applicationPath;
		}
		applicationPath = path;
		if (applicationPath != null && !applicationPath.isEmpty()) {
			if (applicationPath.startsWith("/")) {
				applicationPath = applicationPath.substring(1, applicationPath.length());
			}
			if (!applicationPath.endsWith("/") && !applicationPath.endsWith("/*")) {
				applicationPath += "/*";
				
			}
			if (applicationPath.endsWith("/")) {
				applicationPath += "*";
			}
		}
		return applicationPath;
	}

}
