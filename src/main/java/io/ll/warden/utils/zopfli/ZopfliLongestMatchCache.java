package io.ll.warden.utils.zopfli;

/**
 * Creator: eustas
 * Date: 3/26/2015
 * Project: Warden
 * Usage: Zopfli porting
 */
public class ZopfliLongestMatchCache {

  private final static int CACHE_LENGTH = 8;

  public final char[] length;
  public final char[] dist;
  private final char[] subLenPos;
  private final byte[] subLenLen;

  ZopfliLongestMatchCache(int maxBlockSize) {
    length = new char[maxBlockSize];
    dist = new char[maxBlockSize];
    subLenPos = new char[CACHE_LENGTH * maxBlockSize];
    subLenLen = new byte[CACHE_LENGTH * maxBlockSize];
  }

  void init(int blockSize) {
    ZopfliCookie.fill0(dist, blockSize);
    int n = blockSize << 3; // * CACHE_LENGTH
    char[] subLenPos = this.subLenPos;
    byte[] subLenLen = this.subLenLen;
    char[] length = this.length;

    char[] charZeroes = ZopfliCookie.charZeroes;
    byte[] byteZeroes = ZopfliCookie.byteZeroes;
    char[] charOnes = ZopfliCookie.charOnes;

    int i = 0;
    while (i < n) {
      int j = i + 65536;
      if (j > n) {
        j = n;
      }
      int l = j - i;
      System.arraycopy(byteZeroes, 0, subLenLen, i, l);
      System.arraycopy(charZeroes, 0, subLenPos, i, l);
      i = j;
    }

    i = 0;
    while (i < blockSize) {
      int j = i + 65536;
      if (j > blockSize) {
        j = blockSize;
      }
      int l = j - i;
      System.arraycopy(charOnes, 0, length, i, l);
      System.arraycopy(charZeroes, 0, subLenPos, i, l);
      i = j;
    }
  }

  void subLenToCache(char[] input, int pos, int len) {
    if (len < 3) {
      return;
    }

    int bestLength = 0;
    int j = pos * CACHE_LENGTH;
    int last = j + CACHE_LENGTH - 1;
    for (int i = 3; i <= len; ++i) {
      if (i == len || input[i] != input[i + 1]) {
        subLenPos[j] = input[i];
        subLenLen[j] = (byte) (i - 3);
        bestLength = i;
        j++;
        if (j > last) {
          break;
        }
      }
    }
    if (j <= last) {
      subLenLen[last] = (byte) (bestLength - 3);
    }
  }

  void cacheToSubLen(int pos, int len, char[] output) {
    if (len < 3) {
      return;
    }

    int maxLength = maxCachedSubLen(pos);
    int prevLength = 0;
    int j = CACHE_LENGTH * pos;
    int last = j + CACHE_LENGTH;
    for (; j < last; ++j) {
      int cLen = (subLenLen[j] & 0xFF) + 3;
      char dist = subLenPos[j];
      for (int i = prevLength; i <= cLen; ++i) {
        output[i] = dist;
      }
      if (cLen == maxLength) {
        break;
      }
      prevLength = cLen + 1;
    }
  }

  int maxCachedSubLen(int pos) {
    pos = pos * CACHE_LENGTH;
    if (subLenPos[pos] == 0) {
      return 0;
    }
    return (subLenLen[pos + CACHE_LENGTH - 1] & 0xFF) + 3;
  }
}
