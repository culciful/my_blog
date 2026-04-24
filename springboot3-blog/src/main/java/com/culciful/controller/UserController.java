package com.culciful.controller;

import com.culciful.param.EmailExistParam;
import com.culciful.service.UserInfoService;
import com.culciful.utils.R;
import com.culciful.utils.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author culciful_zy
 * @version 1.0
 * date 2024/1/31 15:25
 * description:
 */

@RestController
@RequestMapping("user")
@CrossOrigin
public class UserController {
    @Autowired
    private UserInfoService userInfoService;

    // @PostMapping("login")
    // public Result login(@RequestBody String requestBody){
    //     // Result result = userInfoService.login(user);
    //     System.out.println("requestBody = " + requestBody);
    //     return null;
    // }
    //
    // @PostMapping("register")
    // public Result register(@RequestBody UserInfo userInfo){
    //     // Result result = userInfoService.login(user);
    //     System.out.println("requestBody = " + userInfo);
    //     userInfoService.saveUserDetails(userInfo);
    //     return null;
    // }

    /**
     * 检查邮箱是否已存在
     * @param emailExistParam
     * @param result
     * @return
     */
    @PostMapping("checkEmailExist")
    public R checkEmailExist(@RequestBody @Validated EmailExistParam emailExistParam, BindingResult result) {
        // 检查是否符合校验注解的规则 符合为false
        boolean b = result.hasErrors();
        if(b) {
            return R.fail(ResultCodeEnum.PARAM_ERROR);
        }
        R r = userInfoService.checkEmailExist(emailExistParam);
        return r;
    }
}
