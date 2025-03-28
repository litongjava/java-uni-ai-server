package com.litongjava.uni.services;

import java.io.File;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.tts.OpenAiTTSClient;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.lang.ChineseUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.uni.consts.UniTableName;
import com.litongjava.volcengine.VolceTtsClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManimTTSService {

  public byte[] tts(String input) {
    log.info("input:{}", input);
    String md5 = Md5Utils.getMD5(input);
    String sql = " select path from %s where md5=?";
    sql = String.format(sql, UniTableName.UNI_TTS_CACHE);
    String path = Db.queryStr(sql, md5);
    
    if (path != null) {
      File file = new File(path);
      if (file.exists()) {
        log.info("read file:{}", path);
        return FileUtil.readBytes(file);
      } else {
        sql = " delete from %s where md5=?";
        sql = String.format(sql, UniTableName.UNI_TTS_CACHE);
        Db.delete(sql, md5);
      }
    }
    byte[] bodyBytes = null;
    boolean containsChinese = ChineseUtils.containsChinese(input);
    if (containsChinese) {
      bodyBytes = VolceTtsClient.tts(input);
    } else {
      ResponseVo responseVo = OpenAiTTSClient.speech(input);
      if (responseVo.isOk()) {
        bodyBytes = responseVo.getBodyBytes();
      } else {
        log.error(responseVo.getBodyString());
      }
    }

    FileUtil.writeBytes(bodyBytes, new File("1.mp3"));
    long id = SnowflakeIdUtils.id();
    path = "cache" + File.separator + id + ".mp3";

    File file = new File(path);
    FileUtil.writeBytes(bodyBytes, file);
    Row row = Row.by("id", id).set("input", input).set("md5", md5).set("path", path);
    Db.save(UniTableName.UNI_TTS_CACHE, row);

    return bodyBytes;
  }

}
