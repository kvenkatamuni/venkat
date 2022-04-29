package com.paanini.jiffy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/public")
public class TestController {

  @GetMapping("/test")
  public ResponseEntity test(){
    return ResponseEntity.ok("test Success");
  }

}
