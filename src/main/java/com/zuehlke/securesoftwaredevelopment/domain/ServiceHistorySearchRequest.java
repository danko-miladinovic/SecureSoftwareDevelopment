package com.zuehlke.securesoftwaredevelopment.domain;

import java.util.Map;

public class ServiceHistorySearchRequest {
    private Map<String, Object> filters;
    private Map<String, Object> view;

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public Map<String, Object> getView() {
        return view;
    }

    public void setView(Map<String, Object> view) {
        this.view = view;
    }
}
