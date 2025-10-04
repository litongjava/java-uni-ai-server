package com.litongjava.uni.handler;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.util.Resps;
import com.litongjava.uni.model.UniTTSResult;
import com.litongjava.uni.services.UniTTSService;

public class TTSHandler implements HttpRequestHandler {
  UniTTSService manimTTSService = Aop.get(UniTTSService.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    HttpResponse response = TioRequestContext.getResponse();
    String input = httpRequest.getParam("input");
    String platform = httpRequest.getParam("platform");
    String voice_id = httpRequest.getParam("voice_id");

    UniTTSResult result = manimTTSService.tts(input, platform, voice_id);
    byte[] audio = result.getData();
    Resps.bytesWithContentType(response, audio, "audio/mp3");
    response.setHeader("path", result.getPath());
    return response;
  }
}
