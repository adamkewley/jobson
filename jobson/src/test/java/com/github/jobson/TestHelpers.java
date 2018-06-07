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

package com.github.jobson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.jobson.api.v1.APIJobDetails;
import com.github.jobson.api.v1.APIJobRequest;
import com.github.jobson.api.v1.APIRestLink;
import com.github.jobson.api.v1.UserId;
import com.github.jobson.auth.AuthenticationBootstrap;
import com.github.jobson.dao.jobs.JobDetails;
import com.github.jobson.dao.jobs.JobOutputDetails;
import com.github.jobson.dao.specs.JobSpecSummary;
import com.github.jobson.dao.users.UserCredentials;
import com.github.jobson.dao.users.UserDAO;
import com.github.jobson.fixtures.ValidJobRequestFixture;
import com.github.jobson.jobinputs.JobExpectedInput;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.jobinputs.select.SelectExpectedInput;
import com.github.jobson.jobinputs.select.SelectInput;
import com.github.jobson.jobinputs.select.SelectOption;
import com.github.jobson.jobinputs.sql.ColumnSchema;
import com.github.jobson.jobinputs.sql.SQLExpectedInput;
import com.github.jobson.jobinputs.sql.SQLInput;
import com.github.jobson.jobinputs.sql.TableSchema;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobOutput;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.JobTimestamp;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.*;
import com.github.jobson.utils.BinaryData;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.reactivex.Observable;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.Servlet;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.stream.Stream;

