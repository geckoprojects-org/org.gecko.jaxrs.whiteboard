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
package org.gecko.rest.jersey.runtime.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.gecko.rest.jersey.dto.DTOConverter;
import org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceObjects;
import org.osgi.service.jaxrs.runtime.dto.BaseExtensionDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * A wrapper class for a JaxRs extensions 
 * @author Mark Hoffmann
 * @param <T>
 * @since 09.10.2017
 */
public class JerseyExtensionProvider<T> extends JerseyApplicationContentProvider<T> implements JaxRsExtensionProvider {

	private static final List<String> POSSIBLE_INTERFACES = Arrays.asList(new String[] {
		ContainerRequestFilter.class.getName(),
		ContainerResponseFilter.class.getName(),
		ReaderInterceptor.class.getName(),
		WriterInterceptor.class.getName(),
		MessageBodyReader.class.getName(),
		MessageBodyWriter.class.getName(),
		ContextResolver.class.getName(),
		ExceptionMapper.class.getName(),
		ParamConverterProvider.class.getName(),
		Feature.class.getName(),
		DynamicFeature.class.getName()
	});
	
	private Class<?>[] contracts = null;
	
	public JerseyExtensionProvider(ServiceObjects<T> serviceObjects, Map<String, Object> properties) {
		super(serviceObjects, properties);
		checkExtensionProperty(properties);
		extractContracts(properties);
		
	}
	
	/**
	 * If the ExtensionProvider does not advertise the property osgi.jaxrs.extension as true then it is not a 
	 * valid extenstion
	 * 
	 * @param properties
	 */
	private void checkExtensionProperty(Map<String, Object> properties) {
		if(!properties.containsKey(JaxrsWhiteboardConstants.JAX_RS_EXTENSION) || 
				properties.get(JaxrsWhiteboardConstants.JAX_RS_EXTENSION).equals(false)) {
			
			updateStatus(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE);
		}
		
	}

	private void extractContracts(Map<String, Object> properties) {
		String[] objectClasses = (String[]) properties.get(Constants.OBJECTCLASS);
		List<Class<?>> possibleContracts = new ArrayList<>(objectClasses.length);
		for (String objectClass : objectClasses) {
			if(POSSIBLE_INTERFACES.contains(objectClass)) {
				try {
					possibleContracts.add(Class.forName(objectClass));
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			} 
		}
		if(!possibleContracts.isEmpty()) {
			contracts = possibleContracts.toArray(new Class[0]);
		}
		else {
			updateStatus(DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE); //if possibleContracts is empty the extension should record a failure DTO
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsExtensionProvider#isExtension()
	 */
	@Override
	public boolean isExtension() {
		return (getProviderStatus() != INVALID) && (getProviderStatus() != DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE) ;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.JaxRsExtensionProvider#getExtensionDTO()
	 */
	@Override
	public BaseExtensionDTO getExtensionDTO() {
		int status = getProviderStatus();
		if (status == NO_FAILURE) {
			return DTOConverter.toExtensionDTO(this);
		} else {
			return DTOConverter.toFailedExtensionDTO(this, status == INVALID ? DTOConstants.FAILURE_REASON_NOT_AN_EXTENSION_TYPE : status);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.JaxRsExtensionProvider#getContracts()
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
		return JaxrsWhiteboardConstants.JAX_RS_EXTENSION;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.application.AbstractJaxRsProvider#updateStatus(int)
	 */
	@Override
	public void updateStatus(int newStatus) {
		super.updateStatus(newStatus);
	}

}
