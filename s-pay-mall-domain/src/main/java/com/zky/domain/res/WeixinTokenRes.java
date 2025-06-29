package com.zky.domain.res;

import lombok.Data;

/**
 * @author : zky
 * @description : 获取 access token dto对象
 * @createDate : 2025/6/28 15:06
 */
@Data
public class WeixinTokenRes {
    private String access_token;
    private int expires_in;
    private String errcode;
    private String errmsg;
}

