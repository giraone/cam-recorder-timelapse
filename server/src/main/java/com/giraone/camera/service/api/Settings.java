package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Settings {

    Status status;
    WorkflowSettings workflow;
    CameraSettings camera;

    public Settings() {
        this(new Status(true, null), new WorkflowSettings(), new CameraSettings());
    }

    public Settings(Status status) {
        this(status, null, null);
    }

    public Settings(WorkflowSettings workflow, CameraSettings camera) {
        this(new Status(true, null), workflow, camera);
    }

    public Settings(Status status, WorkflowSettings workflow, CameraSettings camera) {
        this.status = status;
        this.workflow = workflow;
        this.camera = camera;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public WorkflowSettings getWorkflow() {
        return workflow;
    }

    public void setWorkflow(WorkflowSettings workflow) {
        this.workflow = workflow;
    }

    public CameraSettings getCamera() {
        return camera;
    }

    public void setCamera(CameraSettings camera) {
        this.camera = camera;
    }

    @Override
    public String toString() {
        return "Settings{" +
            "status=" + status +
            ", workflow=" + workflow +
            ", camera=" + camera +
            '}';
    }
}
