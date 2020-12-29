/**
 * 
 */
package org.gecko.rest.jersey.tests.whiteboard.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
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

}
