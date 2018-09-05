package com.github.jobson.jobinputs.i32;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class I32ExpectedInput extends JobExpectedInput<I32Input> {
    @Override
    public Class<I32Input> getExpectedInputClass() {
        return I32Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(I32Input input) {
        return Optional.empty();
    }

    @Override
    public I32Input generateExampleInput() {
        final int value = new Random().nextInt();
        return new I32Input(value);
    }
}
