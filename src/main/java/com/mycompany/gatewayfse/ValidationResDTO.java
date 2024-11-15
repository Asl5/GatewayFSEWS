/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.gatewayfse;

/**
 *
 * @author f.matraxia
 */
public class ValidationResDTO {

    private String traceID;
    private String spanID;
    private String workflowInstanceId;
    private String warning;

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

    public String getWorkflowInstanceId() {
        return workflowInstanceId;
    }

    public void setWorkflowInstanceId(String workflowInstanceId) {
        this.workflowInstanceId = workflowInstanceId;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }

    @Override
    public String toString() {
        return "ValidationResDTO{"
                + "traceID='" + traceID + '\''
                + ", spanID='" + spanID + '\''
                + ", workflowInstanceId='" + workflowInstanceId + '\''
                + ", warning='" + warning + '\''
                + '}';
    }

}
