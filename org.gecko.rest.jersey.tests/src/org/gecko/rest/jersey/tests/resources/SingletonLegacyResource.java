/**
 * 
 */
package org.gecko.rest.jersey.tests.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @author mark
 *
 */
@Path("/singleton/hello")
public class SingletonLegacyResource {
	
	@GET
	@Path("mark")
	public Response getTest() {
		return Response.ok("Hello Mark").build();
	}

}
