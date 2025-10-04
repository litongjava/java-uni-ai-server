package com.litongjava.uni.handler;

import com.litongjava.openai.whisper.WhisperClient;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;

public class SubtitleHandler implements HttpRequestHandler {

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    UploadFile uploadFile = httpRequest.getUploadFile("file");
    String response_format = httpRequest.getParam("response_format");
    String prompt = httpRequest.getParam("prompt");
    byte[] data = uploadFile.getData();
    WhisperClient.transcriptions(null, prompt);
    return null;
  }

}
