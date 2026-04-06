package com.litongjava.tts;

import java.io.File;

import org.junit.Test;

import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.hutool.FileUtil;
import nexus.io.volcengine.VolceTtsClient;

public class VolceTtsClientTest {

  @Test
  public void test() {
    EnvUtils.load();
    byte[] adio = VolceTtsClient.tts("今天天气怎么样");
    FileUtil.writeBytes(adio, new File("2.mp3"));
  }
}
