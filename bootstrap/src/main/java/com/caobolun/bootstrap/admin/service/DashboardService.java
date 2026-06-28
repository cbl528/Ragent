package com.caobolun.bootstrap.admin.service;

import com.caobolun.bootstrap.admin.vo.DashboardOverviewVO;
import com.caobolun.bootstrap.admin.vo.DashboardPerformanceVO;
import com.caobolun.bootstrap.admin.vo.DashboardTrendsVO;

public interface DashboardService {

    DashboardOverviewVO loadOverview(String window);

    DashboardPerformanceVO loadPerformance(String window);

    DashboardTrendsVO loadTrends(String metric, String window, String granularity);
}
