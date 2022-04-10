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

import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.media.sse.internal.JerseySseEventSource;
import org.glassfish.jersey.media.sse.internal.JerseySseEventSource.Builder;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * 
 * @author stbischof
 * @since Apr 10, 2022
 */
@ServiceProvider(value = javax.ws.rs.sse.SseEventSource.Builder.class)
public class SseSourceBuilderService extends JerseySseEventSource.Builder {

	@Override
	public Builder target(WebTarget endpoint) {
		return super.target(endpoint);
	}
}