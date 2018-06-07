package com.github.jobson.scripting.functions;

import com.github.jobson.jobinputs.stringarray.StringArrayInput;
import com.github.jobson.scripting.FreeFunction;

public class JoinFunction implements FreeFunction {
    @Override
    public Object call(Object... args) {
        if (args.length != 2) {
            throw new RuntimeException(String.format("Invalid number of arguments (%s) supplied to a join function", args.length));
        } else if (args[0].getClass() != String.class) {
            throw new RuntimeException(String.format("%s: Is not a valid first argument to join. It should be a string delimiter", args[0].getClass()));
        } else if (args[1].getClass() != StringArrayInput.class) {
            throw new RuntimeException(String.format("%s: Is not a valid second argument to join. It should be a list of strings", args[1].getClass()));
        } else {
            final String separator = (String)args[0];
            final StringArrayInput entries = (StringArrayInput) args[1];

            return String.join(separator, entries.getValues());
        }
    }
}
