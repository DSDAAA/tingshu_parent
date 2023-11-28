package com.atguigu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description="登录对象")
public class LoginVo {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "手机验证码")
    private String code;
    /**
     * 手机号
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
