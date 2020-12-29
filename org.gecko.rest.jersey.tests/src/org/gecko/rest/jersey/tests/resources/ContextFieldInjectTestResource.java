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
package org.gecko.rest.jersey.tests.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

/**
 * 
 * @author ilenia
 * @since Jun 15, 2020
 */
@Path("whiteboard/context")
public class ContextFieldInjectTestResource {

	public static final String	CUSTOM_HEADER	= "customHeader";

	@Context
	private HttpHeaders headers;

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String headerReplay() {
		return headers.getHeaderString(CUSTOM_HEADER);
	}
	
}