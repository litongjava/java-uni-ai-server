package com.litongjava.uni.handler;

import com.litongjava.byteplus.BytePlusVoice;
import com.litongjava.fishaudio.tts.FishAudioReference;
import com.litongjava.genie.GenieCharacter;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.minimax.MiniMaxVoice;
import com.litongjava.minimax.MinimaxLanguageBoost;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.handler.HttpRequestHandler;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.http.ContentTypeUtils;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lang.ChineseDetector;
import com.litongjava.tts.TTSPlatform;
import com.litongjava.uni.services.UniTTSService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TTSStreamHandler implements HttpRequestHandler {
  UniTTSService manimTTSService = Aop.get(UniTTSService.class);

  @Override
  public HttpResponse handle(HttpRequest httpRequest) throws Exception {
    log.info("request id:" + httpRequest.getId());
    // 获取channelContext
    ChannelContext channelContext = httpRequest.getChannelContext();

    HttpResponse response = TioRequestContext.getResponse();

    // 文件扩展名，根据实际情况设置
    String fileExt = "mp3";
    String contentType = ContentTypeUtils.getContentType(fileExt);

    // 设置为流式输出,这样不会计算content-length,because Content-Length can't be present with
    // Transfer-Encoding
    response.setStream(true);
    // 设置响应头
    response.addHeader(HeaderName.Transfer_Encoding, HeaderValue.from("chunked"));
    response.addHeader(HeaderName.Content_Type, HeaderValue.from(contentType));
    CORSUtils.enableCORS(response);

    response.setSend(false);

    String input = httpRequest.getParam("input");
    String platform = httpRequest.getParam("platform");
    String voice_id = httpRequest.getParam("voice_id");
    Boolean useCache = httpRequest.getBoolean("useCache");
    if (useCache == null) {
      useCache = true;
    }

    // 必须设置,否则cosine会读成希腊语
    String language_boost = "auto";

    if (StrUtil.isEmpty(platform)) {
      // 1. 根据输入文本内容判断默认 provider 和 voice_id

      if (ChineseDetector.isChinese(input)) {
        if (StrUtil.isBlank(platform)) {
          // platform = TTSPlatform.minimax;
//          platform = TTSPlatform.fishaudio;
          platform = EnvUtils.getStr("tts.platform", TTSPlatform.minimax);

        }
        if (StrUtil.isBlank(voice_id)) {
          if (TTSPlatform.fishaudio.equals(platform)) {
            voice_id = FishAudioReference.Chinese_Lei_Jun;

          } else if (TTSPlatform.minimax.equals(platform)) {
            voice_id = MiniMaxVoice.Chinese_Mandarin_Gentleman;
            language_boost = MinimaxLanguageBoost.CHINESE.getCode();

          } else if (TTSPlatform.byteplus.equals(platform)) {
            voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;

          } else if (TTSPlatform.genie.equals(platform)) {
            voice_id = GenieCharacter.feibi;
          }
        }
      } else {
        if (StrUtil.isBlank(platform)) {
          platform = EnvUtils.getStr("tts.platform", TTSPlatform.minimax);
        }
        if (StrUtil.isBlank(voice_id)) {
          if (TTSPlatform.fishaudio.equals(platform)) {
            voice_id = FishAudioReference.English_Donald_J_Trump;
          
          } else if (TTSPlatform.minimax.equals(platform)) {
            voice_id = MiniMaxVoice.English_Explanatory_Man;
            language_boost = MinimaxLanguageBoost.ENGLISH.getCode();
          
          } else if (TTSPlatform.byteplus.equals(platform)) {
            voice_id = BytePlusVoice.zh_female_cancan_mars_bigtts;
          
          } else if (TTSPlatform.genie.equals(platform)) {
            voice_id = GenieCharacter.thirtyseven;
          }
        }
      }
    }

    manimTTSService.stream(response,channelContext, input, platform, voice_id, language_boost, useCache);

    return response;
  }
}
