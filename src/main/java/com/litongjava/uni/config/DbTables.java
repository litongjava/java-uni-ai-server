package com.litongjava.uni.config;

import java.net.URL;
import java.util.List;

import com.litongjava.consts.UniTableName;
import com.litongjava.db.activerecord.Db;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.ResourceUtil;

import lombok.extern.slf4j.Slf4j;

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
