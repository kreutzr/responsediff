package com.github.kreutzr.responsediff.proxy;

@FunctionalInterface
  public interface ShutdownCommand {
    void execute();
  }