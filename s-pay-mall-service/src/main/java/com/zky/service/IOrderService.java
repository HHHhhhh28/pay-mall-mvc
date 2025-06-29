package com.zky.service;

import com.zky.domain.req.ShopCartReq;
import com.zky.domain.res.PayOrderRes;

/**
 * @author : zky
 * @description : 订单服务接口
 * @createDate : 2025/6/29 15:00
 */
public interface IOrderService {
    PayOrderRes createOrder(ShopCartReq shopCartReq) throws Exception;
}

