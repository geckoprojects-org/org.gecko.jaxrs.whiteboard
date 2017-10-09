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

import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.JaxRsResourceProvider;
import org.eclipselabs.osgi.jersey.dto.DTOConverter;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A wrapper class for a JaxRs resources 
 * @author Mark Hoffmann
 * @since 09.10.2017
 */
public class JerseyResource implements JaxRsResourceProvider {

	private static final Logger logger = Logger.getLogger("jersey.resource");
	private final Object resource;
	private final Map<String, Object> properties;
	private final String name;
	private Filter applicationFilter = null;
	private Filter extensionFilter = null;
	private int status = NO_FAILURE;

	public JerseyResource(Object resource, Map<String, Object> properties) {
		this.resource = resource;
		this.properties = properties == null ? Collections.emptyMap() : properties;
		validateProperties();
		this.name = getResourceName(properties);
	}

	/**
	 * Returns <code>true</code>, if this resource is a singleton service
	 * @return <code>true</code>, if this resource is a singleton service
	 */
	public boolean isSingleton() {
		String scope = (String) properties.get("service.scope");
		if (!Constants.SCOPE_PROTOTYPE.equalsIgnoreCase(scope)) {
			return true;
		}
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#isResource()
	 */
	@Override
	public boolean isResource() {
		return status != INVALID;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#getName()
	 */
	@Override
	public String getName() {
		if (name == null) {
			throw new IllegalStateException("This resource provider does not contain a name, but should have one");
		}
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#getResourceProperties()
	 */
	@Override
	public Map<String, Object> getResourceProperties() {
		return properties;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#getResourceClass()
	 */
	@Override
	public Class<?> getResourceClass() {
		return resource == null ? null : resource.getClass();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#getResource()
	 */
	@Override
	public Object getResource() {
		return resource;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#canHandleApplication(org.eclipselabs.osgi.jersey.JaxRsApplicationProvider)
	 */
	@Override
	public boolean canHandleApplication(JaxRsApplicationProvider application) {
		if (applicationFilter != null) {
			try {
				boolean applicationMatch = applicationFilter.matches(application.getApplicationProperties());
				if (!applicationMatch && !application.isDefault()) {
					logger.log(Level.WARNING, "The given application select filter does not math to this application for this resource/extension: " + getName());
					return false;
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, "The given application select filter causes an error: " + applicationFilter, e);
				return false;
			}
		} else {
			if (!application.isDefault()) {
				logger.log(Level.WARNING, "There is no application select filter");
				return false;
			}
		}
		return true;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsResourceProvider#getResourceDTO()
	 */
	@Override
	public ResourceDTO getResourceDTO() {
		if (resource == null) {
			throw new IllegalStateException("This resource provider does not contain an resource, but should have one to get a DTO");
		}
		if (status == NO_FAILURE) {
			return DTOConverter.toResourceDTO(resource, properties);
		} else {
			return DTOConverter.toFailedResourceDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE : status);
		}
	}

	/**
	 * Returns the resource name or generates one
	 * @param properties the properties to get the name from
	 * @return the resource name or a generated one
	 */
	public static String getResourceName(Map<String, Object> properties) {
		String name = "." + UUID.randomUUID().toString();
		if (properties != null) {
			String resourceName = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
			if (resourceName != null) {
				name = resourceName;
			} else {
				name = properties.toString();
			}
		}
		return name;
	}

	/**
	 * Validates the application properties for required values and updates the DTO
	 * It first starts checking for required properties, then the appication target filter and extension select filter, if given.
	 */
	private void validateProperties() {
		updateStatus(NO_FAILURE);
		String resourceProp = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
		if (!Boolean.parseBoolean(resourceProp)) {
			logger.log(Level.WARNING, "The resource to add is not declared with the resource property: " + JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
			updateStatus(INVALID);
			return;
		}
		String filter = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
		if (filter != null) {
			try {
				applicationFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given application select filter is invalid: " + filter, e);
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

}
