package com.litongjava.uni.model;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UniTTSResult {

  private String path;
  private File data;

  public UniTTSResult(String path) {
    this.path = path;
    this.data = new File(path);
  }
}
