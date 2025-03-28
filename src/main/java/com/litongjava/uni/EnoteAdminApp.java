package com.litongjava.uni;
import com.litongjava.annotation.AComponentScan;
import com.litongjava.tio.boot.TioApplication;

@AComponentScan
public class EnoteAdminApp {

  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    TioApplication.run(EnoteAdminApp.class, args);
    long end = System.currentTimeMillis();
    System.out.println((end - start) + "(ms)");
  }
}
