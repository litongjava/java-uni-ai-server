package nexus.io.platform.uni.config;

import nexus.io.annotation.AConfiguration;
import nexus.io.annotation.Initialization;
import nexus.io.tio.boot.admin.config.TioAdminDbConfiguration;

@AConfiguration
public class AdminAppConfig {

  @Initialization
  public void config() {
    // 配置数据库相关
    new TioAdminDbConfiguration().config();
    new UniAiAppConfig().config();
  }
}
