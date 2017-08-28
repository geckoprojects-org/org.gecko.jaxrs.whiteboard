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
package org.eclipselabs.osgi.jersey.application;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jaxrs.helper.JaxRsHelper;
import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.dto.DTOConverter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;

/**
 * Implementation of the Application Provider
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public class JerseyApplicationProvider implements JaxRsApplicationProvider {
	
	private String name;
	private String path;
	private Application application;
	private ServletContainer applicationContainer;

	public JerseyApplicationProvider(String name, String path, Application jaxRsApplication) {
		this.name = name ;
		this.application = jaxRsApplication;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#setServletContainer(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	public void setServletContainer(ServletContainer applicationContainer) {
		this.applicationContainer = applicationContainer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getServletContainer()
	 */
	@Override
	public ServletContainer getServletContainer() {
		return applicationContainer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getName()
	 */
	@Override
	public String getName() {
		if (name == null) {
			throw new IllegalStateException("This application provider does not contain a name, but should have one");
		}
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getPath()
	 */
	@Override
	public String getPath() {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to create a context path");
		}
		return JaxRsHelper.getServletPath(application);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getJaxRsApplication()
	 */
	@Override
	public Application getJaxRsApplication() {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to return an application");
		}
		return application;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getApplicationDTO()
	 */
	@Override
	public ApplicationDTO getApplicationDTO() {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to get a DTO");
		}
		return DTOConverter.toApplicationDTO(this);
	}
	
	/**
	 * Adds a service reference for a resource to the default application
	 * @param resourceRef the reference to register
	 */
	public void addResourceReference(ServiceReference<?> resourceRef) {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to add a resource reference");
		}
		if (application instanceof JerseyApplication) {
			((JerseyApplication)application).addResourceReference(resourceRef);
		}
	}
	
	/**
	 * Removed a resource service reference from the default application
	 * @param resourceRef the service reference of the resource to be removed
	 */
	public void removeResourceReference(ServiceReference<?> resourceRef) {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to remove a resource reference");
		}
		if (application instanceof JerseyApplication) {
			((JerseyApplication)application).removeResourceReference(resourceRef);
		}
	}
	
	/**
	 * Adds a service reference for a extension to the default application
	 * @param extensionRef the reference to register
	 */
	public void addExtensionReference(ServiceReference<?> extensionRef) {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to add a resource reference");
		}
		if (application instanceof JerseyApplication) {
			((JerseyApplication)application).addResourceReference(extensionRef);
		}
	}
	
	/**
	 * Removed a extension service reference from the default application
	 * @param extensionRef the service reference of the extension to be removed
	 */
	public void removeExtensionReference(ServiceReference<?> extensionRef) {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to remove a resource reference");
		}
		if (application instanceof JerseyApplication) {
			((JerseyApplication)application).removeResourceReference(extensionRef);
		}
	}
	
	/**
	 * Sets an application instance
	 * @param application the application to set
	 */
	protected void setApplication(Application application) {
		if (application == null) {
			throw new IllegalArgumentException("The application argument must not be null");
		}
		this.application = application;
	}

}
