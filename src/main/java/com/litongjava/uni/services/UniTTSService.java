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
import com.litongjava.fishaudio.tts.FishAudioTTSRequest;
import com.litongjava.genie.GenieClient;
import com.litongjava.genie.GenieTTSRequest;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.media.NativeMedia;
import com.litongjava.minimax.MiniMaxHttpClient;
import com.litongjava.minimax.MiniMaxResponseData;
import com.litongjava.minimax.MiniMaxTTSResponse;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.tts.OpenAiTTSClient;
import com.litongjava.tio.core.ChannelContext;
import com.litongjava.tio.core.Tio;
import com.litongjava.tio.http.common.encoder.ChunkEncoder;
import com.litongjava.tio.http.common.sse.ChunkedPacket;
import com.litongjava.tio.http.server.util.SseEmitter;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hex.HexUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.json.JsonUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.tts.TTSPlatform;
import com.litongjava.uni.consts.ResourcesContainer;
import com.litongjava.uni.consts.UniConsts;
import com.litongjava.uni.model.UniTTSResult;
import com.litongjava.uni.tts.PooledNonStreamingTtsKokoroEn;
import com.litongjava.uni.tts.PooledNonStreamingTtsMatchaZh;
import com.litongjava.volcengine.VolceTtsClient;

import lombok.extern.slf4j.Slf4j;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

@Slf4j
public class UniTTSService {

  private static final Striped<Lock> stripedLocks = Striped.lock(1024);

  public UniTTSResult tts(String input, String provider, String voice_id, String language_boost, boolean useCache) {

    log.info("input: {}, provider: {}, voice_id: {},language_boost:{}", input, provider, voice_id, language_boost);

    // 2. 计算 MD5，并从数据库缓存表里查询是否已有生成记录
    String md5 = Md5Utils.md5Hex(input);
    if (useCache) {
      String sql = String.format("SELECT id, path FROM %s WHERE md5 = ? AND provider = ? AND voice = ?",
          UniTableName.UNI_TTS_CACHE);
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
      return synthesis(input, provider, voice_id, language_boost, md5, cacheAudioDir, useCache);
    } finally {
      lock.unlock();
    }

  }

