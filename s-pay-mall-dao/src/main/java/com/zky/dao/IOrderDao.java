package com.zky.dao;


import com.zky.domain.po.PayOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IOrderDao {

    void insert(PayOrder payOrder);

    PayOrder queryUnPayOrder(PayOrder payOrder);

    void updateOrderPayInfo(PayOrder payOrder);
}

