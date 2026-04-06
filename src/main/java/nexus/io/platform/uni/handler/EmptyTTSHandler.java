package nexus.io.platform.uni.handler;

import java.io.File;

import nexus.io.fishaudio.tts.FishAudioReference;
import nexus.io.jfinal.aop.Aop;
import nexus.io.platform.uni.model.UniTTSResult;
import nexus.io.platform.uni.services.UniTTSService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.boot.utils.HttpFileDataUtils;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.HttpRequestHandler;
import nexus.io.tio.utils.http.ContentTypeUtils;
import nexus.io.tio.utils.hutool.FilenameUtils;
import nexus.io.tio.utils.hutool.StrUtil;
import nexus.io.tio.utils.lang.ChineseDetector;
import nexus.io.tts.TTSPlatform;

public class EmptyTTSHandler implements HttpRequestHandler {
  UniTTSService manimTTSService = Aop.get(UniTTSService.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
//    return v0(httpRequest);
    return v1(httpRequest);
  }

  private HttpResponse v1(HttpRequest httpRequest) {
    HttpResponse response = TioRequestContext.getResponse();

    UniTTSResult result = new UniTTSResult("data/empty.mp3");

    File file = result.getData();
    String path = file.getPath();

    // 生成 ETag
    long fileLength = file.length();
    long lastModified = file.lastModified();

    String etag = HttpFileDataUtils.generateETag(file, lastModified, fileLength);

    // 设置缓存相关头部
    String suffix = FilenameUtils.getSuffix(path);
    String contentType = ContentTypeUtils.getContentType(suffix);
    HttpFileDataUtils.setCacheHeaders(response, lastModified, etag, contentType, suffix);

    // 检查客户端缓存
    if (HttpFileDataUtils.isClientCacheValid(httpRequest, lastModified, etag)) {
      response.setStatus(304); // Not Modified
      return response;
    }

    // 检查是否存在 Range 头信息
    String range = httpRequest.getHeader("range");
    if (range != null && range.startsWith("bytes=")) {
      return HttpFileDataUtils.handleRangeRequest(response, file, range, fileLength, contentType);
    } else {
      return HttpFileDataUtils.handleFullFileRequest(response, file, fileLength, contentType);
    }
  }

  public HttpResponse v0(HttpRequest httpRequest) {
    HttpResponse response = TioRequestContext.getResponse();
    String input = httpRequest.getParam("input");
    String platform = httpRequest.getParam("platform");
    String voice_id = httpRequest.getParam("voice_id");
    boolean useCache = httpRequest.getBool("useCache");

    // 必须设置,否则cosine会读成希腊语
    String language_boost = "auto";

    if (StrUtil.isEmpty(platform)) {
      // 1. 根据输入文本内容判断默认 provider 和 voice_id

      if (ChineseDetector.isChinese(input)) {
        if (StrUtil.isBlank(platform)) {
          // platform = TTSPlatform.minimax;
          platform = TTSPlatform.fishaudio;

        }
        if (StrUtil.isBlank(voice_id)) {
//          voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;
//          voice_id = MiniMaxVoice.Chinese_Mandarin_Gentleman;
//          language_boost = MinimaxLanguageBoost.CHINESE.getCode();
          voice_id = FishAudioReference.Chinese_Lei_Jun;

        }
      } else {
        if (StrUtil.isBlank(platform)) {
          platform = TTSPlatform.fishaudio;

        }
        if (StrUtil.isBlank(voice_id)) {
          // voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;
          // voice_id = MiniMaxVoice.English_magnetic_voiced_man;
          // language_boost = MinimaxLanguageBoost.ENGLISH.getCode();
          voice_id = FishAudioReference.English_Donald_J_Trump;
        }
      }
    }

    UniTTSResult result = manimTTSService.tts(input, platform, voice_id, language_boost, useCache);
    String path = result.getPath();
    if (path != null) {
      response.setHeader("path", path);
    }
    File file = result.getData();

    // 生成 ETag
    long fileLength = file.length();
    long lastModified = file.lastModified();

    String etag = HttpFileDataUtils.generateETag(file, lastModified, fileLength);

    // 设置缓存相关头部
    String suffix = FilenameUtils.getSuffix(path);
    String contentType = ContentTypeUtils.getContentType(suffix);
    HttpFileDataUtils.setCacheHeaders(response, lastModified, etag, contentType, suffix);

    // 检查客户端缓存
    if (HttpFileDataUtils.isClientCacheValid(httpRequest, lastModified, etag)) {
      response.setStatus(304); // Not Modified
      return response;
    }

    // 检查是否存在 Range 头信息
    String range = httpRequest.getHeader("range");
    if (range != null && range.startsWith("bytes=")) {
      return HttpFileDataUtils.handleRangeRequest(response, file, range, fileLength, contentType);
    } else {
      return HttpFileDataUtils.handleFullFileRequest(response, file, fileLength, contentType);
    }
  }
}
