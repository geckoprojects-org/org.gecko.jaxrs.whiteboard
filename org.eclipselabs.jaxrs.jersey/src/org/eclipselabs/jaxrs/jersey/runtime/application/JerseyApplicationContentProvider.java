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
package org.eclipselabs.jaxrs.jersey.runtime.application;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipselabs.jaxrs.jersey.provider.application.AbstractJaxRsProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationContentProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A wrapper class for a JaxRs resources 
 * @author Mark Hoffmann
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyApplicationContentProvider<T extends Object> extends AbstractJaxRsProvider<T> implements JaxRsApplicationContentProvider {

	private static final Logger logger = Logger.getLogger("jersey.rande");
	private Filter applicationFilter;
	private Filter extensionFilter;

	public JerseyApplicationContentProvider(T resource, Map<String, Object> properties) {
		super(resource, properties);
	}

	/**
	 * Returns <code>true</code>, if this resource is a singleton service
	 * @return <code>true</code>, if this resource is a singleton service
	 */
	public boolean isSingleton() {
		String scope = (String) getProviderProperties().get(Constants.SERVICE_SCOPE);
		if (!Constants.SCOPE_PROTOTYPE.equalsIgnoreCase(scope)) {
			return true;
		}
		return false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsRandEProvider#getObjectClass()
	 */
	@Override
	public Class<?> getObjectClass() {
		return getProviderObject() == null ? null : getProviderObject().getClass();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsRandEProvider#getObject()
	 */
	@Override
	public Object getObject() {
		return getProviderObject();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsRandEProvider#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return getProviderProperties();
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
	 * @see org.eclipselabs.jaxrs.jersey.provider.AbstractJaxRsProvider#doValidateProperties(java.util.Map)
	 */
	protected void doValidateProperties(Map<String, Object> properties) {
		String resourceProp = (String) properties.get(getJaxRsResourceConstant());
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
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return new String();
	}

}
