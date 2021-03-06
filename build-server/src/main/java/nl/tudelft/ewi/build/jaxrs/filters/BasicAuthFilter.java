package nl.tudelft.ewi.build.jaxrs.filters;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import nl.tudelft.ewi.build.Config;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.interception.PostMatchContainerRequestContext;
import org.jboss.resteasy.util.Base64;

import com.google.common.base.Strings;
import com.google.inject.Singleton;

@Singleton
@javax.ws.rs.ext.Provider
public class BasicAuthFilter implements ContainerRequestFilter {
	
	private final Config config;

	@Inject
	BasicAuthFilter(Config config) {
		this.config = config;
	}
	
	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		if (!(ctx instanceof PostMatchContainerRequestContext)) {
			return;
		}
		
		PostMatchContainerRequestContext context = (PostMatchContainerRequestContext) ctx;
		ResourceMethodInvoker invoker = context.getResourceMethod();
		Method method = invoker.getMethod();
		Class<?> resource = method.getDeclaringClass();
		
		checkRequireAuthentication(ctx, method, resource);
	}
	
	private void checkRequireAuthentication(ContainerRequestContext ctx, Method method, Class<?> resource) {
		RequireAuthentication annotation = getAnnotation(method, resource, RequireAuthentication.class);
		if (annotation != null) {
			String authHeader = ctx.getHeaderString("Authorization");
			if (Strings.isNullOrEmpty(authHeader) || !authHeader.startsWith("Basic ")) {
				fail(ctx, Status.UNAUTHORIZED, "You need to be authorized to access this resource.");
				return; 
			}
			
			String username = null;
			String password = null;
			
			try {
				authHeader = authHeader.substring(6);
				String decodedHeader = new String(Base64.decode(authHeader));
				String[] chunks = decodedHeader.split(":");
				if (chunks.length != 2) {
					throw new IOException();
				}
				
				username = chunks[0];
				password = chunks[1];
			}
			catch (IOException e) {
				fail(ctx, Status.UNAUTHORIZED, "Could not decode your Authorization header.");
				return;
			}

			if (!config.getClientId().equals(username) || !config.getClientSecret().equals(password)) {
				fail(ctx, Status.UNAUTHORIZED, "You have specified invalid credentials.");
			}
		}
	}
	
	private <T extends Annotation> T getAnnotation(Method method, Class<?> resource, Class<T> annotationClass) {
		T annotation = method.getAnnotation(annotationClass);
		if (annotation == null) {
			annotation = resource.getAnnotation(annotationClass);
		}
		return annotation;
	}
	
	private void fail(ContainerRequestContext context, Status status, String message) {
		Response response = Response.status(status)
				.entity(message)
				.build();
		
		context.abortWith(response);
	}
	
}
