package com.litongjava.uni.config;

import java.io.File;

import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.uni.handler.ManimTTSHandler;

public class UniAiAppConfig {

  public void config() {
    new File("cache").mkdirs();
    DbTables.init();
    // 获取 HTTP 请求路由器
    TioBootServer server = TioBootServer.me();
    HttpRequestRouter r = server.getRequestRouter();

    if (r != null) {
      ManimTTSHandler manimTTSHandler = Aop.get(ManimTTSHandler.class);
      r.add("/api/manim/tts", manimTTSHandler::index);
    }
  }
}
