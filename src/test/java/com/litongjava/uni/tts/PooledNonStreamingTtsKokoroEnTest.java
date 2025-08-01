package com.litongjava.uni.tts;

import org.junit.Test;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.litongjava.media.NativeMedia;

public class PooledNonStreamingTtsKokoroEnTest {

  @Test
  public void test() {
    String text = "Today as always, men fall into two groups: slaves and free men. Whoever does not have"
        + " two-thirds of his day for himself, is a slave, whatever he may be: a statesman, a"
        + " businessman, an official, or a scholar.";
    try {
      GeneratedAudio audio = PooledNonStreamingTtsKokoroEn.synthesize(text, 3, 1.0f);
      String waveFilename = "tts-kokoro-en.wav";
      audio.save(waveFilename);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
  @Test
  public void toMp3() {
    String waveFilename = "tts-kokoro-en.wav";
    String mp3 = NativeMedia.toMp3(waveFilename);
    System.out.println(mp3);
  }
}
