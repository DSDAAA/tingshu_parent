package com.atguigu.login;

import com.atguigu.constant.RedisConstant;
import com.atguigu.entity.UserInfo;
import com.atguigu.execption.GuiguException;
import com.atguigu.result.ResultCodeEnum;
import com.atguigu.util.AuthContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
public class LoginAspect {
    @Autowired
    RedisTemplate redisTemplate;

    //只要有TingShuLogin注解就进行切面编程
    @Around("@annotation(com.atguigu.login.TingShuLogin)")
    public void process(ProceedingJoinPoint joinPoint) throws Throwable {
        //拿到请求里面带的token
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String token = request.getHeader("token");

        //拿到目标方法上面的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = signature.getMethod();
        TingShuLogin annotation = targetMethod.getAnnotation(TingShuLogin.class);
        if (annotation.required()) {
            //需要登录 Todo 后期还会详细解释
            if (StringUtils.isEmpty(token)) {
                throw new GuiguException(ResultCodeEnum.UN_LOGIN);
            }
            UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
            if (userInfo == null) {
                //这是一个线程独享的一个区域
                throw new GuiguException(ResultCodeEnum.UN_LOGIN);
            }
        }
        //看redis里是否有登录信息
        if (!StringUtils.isEmpty(token)) {
            UserInfo userInfo = (UserInfo) redisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);
            if (userInfo != null) {
                //这是一个线程独享的一个区域
                AuthContextHolder.setUserId(userInfo.getId());
                AuthContextHolder.setUsername(userInfo.getNickname());
            }
        }
        //执行目标方法
        joinPoint.proceed();
    }
}
