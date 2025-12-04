package com.litongjava.uni.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.Lock;

import com.google.common.util.concurrent.Striped;
import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.litongjava.byteplus.BytePlusTTSAudio;
import com.litongjava.byteplus.BytePlusTTSHttpStreamClient;
import com.litongjava.consts.UniTableName;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.fishaudio.tts.FishAudioClient;
import com.litongjava.fishaudio.tts.FishAudioTTSRequestVo;
import com.litongjava.media.NativeMedia;
import com.litongjava.minimax.MiniMaxHttpClient;
import com.litongjava.minimax.MiniMaxTTSResponse;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.tts.OpenAiTTSClient;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hex.HexUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tts.TTSPlatform;
import com.litongjava.uni.consts.ResourcesContainer;
import com.litongjava.uni.consts.UniConsts;
import com.litongjava.uni.model.UniTTSResult;
import com.litongjava.uni.tts.PooledNonStreamingTtsKokoroEn;
import com.litongjava.volcengine.VolceTtsClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UniTTSService {

  private static final Striped<Lock> stripedLocks = Striped.lock(1024);

  public UniTTSResult tts(String input, String provider, String voice_id, String language_boost) {

    log.info("input: {}, provider: {}, voice_id: {},language_boost:{}", input, provider, voice_id, language_boost);

    // 2. 计算 MD5，并从数据库缓存表里查询是否已有生成记录
    String md5 = Md5Utils.md5Hex(input);
    String sql = String.format("SELECT id, path FROM %s WHERE md5 = ? AND provider = ? AND voice = ?", UniTableName.UNI_TTS_CACHE);
    Row row = Db.findFirst(sql, md5, provider, voice_id);

    // 3. 如果查到了缓存记录，就尝试读取文件
    if (row != null) {
      long cacheId = row.getLong("id");
      String path = row.getStr("path");
      File cached = validCachedTts(path, cacheId);
      if (cached != null) {
        // 命中缓存且成功读取
        return new UniTTSResult(path, cached);
      }
    }

    Lock lock = stripedLocks.get(md5);
    lock.lock();
    try {
      // 4. 如果缓存无效或不存在，就生成新的音频并写入缓存

      String cacheAudioDir = UniConsts.DATA_DIR + File.separator + "audio";
      File audioDir = new File(cacheAudioDir);
      if (!audioDir.exists()) {
        audioDir.mkdirs();
      }
      return synthesis(input, provider, voice_id, language_boost, md5, cacheAudioDir);
    } finally {
      lock.unlock();
    }

  }

  private UniTTSResult synthesis(String input, String provider, String voice_id, String language_boost,
      //
      String md5, String cacheAudioDir) {
    long id = SnowflakeIdUtils.id();
    String cacheFilePath = cacheAudioDir + File.separator + id + ".mp3";
    File audioFile = new File(cacheFilePath);

    byte[] bodyBytes = null;
    if (TTSPlatform.volce.equals(provider)) {
      bodyBytes = VolceTtsClient.tts(input);

    } else if (TTSPlatform.fishaudio.equals(provider)) {
      FishAudioTTSRequestVo vo = new FishAudioTTSRequestVo();
      vo.setText(input);
      vo.setReference_id(voice_id);
      ResponseVo responseVo = FishAudioClient.speech(vo);
      if (responseVo.isOk()) {
        bodyBytes = responseVo.getBodyBytes();
      } else {
        log.error("FishAudio TTS error: {}", responseVo.getBodyString());
        return new UniTTSResult(null, ResourcesContainer.default_mp3_bytes);
      }

    } else if (TTSPlatform.minimax.equals(provider)) {
      MiniMaxTTSResponse speech = MiniMaxHttpClient.speech(input, voice_id, language_boost);
      String audioHex = speech.getData().getAudio();
      bodyBytes = HexUtils.decodeHex(audioHex);

    } else if (TTSPlatform.byteplus.equals(provider)) {
      BytePlusTTSHttpStreamClient client = new BytePlusTTSHttpStreamClient();
      BytePlusTTSAudio tts = client.tts(input, voice_id);
      bodyBytes = tts.getAudioBytes();

    } else if (TTSPlatform.local_kokoro_en.equals(provider)) {
      try {
        GeneratedAudio synthesize = PooledNonStreamingTtsKokoroEn.synthesize(input, 3, 1.0f);
        String wavCacheFilePath = cacheAudioDir + File.separator + id + ".wav";
        synthesize.save(wavCacheFilePath);
        NativeMedia.toMp3(wavCacheFilePath);
        new File(wavCacheFilePath).delete();
        bodyBytes = FileUtil.readBytes(audioFile);
      } catch (InterruptedException e) {
        return new UniTTSResult(null, ResourcesContainer.default_mp3_bytes);
      }
    } else {
      ResponseVo responseVo = OpenAiTTSClient.speech(input);
      if (responseVo.isOk()) {
        bodyBytes = responseVo.getBodyBytes();
      } else {
        log.error("OpenAI TTS error: {}", responseVo.getBodyString());
        return new UniTTSResult(null, ResourcesContainer.default_mp3_bytes);
      }
    }

    // 5. 将新生成的音频写到本地，并存一条缓存记录
    if (!audioFile.exists()) {
      FileUtil.writeBytes(bodyBytes, audioFile);
    }

    Row saveRow = Row.by("id", id).set("input", input).set("md5", md5).set("path", cacheFilePath).set("provider", provider).set("voice",
        voice_id);
    try {
      Db.save(UniTableName.UNI_TTS_CACHE, saveRow);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return new UniTTSResult(cacheFilePath, audioFile);
  }

  /**
   * 如果 path 有效且文件存在，就尝试读取并返回字节数组；否则删除对应的缓存记录并返回 null。
   */
  private File validCachedTts(String path, long cacheId) {
    if (StrUtil.isBlank(path)) {
      return null;
    }
    String deleteSql = String.format("DELETE FROM %s WHERE id = ?", UniTableName.UNI_TTS_CACHE);
    Path filePath = Paths.get(path);
    if (Files.exists(filePath)) {
      long size;
      try {
        size = Files.size(filePath);
        if (size > 0) {
          log.info("Reading cached TTS file at [{}]", path);
          return filePath.toFile();
        } else {
          Db.delete(deleteSql, cacheId);
          Files.delete(filePath);
          return null;
        }
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }

    } else {
      // 文件实际不存在，直接删除数据库中的缓存记录
      log.warn("Cached file not found at [{}], deleting cache record id={}", path, cacheId);
      Db.delete(deleteSql, cacheId);
      return null;
    }
    return null;
  }
}