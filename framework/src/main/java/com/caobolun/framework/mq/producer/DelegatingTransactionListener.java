package com.caobolun.framework.mq.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.springframework.messaging.Message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * 通用的 RocketMQ 事务消息监听器
 */
@Slf4j
@RocketMQTransactionListener
public class DelegatingTransactionListener implements RocketMQLocalTransactionListener {

    static final String HEADER_TX_ID = "TRANSACTION_CONTEXT_ID";
    static final String HEADER_TOPIC = "TRANSACTION_TOPIC";

    /**
     * 本地事务执行逻辑，per-message，仅当前实例有效
     */
    private final ConcurrentMap<String, Consumer<Object>> localTransactionMap = new ConcurrentHashMap<>();

    /**
     * 事务回查逻辑，per-topic，所有实例共享（Spring Bean 注册）
     */
    private final ConcurrentMap<String, TransactionChecker> checkerMap = new ConcurrentHashMap<>();


    /**
     * 执行本地事务，生产者发送半消息（事务预备消息） 给 Broker 成功后，立刻回调此方法，同步执行
     * @param msg 半消息
     * @param arg 业务参数
     * @return 本地事务执行状态
     * 执行你的本地数据库事务（业务操作）；
     * 根据本地事务执行结果，返回事务状态，通知 Broker 提交 / 回滚半消息；
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {

    }

    /**
     * 只有上一步返回 UNKNOW 时才会触发：
     * 生产者执行本地事务时宕机、重启；
     * 网络阻塞，Broker 没收到本地事务结果；
     * Broker 会按照回查间隔（默认 1 分钟，可配置）多次回调此方法，直到拿到明确 COMMIT / ROLLBACK。
     * @param msg Broker 存储的半消息完整信息（包含消息 body、tags、keys 等，可根据 key 查询本地事务记录）
     * @return 本地事务执行状态
     * Broker 不知道本地事务到底成功还是失败，主动回调生产者，让生产者主动查询本地数据库事务状态，告诉 Broker 最终结果。
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        return null;
    }
}
