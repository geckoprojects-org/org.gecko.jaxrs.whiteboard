/**
 * Copyright (c) 2012 - 2017 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.jetty;

import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

/**
 * Runnable to start a Jetty server in a different thread
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
public class JettyServerRunnable implements Runnable {
	
	private final Server server;
	private final int port;
	private CountDownLatch awaitStart;
	
	public JettyServerRunnable(Server server, int port, CountDownLatch awaitStart) {
		this.server = server;
		this.port = port;
		this.awaitStart = awaitStart;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if (server == null) {
			System.out.println("No server available to start");
			return;
		}
		try {
			server.start();
			System.out.println("Started Jersey server at port " + port + " successfully try http://localhost:" + port);
			awaitStart.countDown();
			server.addLifeCycleListener(new Listener() {
				
				@Override
				public void lifeCycleStopping(LifeCycle arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void lifeCycleStopped(LifeCycle arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void lifeCycleStarting(LifeCycle arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void lifeCycleStarted(LifeCycle arg0) {
					awaitStart.countDown();
				}
				
				@Override
				public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
					// TODO Auto-generated method stub
					
				}
			});
			server.join();
		} catch (Exception e) {
			System.out.println("Error starting Jersey server on port " + port);
		} finally {
			server.destroy();
		}
	}

}
