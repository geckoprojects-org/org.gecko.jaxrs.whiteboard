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

import java.util.Map;

import javax.ws.rs.core.Application;

import org.eclipselabs.osgi.jaxrs.helper.JaxRsHelper;
import org.eclipselabs.osgi.jersey.DispatcherApplication;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;

/**
 * 
 * @author Mark Hoffmann
 * @since 04.09.2017
 */
public class JerseyDispatcherApplication implements DispatcherApplication {
	
	private String name;
	private String path;
	private Application application;
	
	public JerseyDispatcherApplication(String name, String path, Application jaxRsApplication) {
		this.name = name ;
		this.application = jaxRsApplication;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.DispatcherApplication#getName()
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
	 * @see org.eclipselabs.osgi.jersey.DispatcherApplication#getPath()
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
	 * @see org.eclipselabs.osgi.jersey.DispatcherApplication#getJaxRsApplication()
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
	 * @see org.eclipselabs.osgi.jersey.DispatcherApplication#getApplicationDTO()
	 */
	@Override
	public ApplicationDTO getApplicationDTO() {
		if (application == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to get a DTO");
		}
//		return DTOConverter.toApplicationDTO(this);
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.DispatcherApplication#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}

}
