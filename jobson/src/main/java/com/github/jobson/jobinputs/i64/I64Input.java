package com.github.jobson.jobinputs.i64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public class I64Input implements JobInput {

    @NotNull
    private long value;

    @JsonCreator
    public I64Input(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
