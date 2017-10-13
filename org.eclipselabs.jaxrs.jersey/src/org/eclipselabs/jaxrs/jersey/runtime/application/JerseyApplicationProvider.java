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
package org.eclipselabs.jaxrs.jersey.runtime.application;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Application;

import org.eclipselabs.jaxrs.jersey.dto.DTOConverter;
import org.eclipselabs.jaxrs.jersey.helper.JaxRsHelper;
import org.eclipselabs.jaxrs.jersey.provider.application.AbstractJaxRsProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationContentProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider;
import org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider;
import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.jaxrs.runtime.dto.ApplicationDTO;
import org.osgi.service.jaxrs.runtime.dto.DTOConstants;
import org.osgi.service.jaxrs.whiteboard.JaxRSWhiteboardConstants;

/**
 * Implementation of the Application Provider
 * @author Mark Hoffmann
 * @since 30.07.2017
 */
public class JerseyApplicationProvider extends AbstractJaxRsProvider<Application> implements JaxRsApplicationProvider {

	private static final Logger logger = Logger.getLogger("jersey.applicationProvider");
	private ServletContainer applicationContainer;
	private String applicationBase;
	private boolean legacy = false;
	private boolean changed = false;

	public JerseyApplicationProvider(String name, Application jaxRsApplication, String basePath) {
		this(jaxRsApplication, createProperties(name, basePath));
	}

	public JerseyApplicationProvider(Application jaxRsApplication, Map<String, Object> properties) {
		super(jaxRsApplication, properties);
		// create name after validation, because some fields are needed eventually
		if (Application.class == jaxRsApplication.getClass()) {
			setProviderObject(new JerseyApplication(getProviderName()));
		} else {
			legacy = true;
		}
	}
	
