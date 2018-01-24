/**
 * 
 */
package org.gecko.rest.jersey.runtime.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * @author jalbert
 *
 */
public class WhiteboardServletContainer extends ServletContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6509888299005723799L;

	private ResourceConfig config = null;;

	private AtomicBoolean initialized = new AtomicBoolean();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public WhiteboardServletContainer(ResourceConfig config) {
		super(config);
	}

	@Override
	public void init() throws ServletException {
		lock.writeLock().lock();
		try {
			super.init();
			initialized.set(true);
			if (config != null) {
				super.reload(config);
				config = null;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void reload(ResourceConfig configuration) {
		lock.writeLock().lock();
		try {
			if (initialized.get()) {
				super.reload(configuration);
			} else {
				config = configuration;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
	
	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		lock.readLock().lock();
		try {
			super.service(req, res);
		} finally {
			lock.readLock().unlock();
		}
		
	}
	
}
