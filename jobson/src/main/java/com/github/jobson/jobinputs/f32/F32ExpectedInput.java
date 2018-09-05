package com.github.jobson.jobinputs.f32;

import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.utils.ValidationError;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class F32ExpectedInput extends JobExpectedInput<F32Input> {
    @Override
    public Class<F32Input> getExpectedInputClass() {
        return F32Input.class;
    }

    @Override
    public Optional<List<ValidationError>> validate(F32Input input) {
        return Optional.empty();
    }

    @Override
    public F32Input generateExampleInput() {
        final float val = new Random().nextFloat();
        return new F32Input(val);
    }
}
