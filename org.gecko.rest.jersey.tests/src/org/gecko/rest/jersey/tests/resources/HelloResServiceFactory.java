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
package org.gecko.rest.jersey.tests.resources;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * 
 * @author ilenia
 * @since Jun 15, 2020
 */
public class HelloResServiceFactory implements PrototypeServiceFactory<HelloResource>{
	
	Supplier<HelloResource> supplier;
	BiConsumer<ServiceRegistration<HelloResource>,HelloResource> destroyer;
	
	public HelloResServiceFactory(Supplier<HelloResource> supplier, BiConsumer<ServiceRegistration<HelloResource>,HelloResource> destroyer) {
		this.supplier = supplier;
		this.destroyer = destroyer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.PrototypeServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
	 */
	@Override
	public HelloResource getService(Bundle bundle, ServiceRegistration<HelloResource> registration) {
		return supplier.get();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.PrototypeServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
	 */
	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<HelloResource> registration, HelloResource service) {
		destroyer.accept(registration, service);
	}

}
