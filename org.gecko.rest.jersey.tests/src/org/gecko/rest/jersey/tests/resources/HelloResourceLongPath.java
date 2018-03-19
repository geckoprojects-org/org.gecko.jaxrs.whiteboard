/**
 * 
 */
package org.gecko.rest.jersey.tests.resources;

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
@Path("hello")
public class HelloResourceLongPath {
	
	@GET
	public Response getTest() {
		return Response.ok("Hello").build();
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	public Response getTestPost(String body) {
		return Response.ok(body + "_Hello").build();
	}

}
