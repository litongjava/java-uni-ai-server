package com.litongjava.uni.handler;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.uni.services.UniSubtitleService;

public class SubtitleHandler implements HttpRequestHandler {

  private UniSubtitleService subtitleService = Aop.get(UniSubtitleService.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    UploadFile uploadFile = httpRequest.getUploadFile("file");
    String prompt = httpRequest.getParam("prompt");
    String response_format = httpRequest.getParam("response_format");
    String path = httpRequest.getParam("path");
    RespBodyVo index = subtitleService.index(uploadFile, prompt, response_format, path);
    HttpResponse response = TioRequestContext.getResponse();
    response.body(index);
    return response;
  }

}
