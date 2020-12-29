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
import javax.ws.rs.core.MediaType;

/**
 * 
 * @author ilenia
 * @since Jun 16, 2020
 */
@Path("whiteboard/string")
public class StringResource {

	private final String message;

	public StringResource(String message) {
		this.message = message;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getValues() {
		return message;
	}

	@GET
	@Path("length")
	@Produces(MediaType.TEXT_PLAIN)
	public int getLength() {
		return message.length();
	}
}
