package com.giraone.camera.service.api;

public class WorkflowSettings {

    private boolean restart = false;
    private boolean pause = false;
    private int delayMsActive = 20000;
    private int delayMsPaused = 60000;
    private boolean blinkOnSuccess = false;
    private boolean blinkOnFailure = false;
    private boolean flashLedForPicture = false;
    private int flashDurationMs = 100;

    //------------------------------------------------------------------------------------------------------------------

    public boolean isRestart() {
        return restart;
    }

    public void setRestart(boolean restart) {
        this.restart = restart;
    }

    public boolean isPause() {
        return pause;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public int getDelayMsActive() {
        return delayMsActive;
    }

    public void setDelayMsActive(int delayMsActive) {
        this.delayMsActive = delayMsActive;
    }

    public int getDelayMsPaused() {
        return delayMsPaused;
    }

    public void setDelayMsPaused(int delayMsPaused) {
        this.delayMsPaused = delayMsPaused;
    }

    public boolean isBlinkOnSuccess() {
        return blinkOnSuccess;
    }

    public void setBlinkOnSuccess(boolean blinkOnSuccess) {
        this.blinkOnSuccess = blinkOnSuccess;
    }

    public boolean isBlinkOnFailure() {
        return blinkOnFailure;
    }

    public void setBlinkOnFailure(boolean blinkOnFailure) {
        this.blinkOnFailure = blinkOnFailure;
    }

    public boolean isFlashLedForPicture() {
        return flashLedForPicture;
    }

    public void setFlashLedForPicture(boolean flashLedForPicture) {
        this.flashLedForPicture = flashLedForPicture;
    }

    public int getFlashDurationMs() {
        return flashDurationMs;
    }

    public void setFlashDurationMs(int flashDurationMs) {
        this.flashDurationMs = flashDurationMs;
    }

    @Override
    public String toString() {
        return "WorkflowSettings{" +
            "restart=" + restart +
            ", pause=" + pause +
            ", delayMsActive=" + delayMsActive +
            ", delayMsPaused=" + delayMsPaused +
            ", blinkOnSuccess=" + blinkOnSuccess +
            ", blinkOnFailure=" + blinkOnFailure +
            ", flashLedForPicture=" + flashLedForPicture +
            ", flashDurationMs=" + flashDurationMs +
            '}';
    }
}
