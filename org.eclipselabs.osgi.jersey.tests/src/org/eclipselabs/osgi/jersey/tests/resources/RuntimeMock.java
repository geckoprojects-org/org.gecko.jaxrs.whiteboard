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
package org.eclipselabs.osgi.jersey.tests.resources;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime;
import org.osgi.service.component.annotations.Component;

/**
 * 
 * @author Mark Hoffmann
 * @since 28.08.2017
 */
@Component(name="JaxRsRuntimeMock", immediate=true, service=JaxRsJerseyRuntime.class)
public class RuntimeMock implements JaxRsJerseyRuntime {
	
	private List<JaxRsApplicationProvider> applications = new LinkedList<>();

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime#getName()
	 */
	@Override
	public String getName() {
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime#registerApplication(org.eclipselabs.osgi.jersey.JaxRsApplicationProvider)
	 */
	@Override
	public void registerApplication(JaxRsApplicationProvider provider) {
		applications.add(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime#unregisterApplication(org.eclipselabs.osgi.jersey.JaxRsApplicationProvider)
	 */
	@Override
	public void unregisterApplication(JaxRsApplicationProvider provider) {
		applications.remove(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsJerseyRuntime#reloadApplication(org.eclipselabs.osgi.jersey.JaxRsApplicationProvider)
	 */
	@Override
	public void reloadApplication(JaxRsApplicationProvider provider) {
		applications.add(provider);
	}
	
	public List<JaxRsApplicationProvider> getProviders() {
		return Collections.unmodifiableList(applications);
	}

}
