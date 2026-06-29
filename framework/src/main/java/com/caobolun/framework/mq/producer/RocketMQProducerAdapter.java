package com.caobolun.framework.mq.producer;

import jakarta.websocket.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.function.Consumer;

/**
 * 基于 RocketMQ 的消息生产者
 */
@Slf4j
@RequiredArgsConstructor
public class RocketMQProducerAdapter implements MessageQueueProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final DelegatingTransactionListener transactionListener;

    @Override
    public SendResult send(String topic, String keys, String bizDesc, Object body) {
        return null;
    }

    @Override
    public void sendInTransaction(String topic, String keys, String bizDesc, Object body, Consumer<Object> localTransaction) {

    }
}
