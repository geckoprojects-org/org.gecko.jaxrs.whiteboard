# Jakarta RESTful Web Services Whiteboard Implementation

This implementation for the Jakarta namespace has moved it home to the Eclipse foundation:
https://github.com/osgi/jakartarest-osgi

It is the compiant implementation for the Jakarta RESTful Web Services Whiteboard:
https://docs.osgi.org/specification/osgi.cmpn/8.1.0/service.jakartars.html

# Gecko.io JaxRs Whiteboard Implementation

This is a OSGi JaxRs Whiteboard Implementation based on Eclipse Jersey.

https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.jaxrs.html

https://eclipse-ee4j.github.io/jersey/

This implementation is **compliant** on respect to the OSGi JaxRs Whiteboard specifications.

We use Jersey 2.36 together with HK2 2.6.2. To use it you will need the following bundles:

* **org.gecko.rest.jersey** - The whiteboard implementation
* **org.gecko.rest.jersey.config** - The whiteboard implementation default configuration, when you want to use it
* **org.gecko.rest.jersey.sse** - A fragment for the use of server sent events

You will find the Release and Snapshot artifacts at Maven Central, respectively at https://oss.sonatype.org.

```
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey</artifactId>
  <version>${version}</version>
</dependency>
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey.config</artifactId>
  <version>${version}</version>
</dependency>
<dependency>
  <groupId>org.geckoprojects.jaxrs</groupId>
  <artifactId>org.gecko.rest.jersey.sse</artifactId>
  <version>${version}</version>
</dependency>
```

For those, who use Gradle:

```
org.geckoprojects.jaxrs:org.gecko.rest.jersey:${version}
org.geckoprojects.jaxrs:org.gecko.rest.jersey.config:${version}
org.geckoprojects.jaxrs:org.gecko.rest.jersey.sse:${version}
```

## Setup

The default whiteboard uses the Felix Jetty bundle. It starts the server with the default application under 

http://localhost:8181/rest

For this you also need the *org.gecko.rest.jersey.config* fragment in addition to the *org.gecko.rest.jersey* bundle

You can change these setting by using the OSGi Configurator or the Configuration Admin like this:

```json
{
  ":configurator:resource-version": 1,
  
	"JaxRsWhiteboardComponent": 
  	{
	    "jersey.port": 8080,
		"jersey.jaxrs.whiteboard.name" : "demo",
		"jersey.context.path" : "demo" 
	}
}
```

This would run the server at

http://localhost:8080/demo

Please note, that the Felix Jetty implementation runs the OSGi HTTP Servifce by default at port 8080. It may come to an conflict, with the port in this JaxRs server configuration. Therefore you may use the system property `org.osgi.service.http.port=-1` to deactivate the HTTP Service.

## Example 

When using the JaxRs Whiteboard, you register you REST resources and extensions as a service. There are some useful Meta-Annotations, that create component properties for you.
```java
@RequireJaxrsWhiteboard
@JaxrsResource
@JaxrsName("demo")
@Component(service = DemoResource.class, scope = ServiceScope.PROTOTYPE)
@Path("/")
public class DemoResource {

	@GET
	@Path("/hello")
	public String hello() {
		return "Hello World!";
	}

}
```

With the **RequireJaxrsWhiteboard** annotation, you define the dependency to the implementation. In our case Jersey. You need the Whiteboard dependencies in you workspace. 

## Gradle Library Support

When adding the JaxRs Library to you dependencies:
```
org.geckoprojects.jaxrs:org.gecko.rest.bnd.library:${version}
```
you can simply but the instruction `-library: geckoJersey` and you will find a repository in you bnd workspace after reloading the workspace.

It brings you all dependencies, you need to run Jersey. In addition to that it also brings a dependency to:
```
org.geckoprojects.jaxrs:org.gecko.rest.bnd.project.library:${version}
```
This library adds support for *bndrun* files. Calling the instruction `-library: enableJersey` within a *bndrun* adds all Jersey and JaxRs Whiteboard dependencies ti the *runbundles*

If you use the library instruction within a *bnd.bnd* file, it adds the JaxRs API to the buildpath.

Furthermore this dependency brings three bndtools project templates:
1. JaxRs Resource with default configuration
2. JaxRs Resource with Jetty Runtime an a corresponding configuration file
3. JaxRs Resource with a configuration for the use with the OSGI HTTP Whiteboard

All these projects can be imported using the bndtools project wizard.
