package com.outbrain.ob1k.server.netty;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;

import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;


/**
 * Time: 7/23/14 3:48 PM
 *
 * @author Eran Harel
 */
public class NettyResponse implements Response {
  private final HttpResponseStatus status;
  private final ByteBuf rawContent;
  private final HttpHeaders headers;
  private final Object message;

  public NettyResponse(final HttpResponseStatus status, final ByteBuf rawContent, final Object message, final HttpHeaders headers) {
    this.status = status;
    this.rawContent = rawContent;
    this.headers = headers;
    this.message = message;
  }

  FullHttpResponse toFullHttpResponse(final RequestMarshaller marshaller) throws IOException {
    if (null == rawContent && null == message) {
      return new DefaultFullHttpResponse(HTTP_1_1, status);
    }

    final FullHttpResponse response = rawContent == null ?
        marshaller.marshallResponse(message, status) :
        new DefaultFullHttpResponse(HTTP_1_1, status, rawContent);

    if (headers != null) {
      response.headers().add(headers);
    }

    return response;
  }
}
