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
package org.eclipselabs.osgi.jersey.runtime;

import static org.eclipselabs.osgi.jersey.JerseyConstants.JERSEY_CONTEXT_PATH;
import static org.eclipselabs.osgi.jersey.JerseyConstants.JERSEY_HOST;
import static org.eclipselabs.osgi.jersey.JerseyConstants.JERSEY_PORT;
import static org.eclipselabs.osgi.jersey.JerseyConstants.JERSEY_SCHEMA;
import static org.eclipselabs.osgi.jersey.JerseyConstants.WHITEBOARD_DEFAULT_CONTEXT_PATH;
import static org.eclipselabs.osgi.jersey.JerseyConstants.WHITEBOARD_DEFAULT_HOST;
import static org.eclipselabs.osgi.jersey.JerseyConstants.WHITEBOARD_DEFAULT_PORT;
import static org.eclipselabs.osgi.jersey.JerseyConstants.WHITEBOARD_DEFAULT_SCHEMA;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipselabs.osgi.jaxrs.helper.JaxRsHelper;
import org.eclipselabs.osgi.jaxrs.provider.PrototypeResourceProvider;
import org.eclipselabs.osgi.jersey.JaxRsApplicationProvider;
import org.eclipselabs.osgi.jersey.JaxRsJerseyHandler;
import org.eclipselabs.osgi.jersey.JerseyConstants;
import org.eclipselabs.osgi.jersey.JerseyHelper;
import org.eclipselabs.osgi.jersey.application.JerseyApplication;
import org.eclipselabs.osgi.jersey.application.JerseyApplicationProvider;
import org.eclipselabs.osgi.jersey.binder.PrototypeServiceBinder;
import org.eclipselabs.osgi.jersey.dto.DTOConverter;
import org.eclipselabs.osgi.jersey.jetty.JettyServerRunnable;
import org.eclipselabs.osgi.jersey.provider.JerseyResourceInstanceFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntimeConstants;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.RequestInfoDTO;
import org.osgi.service.jaxrs.runtime.dto.RuntimeDTO;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@ProvideCapability(ns = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, 
version="1.0", 
value = "osgi.implementation=\"osgi.jaxrs\"", 
uses= {"javax.ws.rs", "javax.ws.rs.client", "javax.ws.rs.container", "javax.ws.rs.core", "javax.ws.rs.ext", "org.osgi.service.jaxrs.whiteboard"})
public class JerseyServiceRuntime implements JaxRSServiceRuntime, JaxRsJerseyHandler {

