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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author mark
 *
 */
@Path("/")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class HelloResource {
	
	@GET
	@Path("hello")
	public Response getTest() {
		return Response.ok("Hello").build();
	}

	@POST
	@Path("hello")
	public Response getTestPost(String body) {
		Response r = Response.ok(body + "_Hello").build();
		return r;
	}
	
	@POST
	@Path("replace")
	public Response postTest(String body) {
		Response r = Response.ok(body).build();
		return r;
	}

}
