/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.provider.application;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.DestroyListener;
import org.gecko.rest.jersey.provider.JaxRsConstants;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.BaseApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.FailedApplicationDTO;

/**
 * Wrapper interface to provide an JaxRs application with all necessary properties
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public interface JaxRsApplicationProvider extends JaxRsProvider, JaxRsConstants, DestroyListener {
	
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
	public BaseApplicationDTO getApplicationDTO();
	
	/**
	 * Sets the {@link ServletContainer} instance, that represents an application in Jersey
	 * @param applicationContainer the application to set
	 */
	public void addServletContainer(ServletContainer applicationContainer);
	
	/**
	 * Returns the {@link List} of {@link ServletContainer}s of the application
	 * The List will never be null.
	 * @return the {@link List} of {@link ServletContainer}s of the application
	 */
	public List<ServletContainer> getServletContainers();
	
	/**
	 * Returns <code>true</code>, if the application provider is the default application.
	 * @return <code>true</code>, if the application provider is the default application., otherwise <code>false</code>
	 */
	public boolean isDefault();
	
	/**
	 * Returns <code>true</code>, if the application provider is shadowing the default application.
	 * @return <code>true</code>, if the application provider is shadowing the default application, otherwise <code>false</code>
	 */
	public boolean isShadowDefault();
	
	/**
	 * Returns <code>true</code>, if the application is empty and doesn't have a resource or extension
	 * @return <code>true</code>, if the application is empty and doesn't have a resource or extension
	 */
	public boolean isEmpty();
	
	/**
	 * Returns <code>true</code>, if the provider has changed since the last change reset 
	 * @return <code>true</code>, if the provider has changed since the last change reset
	 */
	public boolean isChanged();
	
	/**
	 * Marks the provider as unchanged
	 */
	public void markUnchanged();
	
	/**
	 * Adds a new resource to the application provider. The call returns <code>true</code>,
	 * if adding was successful, otherwise <code>false</code>
	 * @param provider resource provider to add
	 */
	public boolean addResource(JaxRsResourceProvider provider);
	
	/**
	 * Removes a resource from the application provider. The call returns <code>true</code>,
	 * if removing was successful, otherwise <code>false</code>
	 * @param provider resource provider to be removed
	 */
	public boolean removeResource(JaxRsResourceProvider provider);
	
	/**
	 * Adds a new extension to the application provider. The call returns <code>true</code>,
	 * if adding was successful, otherwise <code>false</code>
	 * @param provider extension provider to add
	 */
	public boolean addExtension(JaxRsExtensionProvider provider);
	
	/**
	 * Removes a extension from the application provider. The call returns <code>true</code>,
	 * if removing was successful, otherwise <code>false</code>
	 * @param provider extension provider to be removed
	 */
	public boolean removeExtension(JaxRsExtensionProvider provider);
	
	/**
	 * All registered {@link JaxRsApplicationContentProvider}
	 * @return a {@link Collection} of {@link JaxRsApplicationContentProvider}
	 */
	public Collection<JaxRsApplicationContentProvider> getContentProviers();

	/**
	 * Removes the given {@link ServletContainer}
	 * @param applicationContainer the {@link ServletContainer} to remove
	 */
	public void removeServletContainer(ServletContainer applicationContainer);
	
	/**
	 * Updates the application base property. section 151.6.1 JaxRs Whiteboard Specification
	 * @param applicationBase the property to update
	 */
	public void updateApplicationBase(String applicationBase);

}
