package com.github.jobson.jobinputs.f64;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class F64ExpectedInput extends JobExpectedInput<F64Input> {
    @Override
    public Class<F64Input> getExpectedInputClass() {
        return F64Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(F64Input input) {
        return Optional.empty();
    }

    @Override
    public F64Input generateExampleInput() {
        final long value = new Random().nextLong();
        return new F64Input(value);
    }
}
