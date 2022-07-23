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

import org.gecko.rest.jersey.tests.resources.BoundExtension.NameBound;

/**
 * @author Stefan Bischof
 *
 */
@Path("/dtobound")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class BoundTestResource {

	@GET
	@Produces(MediaType.WILDCARD)
	@Path("bound")
	@NameBound
	public Response getTest() {
		return Response.ok("test").build();
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("unbound")
	public Response getTestPost(String body) {
		return Response.ok().build();
	}

}
