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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;

/**
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
@Path("whiteboard/async")
public class AsyncTestResource {

	private final Runnable	preResume;
	private final Runnable postResume;

	public AsyncTestResource(Runnable preResume, Runnable postResume) {
		this.preResume = preResume;
		this.postResume = postResume;
	}

	@GET
	@Path("{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public void echo(@Suspended AsyncResponse async,
			@PathParam("name") String value) {

		new Thread(() -> {
			try {
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
					preResume.run();
					async.resume(e);
					return;
				}
				preResume.run();
				async.resume(value);
			} finally {
				postResume.run();
			}
		}).start();
	}
}
