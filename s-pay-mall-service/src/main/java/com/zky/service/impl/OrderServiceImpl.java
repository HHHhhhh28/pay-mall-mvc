package com.zky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.google.common.eventbus.EventBus;
import com.zky.common.constants.Constants;
import com.zky.dao.IOrderDao;
import com.zky.domain.po.PayOrder;
import com.zky.domain.req.ShopCartReq;
import com.zky.domain.res.PayOrderRes;
import com.zky.domain.vo.ProductVO;
import com.zky.service.IOrderService;
import com.zky.service.rpc.ProductRPC;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author : zky
 * @description : 订单服务
 * @createDate : 2025/6/29 15:03
 */
@Slf4j
@Service
public class OrderServiceImpl implements IOrderService {
    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    @Resource
    private IOrderDao orderDao;
    @Resource
    private ProductRPC productRPC;
    @Resource
    private AlipayClient alipayClient;

    @Resource
    private EventBus eventBus;


    @Override
    public PayOrderRes createOrder(ShopCartReq shopCartReq) throws Exception {
        // 1. 查询当前用户是否存在未支付订单或掉单订单
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setUserId(shopCartReq.getUserId());
        payOrderReq.setProductId(shopCartReq.getProductId());
        PayOrder unpaidOrder = orderDao.queryUnPayOrder(payOrderReq);
        if(unpaidOrder != null && Constants.OrderStatusEnum.PAY_WAIT.getCode().equals(unpaidOrder.getStatus())){
            log.info("创建订单-存在，已存在未支付订单。userId:{} productId:{} orderId:{}", shopCartReq.getUserId(), shopCartReq.getProductId(), unpaidOrder.getOrderId());
            return PayOrderRes.builder()
                    .orderId(unpaidOrder.getOrderId())
                    .payUrl(unpaidOrder.getPayUrl())
                    .build();
        }else if(unpaidOrder != null && Constants.OrderStatusEnum.CREATE.getCode().equals(unpaidOrder.getStatus())){
            log.info("创建订单-存在，存在未创建支付单订单，创建支付单开始 userId:{} productId:{} orderId:{}", shopCartReq.getUserId(), shopCartReq.getProductId(), unpaidOrder.getOrderId());
            PayOrder payOrder = doPrepayOrder(unpaidOrder.getProductId(), unpaidOrder.getProductName(), unpaidOrder.getOrderId(), unpaidOrder.getTotalAmount());
            return PayOrderRes.builder()
                    .orderId(payOrder.getOrderId())
                    .payUrl(payOrder.getPayUrl())
                    .build();
        }
        // 2. 查询商品 & 创建订单
        ProductVO productVO = productRPC.queryProductByProductId(shopCartReq.getProductId());
        String orderId = RandomStringUtils.randomNumeric(16);
        orderDao.insert(PayOrder.builder()
                        .userId(shopCartReq.getUserId())
                        .productId(shopCartReq.getProductId())
                        .productName(productVO.getProductName())
                        .orderId(orderId)
                        .totalAmount(productVO.getPrice())
                        .orderTime(new Date())
                        .status(Constants.OrderStatusEnum.CREATE.getCode())
                .build());
        // 3. 创建支付单
        PayOrder payOrder = doPrepayOrder(productVO.getProductId(), productVO.getProductName(), orderId, productVO.getPrice());

        return PayOrderRes.builder()
                .orderId(orderId)
                .payUrl(payOrder.getPayUrl())
                .build();
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setOrderId(orderId);
        payOrderReq.setStatus(Constants.OrderStatusEnum.PAY_SUCCESS.getCode());
        orderDao.changeOrderPaySuccess(payOrderReq);
        eventBus.post(JSON.toJSONString(payOrderReq));
    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderDao.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderDao.changeOrderClose(orderId);
    }

    private PayOrder doPrepayOrder(String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(notifyUrl);
        request.setReturnUrl(returnUrl);

        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);
        bizContent.put("total_amount", totalAmount.toString());
        bizContent.put("subject", productName);
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        request.setBizContent(bizContent.toString());

        String form = alipayClient.pageExecute(request).getBody();

        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);
        payOrder.setPayUrl(form);
        payOrder.setStatus(Constants.OrderStatusEnum.PAY_WAIT.getCode());

        orderDao.updateOrderPayInfo(payOrder);

        return payOrder;

    }
}

