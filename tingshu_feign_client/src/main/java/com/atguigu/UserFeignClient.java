package com.atguigu;

import com.atguigu.result.RetVal;
import com.atguigu.vo.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(value = "tingshu-user")
public interface UserFeignClient {
    @GetMapping("api/user/userInfo/getUserById/{userId}")
    public RetVal<UserInfoVo> getUserById(@PathVariable Long userId);

    @PostMapping("api/user/userInfo/getUserShowPaidMarkOrNot/{albumId}")
    public RetVal<Map<Long, Boolean>> getUserShowPaidMarkOrNot(@PathVariable Long albumId, @RequestBody List<Long> needPayTrackIdList);
}
