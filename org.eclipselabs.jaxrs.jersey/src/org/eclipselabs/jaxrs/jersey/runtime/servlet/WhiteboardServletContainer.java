/**
 * 
 */
package org.eclipselabs.jaxrs.jersey.runtime.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * @author jalbert
 *
 */
public class WhiteboardServletContainer extends ServletContainer {
	
	private ResourceConfig config = null;;

	private AtomicBoolean initialized = new AtomicBoolean();
	private ReentrantLock lock = new ReentrantLock();
	
	public WhiteboardServletContainer(ResourceConfig config) {
		super(config);
	}
	
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
		long start = System.currentTimeMillis();
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
		System.out.println("Reload took " + (System.currentTimeMillis() -start) + " ms");
	}
}
