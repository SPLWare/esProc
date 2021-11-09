package com.raqsoft.common;

import java.io.*;

public interface IRecord {
  public byte[] serialize() throws IOException;
  public void fillRecord(byte[] bytes) throws IOException, ClassNotFoundException;
}
