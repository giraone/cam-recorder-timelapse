package com.giraone.camera.service.api;

public class Settings {

    WorkflowSettings workflow;
    CameraSettings camera;

    @SuppressWarnings("unused") // Used by JSON
    public Settings() {
    }

    public Settings(WorkflowSettings workflow, CameraSettings camera) {
        this.workflow = workflow;
        this.camera = camera;
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
            "workflow=" + workflow +
            ", camera=" + camera +
            '}';
    }
}
