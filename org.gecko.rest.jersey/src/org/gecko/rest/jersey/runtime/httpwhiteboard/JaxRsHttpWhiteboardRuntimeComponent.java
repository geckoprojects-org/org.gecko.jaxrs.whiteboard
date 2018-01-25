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
package org.gecko.rest.jersey.runtime.httpwhiteboard;

import java.util.Map;

import javax.ws.rs.core.Application;

import org.gecko.rest.jersey.helper.ReferenceCollector;
import org.gecko.rest.jersey.runtime.JerseyWhiteboardComponent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jaxrs.runtime.JaxRSServiceRuntime;

/**
 * This component handles the lifecycle of a {@link JaxRSServiceRuntime}
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
@Component(name="JaxRsHttpWhiteboardRuntimeComponent", immediate=true, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class JaxRsHttpWhiteboardRuntimeComponent extends JerseyWhiteboardComponent{

//	private static Logger logger = Logger.getLogger("o.e.o.j.JaxRsHttpWhiteboardRuntimeComponent");

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private ReferenceCollector collector;
	
	/**
	 * Called on component activation
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	/* (non-Javadoc)
	 * @see org.gecko.rest.jersey.runtime.JerseyWhiteboardComponent#activate(org.osgi.service.component.ComponentContext)
	 */
	@Activate
	@Override
	public void activate(final ComponentContext context) throws ConfigurationException {
		updateProperties(context);
		if (whiteboard != null) {
			whiteboard.teardown();;
		}
		whiteboard = new HTTPWhiteboardBasedJerseyServiceRuntime();
		whiteboard.initialize(context);
		dispatcher.setWhiteboardProvider(whiteboard);
		collector.connect(dispatcher);
		dispatcher.dispatch();
		whiteboard.startup();
	}

	/**
	 * Called on component modification
	 * @param context the component context
	 * @throws ConfigurationException 
	 */
	@Modified
	public void modified(ComponentContext context) throws ConfigurationException {
		updateProperties(context);
		whiteboard.modified(context);
		dispatcher.dispatch();
	}

	/**
	 * Called on component de-activation
	 * @param context the component context
	 */
	@Deactivate
	public void deactivate(ComponentContext context) {
		if (dispatcher != null) {
			collector.disconnect(dispatcher);
			dispatcher.deactivate();
		}
		if (whiteboard != null) {
			whiteboard.teardown();
			whiteboard = null;
		}
	}
	
	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	@Reference(name="application", cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, unbind="removeApplication", updated = "modifedApplication")
	public void addApplication(Application application, Map<String, Object> properties) {
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Adds a new application
	 * @param application the application to add
	 * @param properties the service properties
	 */
	public void modifedApplication(Application application, Map<String, Object> properties) {
		dispatcher.removeApplication(application, properties);
		dispatcher.addApplication(application, properties);
	}

	/**
	 * Removes a application 
	 * @param application the application to remove
	 * @param properties the service properties
	 */
	public void removeApplication(Application application, Map<String, Object> properties) {
		super.removeApplication(application, properties);
	}
}
