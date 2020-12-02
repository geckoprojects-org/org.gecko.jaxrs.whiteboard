/**
 * 
 */
package org.gecko.rest.jersey.runtime.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.gecko.rest.jersey.helper.DestroyListener;
import org.gecko.rest.jersey.runtime.common.ResourceConfigWrapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * As Wrapper for the {@link ServletContainer} that locks the Servlet while its configuration is reloaded.
 * Furthermore it takes care that a reload is done, if a new configuration comes available while it is initialized
 * @author Juergen Albert
 * @since 1.0
 */
public class WhiteboardServletContainer extends ServletContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6509888299005723799L;

	private ResourceConfig config = null;;

	private AtomicBoolean initialized = new AtomicBoolean();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private DestroyListener destroyListener;

	private ResourceConfigWrapper wrapper;

	public WhiteboardServletContainer(ResourceConfigWrapper config, DestroyListener destroyListener) {
		this(config.config, destroyListener);
		this.wrapper = config;
	}

	public WhiteboardServletContainer(ResourceConfig config, DestroyListener destroyListener) {
		super(config);
		this.destroyListener = destroyListener;
	}

	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#init()
	 */
	@Override
	public void init() throws ServletException {
		
		lock.writeLock().lock();
		try {
			super.init();
			initialized.set(true);
			if (config != null) {
				this.reload(config);
				wrapper.setInjectionManager(getApplicationHandler().getInjectionManager());
				config = null;
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#reload(org.glassfish.jersey.server.ResourceConfig)
	 */
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
	
	/* (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		lock.readLock().lock();
		try {
			super.service(request, response);
		} finally {
			lock.readLock().unlock();
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.glassfish.jersey.servlet.ServletContainer#destroy()
	 */
	@Override
	public void destroy() {
		super.destroy();
		if(destroyListener != null) {
			destroyListener.servletContainerDestroyed(this);
		}
	}

	/**
	 * @param config2
	 */
	public void reloadWrapper(ResourceConfigWrapper wrapper) {
		config = wrapper.config;
		this.wrapper = wrapper;
		if (!initialized.get()) {
			return;
		}
		reload(config);
		if(getApplicationHandler() != null) {
			wrapper.setInjectionManager(getApplicationHandler().getInjectionManager());
		}
	}
}
