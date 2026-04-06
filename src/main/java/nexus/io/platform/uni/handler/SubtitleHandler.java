package nexus.io.platform.uni.handler;

import nexus.io.jfinal.aop.Aop;
import nexus.io.model.body.RespBodyVo;
import nexus.io.model.upload.UploadFile;
import nexus.io.platform.uni.services.UniSubtitleService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.HttpRequestHandler;

public class SubtitleHandler implements HttpRequestHandler {

  private UniSubtitleService subtitleService = Aop.get(UniSubtitleService.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    UploadFile uploadFile = httpRequest.getUploadFile("file");
    String prompt = httpRequest.getParam("prompt");
    if (prompt != null) {
      prompt = "The text of audio is:" + prompt;
    }
    String response_format = httpRequest.getParam("response_format");
    if (response_format == null) {
      response_format = "vtt";
    }
    String path = httpRequest.getParam("path");
    RespBodyVo index = subtitleService.index(uploadFile, prompt, response_format, path);
    HttpResponse response = TioRequestContext.getResponse();
    response.body(index);
    return response;
  }

}
