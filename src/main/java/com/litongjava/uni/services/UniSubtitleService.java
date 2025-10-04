package com.litongjava.uni.services;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import com.google.common.io.Files;
import com.google.common.util.concurrent.Striped;
import com.litongjava.consts.UniTableName;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.whisper.WhisperClient;
import com.litongjava.tio.http.common.UploadFile;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.FilenameUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UniSubtitleService {

  private static final Striped<Lock> stripedLocks = Striped.lock(1024);

  public RespBodyVo index(UploadFile uploadFile, String prompt, String response_format, String path) {
    String asrFolder = "data" + File.separator + "asr";
    File upload = new File(asrFolder);
    if (!upload.exists()) {
      upload.mkdirs();
    }
    File file = null;
    String md5 = null;
    if (path != null) {
      file = new File(path);
      if (!file.exists()) {
        return RespBodyVo.fail("no such file " + path);
      } else {
        byte[] readBytes = FileUtil.readBytes(file);
        md5 = Md5Utils.md5Hex(readBytes);
      }
    } else {
      byte[] data = uploadFile.getData();
      md5 = Md5Utils.md5Hex(data);
      long id = SnowflakeIdUtils.id();
      String name = uploadFile.getName();
      String suffix = FilenameUtils.getSuffix(name);
      name = id + "." + suffix;
      file = new File(asrFolder + File.separator + id + name);
      try {
        Files.write(data, file);
      } catch (IOException e) {
        e.printStackTrace();
        return RespBodyVo.fail(e.getMessage());
      }
    }

    String sql = String.format("SELECT id, path,text FROM %s WHERE md5 = ? and format=?", UniTableName.UNI_ASR_CACHE);
    Row row = Db.findFirst(sql, md5, response_format);

    // 3. 如果查到了缓存记录，就尝试读取文件
    if (row != null) {
      String text = row.getStr("text");
      return RespBodyVo.ok(text);
    }

    Lock lock = stripedLocks.get(md5);
    lock.lock();
    String bodyString = null;
    try {
      row = Db.findFirst(sql, md5);
      // 3. 如果查到了缓存记录，就尝试读取文件
      if (row != null) {
        String text = row.getStr("text");
        return RespBodyVo.ok(text);
      }
      ResponseVo responseVo = WhisperClient.transcriptions(file, response_format, prompt);
      bodyString = responseVo.getBodyString();

      long id = SnowflakeIdUtils.id();
      Row saveRow = Row.by("id", id).set("text", bodyString).set("md5", md5).set("path", path).set("format",
          response_format);
      try {
        Db.save(UniTableName.UNI_ASR_CACHE, saveRow);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }

    } finally {
      lock.unlock();
    }
    return RespBodyVo.ok(bodyString);
  }
}
