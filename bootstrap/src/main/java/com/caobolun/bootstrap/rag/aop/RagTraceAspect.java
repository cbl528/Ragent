package com.caobolun.bootstrap.rag.aop;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.caobolun.bootstrap.rag.config.RagTraceProperties;
import com.caobolun.bootstrap.rag.entity.RagTraceNodeDO;
import com.caobolun.bootstrap.rag.service.RagTraceRecordService;
import com.caobolun.framework.trace.RagTraceContext;
import com.caobolun.framework.trace.RagTraceNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 注解式 RAG Trace 采集切面
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RagTraceAspect {

    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_ERROR = "ERROR";

    private final RagTraceRecordService ragTraceRecordService;
    private final RagTraceProperties ragTraceProperties;

    @Around("@annotation(traceNode)")
    public Object around(ProceedingJoinPoint joinPoint, RagTraceNode traceNode) throws Throwable{
        // 判断是否开启注解追踪节点
        if(!ragTraceProperties.isEnabled()){
            return joinPoint.proceed();
        }
        String traceId = RagTraceContext.getTraceId();
        // 如果追踪ID为空，则不进行追踪
        if(StrUtil.isBlank(traceId)){
            return joinPoint.proceed();
        }

        // 获取方法签名，只能拿类名、方法名；
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取方法
        Method method = signature.getMethod();
        String nodeId = IdUtil.getSnowflakeNextIdStr();
        String parentNodeId = RagTraceContext.currentNodeId();
        int depth = RagTraceContext.depth();
        Date startTime = new Date();
        long startTimeMillis = System.currentTimeMillis();

        ragTraceRecordService.startNode(RagTraceNodeDO.builder()
                .traceId(traceId)
                .nodeId(nodeId)
                .parentNodeId(parentNodeId)
                .depth(depth)
                .nodeType(StrUtil.blankToDefault(traceNode.type(), "METHOD"))
                .nodeName(StrUtil.blankToDefault(traceNode.name(), method.getName()))
                .className(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .status(STATUS_RUNNING)
                .startTime(startTime)
                .build());

        RagTraceContext.pushNode(nodeId);
        try {
            Object result = joinPoint.proceed();
            ragTraceRecordService.finishNode(
                    traceId,
                    nodeId,
                    STATUS_SUCCESS,
                    null,
                    new Date(),
                    System.currentTimeMillis() - startTimeMillis
            );
            return result;
        } catch(Throwable ex) {
            ragTraceRecordService.finishNode(
                    traceId,
                    nodeId,
                    STATUS_ERROR,
                    truncateError(ex),
                    new Date(),
                    System.currentTimeMillis() - startTimeMillis
            );
            throw ex;
        } finally {
            RagTraceContext.popNode();
        }
    }

    private String truncateError(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getClass().getSimpleName() + ": " + StrUtil.blankToDefault(throwable.getMessage(), "");
        if (message.length() <= ragTraceProperties.getMaxErrorLength()) {
            return message;
        }
        return message.substring(0, ragTraceProperties.getMaxErrorLength());
    }

}
