package com.outbrain.ob1k.server.registry.endpoints;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.concurrent.ComposableExecutorService;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.server.ctx.DefaultSyncServerRequestContext;
import com.outbrain.ob1k.server.ResponseHandler;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.server.ctx.SyncServerRequestContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
* Created by aronen on 4/24/14.
*/
public class SyncServerEndpoint extends AbstractServerEndpoint {
  private final ComposableExecutorService executorService;
  public final SyncFilter[] filters;

  public SyncServerEndpoint(final Service service, final SyncFilter[] filters, final Method method,
                            final String[] paramNames, final ComposableExecutorService executorService) {
    super(service, method, paramNames);
    this.executorService = Preconditions.checkNotNull(executorService, "executorService must not be null");
    this.filters = filters;
  }

  public <T> T invokeSync(final SyncServerRequestContext ctx) throws ExecutionException {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final SyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked") final
      T result = (T) filter.handleSync(ctx.nextPhase());
      return result;
    } else {
      try {
        @SuppressWarnings("unchecked") final
        T result = (T) method.invoke(service, ctx.getParams());
        return result;
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new ExecutionException(e);
      }
    }
  }

  @Override
  public void invoke(final Request request, final Object[] params, final ResponseHandler handler) {
    final SyncServerRequestContext ctx = new DefaultSyncServerRequestContext(request, this, params);
    final ComposableFuture<Object> response = executorService.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        return invokeSync(ctx);
      }
    });

    handler.handleAsyncResponse(response);
  }
}
