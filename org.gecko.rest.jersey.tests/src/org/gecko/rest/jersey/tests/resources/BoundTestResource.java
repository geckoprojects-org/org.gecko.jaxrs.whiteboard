/**
 * 
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
