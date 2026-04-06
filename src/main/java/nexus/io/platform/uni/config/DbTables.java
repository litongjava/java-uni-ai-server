package nexus.io.platform.uni.config;

import java.net.URL;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import nexus.io.consts.UniTableName;
import nexus.io.db.activerecord.Db;
import nexus.io.tio.utils.hutool.FileUtil;
import nexus.io.tio.utils.hutool.ResourceUtil;

@Slf4j
public class DbTables {

  public static void init() {

    boolean created = createTable(UniTableName.UNI_TTS_CACHE);
    if (created) {
      log.info("created table:{}", UniTableName.UNI_TTS_CACHE);
    }

    created = createTable(UniTableName.UNI_ASR_CACHE);
    if (created) {
      log.info("created table:{}", UniTableName.UNI_ASR_CACHE);
    }

  }

  private static boolean createTable(String userTableName) {
    String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=?";
    List<String> tables = Db.queryListString(sql, userTableName);
    int size = tables.size();
    if (size < 1) {
      URL url = ResourceUtil.getResource("sql/" + userTableName + ".sql");
      String str = FileUtil.readString(url);
      int update = Db.update(str);
      log.info("created:{},{}", userTableName, update);
      return true;
    }
    return false;
  }
}
