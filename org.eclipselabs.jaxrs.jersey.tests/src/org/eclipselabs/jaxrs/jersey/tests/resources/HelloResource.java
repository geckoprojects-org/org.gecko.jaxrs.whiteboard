/**
 * 
 */
package org.eclipselabs.jaxrs.jersey.tests.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author mark
 *
 */
@Path("/")
public class HelloResource {
	
	@GET
	@Path("hello")
	public Response getTest() {
		return Response.ok("Hello").build();
	}

	@POST
	@Path("hello")
	@Consumes(MediaType.WILDCARD)
	public Response getTestPost(String body) {
		return Response.ok(body + "_Hello").build();
	}

}
