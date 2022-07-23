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
