/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author f.matraxia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationErrorResponseDTO {

    private String traceID;
    private String spanID;
    private String type;
    private String title;
    private String detail;
    private int status;
    private String instance;
    private String workflowInstanceId;
    private String govway_id;

    public String getGovway_id() {
        return govway_id;
    }

    public void setGovway_id(String govway_id) {
        this.govway_id = govway_id;
    }

    // Getters e Setters
    public String getTraceID() {
        return traceID;
    }

    public void setTraceID(String traceID) {
        this.traceID = traceID;
    }

    public String getSpanID() {
        return spanID;
    }

    public void setSpanID(String spanID) {
        this.spanID = spanID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    @Override
    public String toString() {
        return "ValidationErrorResponseDTO{"
                + "traceID='" + traceID + '\''
                + ", spanID='" + spanID + '\''
                + ", type='" + type + '\''
                + ", title='" + title + '\''
                + ", detail='" + detail + '\''
                + ", status=" + status
                + ", instance='" + instance + '\''
                + ", workflowInstanceId='" + workflowInstanceId + '\''
                + '}';
    }

}
