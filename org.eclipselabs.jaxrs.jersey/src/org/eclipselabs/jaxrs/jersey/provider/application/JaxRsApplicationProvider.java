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
package org.eclipselabs.jaxrs.jersey.provider.application;

import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.provider.JaxRsConstants;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;

/**
 * Wrapper interface to provide an JaxRs application with all necessary properties
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public interface JaxRsApplicationProvider extends JaxRsProvider, JaxRsConstants {
	
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
	
	/**
	 * Returns <code>true</code>, if the application provider contains a legacy application.
	 * This applications are not further extensible.
	 * @return <code>true</code>, if the container contains a legacy application, otherwise <code>false</code>
	 */
	public boolean isLegacy();
	
	/**
	 * Returns <code>true</code>, if the application provider is the default application.
	 * @return <code>true</code>, if the application provider is the default application., otherwise <code>false</code>
	 */
	public boolean isDefault();
	
	/**
	 * Adds a new resource to the application provider. The call returns <code>true</code>,
	 * if adding was successful, otherwise <code>false</code>
	 * @param resource resource to add
	 * @param properties the resource service properties
	 */
	public boolean addResource(Object resource, Map<String, Object> properties);
	
	/**
	 * Removes a resource from the application provider. The call returns <code>true</code>,
	 * if removing was successful, otherwise <code>false</code>
	 * @param resource resource to be removed
	 * @param properties the resource service properties
	 */
	public boolean removeResource(Object resource, Map<String, Object> properties);
	
	/**
	 * Adds a new extension to the application provider. The call returns <code>true</code>,
	 * if adding was successful, otherwise <code>false</code>
	 * @param extension extension to add
	 * @param properties the resource service properties
	 */
	public boolean addExtension(Object extension, Map<String, Object> properties);
	
	/**
	 * Removes a extension from the application provider. The call returns <code>true</code>,
	 * if removing was successful, otherwise <code>false</code>
	 * @param extension resource to be removed
	 * @param properties the resource service properties
	 */
	public boolean removeExtension(Object extension, Map<String, Object> properties);

}
