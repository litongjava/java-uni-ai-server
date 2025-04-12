package com.litongjava.tts;

import java.io.File;

import org.junit.Test;

import com.litongjava.model.http.response.ResponseVo;
import com.litongjava.openai.tts.OpenAiTTSClient;
import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.FileUtil;

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
