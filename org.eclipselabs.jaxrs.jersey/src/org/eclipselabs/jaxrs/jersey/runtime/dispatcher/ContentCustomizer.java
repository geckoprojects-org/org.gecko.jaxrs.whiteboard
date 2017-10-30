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
package org.eclipselabs.jaxrs.jersey.runtime.dispatcher;

import java.util.logging.Logger;

import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Customizer
 * @author Mark Hoffmann
 * @since 24.10.2017
 */
public class ContentCustomizer<S, T> implements ServiceTrackerCustomizer<S, T> {
	
	private Logger logger = Logger.getLogger("o.e.o.j.contentCustomizer");
	private final JaxRsWhiteboardDispatcher dispatcher;
	private final BundleContext context;

	public ContentCustomizer(BundleContext context, JaxRsWhiteboardDispatcher dispatcher) {
		this.context = context;
		this.dispatcher = dispatcher;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public T addingService(ServiceReference<S> reference) {
		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<S> reference, T service) {
	}

}
