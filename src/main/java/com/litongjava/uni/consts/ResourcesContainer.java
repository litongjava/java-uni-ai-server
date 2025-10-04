package com.litongjava.uni.consts;

import java.io.File;

import com.litongjava.tio.utils.hutool.FileUtil;

public interface ResourcesContainer {
  byte[] default_mp3_bytes = FileUtil.readBytes(new File("data", "default.mp3"));
}
