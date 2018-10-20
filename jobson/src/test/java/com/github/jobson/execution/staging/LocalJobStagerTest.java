/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.jobson.execution.staging;

import com.github.jobson.api.specs.ExecutionConfiguration;
import com.github.jobson.api.specs.JobSpec;
import com.github.jobson.api.specs.JobSpecId;
import com.github.jobson.api.specs.RawTemplateString;
import com.github.jobson.api.specs.inputs.JobExpectedInput;
import com.github.jobson.api.specs.inputs.JobExpectedInputId;
import com.github.jobson.api.specs.inputs.JobInput;
import com.github.jobson.api.specs.inputs.string.StringExpectedInput;
import com.github.jobson.api.specs.inputs.string.StringInput;
import com.github.jobson.api.specs.inputs.stringarray.StringArrayExpectedInput;
import com.github.jobson.api.specs.inputs.stringarray.StringArrayInput;
import com.github.jobson.execution.subprocess.SubprocessInput;
import com.github.jobson.internal.PersistedJob;
import com.github.jobson.other.TestHelpers;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.jobson.other.TestHelpers.*;
import static com.github.jobson.util.Helpers.toJSON;
import static java.lang.String.format;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

public final class LocalJobStagerTest {

    private static LocalJobStager createInstance() throws IOException {
        final Path workingDirs = Files.createTempDirectory(LocalJobStager.class.getName());
        return createInstance(workingDirs);
    }

    private static LocalJobStager createInstance(Path workingDirs) {
        final JobStagerIO jobStagerIO = new JobStagerIO() {
            @Override
            public void copyDependency(JobSpecId specId, Path source, Path target) {
                throw new RuntimeException("NYI");
            }

            @Override
            public void softlinkDependency(JobSpecId specId, Path linkTarget, Path linkName) {
                throw new RuntimeException("NYI");
            }
        };

        return new LocalJobStager(workingDirs, jobStagerIO);
    }

    private static JobSpec generateStandardJobSpec() {
        return generateBasicJobSpec();  // TODO: something more sophisticated. NEEDS: .someList
    }

    private static PersistedJob generateJobWithInputsAndExecution(
            List<JobExpectedInput<?>> expectedInputs,
            ExecutionConfiguration executionConfiguration,
            Map<JobExpectedInputId, JobInput> actualInputs) {

        final JobSpec jobSpec = generateBasicJobSpec()
                .withExpectedInputs(expectedInputs)
                .withExecutionConfiguration(executionConfiguration);
        final PersistedJob job = generateBasicPersistedJob()
                .withInputs(actualInputs)
                .withSpec(jobSpec);

        return job;
    }

    private static JobSpec generateBasicJobSpec() {
        return new JobSpec(
                generateJobSpecId(),
                generateRandomString(),
                generateRandomString(),
                emptyList(),
                generateBasicExecutionConfig(),
                emptyList());
    }

    private static ExecutionConfiguration generateBasicExecutionConfig() {
        return generateExecutionConfig("echo", generateRandomString());
    }

    private static List<JobExpectedInput<?>> generateSoloStringInput(String id) {
        return singletonList(generateStringInput(id));
    }

    private static JobExpectedInput<?> generateStringInput(String id) {
        return new StringExpectedInput(new JobExpectedInputId(id), id, id, Optional.empty());
    }

    private static List<JobExpectedInput<?>> generateSoloStrlistInput(String id) {
        return singletonList(generateStringListExpectedInput(id));
    }

    private static StringArrayExpectedInput generateStringListExpectedInput(String id) {
        return new StringArrayExpectedInput(new JobExpectedInputId(id), id, id, Optional.empty());
    }

    private static ExecutionConfiguration generateExecutionConfigWithArgs(String... args) {
        return generateExecutionConfig(generateRandomString(), args);
    }

    private static ExecutionConfiguration generateExecutionConfig(String application, String... args) {
        return new ExecutionConfiguration(application, Optional.of(Arrays.stream(args).map(RawTemplateString::new).collect(Collectors.toList())), Optional.empty());
    }

    private static PersistedJob generateStandardJob() {
        return generateBasicPersistedJob();  // TODO: something more sophisticated
    }

    private static PersistedJob generateBasicPersistedJob() {
        return new PersistedJob(
                generateJobId(),
                generateUserId(),
                generateRandomString(),
                emptyMap(),
                emptyList(),
                generateBasicJobSpec());
    }

    private static Map<JobExpectedInputId, JobInput> generateSoloStringInput(String inputId, String val) {
        final HashMap<JobExpectedInputId, JobInput> ret = new HashMap<>();
        ret.put(new JobExpectedInputId(inputId), generateStringInputVal(val));
        return ret;
    }

    private static JobInput generateStringInputVal(String val) {
        return new StringInput(val);
    }

    private static Map<JobExpectedInputId, JobInput> generateSoloStringlistInput(String inputId, List<String> vals) {
        final HashMap<JobExpectedInputId, JobInput> ret = new HashMap<>();
        ret.put(new JobExpectedInputId(inputId), new StringArrayInput(vals));
        return ret;
    }

