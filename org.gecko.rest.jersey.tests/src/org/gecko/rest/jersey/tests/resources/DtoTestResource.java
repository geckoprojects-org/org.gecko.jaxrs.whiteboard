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

/**
 * @author Stefan Bischof
 *
 */
@Path("/dtores")
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
public class DtoTestResource {

	@GET
	@Produces(MediaType.WILDCARD)
	@Path("dtoget")
	public Response getTest() {
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("dtopost")
	public Response getTestPost(String body) {
		return Response.ok().build();
	}

}
