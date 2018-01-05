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
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.runtime.dto.ExtensionDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * A wrapper class for a JaxRs extensions 
 * @author Mark Hoffmann
 * @param <T>
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyExtensionProvider<T> extends JerseyApplicationContentProvider<T> implements JaxRsExtensionProvider {

	private static Class<?>[] contracts = null;
	
	public JerseyExtensionProvider(ServiceObjects<T> serviceObjects, Map<String, Object> properties) {
		super(serviceObjects, properties);
		// TODO: find the provided Contracts 
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
		int status = getProviderStatus();
		if (status == NO_FAILURE) {
			return DTOConverter.toExtensionDTO(this);
		} else {
			return DTOConverter.toFailedExtensionDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE : status);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider#getContracts()
	 */
	@Override
	public Class<?>[] getContracts() {
		return contracts;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return new JerseyExtensionProvider<T>(getProviderObject(), getProviderProperties());
	}
	
	/**
	 * Returns the {@link JaxRSWhiteboardConstants} for this resource type 
	 * @return the {@link JaxRSWhiteboardConstants} for this resource type
	 */
	protected String getJaxRsResourceConstant() {
		return JaxRSWhiteboardConstants.JAX_RS_EXTENSION;
	}

}
