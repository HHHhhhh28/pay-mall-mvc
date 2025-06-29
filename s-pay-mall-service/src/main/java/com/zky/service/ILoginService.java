package com.zky.service;

import java.io.IOException;

/**
 * @author : zky
 * @description : 微信服务
 * @createDate : 2025/6/28 20:43
 */
public interface ILoginService {
    String createQrCodeTicket() throws Exception;

    String checkLogin(String ticket);

    void saveLoginState(String ticket, String openid) throws IOException;

}

