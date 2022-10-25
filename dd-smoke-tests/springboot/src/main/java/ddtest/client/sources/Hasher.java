package ddtest.client.sources;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
  public void sha1() throws NoSuchAlgorithmException {
    MessageDigest.getInstance("SHA1");
  }

  public void md5() throws NoSuchAlgorithmException {
    MessageDigest.getInstance("MD5");
  }
}
