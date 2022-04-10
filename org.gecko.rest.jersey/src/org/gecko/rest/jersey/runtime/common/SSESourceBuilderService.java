/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
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

import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import javax.ws.rs.sse.SseEventSource.Builder;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * 
 * @author stbischof
 * @since Apr 10, 2022
 */
@ServiceProvider(value = javax.ws.rs.sse.SseEventSource.Builder.class)
public class SSESourceBuilderService extends javax.ws.rs.sse.SseEventSource.Builder {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.ws.rs.sse.SseEventSource.Builder#target(javax.ws.rs.client.WebTarget)
	 */
	@Override
	protected Builder target(WebTarget endpoint) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.sse.SseEventSource.Builder#reconnectingEvery(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public Builder reconnectingEvery(long delay, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.ws.rs.sse.SseEventSource.Builder#build()
	 */
	@Override
	public SseEventSource build() {
		// TODO Auto-generated method stub
		return null;
	}
}