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

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipselabs.jaxrs.jersey.provider.JaxRsConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * An abstract provider implementation. This provider is intended to have many instances for the same content.
 * As identifier the name should taken to be unique for the content.
 * @author Mark Hoffmann
 * @since 11.10.2017
 */
public abstract class AbstractJaxRsProvider<T> implements JaxRsProvider, JaxRsConstants {

	private static final Logger logger = Logger.getLogger("jersey.abstractProvider");
	private final Map<String, Object> properties;
	private String name;
	private Long serviceId = null;
	private int status = NO_FAILURE;
	private T object;
	private Filter whiteboardFilter;

	public AbstractJaxRsProvider(T object, Map<String, Object> properties) {
		this.object = object;
		this.properties = properties == null ? Collections.emptyMap() : properties;
		validateProperties();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.DTOProvider#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.DTOProvider#getServiceId()
	 */
	@Override
	public Long getServiceId() {
		return serviceId;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsProvider#getProviderProperties()
	 */
	@Override
	public Map<String, Object> getProviderProperties() {
		return Collections.unmodifiableMap(properties);
	}

	@Override
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties) {
		// in case the application status is invalid, this application cannot be handled
		if (getProviderStatus() != NO_FAILURE) {
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

	/**
	 * Sets a new provider object instance
	 * @param object the new instance to set
	 */
	protected void setProviderObject(T object) {
		this.object = object;
	}

	/**
	 * Returns the provider content
	 * @return the provider content
	 */
	protected T getProviderObject() {
		return object;
	}

	/**
	 * Returns the internal status of the provider
	 * @return the internal status of the provider
	 */
	protected int getProviderStatus() {
		return status;
	}

	/**
	 * Sets a new provider name
	 * @param name the name to set
	 */
	protected void setProviderName(String name) {
		this.name = name;
	}

	/**
	 * Returns the provider name. This method should always return a unique name for the content, so that there can many provider instance can exist.
	 * with same content, that can be identified by the name.
	 * @return the provider name
	 */
	protected String getProviderName() {
		String providerName = "." + UUID.randomUUID().toString();
		if (properties != null) {
			String jaxRsName = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
			if (jaxRsName != null) {
				providerName = jaxRsName;
			} else {
				providerName = properties.toString();
			}
		}
		return providerName;
	}
	
	/**
	 * Validates all properties which are usually the service properties. It starts with the name and serviceId and delegates to custom implementations
	 */
	protected void validateProperties() {
		updateStatus(NO_FAILURE);
		name = getProviderName();
		serviceId = (Long) properties.get(Constants.SERVICE_ID);
		if (serviceId == null) {
			serviceId = (Long) properties.get(ComponentConstants.COMPONENT_ID);
		}
		String filter = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET);
		if (filter != null) {
			try {
				whiteboardFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given whiteboard target filter is invalid: " + filter, e);
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}
		doValidateProperties(properties);
	}

	/**
	 * Validates the properties
	 * @param properties the properties to validate
	 */
	protected abstract void doValidateProperties(Map<String, Object> properties);

	/**
	 * Updates the status. This is an indicator for creating failed DTO's
	 * @param newStatus the new status to update
	 */
	protected void updateStatus(int newStatus) {
		if (newStatus == status) {
			return;
		}
		if (status == NO_FAILURE) {
			status = newStatus;
		}
	}

}
