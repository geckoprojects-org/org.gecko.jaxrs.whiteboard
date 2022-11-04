/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 *     Stefan Bishof - API and implementation
 *     Tim Ward - implementation
 */
package org.gecko.rest.jersey.provider.application;

import static java.util.Objects.isNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gecko.rest.jersey.provider.JaxRsConstants;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

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
	private String id;
	private Long serviceId;
	private Integer serviceRank;
	private int status = NO_FAILURE;
	private Filter whiteboardTargetFilter;
	private List<Filter> extensionFilters = new LinkedList<>();
	private T providerObject;

	public AbstractJaxRsProvider(T providerObject, Map<String, Object> properties) {
		this.properties = properties == null ? Collections.emptyMap() : properties;
		this.providerObject = providerObject;
		validateProperties();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.DTOProvider#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.DTOProvider#getServiceId()
	 */
	@Override
	public Long getServiceId() {
		return serviceId;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#getServiceRank()
	 */
	@Override
	public Integer getServiceRank() {
		return serviceRank;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#isFailed()
	 */
	@Override
	public boolean isFailed() {
		return getProviderStatus() != NO_FAILURE;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsProvider#getProviderProperties()
	 */
	@Override
	public Map<String, Object> getProviderProperties() {
		return Collections.unmodifiableMap(properties);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#requiresExtensions()
	 */
	@Override
	public boolean requiresExtensions() {
		return !extensionFilters.isEmpty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#canHandleWhiteboard(java.util.Map)
	 */
	@Override
	public boolean canHandleWhiteboard(Map<String, Object> runtimeProperties) {
		// in case the application status is invalid, this application cannot be handled
		if (getProviderStatus() != NO_FAILURE) {
			return false;
		}
		/* 
		 * Spec table 151.2: osgi.jaxrs.whiteboard.target: ... If this property is not specified,
		 * all JaxRs Whiteboards can handle this service
		 */
		if (whiteboardTargetFilter == null) {
			return true;
		}
		runtimeProperties = isNull(runtimeProperties)   ? Collections.emptyMap() : runtimeProperties;
		boolean match = whiteboardTargetFilter.matches(runtimeProperties);

		return match;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#getExtensionFilters()
	 */
	@Override
	public List<Filter> getExtensionFilters() {
		return extensionFilters;
	}
//
//	/**
//	 * Returns the {@link ServiceObjects} representing this resource
//	 * @return the {@link ServiceObjects} representing this resource
//	 */
//	public ServiceObjects<T> getServiceObjects() {
//		return serviceObjects;
//	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsProvider#getProviderObject()
	 */
	@SuppressWarnings("unchecked")
	public T getProviderObject() {
		return providerObject;
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
	protected String getProviderId() {
		Long serviceId = getServiceId();
		if (isNull(serviceId)) {
			return "." + UUID.randomUUID().toString();
		}
		return "sid_" + serviceId;
	}

	/**
	 * Returns the provider name. This method should always return a unique name for the content, so that there can many provider instance can exist.
	 * with same content, that can be identified by the name.
	 * @return the provider name
	 */
	protected String getProviderName() {
		String providerName = getProviderId();
		if (properties != null) {
			String jaxRsName = (String) properties.get(JaxrsWhiteboardConstants.JAX_RS_NAME);
			if (jaxRsName != null) {
				providerName = jaxRsName;
				if (jaxRsName.startsWith("osgi") || jaxRsName.startsWith(".")) {
					updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
				}
			}
		}
		return providerName;
	}

	/**
	 * Validates all properties which are usually the service properties. 
	 * It starts with the name and serviceId and delegates to custom implementations
	 */
	protected void validateProperties() {
		updateStatus(NO_FAILURE);
		serviceId = (Long) properties.get(Constants.SERVICE_ID);
		if (serviceId == null) {
			serviceId = (Long) properties.get(ComponentConstants.COMPONENT_ID);
		}
		id = getProviderId();
		name = getProviderName();
		Object sr = properties.get(Constants.SERVICE_RANKING);
		if (sr != null && sr instanceof Integer) {
			serviceRank = (Integer)sr;
		} else {
			serviceRank = Integer.valueOf(0);
		}
		if (serviceId == null) {
			serviceId = (Long) properties.get(ComponentConstants.COMPONENT_ID);
		}
		String filter = (String) properties.get(JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_TARGET);
		if (filter != null) {
			try {
				whiteboardTargetFilter = FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				logger.log(Level.SEVERE, "The given whiteboard target filter is invalid: " + filter);
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}
		Object filterObject = properties.get(JaxrsWhiteboardConstants.JAX_RS_EXTENSION_SELECT);
		String[] filters = null;
		if (filterObject instanceof String) {
			filters = new String[] {filterObject.toString()};
		} else if (filterObject instanceof String[]) {
			filters = (String[])filterObject;
		}
		if (filters != null) {
			for (String f : filters) {
				try {
					Filter extensionFilter = FrameworkUtil.createFilter(f);
					extensionFilters.add(extensionFilter);
				} catch (InvalidSyntaxException e) {
					logger.log(Level.SEVERE, "The given extension select filter is invalid: " + filter, e);
					extensionFilters.clear();
					updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
					break;
				}
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
		} else {
			if (newStatus != status) {
				status = newStatus;
			}
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public final int compareTo(JaxRsProvider o) {
		if (o == null) {
			throw new NullPointerException();
		}

		Long si = getServiceId();

		Long si2 = o.getServiceId();

		// service rank is equal -> same provider
		if(si != null && si.equals(si2)) {
			return 0;
		}


		Integer sr = getServiceRank();
		sr = sr == null ? 0 : sr;

		Integer sr2 = o.getServiceRank();
		sr2 = sr2 == null ? 0 : sr2;

		// Not the same so sort descending by service rank
		int r = sr2.compareTo(sr);

		if (r == 0) {
			// Same rank, so look for lowest service id
			if(si != null) {
				if(si2 != null) {
					return si.compareTo(si2);
				} else {
					// Fake services come last
					r = -1;
				}
			} else {
				if(si2 != null) {
					// Fake services come last
					r = 1;
				} else {
					r = getName().compareTo(o.getName());
				}
			}
		}		
		return r;
	}

}
