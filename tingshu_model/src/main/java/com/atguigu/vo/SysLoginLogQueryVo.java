package com.atguigu.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SysLoginLogQueryVo {
	
	@Schema(description = "用户账号")
	private String username;

	private String createTimeBegin;
	private String createTimeEnd;

}

