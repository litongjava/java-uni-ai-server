package com.litongjava.uni.tts;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.k2fsa.sherpa.onnx.GeneratedAudio;
import com.k2fsa.sherpa.onnx.OfflineTts;
import com.k2fsa.sherpa.onnx.OfflineTtsConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsMatchaModelConfig;
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig;
import com.litongjava.tio.utils.environment.EnvUtils;

public class PooledNonStreamingTtsMatchaZh {

  private static final BlockingQueue<OfflineTts> pool;

  static {
    Integer poolSize = EnvUtils.getInteger("sherpa.pool.size");
    if (poolSize == null) {
      poolSize = Runtime.getRuntime().availableProcessors();
    }
    pool = new LinkedBlockingQueue<>(poolSize);

    String userHome = System.getProperty("user.home");
    String modelHome = userHome + File.separator + "models";

    String acousticModel = modelHome + "/matcha-icefall-zh-baker/model-steps-3.onnx";
    String vocoder = modelHome + "/matcha-icefall-zh-baker/vocos-22khz-univ.onnx";
    String tokens = modelHome + "/matcha-icefall-zh-baker/tokens.txt";
    String lexicon = modelHome + "/matcha-icefall-zh-baker/lexicon.txt";
    String dictDir = modelHome + "/matcha-icefall-zh-baker/dict";

    String[] ruleFstsRelpath = { "/matcha-icefall-zh-baker/phone.fst", "/matcha-icefall-zh-baker/date.fst",
        "/matcha-icefall-zh-baker/number.fst" };

    for (int i = 0; i < ruleFstsRelpath.length; i++) {
      ruleFstsRelpath[i] = modelHome + ruleFstsRelpath[i];
    }
    String ruleFsts = String.join(",", ruleFstsRelpath);

    OfflineTtsMatchaModelConfig matchaModelConfig = OfflineTtsMatchaModelConfig.builder().setAcousticModel(acousticModel)
        .setVocoder(vocoder).setTokens(tokens).setLexicon(lexicon).setDictDir(dictDir).build();

    OfflineTtsModelConfig modelConfig = OfflineTtsModelConfig.builder().setMatcha(matchaModelConfig)
        //
        .setNumThreads(1).setDebug(false)
        .build();

    OfflineTtsConfig config = OfflineTtsConfig.builder().setModel(modelConfig).setRuleFsts(ruleFsts).build();

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
