package com.atguigu.config;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.WxMaConfig;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeChatLoginConfig {
    @Autowired
    private WeChatProperties weChatProperties;
    //交给了spring管理
    @Bean
    public WxMaService wxMaService(){
        WxMaDefaultConfigImpl config = new WxMaDefaultConfigImpl();
        config.setAppid(weChatProperties.getAppId());
        config.setSecret(weChatProperties.getAppSecret());
        config.setMsgDataFormat("JSON");
        WxMaServiceImpl service = new WxMaServiceImpl();
        service.setWxMaConfig(config);
        return service;
    }
}
