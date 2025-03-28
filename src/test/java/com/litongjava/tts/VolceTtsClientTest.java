package com.litongjava.tts;

import java.io.File;

import org.junit.Test;

import com.litongjava.tio.utils.environment.EnvUtils;
import com.litongjava.tio.utils.hutool.FileUtil;
import com.litongjava.volcengine.VolceTtsClient;

public class VolceTtsClientTest {

  @Test
  public void test() {
    EnvUtils.load();
    byte[] adio = VolceTtsClient.tts("今天天气怎么样");
    FileUtil.writeBytes(adio, new File("2.mp3"));
  }
}
