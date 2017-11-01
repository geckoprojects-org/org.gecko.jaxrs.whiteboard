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
package org.eclipselabs.jaxrs.jersey.servlet;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * @author jalbert
 *
 */
public class WhiteboardServletContainer extends ServletContainer {
	
	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private ResourceConfig config = null;;

	private AtomicBoolean initialized = new AtomicBoolean();
	private ReentrantLock lock = new ReentrantLock();
	
	public WhiteboardServletContainer(ResourceConfig config) {
		super(config);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#init()
	 */
	@Override
	public void init() throws ServletException {
		lock.lock();
		try {
			super.init();
			initialized.set(true);
			if(config != null) {
				super.reload(config);
				config = null;
			}
		} finally {
			lock.unlock();
		}
	}
	
	@Override
	public void reload(ResourceConfig configuration) {
		lock.lock();
		try {
			if(initialized.get()) {
				super.reload(configuration);
			} else {
				config = configuration;
			}
		} finally {
			lock.unlock();
		}
	}

}
