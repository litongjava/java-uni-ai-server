package com.litongjava.uni.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UniTTSResult {
  private String path;
  private byte[] data;
}
