/**
 * Copyright (c) 2012 - 2020 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.runtime.common;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

import org.osgi.service.jaxrs.client.SseEventSourceFactory;

/**
 * 
 * @author ilenia
 * @since Jun 11, 2020
 */
public class SseEventSourceFactoryImpl implements SseEventSourceFactory {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.SseEventSourceFactory#newBuilder(javax.ws.rs.client.WebTarget)
	 */
	@Override
	public Builder newBuilder(WebTarget target) {		
		return SseEventSource.target(target);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.client.SseEventSourceFactory#newSource(javax.ws.rs.client.WebTarget)
	 */
	@Override
	public SseEventSource newSource(WebTarget target) {
		return SseEventSource.target(target).build();
	}

}
