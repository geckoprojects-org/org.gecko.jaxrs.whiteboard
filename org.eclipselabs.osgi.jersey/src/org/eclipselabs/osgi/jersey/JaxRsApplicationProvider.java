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

import javax.ws.rs.core.Application;

import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;

/**
 * Wrapper interface to provide an JaxRs application with all neccessary properties
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public interface JaxRsApplicationProvider {
	
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
	 * Returns the {@link ApplicationDTO} for this JaxRsApplication
	 * @return the {@link ApplicationDTO} for this JaxRsApplication
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
