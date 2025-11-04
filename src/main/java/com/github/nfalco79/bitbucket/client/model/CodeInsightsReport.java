package com.github.nfalco79.bitbucket.client.model;

import java.io.Serializable;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CodeInsightsReport implements Serializable {

    public enum ReportType {
        SECURITY, COVERAGE, TEST, BUG;
    }

    public enum ReportResult {
        PASSED, FAILED, PENDING;
    }

    private static final long serialVersionUID = 8033145626502387440L;

    private String title;
    private String details;
    @JsonProperty("external_id")
    private String externalId;
    @JsonProperty("report_type")
    private ReportType type;
    private String reporter;
    private URL link;
    private ReportResult result;
    private Collection<ReportData> data = new LinkedList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public URL getLink() {
        return link;
    }

    public void setLink(URL link) {
        this.link = link;
    }

    public ReportResult getResult() {
        return result;
    }

    public void setResult(ReportResult result) {
        this.result = result;
    }

    public Collection<ReportData> getData() {
        return data;
    }

    public void setData(Collection<ReportData> data) {
        this.data = data;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    @Override
    public String toString() {
        return title;
    }
}
