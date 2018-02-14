/**
 * Copyright (c) 2012 - 2016 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.tests.customizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Helper class that helps waiting for services to create, update or delete.
 * It adds support for blocking using {@link CountDownLatch}. Internally it uses the service tracker to
 * Track services
 * @author Mark Hoffmann
 * @param <T>
 * @since 10.11.2016
 */
public class ServiceChecker<T> {
	
	private int createCount = 1;
	private int modifyCount = 1;
	private int deleteCount = 1;
	private int createTimeout = 5;
	private int modifyTimeout = 5;
	private int deleteTimeout = 5;
	private CountDownLatch createLatch = null;
	private CountDownLatch modifyLatch = null;
	private CountDownLatch removeLatch = null;
	private ServiceTracker<T, T> tracker;
	private ServiceProviderCustomizer<T, T> customizer = null;
	private Class<T> serviceClass = null;;
	final private BundleContext context;
	private Filter filter = null;;
	
	
	public int getCreateCount() {
		return createCount;
	}

	public void setCreateCount(int createCount) {
		this.createCount = createCount;
	}

	public int getModifyCount() {
		return modifyCount;
	}

	public void setModifyCount(int modifyCount) {
		this.modifyCount = modifyCount;
	}

	public int getDeleteCount() {
		return deleteCount;
	}

	public void setDeleteCount(int deleteCount) {
		this.deleteCount = deleteCount;
	}

	public int getCreateTimeout() {
		return createTimeout;
	}

	public void setCreateTimeout(int createTimeout) {
		this.createTimeout = createTimeout;
	}

	public int getModifyTimeout() {
		return modifyTimeout;
	}

	public void setModifyTimeout(int modifyTimeout) {
		this.modifyTimeout = modifyTimeout;
	}

	public int getDeleteTimeout() {
		return deleteTimeout;
	}

	public void setDeleteTimeout(int deleteTimeout) {
		this.deleteTimeout = deleteTimeout;
	}

	public ServiceChecker(Class<T> serviceClass, BundleContext context) {
		this.serviceClass = serviceClass;
		this.context = context;
	}
	public ServiceChecker(String filter, BundleContext context) throws InvalidSyntaxException {
		this.filter = context.createFilter(filter);
		this.context = context;
	}
	
	public void start() {
		if (context == null || (serviceClass == null && filter == null)) {
			throw new IllegalStateException("Error checking service because service class and filter or bundle context is null");
		}
		createLatch = new CountDownLatch(getCreateCount());
		modifyLatch = new CountDownLatch(getModifyCount());
		removeLatch = new CountDownLatch(getDeleteCount());
		customizer = new ServiceProviderCustomizer<T, T>(context, createLatch, removeLatch, modifyLatch);
		if(serviceClass != null) {
			tracker = new ServiceTracker<T, T>(context, serviceClass, customizer);
		} else {
			tracker = new ServiceTracker<T, T>(context, filter, customizer);
		}
		tracker.open(true);
	}
	
	public void stop() {
		if (tracker != null) {
			tracker.close();
			tracker = null;
		}
		if (customizer != null) {
			customizer = null;
		}
		createLatch = null;
		modifyLatch = null;
		removeLatch = null;
	}
	
	public boolean waitCreate() throws InterruptedException {
		return createLatch.await(createTimeout, TimeUnit.SECONDS);
	}
	
	public boolean waitModify() throws InterruptedException {
		return modifyLatch.await(modifyTimeout, TimeUnit.SECONDS);
	}
	
	public boolean waitRemove() throws InterruptedException {
		return removeLatch.await(deleteTimeout, TimeUnit.SECONDS);
	}
	
	public int getCurrentCreateCount(boolean wait) {
		if (customizer != null) {
			if (wait) {
				try {
					waitCreate();
				} catch (InterruptedException e) {
				}
			}
			return customizer.getAddCount();
		} else {
			throw new IllegalStateException("Error, no start was called to initialize the checker");
		}
	}
	public int getCurrentModifyCount(boolean wait) {
		if (customizer != null) {
			if (wait) {
				try {
					waitModify();
				} catch (InterruptedException e) {
				}
			}
			return customizer.getModifyCount();
		} else {
			throw new IllegalStateException("Error, no start was called to initialize the checker");
		}
	}
	public int getCurrentRemoveCount(boolean wait) {
		if (customizer != null) {
			if (wait) {
				try {
					waitRemove();
				} catch (InterruptedException e) {
				}
			}
			return customizer.getRemoveCount();
		} else {
			throw new IllegalStateException("Error, no start was called to initialize the checker");
		}
	}

}
