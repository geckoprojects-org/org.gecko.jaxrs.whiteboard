package org.gecko.rest.jersey.tests.collector;

import static org.junit.Assert.*;

import java.time.Duration;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.gecko.rest.jersey.helper.ReferenceCollector;
import org.gecko.rest.jersey.helper.ServiceReferenceEvent;
import org.gecko.rest.jersey.tests.resources.HelloResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;
import org.osgi.util.pushstream.PushStream;
import org.osgi.util.tracker.ServiceTracker;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceCollectorTest {

	private BundleContext context;

	List<ServiceRegistration> toUnregister = new LinkedList<>();

	@Before
	public void before() {
		context = FrameworkUtil.getBundle(getClass()).getBundleContext();
	}
	
	@After
	public void after() {
		toUnregister.forEach(s -> s.unregister());
		toUnregister.clear();
	}
	
	@Test
	public void testBasic() throws InterruptedException {
		
		ReferenceCollector collector = getService(ReferenceCollector.class, 1000L);
//		
//		try(PushStream<ServiceReferenceEvent> stream = collector.createPushStream()){
//			final CountDownLatch latch = new CountDownLatch(3);
//			stream.forEach(s -> {
//				System.out.println(s.getReference().getProperty(JaxRSWhiteboardConstants.JAX_RS_NAME));
//				latch.countDown();
//			});
//			
//			assertFalse(latch.await(1, TimeUnit.SECONDS));
//			
//			Dictionary<String, Object> helloProps = new Hashtable<>();
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
//			System.out.println("Register resource for uri /hello");
//			toUnregister.add(context.registerService(Object.class, new HelloResource(), helloProps));
//		
//			assertTrue(latch.await(1, TimeUnit.SECONDS));
//		}
	}
	
	@Test
	public void testMultiple() throws InterruptedException {
			
		ReferenceCollector collector = getService(ReferenceCollector.class, 1000L);
		
//		try(PushStream<ServiceReferenceEvent> stream = collector.createPushStream()){
//			final CountDownLatch latch = new CountDownLatch(3);
//			stream.forEach(s -> {
//				System.out.println(s.getReference().getProperty(JaxRSWhiteboardConstants.JAX_RS_NAME));
//				latch.countDown();
//			});
//			
//			assertFalse(latch.await(1, TimeUnit.SECONDS));
//			
//			Dictionary<String, Object> helloProps = new Hashtable<>();
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
//			System.out.println("Register resource for uri /hello");
//			toUnregister.add(context.registerService(Object.class, new HelloResource(), helloProps));
//			
//			assertTrue(latch.await(1, TimeUnit.SECONDS));
//			
//			try(PushStream<ServiceReferenceEvent> innerStream = collector.createPushStream()){
//				final CountDownLatch innerLatch = new CountDownLatch(3);
//				innerStream.forEach(s -> {
//					System.out.println(s.getReference().getProperty(JaxRSWhiteboardConstants.JAX_RS_NAME));
//					innerLatch.countDown();
//				});
//				
//				assertTrue(innerLatch.await(1, TimeUnit.SECONDS));
//			}
//		}
	}
	
	@Test
	public void testFork() throws InterruptedException {
		
		ReferenceCollector collector = getService(ReferenceCollector.class, 1000L);
		
//		try(PushStream<ServiceReferenceEvent> stream = collector.createPushStream()){
//			final CountDownLatch latch = new CountDownLatch(3);
//			
//			assertFalse(latch.await(1, TimeUnit.SECONDS));
//			
//			Dictionary<String, Object> helloProps = new Hashtable<>();
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_RESOURCE, "true");
//			helloProps.put(JaxRSWhiteboardConstants.JAX_RS_NAME, "Hello");
//			System.out.println("Register resource for uri /hello");
//			toUnregister.add(context.registerService(Object.class, new HelloResource(), helloProps));
//			
//			Set<ServiceReferenceEvent> resources = new HashSet<>();
//			Set<ServiceReferenceEvent> extensions = new HashSet<>();
//			stream.window(Duration.ofSeconds(1), sec -> sec).forEach(sec -> {
//				System.out.println(System.currentTimeMillis() + " here we are " + sec.size());
//				sec.forEach(se -> System.out.println("ref: " + se.getReference().getProperty(JaxRSWhiteboardConstants.JAX_RS_NAME)));
//				sec.stream().filter(sre -> sre.isResource()).forEach(resources::add);
//				sec.stream().filter(sre -> sre.isExtension()).forEach(extensions::add);
//				 
//				sec.forEach(s -> latch.countDown());
//			});
//
//			assertTrue(latch.await(5, TimeUnit.SECONDS));
//			
//			assertEquals(2, resources.size()); 
//			assertEquals(1, extensions.size()); 
//		}
	}
	
	<T> T getService(Class<T> clazz, long timeout) throws InterruptedException {
		ServiceTracker<T, T> tracker = new ServiceTracker<>(context, clazz, null);
		tracker.open();
		return tracker.waitForService(timeout);
	}
	
}
