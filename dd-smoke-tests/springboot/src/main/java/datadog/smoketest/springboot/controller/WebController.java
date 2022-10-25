package datadog.smoketest.springboot.controller;

import ddtest.client.sources.Hasher;
import javax.annotation.PostConstruct;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

  @PostConstruct
  public void setup() {
    try {
      new Hasher().sha1();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @RequestMapping("/greeting")
  public String greeting() {
    return "Sup Dawg";
  }

  @RequestMapping("/weakhash")
  public String weakhash() {
    try {
      new Hasher().md5();
      return "MessageDigest.getInstance executed";
    } catch (Exception e) {
      return e.toString();
    }
  }
}
