package com.litongjava.uni.config;

import java.io.File;

import com.litongjava.annotation.AConfiguration;
import com.litongjava.annotation.Initialization;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.tio.boot.admin.config.TioAdminDbConfiguration;
import com.litongjava.tio.boot.admin.config.TioAdminInterceptorConfiguration;
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
    //    new TioAdminRedisDbConfiguration().config();
    //    new TioAdminMongoDbConfiguration().config();
    new TioAdminInterceptorConfiguration().config();
    //    new TioAdminHandlerConfiguration().config();
    //
    //    // 获取 HTTP 请求路由器
    TioBootServer server = TioBootServer.me();
    HttpRequestRouter r = server.getRequestRouter();

    if (r != null) {
      ManimTTSHandler manimTTSHandler = Aop.get(ManimTTSHandler.class);
      r.add("/api/manim/tts", manimTTSHandler::index);
      // 获取文件处理器，并添加文件上传和获取 URL 的接口
      //      SystemFileTencentCosHandler systemUploadHandler = Aop.get(SystemFileTencentCosHandler.class);
      //      r.add("/api/system/file/upload", systemUploadHandler::upload);
      //      r.add("/api/system/file/url", systemUploadHandler::getUrl);
    }
    //
    //    // 配置控制器
    //    new TioAdminControllerConfiguration().config();
  }
}
