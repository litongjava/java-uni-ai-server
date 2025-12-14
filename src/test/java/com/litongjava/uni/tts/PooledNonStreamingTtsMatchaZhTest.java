package com.litongjava.uni.tts;

import org.junit.Test;

import com.k2fsa.sherpa.onnx.GeneratedAudio;

public class PooledNonStreamingTtsMatchaZhTest {

  @Test
  public void test() {
    String text = "某某银行的副行长和一些行政领导表示，他们去过长江" + "和长白山; 经济不断增长。" + "2024年12月31号，拨打110或者18920240511。" + "123456块钱。";
    try {
      GeneratedAudio audio = PooledNonStreamingTtsMatchaZh.synthesize(text, 3, 1.0f);
      String waveFilename = "tts-matcha-icefall-zh.wav";
      audio.save(waveFilename);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
