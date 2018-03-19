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
package org.gecko.rest.jersey.runtime;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.gecko.rest.jersey.helper.JaxRsHelper;
import org.gecko.rest.jersey.helper.JerseyHelper;
import org.gecko.rest.jersey.jetty.JettyServerRunnable;
import org.gecko.rest.jersey.provider.JerseyConstants;
import org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider;
import org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime;
import org.gecko.rest.jersey.runtime.servlet.WhiteboardServletContainer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;

/**
 * Implementation of the {@link JaxRSServiceRuntime} for a Jersey implementation
 * @author Mark Hoffmann
 * @since 12.07.2017
 */
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, 
version = JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_SPECIFICATION_VERSION, 
name = JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_IMPLEMENTATION)
public class JerseyServiceRuntime extends AbstractJerseyServiceRuntime {

	private volatile Server jettyServer;
	private volatile ServletContextHandler contextHandler;
	private Integer port = JerseyConstants.WHITEBOARD_DEFAULT_PORT;
	private String contextPath = JerseyConstants.WHITEBOARD_DEFAULT_CONTEXT_PATH;
	private Logger logger = Logger.getLogger("o.e.o.j.serviceRuntime");

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doInitialize(org.osgi.service.component.ComponentContext)
	 */
	@Override
	protected void doInitialize(ComponentContext context){
		createServerAndContext();
	}
	
	

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#modified(org.osgi.service.component.ComponentContext)
	 */
	public void doModified(ComponentContext ctx) throws ConfigurationException {
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
			stopContextHandler();
			stopServer();
			createServerAndContext();
			startServer();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doStartup()
	 */
	@Override
	public void doStartup() {
		startServer();
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doTeardown()
	 */
	@Override
	protected void doTeardown() {
		stopContextHandler();
		stopServer();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.gecko.rest.jersey.provider.whiteboard.JaxRsWhiteboardProvider#getURLs(org.osgi.service.component.ComponentContext)
	 */
	public String[] getURLs(ComponentContext context) {
		StringBuilder sb = new StringBuilder();
		String schema = JerseyHelper.getPropertyWithDefault(context, JerseyConstants.JERSEY_SCHEMA, JerseyConstants.WHITEBOARD_DEFAULT_SCHEMA);
		sb.append(schema);
		sb.append("://");
		String host = JerseyHelper.getPropertyWithDefault(context, JerseyConstants.JERSEY_HOST, JerseyConstants.WHITEBOARD_DEFAULT_HOST);
		sb.append(host);
		Object port = JerseyHelper.getPropertyWithDefault(context, JerseyConstants.JERSEY_PORT, null);
		if (port != null) {
			sb.append(":");
			sb.append(port.toString());
		}
		String path = JerseyHelper.getPropertyWithDefault(context, JerseyConstants.JERSEY_CONTEXT_PATH, JerseyConstants.WHITEBOARD_DEFAULT_CONTEXT_PATH);
		path = JaxRsHelper.toServletPath(path);
		sb.append(path);
		return new String[] {sb.substring(0,  sb.length() - 1)};
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doRegisterServletContainer(org.glassfish.jersey.servlet.ServletContainer, org.gecko.rest.jersey.provider.application.JaxRsApplicationProvider)
	 */
	@Override
	protected void doRegisterServletContainer(JaxRsApplicationProvider applicationProvider, String path, ResourceConfig config) {
		WhiteboardServletContainer container = new WhiteboardServletContainer(config);
		if(!applicationProvider.getServletContainers().isEmpty()) {
			throw new IllegalStateException("There is alread a ServletContainer registered for this application " + applicationProvider.getId());
		}
		applicationProvider.getServletContainers().add(container);
		ServletHolder servlet = new ServletHolder(container);
		contextHandler.addServlet(servlet, path);
	}

	@Override
	protected void doUnregisterApplication(JaxRsApplicationProvider applicationProvider) {
		List<ServletContainer> servletContainers = applicationProvider.getServletContainers();
		if(!servletContainers.isEmpty()) {
			ServletContainer container = servletContainers.remove(0);
			if (container != null && contextHandler != null) {
				ServletHandler handler = contextHandler.getServletHandler();
				List<ServletHolder> servlets = new ArrayList<ServletHolder>();
				Set<String> names = new HashSet<String>();
				for( ServletHolder holder : handler.getServlets()) {
					/* If it is the class we want to remove, then just keep track of its name */
					try {
						if (container.equals(holder.getServlet())) {
							names.add(holder.getName());
						} else /* We keep it */ {
							servlets.add(holder);
						}
					} catch (ServletException e) {
						logger.log(Level.SEVERE, "Error unregistering servlets from holder with name: " + holder.getName());
					}
				}
				
				List<ServletMapping> mappings = new ArrayList<ServletMapping>();
				for( ServletMapping mapping : handler.getServletMappings() )  {
					/* Only keep the mappings that didn't point to one of the servlets we removed */
					if(!names.contains(mapping.getServletName())) {
						mappings.add(mapping);
					}
				}
				
				/* Set the new configuration for the mappings and the servlets */
				handler.setServletMappings( mappings.toArray(new ServletMapping[0]) );
				handler.setServlets( servlets.toArray(new ServletHolder[0]) );
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.common.AbstractJerseyServiceRuntime#doUpdateProperties(org.osgi.service.component.ComponentContext)
	 */
	protected void doUpdateProperties(ComponentContext ctx) {
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

	/**
	 * Creates the Jetty server and initializes the current context handler
	 */
	private void createServerAndContext() {
		try {
			if (jettyServer != null && !jettyServer.isStopped()) {
				logger.log(Level.WARNING, "Stopping JaxRs white-board server on startup, but it wasn't exepected to run");
				stopContextHandler();
				stopServer();
			}
			jettyServer = new Server(port);
			contextHandler = new ServletContextHandler(jettyServer, contextPath);
			logger.info("Created white-board server context handler for context: " + contextPath);
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
				CountDownLatch awaitStart = new CountDownLatch(1);
				Executors.newSingleThreadExecutor().submit(new JettyServerRunnable(jettyServer, port, awaitStart));
				if(!awaitStart.await(1, TimeUnit.SECONDS)) {
					// TODO: We have todo something I guess
					logger.info("Started JaxRs white-board server for port: " + port + " and context: " + contextPath + " took to long");
					throw new IllegalStateException("Server Startup took too long");
				} else {
					logger.info("Started JaxRs white-board server for port: " + port + " and context: " + contextPath);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error starting JaxRs white-board because of an exception", e);
				// TODO: We have todo something I guess like throw an Exception
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

}
