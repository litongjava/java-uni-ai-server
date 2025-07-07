package com.litongjava.uni.config;

import java.io.File;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.admin.config.TioAdminDbConfiguration;
import com.litongjava.tio.boot.server.TioBootServer;
import com.litongjava.tio.http.server.router.HttpRequestRouter;
import com.litongjava.uni.handler.ManimTTSHandler;

@AConfiguration
public class AdminAppConfig {

  @Initialization
  public void config() {
    new File("cache").mkdirs();

    // 配置数据库相关
    new TioAdminDbConfiguration().config();
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
