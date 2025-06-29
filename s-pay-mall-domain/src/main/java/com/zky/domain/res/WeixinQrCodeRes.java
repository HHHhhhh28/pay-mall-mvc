package com.zky.domain.res;

import lombok.Data;

/**
 * @author : zky
 * @description : 获取微信登录二维码响应对象
 * @createDate : 2025/6/28 16:07
 */
@Data
public class WeixinQrCodeRes {

    private String ticket;
    private Long expire_seconds;
    private String url;

}

