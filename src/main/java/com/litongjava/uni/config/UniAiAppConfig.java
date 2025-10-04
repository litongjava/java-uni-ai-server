package com.litongjava.uni.config;

import java.io.File;

import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.uni.consts.UniConsts;
import com.litongjava.uni.handler.SubtitleHandler;
import com.litongjava.uni.handler.TTSHandler;

public class UniAiAppConfig {

  public void config() {
    new File(UniConsts.DATA_DIR, "audio").mkdirs();
    DbTables.init();
    // 获取 HTTP 请求路由器
    TioBootServer server = TioBootServer.me();
    HttpRequestRouter r = server.getRequestRouter();

    if (r != null) {
      TTSHandler manimTTSHandler = new TTSHandler();
      r.add("/api/manim/tts", manimTTSHandler);
      r.add("/tts", manimTTSHandler);
      
      SubtitleHandler subtitleHandler = new SubtitleHandler();
      r.add("/subtitle", subtitleHandler);
    }
  }
}
