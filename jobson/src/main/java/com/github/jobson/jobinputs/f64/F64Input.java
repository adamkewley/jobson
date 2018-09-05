package com.github.jobson.jobinputs.f64;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.jobson.jobinputs.JobInput;

import javax.validation.constraints.NotNull;

public class F64Input implements JobInput {

    @NotNull
    private final double value;

    @JsonCreator
    public F64Input(double value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }
}
