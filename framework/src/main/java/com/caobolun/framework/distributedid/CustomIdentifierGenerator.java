package com.caobolun.framework.distributedid;


import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

/**
 * 自定义ID生成器
 */
public class CustomIdentifierGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return IdUtil.getSnowflakeNextId();
    }

    @Override
    public String nextUUID(Object entity) {
        return IdUtil.getSnowflakeNextIdStr();
    }
}
