package com.github.jobson.jobinputs.i32;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public class I32Input implements JobInput {

    @NotNull
    private final int value;

    @JsonCreator
    public I32Input(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }
}
