package com.culciful.controller;

import com.culciful.common.api.R;
import com.culciful.security.crypto.RSAUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CommonController {

    @GetMapping("/getConf")
    public R<Map<String, String>> getConf() {
        return R.ok(Map.of("data", RSAUtil.getPublicKey()));
    }
}