  private UniTTSResult synthesis(String input, String provider, String voice_id, String language_boost,
      //
      String md5, String cacheAudioDir, boolean useCache) {
    long id = SnowflakeIdUtils.id();
    String cacheFilePath = cacheAudioDir + File.separator + id + ".mp3";
    File audioFile = new File(cacheFilePath);

    byte[] bodyBytes = null;

    if (TTSPlatform.volce.equals(provider)) {
      bodyBytes = VolceTtsClient.tts(input);
    } else if (TTSPlatform.fishaudio.equals(provider)) {
      FishAudioTTSRequest vo = new FishAudioTTSRequest();
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

    } else if (TTSPlatform.genie.equals(provider)) {
      GenieTTSRequest reqVo = new GenieTTSRequest(voice_id, input);
      ResponseVo tts = Aop.get(GenieClient.class).tts(reqVo);
      bodyBytes = tts.getBodyBytes();

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
    } else if (TTSPlatform.local_matcha_cn.equals(provider)) {
      try {
        GeneratedAudio synthesize = PooledNonStreamingTtsMatchaZh.synthesize(input, 3, 1.0f);
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

    if (useCache) {
      Row saveRow = Row.by("id", id).set("input", input).set("md5", md5).set("path", cacheFilePath)
          .set("provider", provider).set("voice", voice_id);
      try {
        Db.save(UniTableName.UNI_TTS_CACHE, saveRow);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
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

  public void stream(ChannelContext channelContext, String input, String platform, String voice_id,
      String language_boost, boolean useCache) {

    // 1) 计算 md5（与非流式逻辑一致）
    String md5 = Md5Utils.md5Hex(input);

    // 2) 如果开启缓存，先查库命中就直接回放文件流
    if (useCache) {
      String sql = String.format("SELECT id, path FROM %s WHERE md5 = ? AND provider = ? AND voice = ?",
          UniTableName.UNI_TTS_CACHE);
      Row row = Db.findFirst(sql, md5, platform, voice_id);
      if (row != null) {
        long cacheId = row.getLong("id");
        String path = row.getStr("path");
        File cached = validCachedTts(path, cacheId);
        if (cached != null) {
          // 命中缓存：直接把文件按 chunk 写回客户端并关闭连接
          streamFileAsChunked(channelContext, cached);
          SseEmitter.closeChunkConnection(channelContext);
          return;
        }
      }
    }

    // 3) 未命中缓存：准备写入本地文件（边下边写）
    String cacheAudioDir = UniConsts.DATA_DIR + File.separator + "audio";
    File audioDir = new File(cacheAudioDir);
    if (!audioDir.exists()) {
      audioDir.mkdirs();
    }

    long id = SnowflakeIdUtils.id();
    String cacheFilePath = cacheAudioDir + File.separator + id + ".mp3";
    File audioFile = new File(cacheFilePath);

    // 文件输出流（在回调里写）
    final java.io.OutputStream[] fosRef = new java.io.OutputStream[1];
    try {
      fosRef[0] = new java.io.BufferedOutputStream(new java.io.FileOutputStream(audioFile));
    } catch (Exception e) {
      log.error("Failed to open cache file output stream: {}", cacheFilePath, e);
      // 回退：不落盘，只流式回给客户端
      fosRef[0] = null;
    }

    EventSourceListener listener = new EventSourceListener() {

      @Override
      public void onEvent(EventSource eventSource, String idStr, String type, String data) {
        try {
          MiniMaxTTSResponse speech = JsonUtils.parse(data, MiniMaxTTSResponse.class);
          MiniMaxResponseData audioData = speech.getData();
          String audioHex = audioData.getAudio();
          if (StrUtil.isBlank(audioHex)) {
            return;
          }

          int status = audioData.getStatus();
          if (status == 1) {
            byte[] audioBytes = HexUtils.decodeHex(audioHex);
            // audioData.setAudio("" + audioBytes.length);
            // log.info("data:{}", JsonUtils.toJson(speech));

            // 1) 写回浏览器（chunked）
            ChunkedPacket packet = new ChunkedPacket(ChunkEncoder.encodeChunk(audioBytes));
            Tio.bSend(channelContext, packet);

            // 2) 同步落盘
            if (fosRef[0] != null) {
              fosRef[0].write(audioBytes);
            }
          }
        } catch (Exception e) {
          log.error("stream onEvent error", e);
          // 出错直接关闭连接
          safeCloseOutput(fosRef[0]);
          SseEmitter.closeChunkConnection(channelContext);
          eventSource.cancel();
        }
      }

      @Override
      public void onClosed(EventSource eventSource) {
        // 先关文件
        safeCloseOutput(fosRef[0]);

        // 结束 chunked 连接
        SseEmitter.closeChunkConnection(channelContext);

        // onClosed 入库（useCache=true 才入库）
        if (!useCache) {
          // 不使用缓存的话，临时文件可以按你策略删掉（可选）
          return;
        }

        // 文件有效才入库
        if (!audioFile.exists() || audioFile.length() <= 0) {
          if (audioFile.exists()) {
            try {
              audioFile.delete();
            } catch (Exception ignore) {
            }
          }
          return;
        }

        // 避免并发重复：短锁 + 再查一次
        Lock lock = stripedLocks.get(md5);
        lock.lock();
        try {
          String sql = String.format("SELECT id FROM %s WHERE md5 = ? AND provider = ? AND voice = ?",
              UniTableName.UNI_TTS_CACHE);
          Row existed = Db.findFirst(sql, md5, platform, voice_id);
          if (existed != null) {
            // 已经有人入库了：当前文件可删除，避免垃圾
            try {
              audioFile.delete();
            } catch (Exception ignore) {
            }
            return;
          }

          Row saveRow = Row.by("id", id).set("input", input).set("md5", md5).set("path", cacheFilePath)
              .set("provider", platform).set("voice", voice_id);

          try {
            Db.save(UniTableName.UNI_TTS_CACHE, saveRow);
          } catch (Exception e) {
            log.error("save cache row error", e);
          }
        } finally {
          lock.unlock();
        }
      }

      @Override
      public void onFailure(EventSource eventSource, Throwable t, okhttp3.Response response) {
        log.error("stream onFailure, response: {}", response, t);

        safeCloseOutput(fosRef[0]);

        // 失败时删除未完成文件（避免缓存脏数据）
        if (audioFile.exists()) {
          try {
            audioFile.delete();
          } catch (Exception ignore) {
          }
        }

        SseEmitter.closeChunkConnection(channelContext);
      }
    };

    // 4) 发起流式请求
    MiniMaxHttpClient.speechStream(input, voice_id, language_boost, listener);
  }

  /**
   * 把本地文件以 chunked 方式写到客户端
   */
  private void streamFileAsChunked(ChannelContext channelContext, File file) {
    java.io.InputStream in = null;
    try {
      in = new java.io.BufferedInputStream(new java.io.FileInputStream(file));
      byte[] buf = new byte[16 * 1024];
      int n;
      while ((n = in.read(buf)) != -1) {
        byte[] chunk = (n == buf.length) ? buf : java.util.Arrays.copyOf(buf, n);
        ChunkedPacket packet = new ChunkedPacket(ChunkEncoder.encodeChunk(chunk));
        Tio.bSend(channelContext, packet);
      }
    } catch (Exception e) {
      log.error("streamFileAsChunked error, file={}", file.getAbsolutePath(), e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception ignore) {
        }
      }
    }
  }

  private void safeCloseOutput(java.io.OutputStream out) {
    if (out == null)
      return;
    try {
      out.flush();
    } catch (Exception ignore) {
    }
    try {
      out.close();
    } catch (Exception ignore) {
    }
  }

}