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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import javax.inject.Singleton;

/**
 * 
 * @author ilenia
 * @since Jun 12, 2020
 */
@Singleton
@Path("whiteboard/stream")
public class SseResource {

	@Context
	Sse	sse;

	@GET
	@Produces(MediaType.SERVER_SENT_EVENTS)
	public void stream(@Context SseEventSink sink) {
		System.out.println("SSE RESOURCE");
		new Thread(() -> {
			try {
				for (int i = 0; i < 10; i++) {
					Thread.sleep(100);
					System.out.println("SSE RESOURCE SEND " + i);
					final OutboundSseEvent event = sse.newEventBuilder()
							.name("message-to-client")
							.data(Integer.class, i)
							.build();
					sink.send(event);
				}
			} catch (Exception e) {
				e.printStackTrace();
				sink.send(sse.newEvent("error", e.getMessage()));
			} finally {
				System.out.println("SSE RESOURCE SINK CLOSE");
				sink.close();
			}
		}).start();
	}
}