	private static Map<String, Object> createProperties(String name, String basePath) {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(JaxRSWhiteboardConstants.JAX_RS_NAME, name);
		properties.put(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE, basePath);
		return properties;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#setServletContainer(org.glassfish.jersey.servlet.ServletContainer)
	 */
	@Override
	public void setServletContainer(ServletContainer applicationContainer) {
		this.applicationContainer = applicationContainer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getServletContainer()
	 */
	@Override
	public ServletContainer getServletContainer() {
		return applicationContainer;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getPath()
	 */
	@Override
	public String getPath() {
		if (getProviderObject() == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to create a context path");
		}
		return applicationBase == null ? null : JaxRsHelper.toApplicationPath(applicationBase);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getJaxRsApplication()
	 */
	@Override
	public Application getJaxRsApplication() {
		if (getProviderObject() == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to return an application");
		}
		return getProviderObject();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#getApplicationProperties()
	 */
	@Override
	public Map<String, Object> getApplicationProperties() {
		return getProviderProperties();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.runtime.JaxRsApplicationProvider#getApplicationDTO()
	 */
	@Override
	public ApplicationDTO getApplicationDTO() {
		int status = getProviderStatus();
		if (getProviderObject() == null) {
			throw new IllegalStateException("This application provider does not contain an application, but should have one to get a DTO");
		}
		if (status == NO_FAILURE) {
			return DTOConverter.toApplicationDTO(this);
		} else {
			return DTOConverter.toFailedApplicationDTO(this, status);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#isLegacy()
	 */
	@Override
	public boolean isLegacy() {
		return legacy;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#isDefault()
	 */
	public boolean isDefault() {
		return getName().equals(".default");
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return getProviderObject().getClasses().isEmpty() && getProviderObject().getSingletons().isEmpty();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#isChanged()
	 */
	@Override
	public boolean isChanged() {
		return changed;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#markUnchanged()
	 */
	@Override
	public void markUnchanged() {
		changed = false;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#addResource(java.lang.Object, java.util.Map)
	 */
	@Override
	public boolean addResource(Object resource, Map<String, Object> properties) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy application and therefore not extensible: " + getName());
			return false;
		}
		if (properties == null) {
			properties = Collections.emptyMap();
		}
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		return addResource(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#addResource(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider)
	 */
	@Override
	public boolean addResource(JaxRsResourceProvider provider) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy application and therefore not extensible for resources: " + getName());
			return false;
		}
		if (!provider.isResource()) {
			logger.log(Level.WARNING, "The resource to add is not declared with the resource property: " + JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
			return false;
		}
		return doAddContent(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#removeResource(java.lang.Object, java.util.Map)
	 */
	@Override
	public boolean removeResource(Object resource, Map<String, Object> properties) {
		if (properties == null) {
			properties = Collections.emptyMap();
		}
		JaxRsResourceProvider provider = new JerseyResourceProvider<Object>(resource, properties);
		return removeResource(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#removeResource(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsResourceProvider)
	 */
	@Override
	public boolean removeResource(JaxRsResourceProvider provider) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy application and therefore there is nothing to remove: ");
			return false;
		}
		if (provider == null) {
			logger.log(Level.WARNING, "The resource provider is null. There is nothing to remove.");
			return false;
		}
		if (!provider.isResource()) {
			logger.log(Level.WARNING, "The resource to be removed is not declared with the resource property: " + JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
			return false;
		}
		return doRemoveContent(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#addExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public boolean addExtension(Object extension, Map<String, Object> properties) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy# application and therefore not extensible: " + getName());
			return false;
		}
		if (properties == null) {
			properties = Collections.emptyMap();
		}
		String resourceProp = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_EXTENSION);
		if (!Boolean.parseBoolean(resourceProp)) {
			logger.log(Level.WARNING, "The extension to add is not declared with the extension resource property: " + JaxRSWhiteboardConstants.JAX_RS_RESOURCE);
			return false;
		}
		boolean filterValid = isApplicationFilterValid(properties);
		return filterValid;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#addExtension(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider)
	 */
	@Override
	public boolean addExtension(JaxRsExtensionProvider provider) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy application and therefore not extensible for extensions: " + getName());
			return false;
		}
		if (!provider.isExtension()) {
			logger.log(Level.WARNING, "The extension to add is not declared with the extension property: " + JaxRSWhiteboardConstants.JAX_RS_EXTENSION);
			return false;
		}
		return doAddContent(provider);
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.osgi.jersey.JaxRsApplicationProvider#removeExtension(java.lang.Object, java.util.Map)
	 */
	@Override
	public boolean removeExtension(Object extension, Map<String, Object> properties) {
		if (properties == null) {
			properties = Collections.emptyMap();
		}
		JaxRsExtensionProvider provider = new JerseyExtensionProvider<Object>(extension, properties);
		return removeExtension(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.application.JaxRsApplicationProvider#removeExtension(org.eclipselabs.jaxrs.jersey.provider.application.JaxRsExtensionProvider)
	 */
	@Override
	public boolean removeExtension(JaxRsExtensionProvider provider) {
		if (isLegacy()) {
			logger.log(Level.WARNING, "This application is a legacy application and therefore there is nothing to remove: ");
			return false;
		}
		if (provider == null) {
			logger.log(Level.WARNING, "The extension provider is null. There is nothing to remove.");
			return false;
		}
		if (!provider.isExtension()) {
			logger.log(Level.WARNING, "The extension to be removed is not declared with the extension property: " + JaxRSWhiteboardConstants.JAX_RS_EXTENSION);
			return false;
		}
		return doRemoveContent(provider);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.AbstractJaxRsProvider#getProviderName()
	 */
	@Override
	protected String getProviderName() {
		String name = null;
		Map<String, Object> providerProperties = getProviderProperties();
		if (providerProperties != null) {
			String baseProperty = (String) providerProperties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE);
			name = (String) providerProperties.get(JaxRSWhiteboardConstants.JAX_RS_NAME);
			if (name == null && baseProperty != null) {
				name = "." + baseProperty;
			} else if (name != null && !name.equals(".default") && (name.startsWith(".") || name.startsWith("osgi"))) {
				updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			}
		}
		return name == null ? "." + UUID.randomUUID().toString() : name;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipselabs.jaxrs.jersey.provider.AbstractJaxRsProvider#doValidateProperties(java.util.Map)
	 */
	@Override
	protected void doValidateProperties(Map<String, Object> properties) {
		String baseProperty = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_BASE);
		if (applicationBase == null && (baseProperty == null || baseProperty.isEmpty())) {
			updateStatus(DTOConstants.FAILURE_REASON_VALIDATION_FAILED);
			return;
		}
		if (baseProperty != null && !baseProperty.isEmpty()) {
			applicationBase = baseProperty;
		} 
	}

	/**
	 * Sets an application instance
	 * @param application the application to set
	 */
	protected void setApplication(Application application) {
		if (application == null) {
			throw new IllegalArgumentException("The application argument must not be null");
		}
		setProviderObject(application);
	}
	
	/**
	 * Adds content to the underlying {@link JerseyApplication}, if valid
	 * @param provider the content provider to be added
	 * @return <code>true</code>, if add was successful, otherwise <code>false</code>
	 */
	private boolean doAddContent(JaxRsApplicationContentProvider provider) {
		boolean filterValid = provider.canHandleApplication(this);
		if (filterValid && !isLegacy()) {
			JerseyApplication ja = (JerseyApplication) getProviderObject();
			changed = ja.addContent(provider);
			return changed;
		}
		return filterValid;
	}
	
	/**
	 * Removed content from the underlying {@link JerseyApplication}, if valid
	 * @param provider the content provider to be removed
	 * @return <code>true</code>, if removal was successful, otherwise <code>false</code>
	 */
	private boolean doRemoveContent(JaxRsApplicationContentProvider provider) {
		Application application = getProviderObject();
		if (application instanceof JerseyApplication) {
			JerseyApplication ja = (JerseyApplication) application;
			changed = ja.removeContent(provider);
			return changed;
		}
		return true;
	}

	/**
	 * Returns <code>true</code>, if the application filter property handling is valid.
	 * @param properties the resource/extension properties
	 * @return <code>true</code>, if the handling is valid
	 */
	private boolean isApplicationFilterValid(Map<String, Object> properties) {
		if (isDefault()) {
			logger.log(Level.WARNING, "There is no application select filter valid for the default application");
			return false;
		}
		String applicationFilter = (String) properties.get(JaxRSWhiteboardConstants.JAX_RS_APPLICATION_SELECT);
		if (applicationFilter != null) {
			try {
				Filter filter = FrameworkUtil.createFilter(applicationFilter);
				boolean applicationMatch = filter.matches(getApplicationProperties());
				if (!applicationMatch) {
					logger.log(Level.WARNING, "The given application select filter does not math to this application for this resource/extension: " + getName());
					return false;
				}
			} catch (InvalidSyntaxException e) {
				logger.log(Level.WARNING, "The given application select filter is invalid: " + applicationFilter, e);
				return false;
			}
		} else {
			logger.log(Level.WARNING, "There is no application select filter");
			return false;
		}
		return true;
	}

}
