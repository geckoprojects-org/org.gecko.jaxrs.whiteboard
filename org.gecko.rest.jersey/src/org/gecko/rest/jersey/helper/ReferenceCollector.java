/**
 * Copyright (c) 2012 - 2018 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.rest.jersey.helper;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.gecko.rest.jersey.helper.ServiceReferenceEvent.Type;
import org.gecko.rest.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * {@link PushStream} based service tracker customizer
 * @author Juergen Albert
 * @since 03.01.2018
 */
@Component(service=ReferenceCollector.class, immediate = true, enabled=true)
@SuppressWarnings("rawtypes")
public class ReferenceCollector implements ServiceTrackerCustomizer<Object, Object> {

	private static final Logger logger = Logger.getLogger("jersey.referenceCollector");
	private ServiceTracker<Object, Object> serviceTracker;
	private PushStreamProvider provider = new PushStreamProvider();
	private final Map<ServiceReference, ServiceReferenceEvent> contentReferences = new ConcurrentHashMap<>(); 
	private SimplePushEventSource<ServiceReferenceEvent> source; 

	private BundleContext context; 
	
	private Map<JaxRsWhiteboardDispatcher, PushStream<ServiceReferenceEvent>> dispatcherMap = new ConcurrentHashMap<>();
	
	/**
	 * Activated at component activation
	 * @param context the component context
	 * @throws InvalidSyntaxException
	 */
	@Activate
	public void activate(BundleContext context) throws InvalidSyntaxException {
		this.context = context;
		source = provider.buildSimpleEventSource(ServiceReferenceEvent.class).build();

		serviceTracker = new ServiceTracker<>(context, context.createFilter("(|(" + JaxrsWhiteboardConstants.JAX_RS_RESOURCE + "=true)(" + JaxrsWhiteboardConstants.JAX_RS_EXTENSION + "=true))"), this);
		serviceTracker.open();

	}

	/**
	 * Connects the {@link PushStream} with the JaxRs dispatcher to forward
	 * the services it
	 * @param dispatcher the dispatcher instance
	 */
	public void connect(final JaxRsWhiteboardDispatcher dispatcher){
		if (dispatcher == null) {
			throw new IllegalArgumentException("Dispatcher instance must not be null");
		}
		PushStream<ServiceReferenceEvent> pushStream = dispatcherMap.get(dispatcher);
		if(pushStream == null) {
			contentReferences.forEach((sr, sre) -> {
				if(sre.isExtension()) {
					handleExtensionReferences(dispatcher, sre);
				} else {
					handleResourceReferences(dispatcher, sre);
				}
			});
			
			pushStream = provider.buildStream(source).withScheduler(Executors.newScheduledThreadPool(1)).build();
			
			dispatcherMap.put(dispatcher, pushStream);
			
			pushStream = pushStream.buffer().distinct();
			
//			final Duration batchDuration = Duration.ofMillis(50);
			final Duration batchDuration = Duration.ofMillis(500);
			pushStream.window(batchDuration, sec -> sec).forEach(sec -> {
				sec.stream().filter(sre -> sre.isResource()).forEach(sre -> {
					handleResourceReferences(dispatcher, sre);
				});
				sec.stream().filter(sre -> sre.isExtension()).forEach(sre -> {
					handleExtensionReferences(dispatcher, sre);
				});
//				dispatcher.batchDispatch();
			});
		}
		
	}
	
	/**
	 * @param dispatcher
	 * @param sre
	 */
	@SuppressWarnings("unchecked")
	private void handleExtensionReferences(final JaxRsWhiteboardDispatcher dispatcher, ServiceReferenceEvent sre) {
		Map<String, Object> properties = JerseyHelper.getServiceProperties(sre.getReference());
		switch (sre.getType()) {
		case ADD:
		case MODIFY:
			logger.info("Handle extension " + sre.getType() + " properties: " + properties);
			ServiceObjects so = context.getServiceObjects(sre.getReference());
			dispatcher.addExtension(so, properties);
			break;
		default:
			dispatcher.removeExtension(properties);
			break;
		};
	}

	/**
	 * @param dispatcher
	 * @param sre
	 */
	@SuppressWarnings("unchecked")
	private void handleResourceReferences(final JaxRsWhiteboardDispatcher dispatcher, ServiceReferenceEvent sre) {
		Map<String, Object> properties = JerseyHelper.getServiceProperties(sre.getReference());
		switch (sre.getType()) {
		case ADD:
		case MODIFY:
			logger.info("Handle resource " + sre.getType() + " properties: " + properties);
			ServiceObjects so = context.getServiceObjects(sre.getReference());
			dispatcher.addResource(so, properties);
			break;
		default:
			dispatcher.removeResource(properties);
			break;
		};
	}

	/**
	 * Disconnects and closes the given {@link JaxRsWhiteboardDispatcher}s {@link PushStream}
	 * @param dispatcher the {@link JaxRsWhiteboardDispatcher} to disconnect
	 */
	public void disconnect(JaxRsWhiteboardDispatcher dispatcher) {
		PushStream<ServiceReferenceEvent> removed = dispatcherMap.remove(dispatcher);
		if(removed != null) {
			removed.close();
		}
	}
	
	@Deactivate
	public void deactivate() {
		serviceTracker.close();
		source.close();
		dispatcherMap.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(ServiceReference<Object> reference) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.ADD);
		source.publish(event);
		contentReferences.put(reference, event);
		return context.getService(reference);
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.MODIFY);
		source.publish(event);
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.REMOVE);
		contentReferences.remove(reference);
		source.publish(event);
		context.ungetService(reference);
	}
	
	
}
