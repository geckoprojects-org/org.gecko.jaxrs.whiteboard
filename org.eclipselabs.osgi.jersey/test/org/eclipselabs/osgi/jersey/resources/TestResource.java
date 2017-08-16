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
package org.eclipselabs.osgi.jersey.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * 
 * @author Mark Hoffmann
 * @since 14.07.2017
 */
@Path("test")
@Produces({"xml", "json"})
public class TestResource {
	
	@POST
	@PUT
	@Produces("text")
	public Response postAndOut() {
		return Response.ok().build();
	}
	
	@POST
	@Path("pdf")
	@Consumes("pdf")
	@Produces("text")
	public Response postMe(String text) {
		return Response.ok().build();
	}

	protected String helloWorld() {
		return "hello world";
	}
}
