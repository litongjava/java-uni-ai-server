package com.litongjava.tts;

import java.io.File;

import org.junit.Test;

import nexus.io.model.http.response.ResponseVo;
import nexus.io.openai.tts.OpenAiTTSClient;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.hutool.FileUtil;

public class OpenAiTTSClientTest {

  @Test
  public void testTTS() {
    EnvUtils.load();
    ResponseVo responseVo = OpenAiTTSClient.speech("语音合成失败,请联系管理员");
    if(responseVo.isOk()){
      FileUtil.writeBytes(responseVo.getBodyBytes(), new File("default.mp3"));
    }else {
      System.out.println(responseVo.getBodyString());
    }
  }
}
