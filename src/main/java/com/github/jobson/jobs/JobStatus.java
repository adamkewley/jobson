package com.github.jobson.jobs;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

import java.util.Set;

import static com.github.jobson.Helpers.setOf;

@ApiModel(description = "The status of a job")
public enum JobStatus {

    @JsonProperty("submitted")
    SUBMITTED,

    @JsonProperty("running")
    RUNNING,

    @JsonProperty("aborted")
    ABORTED,

    @JsonProperty("fatal-error")
    FATAL_ERROR,

    @JsonProperty("finished")
    FINISHED;


    public static JobStatus fromExitCode(int exitCode) {
        switch (exitCode) {
            case 0: return FINISHED;
            case 130: return ABORTED; // CTRL+C
            case 143: return ABORTED; // SIGTERM
            default: return FATAL_ERROR;
        }
    }

    public static Set<JobStatus> getAbortableStatuses() {
        return setOf(SUBMITTED, RUNNING);
    }


    public boolean isAbortable() {
        return getAbortableStatuses().contains(this);
    }
}