	private volatile PrototypeServiceBinder binder;
	private volatile Server jettyServer;
	private volatile ServletContextHandler contextHandler;
	private volatile RuntimeDTO runtimeDTO = new RuntimeDTO();
	private volatile String name;
	private Integer port = WHITEBOARD_DEFAULT_PORT;
	private String contextPath = WHITEBOARD_DEFAULT_CONTEXT_PATH;
	private ComponentContext context;
	// hold all resource references of the default application 
	//	private final List<ServiceReference<?>> resourcesRefList = new LinkedList<>();
	private JerseyApplication defaultApplication;
	private final Map<String, JaxRsApplicationProvider> applicationContainerMap = new ConcurrentHashMap<>();
	private Logger logger = Logger.getLogger("o.e.o.j.serviceRuntime");

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime#getRuntimeDTO()
	 */
	@Override
	public RuntimeDTO getRuntimeDTO() {
		return runtimeDTO;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime#calculateRequestInfoDTO(java.lang.String)
	 */
	@Override
	public RequestInfoDTO calculateRequestInfoDTO(String path) {
		RequestInfoDTO ridto = new RequestInfoDTO();
		return ridto;
	}

	/**
	 * Called on component activation
	 * @param ctx the component context
	 * @throws ConfigurationException 
	 */
	public void activate(ComponentContext ctx) throws ConfigurationException {
		this.context = ctx;
		System.out.println("activate " + this.toString());
		updateProperties(context);
		createServerAndContext();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#initialize(org.osgi.service.component.ComponentContext)
	 */
	@Override
	public void initialize(ComponentContext context) throws ConfigurationException {
		activate(context);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#startup()
	 */
	@Override
	public void startup() {
		startServer();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#teardown()
	 */
	public void teardown() {
		/*
		 * Unregister the default application
		 */
		unregisterApplication(new JerseyApplicationProvider(".default", defaultApplication));
		stopContextHandler();
		stopServer();
		binder.dispose();
		if (defaultApplication != null) {
			defaultApplication.dispose();
			defaultApplication = null;
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#modified(org.osgi.service.component.ComponentContext)
	 */
	public void modified(ComponentContext ctx) throws ConfigurationException {
		System.out.println("modify " + this.toString());
		Integer oldPort = port;
		String oldContextPath = contextPath;
		updateProperties(ctx);
		boolean portChanged = !this.port.equals(oldPort);
		boolean pathChanged = !this.contextPath.equals(oldContextPath);

		if (!pathChanged && !portChanged) {
			return;
		}
		// if port changed, both parts need to be restarted, no matter, if the context path has changed
		if (portChanged || pathChanged) {
			unregisterApplication(new JerseyApplicationProvider(".default", defaultApplication));
			stopContextHandler();
			stopServer();
			createServerAndContext();
			/*
			 * Setup the default application
			 */
			if (defaultApplication == null) {
				defaultApplication = new JerseyApplication(name, context.getBundleContext());
			} else {
				defaultApplication.dispose();
			}
			startServer();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#getURLs(org.osgi.service.component.ComponentContext)
	 */
	public String[] getURLs(ComponentContext context) {
		StringBuilder sb = new StringBuilder();
		String schema = JerseyHelper.getPropertyWithDefault(context, JERSEY_SCHEMA, WHITEBOARD_DEFAULT_SCHEMA);
		sb.append(schema);
		sb.append("://");
		String host = JerseyHelper.getPropertyWithDefault(context, JERSEY_HOST, WHITEBOARD_DEFAULT_HOST);
		sb.append(host);
		Integer port = JerseyHelper.getPropertyWithDefault(context, JERSEY_PORT, null);
		if (port != null) {
			sb.append(":");
			sb.append(port.intValue());
		}
		String path = JerseyHelper.getPropertyWithDefault(context, JERSEY_CONTEXT_PATH, WHITEBOARD_DEFAULT_CONTEXT_PATH);
		path = JaxRsHelper.toServletPath(path);
		sb.append(path);
		return new String[] {sb.toString()};
	}

	/**
	 * Creates a new {@link ResourceConfig} for a given application. this method takes care of registering
	 * Jersey factories for prototype scoped resource services and singletons separately
	 * @param application the JaxRs application
	 */
	private ResourceConfig createResourceConfig(Application application) {
		if (application == null) {
			logger.log(Level.WARNING, "Cannot create a resource configuration for null application");
			return null;
		}
		ResourceConfig config = ResourceConfig.forApplication(application);
		// prepare factory creation to forward prototype functionality to Jersey
		if (application instanceof PrototypeResourceProvider) {
			System.out.println("Register a prototype provider like application");
			if (context == null) {
				throw new IllegalStateException("Cannot create prototype factories without component context");
			}
			PrototypeResourceProvider prp = (PrototypeResourceProvider) application;
			BundleContext bctx = context.getBundleContext();
			Set<Class<?>> classes = prp.getPrototypeResourceClasses();
			classes.forEach((c)->{
				Factory<?> factory = new JerseyResourceInstanceFactory<>(bctx, c);
				binder.register(c, factory);
			});
			config.register(binder);
		}
		return config;
	}

	/**
	 * Updates the fields that are provided by service properties.
	 * @param ctx the component context
	 * @throws ConfigurationException thrown when no context is available or the expected property was not provided 
	 */
	private void updateProperties(ComponentContext ctx) throws ConfigurationException {
		if (ctx == null) {
			throw new ConfigurationException(JaxRSServiceRuntimeConstants.JAX_RS_SERVICE_ENDPOINT, "No component context is availble to get properties from");
		}
		name = JerseyHelper.getPropertyWithDefault(ctx, JaxRSWhiteboardConstants.JAX_RS_NAME, null);
		if (name == null) {
			name = JerseyHelper.getPropertyWithDefault(ctx, JerseyConstants.JERSEY_WHITEBOARD_NAME, null);
			if (name == null) {
				throw new ConfigurationException(JaxRSWhiteboardConstants.JAX_RS_NAME, "No name was defined for the whiteboard");
			}
		}
		String[] urls = getURLs(ctx);
		URI[] uris = new URI[urls.length];
		for (int i = 0; i < urls.length; i++) {
			uris[i] = URI.create(urls[i]);
		}
		URI uri = uris[0];
		if (uri.getPort() > 0) {
			port = uri.getPort();
		}
		if (uri.getPath() != null) {
			contextPath = uri.getPath();
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#updateRuntimeDTO(org.osgi.framework.ServiceReference)
	 */
	public synchronized void updateRuntimeDTO(ServiceReference<?> serviceRef) {
		List<ApplicationDTO> appDTOList = new LinkedList<>();
		applicationContainerMap.forEach((name, ap)->{
			ApplicationDTO appDTO = ap.getApplicationDTO();
			if (name.equals(".default")) {
				runtimeDTO.defaultApplication = appDTO;
			} else {
				appDTOList.add(appDTO);
			}
		});
		if (serviceRef != null) {
			ServiceReferenceDTO srDTO = DTOConverter.toServiceReferenceDTO(serviceRef);
			runtimeDTO.serviceDTO = srDTO;
			// the defaults application service id is the same, like this, because it comes from here
//			runtimeDTO.defaultApplication.serviceId = srDTO.id;
		}
		runtimeDTO.applicationDTOs = appDTOList.toArray(new ApplicationDTO[appDTOList.size()]);
	}

	/**
	 * Creates the Jetty server and initializes the current context handler
	 */
	private void createServerAndContext() {
		if (binder != null) {
			binder.dispose();
		}
		binder = new PrototypeServiceBinder();
		try {
			if (jettyServer != null && !jettyServer.isStopped()) {
				logger.log(Level.WARNING, "Stopping JaxRs white-board server on startup, but it wasn't exepected to run");
				stopContextHandler();
				stopServer();
			}
			jettyServer = new Server(port);
			contextHandler = new ServletContextHandler(jettyServer, contextPath);
			logger.info("Started JaxRs white-board server and context handler for port: " + port + " and context: " + contextPath);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error starting JaxRs white-board because of an exception", e);
		}
	}

	/**
	 * Starts the Jetty server 
	 */
	private void startServer() {
		if (jettyServer != null && contextHandler != null && !jettyServer.isRunning()) {
			try {
				Executors.newSingleThreadExecutor().submit(new JettyServerRunnable(jettyServer, port));
				logger.info("Started JaxRs white-board server and context handler for port: " + port + " and context: " + contextPath);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error starting JaxRs white-board because of an exception", e);
			}
		}
	}

	/**
	 * Stopps the Jetty server;
	 */
	private void stopServer() {
		if (jettyServer == null) {
			logger.log(Level.WARNING, "Try to stop JaxRs whiteboard server, but there is none");
			return;
		}
		if (jettyServer.isStopped()) {
			logger.log(Level.WARNING, "Try to stop JaxRs whiteboard server, but it was already stopped");
			return;
		}
		try {
			jettyServer.stop();
			jettyServer.destroy();
			jettyServer = null;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error stopping Jetty server", e);
		}
	}

	/**
	 * Stopps the Jetty context handler;
	 */
	private void stopContextHandler() {
		if (contextHandler == null) {
			logger.log(Level.WARNING, "Try to stop Jetty context handler, but there is none");
			return;
		}
		if (contextHandler.isStopped()) {
			logger.log(Level.WARNING, "Try to stop Jetty context handler, but it was already stopped");
			return;
		}
		try {
			contextHandler.stop();
			contextHandler.destroy();
			contextHandler = null;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error stopping Jetty context handler", e);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#registerApplication(org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider)
	 */
	@Override
	public void registerApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot register an null application provider");
			return;
		}
		if (applicationContainerMap.containsKey(applicationProvider.getName())) {
			logger.log(Level.SEVERE, "There is already an application registered with name: " + applicationProvider.getName());
			throw new IllegalStateException("There is already an application registered with name: " + applicationProvider.getName());
		}
		Application application = applicationProvider.getJaxRsApplication();
		ResourceConfig config = createResourceConfig(application);

		ServletContainer container = new ServletContainer(config);
		applicationProvider.setServletContainer(container);
		ServletHolder servlet = new ServletHolder(container);
		String applicationPath = JaxRsHelper.getServletPath(application);
		contextHandler.addServlet(servlet, applicationPath);
		applicationContainerMap.put(applicationProvider.getName(), applicationProvider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#unregisterApplication(org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider)
	 */
	@Override
	public void unregisterApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "Cannot unregister an null application provider");
			return;
		}
		JaxRsApplicationProvider provider = null;
		synchronized (applicationContainerMap) {
			provider = applicationContainerMap.remove(applicationProvider.getName());
		}
		if (provider == null) {
			logger.log(Level.WARNING, "There is no application registered with the name: " + applicationProvider.getName());
			return;
		}
		ServletContainer container = provider.getServletContainer();
		if (container != null) {
			logger.log(Level.SEVERE, "Implement the remove servlet here");
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsJerseyHandler#reloadApplication(org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider)
	 */
	@Override
	public void reloadApplication(JaxRsApplicationProvider applicationProvider) {
		if (applicationProvider == null) {
			logger.log(Level.WARNING, "No application provider was given to be reloaded");
		}
		synchronized (applicationContainerMap) {
			JaxRsApplicationProvider provider = applicationContainerMap.get(applicationProvider.getName());
			if (provider == null) {
				logger.log(Level.WARNING, "No application provider was registered nothing to reload, registering instead");
				registerApplication(applicationProvider);
			} else {
				ServletContainer servletContainer = provider.getServletContainer();
				if (servletContainer != null) {
					ResourceConfig config = createResourceConfig(provider.getJaxRsApplication());
					servletContainer.reload(config);
				}
			}
		}
	}

}
