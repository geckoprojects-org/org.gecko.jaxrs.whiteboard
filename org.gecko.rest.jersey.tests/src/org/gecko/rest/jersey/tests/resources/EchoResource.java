/**
 * Copyright (c) 2012 - 2020 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.tests.resources;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.util.promise.Promise;

/**
 * 
 * @author ilenia
 * @since Nov 24, 2020
 */
@Path("echo")
public class EchoResource {

	//	@GET
	//	@Path("body")
	//	public Response echoBody(@Context HttpHeaders headers, String body) {
	//		MediaType mediaType = headers.getMediaType();
	//		boolean returnOSGiText = mediaType.getType().equals("osgi")
	//				&& mediaType.getSubtype().equals("text");
	//		return Response.ok(body)
	//				.type(returnOSGiText ? mediaType : MediaType.TEXT_PLAIN_TYPE)
	//				.build();
	//	}

	@POST
	@Path("body")
	public Response echoBody(@Context HttpHeaders headers, String body) {
		MediaType mediaType = headers.getMediaType();
		boolean returnOSGiText = mediaType.getType().equals("osgi")
				&& mediaType.getSubtype().equals("text");
		return Response.ok(body)
				.type(returnOSGiText ? mediaType : MediaType.TEXT_PLAIN_TYPE)
				.build();
	}

	@GET
	@Path("header")
	@Produces(MediaType.TEXT_PLAIN)
	public Response echoHeader(@HeaderParam("echo") String echo) {
		return Response.ok(echo)
				.header("echo", echo)
				.type(MediaType.TEXT_PLAIN)
				.build();
	}

	@GET
	@Path("promise")
	@Produces(MediaType.TEXT_PLAIN)
	public void echoHeader(@Suspended AsyncResponse async,
			@HeaderParam("echo") Promise<String> echo) {

		echo.onSuccess(s -> async
				.resume(Response.ok(echo.getValue()).type(MediaType.TEXT_PLAIN).build()));
	}



}