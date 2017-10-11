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

import org.eclipselabs.jaxrs.jersey.dto.DTOConverter;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A wrapper class for a JaxRs extensions 
 * @author Mark Hoffmann
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyExtensionProvider<T extends Object> extends JerseyApplicationContentProvider<T> implements JaxRsExtensionProvider {

	public JerseyExtensionProvider(T resource, Map<String, Object> properties) {
		super(resource, properties);
	}
	
	/**
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return JaxRSWhiteboardConstants.JAX_RS_EXTENSION;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsExtensionProvider#isExtension()
	 */
	@Override
	public boolean isExtension() {
		return getProviderStatus() != INVALID;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.JaxRsExtensionProvider#getExtensionDTO()
	 */
	@Override
	public ExtensionDTO getExtensionDTO() {
		if (getProviderObject() == null) {
			throw new IllegalStateException("This extension provider does not contain an extension, but should have one to get a DTO");
		}
		int status = getProviderStatus();
		if (status == NO_FAILURE) {
			return DTOConverter.toExtensionDTO(this);
		} else {
			return DTOConverter.toFailedExtensionDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_SERVICE_NOT_GETTABLE : status);
		}
	}

}
