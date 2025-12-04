package com.litongjava.uni.handler;

import java.io.File;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.minimax.MiniMaxVoice;
import com.litongjava.minimax.MinimaxLanguageBoost;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.boot.utils.HttpFileDataUtils;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lang.ChineseDetector;
import com.litongjava.tts.TTSPlatform;
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

    // 必须设置,否则cosine会读成希腊语
    String language_boost = "auto";

    if (StrUtil.isEmpty(platform)) {
      // 1. 根据输入文本内容判断默认 provider 和 voice_id

      if (ChineseDetector.isChinese(input)) {
        if (StrUtil.isBlank(platform)) {
          platform = TTSPlatform.minimax;

        }
        if (StrUtil.isBlank(voice_id)) {
//          voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;
          voice_id = MiniMaxVoice.Chinese_Mandarin_Gentleman;
          language_boost = MinimaxLanguageBoost.CHINESE.getCode();
        }
      } else {
        if (StrUtil.isBlank(platform)) {
          platform = TTSPlatform.minimax;
        }
        if (StrUtil.isBlank(voice_id)) {
          // voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;
          voice_id = MiniMaxVoice.English_magnetic_voiced_man;
          language_boost = MinimaxLanguageBoost.ENGLISH.getCode();
        }
      }
    }

    UniTTSResult result = manimTTSService.tts(input, platform, voice_id, language_boost);
    String path = result.getPath();
    response.setHeader("path", path);
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
