/**
 * 
 */
package org.eclipselabs.jaxrs.jersey.helper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.eclipselabs.jaxrs.jersey.helper.ServiceReferenceEvent.Type;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsWhiteboardDispatcher;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.pushstream.PushStreamProvider;
import org.osgi.util.pushstream.SimplePushEventSource;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author jalbert
 *
 */
@Component(service=ReferenceCollector.class, immediate = true, enabled=true)
@SuppressWarnings("rawtypes")
public class ReferenceCollector implements ServiceTrackerCustomizer<Object, Object> {

	private ServiceTracker<Object, Object> serviceTracker;

	private PushStreamProvider provider = new PushStreamProvider();
	
	private Map<ServiceReference, ServiceReferenceEvent> contentReferences = new HashMap<>(); 
	
	private SimplePushEventSource<ServiceReferenceEvent> source; 

	private BundleContext context; 
	
	private Map<JaxRsWhiteboardDispatcher, PushStream<ServiceReferenceEvent>> dispatcherMap = new ConcurrentHashMap<>();
	
	@Activate
	public void activate(BundleContext context) throws InvalidSyntaxException {
		this.context = context;
		serviceTracker = new ServiceTracker<>(context, context.createFilter("(|(" + JaxRSWhiteboardConstants.JAX_RS_RESOURCE + "=true)(" + JaxRSWhiteboardConstants.JAX_RS_EXTENSION + "=true))"), this);
		serviceTracker.open();

		source = provider.buildSimpleEventSource(ServiceReferenceEvent.class).build();
	
	}

	@SuppressWarnings("unchecked")
	public void connect(final JaxRsWhiteboardDispatcher dispatcher){

		PushStream<ServiceReferenceEvent> pushStream = dispatcherMap.get(dispatcher);
		
		if(pushStream == null) {
			
			pushStream = provider.buildStream(source).withScheduler(Executors.newScheduledThreadPool(1)).build();
			pushStream = pushStream.merge(provider.streamOf(contentReferences.values().stream()));
			
			dispatcherMap.put(dispatcher, pushStream);
			
			pushStream = pushStream.buffer().distinct();
			
			pushStream.window(Duration.ofSeconds(1), sec -> sec).forEach(sec -> {
				sec.stream().filter(sre -> sre.isResource()).forEach(sre -> {
					Map<String, Object> properties = JerseyHelper.getServiceProperties(sre.getReference());
					switch (sre.getType()) {
					case ADD:
						ServiceObjects so = context.getServiceObjects(sre.getReference());
						dispatcher.addResource(so, properties);
						break;
					case MODIFY:
						break;
					default:
						dispatcher.removeResource(properties);
						break;
					};
				});
				sec.stream().filter(sre -> sre.isExtension()).forEach(sre -> {
					Map<String, Object> properties = JerseyHelper.getServiceProperties(sre.getReference());
					switch (sre.getType()) {
					case ADD:
						ServiceObjects so = context.getServiceObjects(sre.getReference());
						dispatcher.addExtension(so, properties);
						break;
					case MODIFY:
						break;
					default:
						dispatcher.removeExtension(properties);
						break;
					};
				});
			});
		}
		
//		contentReferences.forEach((sr, sre) -> {
//			dis
//		});
		
	}

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
	}
	
	@Override
	public Object addingService(ServiceReference<Object> reference) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.ADD);
		source.publish(event);
		contentReferences.put(reference, event);
		System.out.println(System.currentTimeMillis() + " adding " + reference.getProperty(JaxRSWhiteboardConstants.JAX_RS_NAME));
		return context.getService(reference);
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.MODIFY);
		source.publish(event);
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		ServiceReferenceEvent event = new ServiceReferenceEvent(reference, Type.REMOVE);
		contentReferences.remove(reference);
		source.publish(event);
		context.ungetService(reference);
	}
	
	
}
