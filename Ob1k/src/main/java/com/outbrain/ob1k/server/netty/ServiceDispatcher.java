package com.outbrain.ob1k.server.netty;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.ResponseHandler;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;

/**
 * User: aronen
 * Date: 8/8/13
 * Time: 5:33 PM
 */
public class ServiceDispatcher {
  private final ServiceRegistry registry;
  private final RequestMarshallerRegistry marshallerRegistry;


  public ServiceDispatcher(final ServiceRegistry registry, final RequestMarshallerRegistry marshallerRegistry) {
    this.registry = registry;
    this.marshallerRegistry = marshallerRegistry;
  }

  public void callServiceRequest(final Request request, final ResponseHandler handler)
      throws InvocationTargetException, IllegalAccessException, IOException {

    final String path = request.getPath();
    final AbstractServerEndpoint endpoint = registry.findEndpoint(path, request.getPathParams());
    if (endpoint == null) {
      throw new IllegalArgumentException("no matching service/method found for path: " + path);
    }

    callMethod(endpoint, request, handler);
  }

  private void callMethod(final AbstractServerEndpoint endpoint, final Request request, final ResponseHandler handler)
      throws InvocationTargetException, IllegalAccessException, IOException {

    final Object[] params;
    final Method method = endpoint.method;
    final Class<?>[] parameterTypes = method.getParameterTypes();

    if (parameterTypes.length == 1 && parameterTypes[0] == Request.class) {
      params = new Object[]{ request };
    } else {
      final RequestMarshaller marshaller = marshallerRegistry.getMarshaller(request.getContentType());
      params = marshaller.unmarshallRequestParams(request, method, endpoint.paramNames);
    }

    endpoint.invoke(request, params, handler);
  }

}
