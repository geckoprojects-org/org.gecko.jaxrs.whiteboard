/**
 * 
 */
package org.eclipselabs.jaxrs.jersey.runtime.servlet;

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

	/**
	 * 
	 */
	private static final long serialVersionUID = 6509888299005723799L;

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
			if (config != null) {
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
			if (initialized.get()) {
				super.reload(configuration);
			} else {
				config = configuration;
			}
		} finally {
			lock.unlock();
		}
	}
}
