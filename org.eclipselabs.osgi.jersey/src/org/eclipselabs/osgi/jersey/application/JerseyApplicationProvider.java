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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jaxrs.helper.JaxRsHelper;
import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.dto.DTOConverter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Implementation of the Application Provider
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public class JerseyApplicationProvider implements JaxRsApplicationProvider {

	private static final Logger logger = Logger.getLogger("jersey.applicationDispatcher");
	private final String name;
	private final Map<String, Object> properties;
	private Application application;
	private ServletContainer applicationContainer;
	private String applicationBase;
	private Filter whiteboardFilter = null;
	private Filter extensionFilter = null;
	private int status = NO_FAILURE;

	public JerseyApplicationProvider(String name, Application jaxRsApplication, String basePath) {
		this.name = name ;
		this.application = jaxRsApplication;
		this.applicationBase = basePath;
		if (basePath == null || basePath.isEmpty()) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		this.properties = Collections.emptyMap();
	}

	public JerseyApplicationProvider(Application jaxRsApplication, Map<String, Object> properties) {
		this.application = jaxRsApplication;
		this.properties = properties;
		// first validate all properties
		validateProperties();
		// create name after validation, because some fields are needed eventually
		this.name = getApplicationName(properties);
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
		return applicationBase == null ? null : JaxRsHelper.toApplicationPath(applicationBase);
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
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#getApplicationProperties()
	 */
	@Override
	public Map<String, Object> getApplicationProperties() {
		return properties;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#canHandleWhiteboard(java.util.Map)
	 */
	@Override
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties) {
		// in case the application status is invalid, this application cannot be handled
		if (status != NO_FAILURE) {
			return false;
		}
		if (whiteboardFilter == null) {
			return true;
		}
		runtimeProperties = runtimeProperties == null ? Collections.emptyMap() : runtimeProperties;
		boolean match = whiteboardFilter.matches(runtimeProperties);
		if (!match) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
		}
		return match;
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
		if (status == NO_FAILURE) {
			return DTOConverter.toApplicationDTO(this);
		} else {
			return DTOConverter.toFailedApplicationDTO(this, status);
		}
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

	/**
	 * Validates the application properties for required values and updates the DTO
	 * It first starts checking for required properties, than the whiteboard target filter and extension select filter, if given.
	 */
	private void validateProperties() {
		updateStatus(NO_FAILURE);
		String baseProperty = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE);
		if (applicationBase == null && baseProperty == null) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			return;
		}
		if (baseProperty != null) {
			applicationBase = baseProperty;
		}
		String filter = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET);
		if (filter != null) {
			try {
				whiteboardFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given whiteboard target filter is invalid: " + filter, e);
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
				return;
			}
		}
		filter = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_EXTENSION_SELECT);
		if (filter != null) {
			try {
				extensionFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given extension select filter is invalid: " + filter, e);
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
				return;
			}
		}
	}

	/**
	 * Updates the status. 
	 * @param newStatus
	 */
	private void updateStatus(int newStatus) {
		if (newStatus == status) {
			return;
		}
		if (status == NO_FAILURE) {
			status = newStatus;
		}
	}

	/**
	 * Returns the application name or generates one
	 * @param properties the properties to get the name from
	 * @return the application name or a generated one
	 */
	public static String getApplicationName(Map<String, Object> properties) {
		String name = null;
		if (properties != null) {
			String baseProperty = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE);
			name = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
			if (name == null && baseProperty != null) {
				name = "." + baseProperty;
			}
		}
		return name == null ? "." + UUID.randomUUID().toString() : name;
	}

}
