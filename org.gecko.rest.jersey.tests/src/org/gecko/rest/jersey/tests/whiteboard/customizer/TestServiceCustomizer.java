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
package org.gecko.rest.jersey.tests.whiteboard.customizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Customizer that tracks the Mongo providers
 * Each call of add, modify or remove will be counted. Further a {@link CountDownLatch} can be given,
 * to ensure to wait till the service is available
 * @author Mark Hoffmann
 * @since 16.06.2016
 */
public class TestServiceCustomizer<S, T>
		implements ServiceTrackerCustomizer<S, T> {
	
	private AtomicInteger addCount = new AtomicInteger();
	private AtomicInteger modifyCount = new AtomicInteger();
	private AtomicInteger removeCount = new AtomicInteger();
	private final BundleContext context;
	private final CountDownLatch createLatch;
	private final CountDownLatch removeLatch;
	private final CountDownLatch modifyLatch;
	
	public TestServiceCustomizer(BundleContext context, CountDownLatch createLatch) {
		this.context = context;
		this.createLatch = createLatch;
		this.removeLatch = null;
		this.modifyLatch = null;
	}
	
	public TestServiceCustomizer(BundleContext context, CountDownLatch createLatch, CountDownLatch removeLatch) {
		this.context = context;
		this.createLatch = createLatch;
		this.removeLatch = removeLatch;
		this.modifyLatch = null;
	}
	
	public TestServiceCustomizer(BundleContext context, CountDownLatch createLatch, CountDownLatch removeLatch, CountDownLatch modifyLatch) {
		this.context = context;
		this.createLatch = createLatch;
		this.removeLatch = removeLatch;
		this.modifyLatch = modifyLatch;
	}
	
	public int getAddCount() {
		return addCount.get();
	}
	
	public int getRemoveCount() {
		return removeCount.get();
	}
	
	public int getModifyCount() {
		return modifyCount.get();
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T addingService(ServiceReference<S> reference) {
		if (createLatch != null) {
			createLatch.countDown();
		}
		addCount.incrementAndGet();
		return (T)context.getService(reference);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<S> reference, T service) {
		if (modifyLatch != null) {
			modifyLatch.countDown();
		}
		modifyCount.incrementAndGet();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<S> reference, T service) {
		if (removeLatch != null) {
			removeLatch.countDown();
		}
		removeCount.incrementAndGet();
	}

}
