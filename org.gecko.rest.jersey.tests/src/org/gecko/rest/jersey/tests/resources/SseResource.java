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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

/**
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
@Path("whiteboard/stream")
public class SseResource {

	@Context
	Sse						sse;

	private final MediaType	type;

	public SseResource(MediaType type) {
		this.type = type;
	}

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void stream(@Context SseEventSink sink) {
		new Thread(() -> {

			CompletionStage< ? > cs = CompletableFuture.completedFuture(null);

			try {
				for (int i = 0; i < 10; i++) {
					Thread.sleep(500);
					cs = cs.thenCombine(sink.send(sse.newEventBuilder()
							.data(i)
							.mediaType(type)
							.build()), (a, b) -> null);
				}
			} catch (Exception e) {
				e.printStackTrace();
				cs = cs.thenCombine(
						sink.send(sse.newEvent("error", e.getMessage())),
						(a, b) -> null);
			}
			cs.whenComplete((a, b) -> sink.close());
		}).start();
	}
}
