# Template Strings

Template strings, such as `${request.id}` allow developers to add
runtime evaluation to their job specs. For example, template strings
allow runtime input arguments to be used as application arguments:

```yaml
execution:
  application: bash
  arguments:
  - ${inputs.firstName}
```

This is the main way of passing client inputs into an application at
runtime.

Template strings are ordinary strings with support for special
expressions between `${` and `}`. Jobson interprets those expressions
at runtime, evaluating them against an environment containing data
(e.g. `inputs`, `request`) and functions (e.g. `toJSON`, `toFile`). 

## Template String Functions + Variables

Template strings have access to the following:


### `String toJSON(Object arg)`

A function that takes an input (e.g. `request`, `request.id`, `'string
literal'`) and returns a JSON representation of the
input. Object-/Hash-like arguments are converted into a JSON object,
strings are converted into a JSON string, numbers into JSON numbers,
etc.


### `String toFile(String content)`

A function that takes a string input (e.g. `request.id`,
`toJSON(request)`), write the input to a file, and returns an absolute
path to that file.


### `String join(String delimiter, StringArray strings)`

A function that takes a delimiter (e.g. `','`) and a `StringArray`
(usually, from a Jobson `string[]` `expectedInput`) and returns a
string that contains each element in the `StringArray` joined with the
delimiter.


### `Object request`

A reference to the request that initiated the job. Useful fields in
the request are:

- `String id`: The job's ID (e.g. `57s9fmopb`)
- `String owner`: The client that initaited the job request
  (e.g. `foouser`)
- `String name`: The name the client given the job
- `Map<JobExpectedInputId, JobInput> inputs`: The inputs given for the
  job. 
  - Each input is accessible via the dot accessor
  (e.g. `request.inputs.firstName`).
  - The `Map` is serializable as JSON (e.g. `toJSON(request.inputs)`).
- `JobSpec spec`: The job spec the job was submitted against

### `Object inputs`

Shortcut for `request.inputs`.
