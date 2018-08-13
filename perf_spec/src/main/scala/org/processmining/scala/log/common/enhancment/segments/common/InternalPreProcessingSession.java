package org.processmining.scala.log.common.enhancment.segments.common;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PreprocessingSession")
class InternalPreProcessingSession {

    public InternalPreProcessingSession() {

    }

    public InternalPreProcessingSession(long startMs,
                                        long endMs,
                                        long twSizeMs,
                                        int classCount,
                                        long preprocessingStartMs,
                                        long preprocessingEndMs,
                                        String userInfo,
                                        String legend) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.twSizeMs = twSizeMs;
        this.classCount = classCount;
        this.preprocessingStartMs = preprocessingStartMs;
        this.preprocessingEndMs = preprocessingEndMs;
        this.userInfo = userInfo;
        this.legend = legend;

    }


    long startMs;
    long endMs;
    long twSizeMs;
    int classCount;
    long preprocessingStartMs;
    long preprocessingEndMs;
    String userInfo;
    String legend;

    public long getStartMs() {
        return startMs;
    }

    public void setStartMs(long startMs) {
        this.startMs = startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public void setEndMs(long endMs) {
        this.endMs = endMs;
    }

    public long getTwSizeMs() {
        return twSizeMs;
    }

    public void setTwSizeMs(long twSizeMs) {
        this.twSizeMs = twSizeMs;
    }

    public int getClassCount() {
        return classCount;
    }

    public void setClassCount(int classCount) {
        this.classCount = classCount;
    }

    public long getPreprocessingStartMs() {
        return preprocessingStartMs;
    }

    public void setPreprocessingStartMs(long preprocessingStartMs) {
        this.preprocessingStartMs = preprocessingStartMs;
    }

    public long getPreprocessingEndMs() {
        return preprocessingEndMs;
    }

    public void setPreprocessingEndMs(long preprocessingEndMs) {
        this.preprocessingEndMs = preprocessingEndMs;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }
}
