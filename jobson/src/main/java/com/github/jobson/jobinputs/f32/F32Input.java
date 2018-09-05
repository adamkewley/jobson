package com.github.jobson.jobinputs.f32;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public class F32Input implements JobInput {

    @NotNull
    private float value;

    @JsonCreator
    public F32Input(float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
}
