package com.litongjava.uni.services;

import java.io.File;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.fishaudio.tts.FishAudioClient;
import com.litongjava.fishaudio.tts.FishAudioTTSRequestVo;
import com.litongjava.minimax.MiniMaxHttpClient;
import com.litongjava.minimax.MiniMaxTTSResponse;
import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.tts.OpenAiTTSClient;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.tio.utils.hutool.StrUtil;
import com.litongjava.tio.utils.lang.ChineseUtils;
import com.litongjava.tio.utils.snowflake.SnowflakeIdUtils;
import com.litongjava.uni.consts.TTSPlatform;
import com.litongjava.uni.consts.UniTableName;
import com.litongjava.volcengine.VolceTtsClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManimTTSService {

  public byte[] tts(String input, String provider, String voice_id) {
    //    if (ChineseUtils.containsChinese(input)) {
    //      if (StrUtil.isBlank(provider)) {
    //        provider = "volce";
    //      }
    //      if (StrUtil.isBlank(voice_id)) {
    //        voice_id = "zh_male_beijingxiaoye_moon_bigtts";
    //      }
    //    } else {
    //      if (StrUtil.isBlank(provider)) {
    //        provider = "openai";
    //      }
    //      if (StrUtil.isBlank(voice_id)) {
    //        voice_id = "shimmer";
    //      }
    //    }

    if (ChineseUtils.containsChinese(input)) {
      if (StrUtil.isBlank(provider)) {
        provider = "minimax";
      }
      if (StrUtil.isBlank(voice_id)) {
        voice_id = "Chinese_Mandarin_Gentleman";
      }
    } else {
      if (StrUtil.isBlank(provider)) {
        provider = "minimax";
      }
      if (StrUtil.isBlank(voice_id)) {
        voice_id = "English_expressive_narrator ";
      }
    }
    log.info("input:{},{},{}", input, provider, voice_id);

    String md5 = Md5Utils.getMD5(input);
    String sql = " select id,path from %s where md5=? and provider=? and voice=?";
    sql = String.format(sql, UniTableName.UNI_TTS_CACHE);
    Row row = Db.findFirst(sql, md5, provider, voice_id);

    if (row != null) {
      String path = row.getStr("path");
      Long id = row.getLong("id");
      if (path != null) {
        File file = new File(path);
        if (file.exists()) {
          log.info("read file:{}", path);
          return FileUtil.readBytes(file);
        } else {
          sql = " delete from %s where id=?";
          sql = String.format(sql, UniTableName.UNI_TTS_CACHE);
          Db.delete(sql, id);
        }
      }
    }

    byte[] bodyBytes = null;
    long id = SnowflakeIdUtils.id();
    String path = "cache" + File.separator + id + ".mp3";

    File audioFile = new File(path);
    if (TTSPlatform.volce.equals(provider)) {
      bodyBytes = VolceTtsClient.tts(input);

    } else if (TTSPlatform.fishaudio.equals(provider)) {
      // 构造请求对象，并指定参考语音ID（发音人）
      FishAudioTTSRequestVo vo = new FishAudioTTSRequestVo();
      vo.setText(input);
      vo.setReference_id(voice_id);
      // 使用 FishAudioClient 发起请求
      ResponseVo responseVo = FishAudioClient.speech(vo);
      if (responseVo.isOk()) {
        // 处理返回的音频数据，例如保存到文件
        bodyBytes = responseVo.getBodyBytes();
      } else {
        log.error(responseVo.getBodyString());
        path = "default.mp3";
        return FileUtil.readBytes(audioFile);
      }
    } else if (TTSPlatform.minimax.equals(provider)) {
      MiniMaxTTSResponse speech = MiniMaxHttpClient.speech(input, voice_id);
      String audio = speech.getData().getAudio();
      
      byte[] decodeToBytes;
      
      try {
        decodeToBytes = Hex.decodeHex(audio);
        FileUtil.writeBytes(decodeToBytes, audioFile);
        return decodeToBytes;
      } catch (DecoderException e) {
        e.printStackTrace();
        path = "default.mp3";
        return FileUtil.readBytes(audioFile);
      }

    } else {
      ResponseVo responseVo = OpenAiTTSClient.speech(input);
      if (responseVo.isOk()) {
        bodyBytes = responseVo.getBodyBytes();
      } else {
        log.error(responseVo.getBodyString());
        path = "default.mp3";
        return FileUtil.readBytes(audioFile);
      }
    }

    File file = audioFile;
    FileUtil.writeBytes(bodyBytes, file);
    Row saveRow = Row.by("id", id).set("input", input).set("md5", md5).set("path", path)
        //
        .set("provider", provider).set("voice", voice_id);
    Db.save(UniTableName.UNI_TTS_CACHE, saveRow);
    return bodyBytes;
  }

}