import static io.dropwizard.testing.FixtureHelpers.fixture;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public final class TestHelpers {

    public static final ObjectMapper JSON_MAPPER = 
            new ObjectMapper().registerModule(new Jdk8Module());
    public static final ObjectMapper YAML_MAPPER = 
            new ObjectMapper(new YAMLFactory()).registerModule(new Jdk8Module());
    private static final Random rng = new Random();
    public static final ValidJobRequest STANDARD_VALID_REQUEST;

    private static final char[] alphaChars = new char[] {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
            'W', 'X', 'Y', 'Z'
    };
    
    private static final char[] alphanumChars = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 
            'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 
            'Y', 'Z'
    };


    static {
        STANDARD_VALID_REQUEST = readJobRequestFixture("fixtures/dao/jobs/FilesystemBasedJobsDAO/standard-resolved-request.json");
    }


    public static String generateAlphanumStr() {
        return generateAlphanumStr(10);
    }

    public static String generateAlphanumStr(int len) {
        final char chars[] = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = alphanumChars[randomIntBetween(0, alphanumChars.length)];
        }
        return new String(chars);
    }

    public static String generateClassName() {
        final int len = randomIntBetween(5, 15);
        final char ret[] = new char[len];

        for (int i = 0; i < len; i++) {
            ret[i] = alphaChars[randomIntBetween(0, alphaChars.length)];
        }

        return new String(ret);
    }

    /**
     * Generate int between [low, high).
     */
    public static int randomIntBetween(int low, int high) {
        if (low > high) throw new IllegalArgumentException();
        Random rng = new Random();
        return rng.nextInt(high - low) + low;
    }

    /**
     * GenerateSpecsCommand a random string that is guaranteed to be unique.
     * @return
     */
    public static String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    public static byte[] generateRandomBytes() {
        return generateRandomBytes(randomIntBetween(64, 1024));
    }

    public static byte[] generateRandomBytes(int numBytes) {
        final byte[] b = new byte[numBytes];
        rng.nextBytes(b);
        return b;
    }

    public static BinaryData generateRandomBinaryData() {
        return BinaryData.wrap(generateRandomBytes());
    }

    public static JobOutput generateRandomJobOutput() {
        return generateRandomJobOutput(generateJobOutputId());
    }

    public static JobOutput generateRandomJobOutput(JobOutputId jobOutputId) {
        return generateRandomJobOutput(jobOutputId, generateRandomBytes());
    }

    public static JobOutput generateRandomJobOutput(byte data[]) {
        return generateRandomJobOutput(generateJobOutputId(), data);
    }

    public static JobOutput generateRandomJobOutput(JobOutputId jobOutputId, byte data[]) {
        return new JobOutput(
                jobOutputId,
                BinaryData.wrap(data),
                Optional.of(generateRandomString()),
                Optional.of(generateRandomString()),
                new HashMap<>());
    }

    public static JobOutputId generateJobOutputId() {
        return new JobOutputId(generateRandomString());
    }

    public static UserId generateUserId() {
        return new UserId(TestHelpers.generateRandomString());
    }

    public static String generateUserName() {
        return TestHelpers.generateRandomString();
    }

    public static UserCredentials generateUserDetails() {
        return new UserCredentials(generateUserId(), "someauthname", "someauthfield");
    }

    public static JobId generateJobId() {
        return new JobId(TestHelpers.generateAlphanumStr(10));
    }

    public static JobSpecId generateJobSpecId() {
        return new JobSpecId(generateRandomString());
    }

    public static JobSpecSummary generateJobSpecSummary() {
        return new JobSpecSummary(generateJobSpecId(), generateUserName(), generateRandomString());
    }

    public static JobTimestamp generateJobStatusChangeTimestamp() {
        return new JobTimestamp(JobStatus.ABORTED, new Date(), Optional.of(generateRandomString()));
    }

    public static List<JobTimestamp> generateTypicalJobStatusTimestamps() {
        final List<JobTimestamp> ret = new ArrayList<>();

        final int numEntries = rng.nextInt(5) + 1;

        for (int i = 0; i < numEntries; i++) {
            ret.add(generateJobStatusChangeTimestamp());
        }

        return ret;
    }

    public static JobExpectedOutput generateJobOutput(RawTemplateString id, RawTemplateString path, String mimeType) {
        return new JobExpectedOutput(
                id,
                path,
                mimeType,
                Optional.of(generateRandomString()),
                Optional.of(generateRandomString()),
                new HashMap<>());
    }

    public static JobExpectedOutput generateRequiredJobOutput(RawTemplateString id, RawTemplateString path, String mimeType) {
        return new JobExpectedOutput(
                id,
                path,
                Optional.of(mimeType),
                Optional.of(generateRandomString()),
                Optional.of(generateRandomString()),
                new HashMap<>(),
                true);
    }

    public static List<JobDetails> generateRandomJobDetails() {
        return Stream.generate(TestHelpers::generateValidJobDetails)
                .limit(randomIntBetween(5, 20))
                .collect(toList());
    }

    /**
     * GenerateSpecsCommand a valid instance of JobDetails.
     *
     * @return A valid instance of JobDetails.
     */
    public static JobDetails generateValidJobDetails() {
        return generateJobDetailsWithStatuses(generateTypicalJobStatusTimestamps());
    }

    public static APIJobDetails generateJobDetailsWithStatus(JobStatus jobStatus) {
        return generateJobDetailsWithStatuses(Arrays.asList(JobTimestamp.now(jobStatus)));
    }

    public static APIJobDetails generateJobDetailsWithStatuses(List<JobTimestamp> jobStatuses) {
        return new APIJobDetails(
                generateJobId(),
                generateUserName(),
                generateUserId(),
                jobStatuses,
                new HashMap<>());
    }

    public static SecurityContext generateSecureSecurityContext() {
        final String name = TestHelpers.generateUserName();
        final String authScheme = TestHelpers.generateRandomString();

        final Principal principal = new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };

        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String s) {
                return true;
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return authScheme;
            }
        };
    }

    public static APIJobRequest generateJobSubmissionRequest() {
        final JobSpecId jobSpecId = generateJobSpecId();
        final String description = generateRandomString();
        final Map<JobExpectedInputId, JobInput> inputs = generateRandomJobInputs();
        final Map<JobExpectedInputId, JsonNode> anonymizedInputs =
                Helpers.mapValues(inputs, input -> {
                    try {
                        return JSON_MAPPER.readValue(
                                JSON_MAPPER.writeValueAsString(input),
                                JsonNode.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        return new APIJobRequest(jobSpecId, description, anonymizedInputs);
    }

    public static Map<JobExpectedInputId, JobInput> generateRandomJobInputs() {
        final Map<JobExpectedInputId, JobInput> ret = new HashMap<>();

        for (int i = 0; i < 15; i++) {
            ret.put(generateJobInputSchemaId(), generateJobInput());
        }

        return ret;
    }

    public static <T> List<T> generateRandomList(int min, int max, Producer<T> generator) {
        final List<T> ret = new ArrayList<>();
        final int retLen = randomIntBetween(min, max);

        for (int i = 0; i < retLen; i++) {
            ret.add(generator.call());
        }

        return ret;
    }

    public static JobInput generateJobInput() {
        if (randomIntBetween(0, 1) == 0) {
            return new SelectInput(generateRandomString());
        } else {
            return new SQLInput(generateSQLString());
        }
    }

    public static JobExpectedInputId generateJobInputSchemaId() {
        return new JobExpectedInputId(generateRandomString());
    }

    public static String generateSQLString() {
        return "select a, b, c from table where a < b and b > c;";
    }

    public static List<JobSpecSummary> generateNJobSpecSummaries(int n) {
        return generateListContainingNElements(n, TestHelpers::generateJobSpecSummary);
    }

    public static <T> List<T> generateListContainingNElements(int n, Producer<T> producer) {
        final ArrayList<T> ret = new ArrayList<T>();

        for(int i = 0; i < n; i++) {
            ret.add(producer.call());
        }

        return ret;
    }


    public static JobSpec generateJobSpec() {
        return new JobSpec(
                generateJobSpecId(),
                generateRandomString(),
                generateRandomString(),
                generateRandomJobInputSchemas(),
                generateExecutionConfiguration());
    }

    public static ExecutionConfiguration generateExecutionConfiguration() {
        return new ExecutionConfiguration(
                "echo",
                Optional.of(singletonList(new RawTemplateString(generateRandomString()))), Optional.empty());
    }

    public static List<JobExpectedInput<?>> generateRandomJobInputSchemas() {
        return generateRandomList(5, 20, TestHelpers::generateRandomJobInputSchema);
    }

    public static JobExpectedInput<?> generateRandomJobInputSchema() {
        if (randomIntBetween(0, 1) == 0) {
            return new SQLExpectedInput(
                    generateJobInputSchemaId(),
                    generateRandomString(),
                    generateRandomString(),
                    generateRandomTableSchemas());
        } else {
            return new SelectExpectedInput(
                    generateJobInputSchemaId(),
                    generateRandomString(),
                    generateRandomString(),
                    generateRandomOptionInputSchemas(),
                    Optional.empty());
        }
    }

    public static List<TableSchema> generateRandomTableSchemas() {
        return generateRandomList(5, 20, TestHelpers::generateRandomTableSchema);
    }

    public static TableSchema generateRandomTableSchema() {
        return new TableSchema(
                generateRandomString(),
                generateRandomString(),
                generateRandomString(),
                generateRandomColumnSchemas());
    }

    public static List<ColumnSchema> generateRandomColumnSchemas() {
        return generateRandomList(5, 20, TestHelpers::generateRandomColumnSchema);
    }

    public static ColumnSchema generateRandomColumnSchema() {
        return new ColumnSchema(
                generateRandomString(),
                generateRandomString(),
                generateRandomString(),
                generateRandomString());
    }

    public static List<SelectOption> generateRandomOptionInputSchemas() {
        return generateRandomList(10, 30, TestHelpers::generateRandomOptionInputSchema);
    }

    public static SelectOption generateRandomOptionInputSchema() {
        return new SelectOption(
                generateRandomString(),
                generateRandomString(),
                Optional.of(generateRandomString()));
    }

    public static JobStatus generateJobStatus() {
        return JobStatus.SUBMITTED;
    }


    public static <T> T readJSONFixture(String path, Class<T> klass) {
        final String json = fixture(path);
        return  readJSON(json, klass);
    }

    public static <T> T readJSON(String json, Class<T> klass) {
        try {
            return JSON_MAPPER.readValue(json, klass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readYAMLFixture(String path, Class<T> klass) {
        try {
            final String yaml = fixture(path);
            return YAML_MAPPER.readValue(yaml, klass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readYAML(String yaml, Class<T> klass) {
        try {
            return YAML_MAPPER.readValue(yaml, klass);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public static JsonNode toJsonNode(Object o) {
        return JSON_MAPPER.valueToTree(o);
    }

    public static Observable<byte[]> generateRandomByteObservable() {
        return Observable.just(generateRandomBytes());
    }


    public static ValidJobRequest readJobRequestFixture(String path) {
        final ValidJobRequestFixture fixture = readJSONFixture(
                path,
                ValidJobRequestFixture.class);

        return fixture.toValidJobRequest();
    }

    public static ValidJobRequest validRequestWithName(String name) {
        return new ValidJobRequest(
                STANDARD_VALID_REQUEST.getOwner(),
                name,
                STANDARD_VALID_REQUEST.getInputs(),
                STANDARD_VALID_REQUEST.getSpec());
    }

    public static ValidJobRequest validRequestWithOwner(UserId owner) {
        return new ValidJobRequest(
                owner,
                STANDARD_VALID_REQUEST.getName(),
                STANDARD_VALID_REQUEST.getInputs(),
                STANDARD_VALID_REQUEST.getSpec());
    }

    public static float randomFloat() {
        return rng.nextFloat();
    }

    public static Path createTmpDir(Class testClass) throws IOException {
        return createTempDirectory(testClass.getSimpleName());
    }

    public static JobOutputDetails generateJobOutputDetails() {
        return new JobOutputDetails(
                generateJobOutputId(),
                randomIntBetween(50, 100),
                Optional.of(generateRandomString()),
                Optional.of(generateRandomString()),
                Optional.of(generateRandomString()),
                emptyMap());
    }

    public static void assertJSONEqual(String s1, String s2) throws IOException {
        assertThat(JSON_MAPPER.readTree(s1)).isEqualTo(JSON_MAPPER.readTree(s2));
    }

    public static void assertHasKeyWithValue(Map<String, APIRestLink> m, String expectedKey, String expectedHref) {
        assertThat(m.containsKey(expectedKey)).isTrue();
        assertThat(m.get(expectedKey).getHref().toString()).isEqualTo(expectedHref);
    }

    public static AuthenticationBootstrap createTypicalAuthBootstrap() {
        final UserDAO userDAO = mock(UserDAO.class);
        final Server s = new Server(0);
        final Servlet se = new ServletContainer();
        final JerseyEnvironment env = new JerseyEnvironment(new JerseyContainerHolder(se), new DropwizardResourceConfig());

        return new AuthenticationBootstrap(env, userDAO);
    }

    public static String generateBase64SecretKey() {
        final byte[] keyData = generateRandomBytes(256);
        return new String(Base64.getEncoder().encode(keyData));
    }
}