    @Test
    public void testStageJobEvaluatesJobInputsAsExpected() throws IOException {
        final String expectedInputId = generateRandomString();
        final String inputValue = generateRandomString();

        final List<JobExpectedInput<?>> expectedInputs = generateSoloStringInput(expectedInputId);
        final ExecutionConfiguration executionConfiguration = generateExecutionConfigWithArgs(format("${inputs.%s}", expectedInputId));
        final Map<JobExpectedInputId, JobInput> inputs = generateSoloStringInput(expectedInputId, inputValue);

        final PersistedJob job = generateJobWithInputsAndExecution(expectedInputs, executionConfiguration, inputs);

        final LocalJobStager jobStager = createInstance();
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        assertThat(subprocessInput.getArgs().size()).isEqualTo(2);
        assertThat(subprocessInput.getArgs().get(1)).isEqualTo(inputValue);
    }

    @Test
    public void testStageJobEvaluatesToJSONFunctionAsExpected() throws IOException {
        final PersistedJob job = generateStandardJob()
                .withExecutionConfiguration(generateExecutionConfig(generateRandomString(), "${toJSON(inputs)}"));

        final LocalJobStager jobStager = createInstance();
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        assertThat(subprocessInput.getArgs().size()).isEqualTo(2);
        assertJSONEqual(subprocessInput.getArgs().get(1), toJSON(job.getInputs()));
    }

    @Test
    public void testStageJobEvaluatesToFileAsExpected() throws IOException {
        final PersistedJob job = generateStandardJob()
                .withExecutionConfiguration(generateExecutionConfig(generateRandomString(), "${toFile(toJSON(inputs))}"));

        final LocalJobStager jobStager = createInstance();
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        assertThat(subprocessInput.getArgs().size()).isEqualTo(2);

        final String fileArg = subprocessInput.getArgs().get(1);
        final Path filePath = Paths.get(fileArg);

        assertThat(filePath.toFile()).exists();

        final String jsonInFile = new String(Files.readAllBytes(filePath));

        assertJSONEqual(jsonInFile, toJSON(job.getInputs()));
    }

    @Test
    public void testStageJobEvaluatesJoinAsExpected() throws IOException {
        final String expectedInputId = generateRandomString();
        final List<String> inputs = generateRandomList(0, 5, TestHelpers::generateAlphanumStr);

        final List<JobExpectedInput<?>> expectedInputs = generateSoloStrlistInput(expectedInputId);
        final ExecutionConfiguration executionConfiguration = generateExecutionConfigWithArgs(format("${join(',', inputs.%s)}", expectedInputId));
        final Map<JobExpectedInputId, JobInput> actualInputs = generateSoloStringlistInput(expectedInputId, inputs);

        final PersistedJob job = generateJobWithInputsAndExecution(expectedInputs, executionConfiguration, actualInputs);

        final LocalJobStager jobStager = createInstance();
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        assertThat(subprocessInput.getArgs().get(1)).isEqualTo("a,b,c,d");
    }

    @Test
    public void testStageJobEvaluatesToStringAsExpected() throws IOException {
        final String expectedInputId = generateRandomString();
        final String inputValue = generateRandomString();

        final List<JobExpectedInput<?>> expectedInputs = generateSoloStringInput(expectedInputId);
        final ExecutionConfiguration executionConfiguration = generateExecutionConfigWithArgs(format("${toString(inputs.%s)}", expectedInputId));
        final Map<JobExpectedInputId, JobInput> actualInputs = generateSoloStringInput(expectedInputId, inputValue);

        final PersistedJob job = generateJobWithInputsAndExecution(expectedInputs, executionConfiguration, actualInputs);

        final LocalJobStager jobStager = createInstance();
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        assertThat(subprocessInput.getArgs().get(1)).isEqualTo(inputValue);
    }

    @Test
    public void testStageJobEvaluatesOutputDirAsExpected() throws IOException {
        final Path outputDirs = Files.createTempDirectory(LocalJobStagerTest.class.getName());

        final PersistedJob job = generateStandardJob()
                .withExecutionConfiguration(generateExecutionConfig(generateRandomString(), "${outputDir}"));

        final LocalJobStager jobStager = createInstance(outputDirs);
        final SubprocessInput subprocessInput = jobStager.stageJob(job);

        final Path echoedPath = Paths.get(subprocessInput.getArgs().get(1));

        assertThat(echoedPath.toFile()).exists();
        assertThat(echoedPath.getParent()).isEqualTo(outputDirs);
    }

    @Test
    public void testStageJobCopiesFileDependencyWhenSoftlinkFalse() {
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobSoftlinksFileDependencyWhenSoftlinkTrue() {
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobFileIsCopiedWithExecutePermissionsMaintained() {
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobDirectoryIsCopiedWithExecutePermissionsMaintained() {
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobDirectoryIsSoftlinkedWithExecutePermissionsMaintained() {
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobSoftlinkedFileDependencyIsSoftLinkedFromTheDestinationToTheSource() {
        // Files.isSymbolicLink
        // Files.readSymbolicLink
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobTemplatedDependencySourceIsResolvedAsATemplateString() {
        // The source can be a template string (e.g. ${request.id})
        // e.g.
        // - set source ${request.id}
        // - stage it
        // - source should be ${reqeust.id}
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobTemplatedDependencyDestinationIsResolvedAsATemplateString() {
        // The destination can be a template string
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobTemplatedDependencyCanContainReferenceToJobInputs() {
        // The dependency template can reference an input (e.g. ${inputs.foo})
        assertThat(false).isTrue();
    }

    @Test
    public void testStageJobTemplatedDependencyDestinationCanContainReferenceToJobInputs() {
        // The dependency destination template can reference an input (e.g. ${inputs.foo})
        assertThat(false).isTrue();
    }
}
