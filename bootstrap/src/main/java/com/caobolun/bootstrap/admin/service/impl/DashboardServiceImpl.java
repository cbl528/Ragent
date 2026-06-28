package com.caobolun.bootstrap.admin.service.impl;

import com.caobolun.bootstrap.admin.service.DashboardService;
import com.caobolun.bootstrap.admin.vo.DashboardOverviewVO;
import com.caobolun.bootstrap.admin.vo.DashboardPerformanceVO;
import com.caobolun.bootstrap.admin.vo.DashboardTrendsVO;
import com.caobolun.bootstrap.rag.mapper.ConversationMapper;
import com.caobolun.bootstrap.rag.mapper.ConversationMessageMapper;
import com.caobolun.bootstrap.rag.mapper.RagTraceRunMapper;
import com.caobolun.bootstrap.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_ERROR = "ERROR";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String NO_DOC_REPLY = "未检索到与问题相关的文档内容。";
    private static final String GRANULARITY_DAY = "day";
    private static final String GRANULARITY_HOUR = "hour";
    private static final long SLOW_LATENCY_THRESHOLD_MS = 20000L;
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper messageMapper;
    private final RagTraceRunMapper traceRunMapper;

    /**
     * 加载概览数据
     * @param window 时间窗口
     * @return 概览数据
     */
    @Override
    public DashboardOverviewVO loadOverview(String window) {
        return null;
    }

    private WindowRange resolveWindowRange(String window, Duration fallback){
        Duration duration = parseWindow(window, fallback); // 解析时间窗口
        Instant now = Instant.now(); // 获取当前时间
        Instant start = now.minus(duration); // 计算当前时间窗口的起始时间
        Instant prevStart = start.minus(duration); // 计算上一个时间窗口的起始时间
        return new WindowRange(Date.from(start), Date.from(now), Date.from(prevStart), Date.from(start),
            window == null ? formatDuration(fallback) : window, "prev_" + (window == null ? formatDuration(fallback) : window));
    }

    /**
     * 将 Duration 对象转换成字符串表示
     * @param duration 时间间隔
     * @return 时间间隔字符串
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        if (hours % 24 == 0) {
            return (hours / 24) + "d";
        }
        return hours + "h";
    }

    /**
     * 接收前端 / 外部传入的窗口字符串，转换成 Java Duration 时间间隔对象
     * @param window 时间窗口
     * @param fallback 回退时间窗口
     * @return 时间窗口
     */
    private Duration parseWindow(String window, Duration fallback){
        // 如果参数为空、空白、格式不识别，直接返回兜底默认时长 fallback
        if(window == null || window.isEmpty()){
            return fallback;
        }
        // 去除前后空格并转换成小写
        String normalized = window.trim().toLowerCase();
        // 如果以 "h" 结尾，则解析为小时
        if(normalized.endsWith("h")){
            // 提取小时数部分并解析为 long 类型，如果失败则使用 fallback 的小时数
            long hours = parseNumber(normalized.substring(0, normalized.length() - 1), fallback.toHours());
            // 返回 Duration 对象
            return Duration.ofHours(hours);
        }
        // 如果以 "d" 结尾，则解析为天
        if(normalized.endsWith("d")){
            // 提取天数部分并解析为 long 类型，如果失败则使用 fallback 的天数
            long days = parseNumber(normalized.substring(0, normalized.length() - 1), fallback.toDays());
            // 返回 Duration 对象
            return Duration.ofDays(days);
        }
        // 如果以上条件都不满足，返回兜底时间窗口
        return fallback;
    }

    /**
     * 解析时间值字符串，如果报错返回兜底数据
     *
     * @param value 时间值字符串
     * @return 时间值
     */
    private long parseNumber(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }


    /**
     * 加载性能数据
     * @param window 时间窗口
     * @return 性能数据
     */
    @Override
    public DashboardPerformanceVO loadPerformance(String window) {
        return null;
    }

    @Override
    public DashboardTrendsVO loadTrends(String metric, String window, String granularity) {
        return null;
    }

    private static class WindowRange {
        private final Date start;
        private final Date end;
        private final Date prevStart;
        private final Date prevEnd;
        private final String windowLabel;
        private final String compareLabel;

        WindowRange(Date start, Date end, Date prevStart, Date prevEnd, String windowLabel, String compareLabel) {
            this.start = start;
            this.end = end;
            this.prevStart = prevStart;
            this.prevEnd = prevEnd;
            this.windowLabel = windowLabel;
            this.compareLabel = compareLabel;
        }
    }
}