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
package org.eclipselabs.osgi.jersey;

import java.util.Map;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;

/**
 * Wrapper interface to provide an JaxRs application with all neccessary properties
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public interface JaxRsApplicationProvider {
	
	static final int NO_FAILURE = -1;
	
	/**
	 * Returns the application name which targets to the property osgi.jaxrs.name
	 * @return the application name
	 */
	public String getName();
	
	/**
	 * Return the context path for this application
	 * @return the context path for this application
	 */
	public String getPath();
	
	/**
	 * Returns the JaxRs application instance
	 * @return the JaxRs application instance
	 */
	public Application getJaxRsApplication();
	
	/**
	 * Returns the application properties or an empty map
	 * @return the application properties or an empty map
	 */
	public Map<String, Object> getApplicationProperties();
	
	/**
	 * Returns <code>true</code>, if this application can handle the given properties.
	 * If the application contains a whiteboard target select, than the properties are checked against
	 * the select filter and returns the result.
	 * If the application has no whiteboard select filter, the method returns <code>true</code>
	 * @param runtimeProperties the properties of the whiteboard runtime
	 * @return <code>true</code>, if the application can be handled by a whiteboard runtime with the given properties
	 */
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties);
	
	/**
	 * Returns the {@link ApplicationDTO} for this JaxRsApplication.
	 * In case of an error a {@link FailedApplicationDTO} instance will be returned
	 * @return the {@link ApplicationDTO} or {@link FailedApplicationDTO} for this JaxRsApplication
	 */
	public ApplicationDTO getApplicationDTO();
	
	/**
	 * Sets the {@link ServletContainer} instance, that represents an application in Jersey
	 * @param applicationContainer the application to set
	 */
	public void setServletContainer(ServletContainer applicationContainer);
	
	/**
	 * Returns the servlet container of the application
	 * @return the servlet container of the application
	 */
	public ServletContainer getServletContainer();

}
