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

import java.util.HashMap;
import java.util.Map;

import org.eclipselabs.jaxrs.jersey.dto.DTOConverter;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ResourceDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A wrapper class for a JaxRs resources 
 * @author Mark Hoffmann
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyResourceProvider<T extends Object> extends JerseyApplicationContentProvider<T> implements JaxRsResourceProvider {

	public JerseyResourceProvider(T resource, Map<String, Object> properties) {
		super(resource, properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsResourceProvider#isResource()
	 */
	@Override
	public boolean isResource() {
		return getProviderStatus() != INVALID;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsResourceProvider#getResourceDTO()
	 */
	@Override
	public ResourceDTO getResourceDTO() {
		if (getProviderObject() == null) {
			throw new IllegalStateException("This resource provider does not contain an resource, but should have one to get a DTO");
		}
		int status = getProviderStatus();
		if (status == NO_FAILURE) {
			return DTOConverter.toResourceDTO(this);
		} else {
			return DTOConverter.toFailedResourceDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE : status);
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Object resource = getProviderObject();
		Map<String, Object> properties = new HashMap<>(getProviderProperties());
		return new JerseyResourceProvider<Object>(resource, properties);
	}

	/**
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return JaxRSWhiteboardConstants.JAX_RS_RESOURCE;
	}

}
