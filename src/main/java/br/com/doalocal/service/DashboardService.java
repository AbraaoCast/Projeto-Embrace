package br.com.doalocal.service;

import br.com.doalocal.dao.DashboardDao;
import br.com.doalocal.model.DashboardResultado;

import java.util.List;

/** Consolida as informações reais necessárias à visão geral do sistema. */
public final class DashboardService {

    static final int RECENT_ACTIVITIES_LIMIT = 5;

    private final DashboardDao dashboardDao;

    public DashboardService() {
        this(new DashboardDao());
    }

    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    public DashboardResultado load() {
        return new DashboardResultado(
                dashboardDao.findSummary(),
                List.copyOf(dashboardDao.findRecentActivities(RECENT_ACTIVITIES_LIMIT))
        );
    }
}
