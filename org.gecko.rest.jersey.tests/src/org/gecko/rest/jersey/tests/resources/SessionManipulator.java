/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gecko.rest.jersey.tests.resources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("whiteboard/session")
public class SessionManipulator {

	@GET
	@Path("{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getValue(@Context HttpServletRequest req,
			@PathParam("name") String name) {
		HttpSession session = req.getSession();
		System.out.println("Session in GET " + (session == null ? "null" : session.getId())  + " this is " + this);
//		String r = String.valueOf(req.getSession().getAttribute(name));
		String r = String.valueOf(session.getAttribute(name));
		System.out.println("Value of name in GET " + name);
		System.out.println("Value of parameter in GET " + r);
		return Response.ok(r).build();
	}

	@PUT
	@Path("{name}")
	public Response setValue(@Context HttpServletRequest req,
			@PathParam("name") String name, String body) {			
		HttpSession session = req.getSession();
		System.out.println("Session in PUT " + (session == null ? "null" : session.getId()) + " this is " + this);
		req.getSession().setAttribute(name, body);
		String r = String.valueOf(req.getAttribute(name));
		System.out.println("Value of parameter in PUT " + r + "; key: " + name);
		return Response.ok().build();
	}

}
