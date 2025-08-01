package com.litongjava.uni.tts;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.litongjava.tio.utils.environment.EnvUtils;

public class PooledNonStreamingTtsKokoroEn {
  private static final BlockingQueue<OfflineTts> pool;

  static {
    Integer poolSize = EnvUtils.getInteger("sherpa.pool.size");
    if (poolSize == null) {
      poolSize = Runtime.getRuntime().availableProcessors();
    }
    pool = new LinkedBlockingQueue<>(poolSize);

    String userHome = System.getProperty("user.home");
    String model = userHome + "/models/kokoro-en-v0_19/model.onnx";
    String voices = userHome + "/models/kokoro-en-v0_19/voices.bin";
    String tokens = userHome + "./models/kokoro-en-v0_19/tokens.txt";
    String dataDir = userHome + "/models/kokoro-en-v0_19/espeak-ng-data";

    OfflineTtsKokoroModelConfig kokoroModelConfig = OfflineTtsKokoroModelConfig.builder().setModel(model)
        .setVoices(voices).setTokens(tokens).setDataDir(dataDir).build();

    OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder().setKokoro(kokoroModelConfig).setNumThreads(2)
        .setDebug(false).build();

    OfflineTtsConfig config = OfflineTtsConfig.builder().setModel(modelConfig).build();

    for (int i = 0; i < poolSize; i++) {
      pool.add(new OfflineTts(config));
    }
  }

  public static GeneratedAudio synthesize(String text, int sid, float speed) throws InterruptedException {
    OfflineTts tts = pool.take();
    if (tts == null) {
      throw new RuntimeException("No TTS instance available");
    }
    try {
      return tts.generate(text, sid, speed);
    } finally {
      pool.offer(tts);
    }
  }

  public static void shutdown() {
    for (OfflineTts tts : pool) {
      tts.release();
    }
  }
}
