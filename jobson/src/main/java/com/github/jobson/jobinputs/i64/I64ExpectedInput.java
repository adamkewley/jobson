package com.github.jobson.jobinputs.i64;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class I64ExpectedInput extends JobExpectedInput<I64Input> {
    @Override
    public Class<I64Input> getExpectedInputClass() {
        return I64Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(I64Input input) {
        return Optional.empty();
    }

    @Override
    public I64Input generateExampleInput() {
        final long value = new Random().nextLong();
        return new I64Input(value);
    }
}
