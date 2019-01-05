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

package com.github.jobson.resources.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jobson.Constants;
import com.github.jobson.HttpStatusCodes;
import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.*;
import com.github.jobson.dao.jobs.JobDAO;
import com.github.jobson.dao.jobs.JobDetails;
import com.github.jobson.dao.jobs.JobOutputDetails;
import com.github.jobson.dao.jobs.ReadonlyJobDAO;
import com.github.jobson.dao.specs.JobSpecConfigurationDAO;
import com.github.jobson.jobinputs.JobExpectedInputId;
import com.github.jobson.jobinputs.JobInput;
import com.github.jobson.jobinputs.select.SelectInput;
import com.github.jobson.jobs.JobId;
import com.github.jobson.jobs.JobManagerActions;
import com.github.jobson.jobs.JobStatus;
import com.github.jobson.jobs.jobstates.FinalizedJob;
import com.github.jobson.jobs.jobstates.ValidJobRequest;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import com.github.jobson.utils.BinaryData;
import com.github.jobson.utils.CancelablePromise;
import com.github.jobson.utils.SimpleCancelablePromise;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.jobson.Constants.HTTP_JOBS_PATH;
import static com.github.jobson.HttpStatusCodes.NOT_FOUND;
import static com.github.jobson.TestHelpers.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public final class JobResourceTest {

    @Test(expected = NullPointerException.class)
    public void testCtorThrowIfNullableArgumentsAreNull() {
        final JobManagerActions jobManager = mock(JobManagerActions.class);
        final JobDAO jobDAO = mock(JobDAO.class);
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mock(JobSpecConfigurationDAO.class);

        new JobResource(jobManager, jobDAO, null, Constants.DEFAULT_PAGE_SIZE);
        new JobResource(jobManager, null, jobSpecConfigurationDAO, Constants.DEFAULT_PAGE_SIZE);
        new JobResource(null, jobDAO, jobSpecConfigurationDAO, Constants.DEFAULT_PAGE_SIZE);
    }

    @Test(expected = RuntimeException.class)
    public void testCtorThrowsIfPageSizeIsNegative() {
        new JobResource(
                mock(JobManagerActions.class),
                mock(JobDAO.class),
                mock(JobSpecConfigurationDAO.class),
                -1);
    }



    @Test
    public void testFetchJobSummariesReturnsASummariesResponseContainingSummariesFromTheDAO() throws IOException {
        final List<JobDetails> summariesReturnedByDAO = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(summariesReturnedByDAO);
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection returnedSummaries = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        Assertions.assertThat(returnedSummaries.getEntries()).isEqualTo(summariesReturnedByDAO);
    }

    private JobDAO mockJobDAOThatReturns(List<JobDetails> details) {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobs(anyInt(), anyInt())).thenReturn(details);
        return jobDAO;
    }

    private JobResource resourceThatUses(JobDAO jobDAO) {
        return new JobResource(
                mock(JobManagerActions.class),
                jobDAO,
                mock(JobSpecConfigurationDAO.class),
                Constants.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testFetchJobSummariesCallsTheDAOWithPageIndex0IfPageIsNotSpecified() throws IOException {
        final List<JobDetails> summariesReturnedByDAO = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(summariesReturnedByDAO);
        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        verify(jobDAO, times(1))
                .getJobs(anyInt(), eq(0));
    }

    @Test
    public void testFetchJobSummariesCallsTheDAOWithSpecifiedPageIndex() throws IOException {
        final List<JobDetails> summariesReturnedByDAO = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(summariesReturnedByDAO);
        final JobResource jobResource = resourceThatUses(jobDAO);
        final int requestedPage = 5;

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.of(requestedPage),
                Optional.empty(),
                Optional.empty());

        verify(jobDAO, times(1))
                .getJobs(anyInt(), eq(requestedPage));
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobSummariesThrowsExceptionIfSpecifiedPageIndexIsNegative() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummaries);
        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.of(-1), // Should cause exception
                Optional.empty(),
                Optional.empty());
    }

    @Test
    public void testFetchJobSummariesCallsTheDAOWithTheDefaultPageSizeIfPageSizeNotSpecifed() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummaries);
        final int defaultPageSize = TestHelpers.randomIntBetween(5, 15);

        final JobResource jobResource = new JobResource(
                mock(JobManagerActions.class),
                jobDAO,
                mock(JobSpecConfigurationDAO.class),
                defaultPageSize);

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        verify(jobDAO, times(1))
                .getJobs(eq(defaultPageSize), anyInt());
    }

    @Test
    public void testFetchJobSummariesCallsTheDAOWithThePageSizeIfItIsSpecified() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummaries);
        final JobResource jobResource = resourceThatUses(jobDAO);
        final int requestedPageSize = TestHelpers.randomIntBetween(1, jobSummaries.size());

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.of(requestedPageSize),
                Optional.empty());

        verify(jobDAO, times(1))
                .getJobs(eq(requestedPageSize), anyInt());
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobSummariesThrowsExceptionIfRequestedPageSizeIsNegative() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummaries);
        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.of(-1), // Should cause exception
                Optional.empty());
    }

    @Test
    public void testFetchJobSummariesCallsTheQueryOverloadOnTheDAOIfAQueryWasSpecified() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummaries);
        final JobResource jobResource = resourceThatUses(jobDAO);
        final String queryString = TestHelpers.generateRandomString();

        jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(queryString));

        verify(jobDAO, times(1))
                .getJobs(anyInt(), anyInt(), eq(queryString));
    }


    @Test
    public void testFetchJobSummariesContainsLinksToAbortJobIfJobIsAbortable() throws IOException {
        final List<JobDetails> jobSummariesWithAbortableStatus =
                generateRandomList(10, 20, () -> generateJobDetailsWithStatus(JobStatus.RUNNING));

        final JobDAO jobDAO = mockJobDAOThatReturns(jobSummariesWithAbortableStatus);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertHasAJobAbortionRESTLink);
    }

    private void assertHasAJobAbortionRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("abort")).isTrue();
        assertThat(jobSummary.getLinks().get("abort").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + jobSummary.getId().toString() + "/abort");
    }

    @Test
    public void testFetchJobSummariesDoesNotContainsLinksToAbortJobIfJobIsNotAbortable() throws IOException {
        final List<JobDetails> notAbortableSummaries =
                generateRandomList(10, 20, () -> generateJobDetailsWithStatus(JobStatus.FINISHED));

        final JobDAO jobDAO = mockJobDAOThatReturns(notAbortableSummaries);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertDoesNotHaveAbortionRESTLink);
    }

    private void assertDoesNotHaveAbortionRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("abort")).isFalse();
    }

    @Test
    public void testFetchJobSummariesContainsLinksToJobStdoutIfJobHasStdout() throws IOException {
        final List<JobDetails> summaries = generateRandomJobDetails();

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobs(anyInt(), anyInt())).thenReturn(summaries);
        when(jobDAO.hasStdout(any())).thenReturn(true);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertHasAValidStdoutRESTLink);
    }

    private void assertHasAValidStdoutRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("stdout")).isTrue();
        assertThat(jobSummary.getLinks().get("stdout").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + jobSummary.getId().toString() + "/stdout");
    }

    @Test
    public void testFetchJobSummariesDoesNotContainLinksToStdoutIfJobHasNoStdout() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobs(anyInt(), anyInt())).thenReturn(jobSummaries);
        when(jobDAO.hasStdout(any())).thenReturn(false);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertDoesNotHaveAnStdoutRESTLink);
    }

    private void assertDoesNotHaveAnStdoutRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("stdout")).isFalse();
    }

    @Test
    public void testFetchJobSummariesContainsLinksToJobStderrIfJobHasStderr() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobs(anyInt(), anyInt())).thenReturn(jobSummaries);
        when(jobDAO.hasStderr(any())).thenReturn(true);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertHasAnStderrRESTLink);
    }

    private void assertHasAnStderrRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("stderr")).isTrue();
        assertThat(jobSummary.getLinks().get("stderr").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + jobSummary.getId().toString() + "/stderr");
    }


    @Test
    public void testFetchJobSummariesDoesNotContainLinksToStderrIfJobHasNoStderr() throws IOException {
        final List<JobDetails> jobSummaries = generateRandomJobDetails();

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobs(anyInt(), anyInt())).thenReturn(jobSummaries);
        when(jobDAO.hasStderr(any())).thenReturn(false);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetailsCollection resp = jobResource.getJobs(
                TestHelpers.generateSecureSecurityContext(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        resp.getEntries().forEach(this::assertDoesNotHaveAnStderrRESTLink);
    }

    private void assertDoesNotHaveAnStderrRESTLink(APIJobDetails jobSummary) {
        assertThat(jobSummary.getLinks().containsKey("stderr")).isFalse();
    }


    @Test(expected = WebApplicationException.class)
    public void testFetchJobDetailsByIdThrowsWebApplicationExceptionIfJobIdIsNotSpecified() {
        final JobResource jobResource = resourceThatUses(mock(JobDAO.class));

        jobResource.getJobDetailsById(TestHelpers.generateSecureSecurityContext(), null);
    }

    @Test
    public void testFetchJobDetailsByIdCallsTheDAOWithTheProvidedJobIdAndReturnsTheJobDetailsFromTheDAO() throws IOException {
        final JobDetails jobDetailsFromDAO = generateValidJobDetails();
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsFromDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final Optional<APIJobDetails> jobDetailsResponse = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsFromDAO.getId());

        verify(jobDAO, times(1))
                .getJobDetailsById(eq(jobDetailsFromDAO.getId()));
        assertThat(jobDetailsResponse.get()).isEqualTo(jobDetailsFromDAO);
    }

    private JobDAO mockJobDAOThatReturns(Optional<JobDetails> jobDetailsReturnedByDAO) {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getJobDetailsById(any())).thenReturn(jobDetailsReturnedByDAO);
        return jobDAO;
    }

    private JobDAO mockJobDAOThatReturnsSpec(Optional<JobSpec> jobSpec) {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getSpecJobWasSubmittedAgainst(any())).thenReturn(jobSpec);
        return jobDAO;
    }

    @Test
    public void testFetchJobDetailsByIdReturnsEmptyOptionalIfJobIDCannotBeFoundInTheJobsDAO() throws IOException {
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.empty());
        final JobResource jobResource = resourceThatUses(jobDAO);
        final JobId jobId = TestHelpers.generateJobId();

        final Optional<APIJobDetails> resp =
                jobResource.getJobDetailsById(TestHelpers.generateSecureSecurityContext(), jobId);

        assertThat(resp).isEqualTo(Optional.empty());
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobDetailsByIdThrowsWebApplicationExceptionIfJobIdIsNull() throws IOException {
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.empty());
        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.getJobDetailsById(TestHelpers.generateSecureSecurityContext(), null);
    }

    @Test
    public void testFetchJobDetailsByIdSetsASelfRESTLink() {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.RUNNING);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails APIJobDetailsDetails = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertHasASelfRestLink(APIJobDetailsDetails);
    }

    private void assertHasASelfRestLink(APIJobDetails APIJobDetailsDetails) {
        assertThat(APIJobDetailsDetails.getLinks().containsKey("self")).isTrue();
        assertThat(APIJobDetailsDetails.getLinks().get("self").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + APIJobDetailsDetails.getId());
    }

    @Test
    public void testFetchJobDetailsByIdSetsAnInputsRESTLink() {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.RUNNING);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        when(jobDAO.hasJobInputs(any())).thenReturn(true);
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails APIJobDetailsDetails = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertHasInputsRESTLink(APIJobDetailsDetails);
    }

    private void assertHasInputsRESTLink(APIJobDetails APIJobDetailsDetails) {
        assertThat(APIJobDetailsDetails.getLinks().containsKey("inputs")).isTrue();
        assertThat(APIJobDetailsDetails.getLinks().get("inputs").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + APIJobDetailsDetails.getId() + "/inputs");
    }

    @Test
    public void testFetchJobDetailsByIdSetsAnOutputsRESTLink() {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.RUNNING);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails APIJobDetailsDetails = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertHasOutputsRESTLink(APIJobDetailsDetails);
    }

    private void assertHasOutputsRESTLink(APIJobDetails APIJobDetailsDetails) {
        assertThat(APIJobDetailsDetails.getLinks().containsKey("outputs")).isTrue();
        assertThat(APIJobDetailsDetails.getLinks().get("outputs").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + APIJobDetailsDetails.getId() + "/outputs");
    }

    @Test
    public void testFetchJobDetailsByIdSetsAnAbortRESTLinkIfJobIsAbortable() throws IOException {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.RUNNING);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails APIJobDetailsDetails = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertHasAnAbortRESTLink(APIJobDetailsDetails);
    }

    private void assertHasAnAbortRESTLink(APIJobDetails APIJobDetailsDetails) {
        assertThat(APIJobDetailsDetails.getLinks().containsKey("abort")).isTrue();
        assertThat(APIJobDetailsDetails.getLinks().get("abort").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + APIJobDetailsDetails.getId() + "/abort");
    }

    @Test
    public void testFetchJobDetailsByIdDoesNotSetAnAbortRESTLinkIfJobIsNotAbortable() throws IOException {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.FINISHED);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails APIJobDetailsDetails = jobResource.getJobDetailsById(
                TestHelpers.generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertDoesNotHaveAnAbortRESTLink(APIJobDetailsDetails);
    }

    private void assertDoesNotHaveAnAbortRESTLink(APIJobDetails APIJobDetailsDetails) {
        assertThat(APIJobDetailsDetails.getLinks().containsKey("abort")).isFalse();
    }


    @Test
    public void testFetchJobDetailsByIdSetsARESTLinkForTheSpec() {
        final APIJobDetails jobDetailsReturnedByDAO =
                generateJobDetailsWithStatus(JobStatus.FINISHED);
        final JobDAO jobDAO = mockJobDAOThatReturns(Optional.of(jobDetailsReturnedByDAO));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobDetails apiJobDetailsDetails = jobResource.getJobDetailsById(
                generateSecureSecurityContext(),
                jobDetailsReturnedByDAO.getId())
                .get();

        assertHasASpecRESTLink(apiJobDetailsDetails);
    }

    private void assertHasASpecRESTLink(APIJobDetails apiJobDetailsDetails) {
        assertThat(apiJobDetailsDetails.getLinks().containsKey("spec")).isTrue();
        assertThat(apiJobDetailsDetails.getLinks().get("spec").getHref().toString())
                .isEqualTo(HTTP_JOBS_PATH + "/" + apiJobDetailsDetails.getId() + "/spec");
    }



    @Test(expected = WebApplicationException.class)
    public void testSubmitJobThrowsAWebApplicationExceptionIfJobSubmissionRequestIsNull() {
        final JobResource jobResource = mockedJobResource();
        jobResource.submitJob(TestHelpers.generateSecureSecurityContext(), null);
    }

    private JobResource mockedJobResource() {
        return new JobResource(
                mock(JobManagerActions.class),
                mock(JobDAO.class),
                mock(JobSpecConfigurationDAO.class),
                Constants.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testSubmitJobCallsTheDAOSubmitMethodIfValidAgainstSpec() throws IOException {
        final JobManagerActions jobManagerActions = mockJobManagerThatReturns(typicalSubmissionReturn());
        final JobDAO jobDAO = mock(JobDAO.class);
        final JobSpec jobSpec = generateValidJobSpec();
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mockJobSpecDAOThatReturns(jobSpec);

        final JobResource jobResource = new JobResource(
                jobManagerActions,
                jobDAO,
                jobSpecConfigurationDAO,
                Constants.DEFAULT_PAGE_SIZE);

        jobResource.submitJob(TestHelpers.generateSecureSecurityContext(), generateValidJobRequest());

        verify(jobSpecConfigurationDAO, times(1))
                .getJobSpecById(new JobSpecId("job-schema-1"));

        verify(jobManagerActions, times(1)).submit(any());
    }

    private Pair<JobId, CancelablePromise<FinalizedJob>> typicalSubmissionReturn() {
        return Pair.of(TestHelpers.generateJobId(), new SimpleCancelablePromise<>());
    }

    private JobManagerActions mockJobManagerThatReturns(Pair<JobId, CancelablePromise<FinalizedJob>> ret) {
        final JobManagerActions jobManagerActions = mock(JobManagerActions.class);
        when(jobManagerActions.submit(any())).thenReturn(ret);
        return jobManagerActions;
    }

    private JobSpec generateValidJobSpec() {
        return TestHelpers.readJSONFixture("fixtures/resources/1_valid-job-spec-configuration.json", JobSpec.class);
    }

    private JobSpecConfigurationDAO mockJobSpecDAOThatReturns(JobSpec jobSpec) {
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mock(JobSpecConfigurationDAO.class);
        when(jobSpecConfigurationDAO.getJobSpecById(any())).thenReturn(Optional.of(jobSpec));
        return jobSpecConfigurationDAO;
    }

    private APIJobRequest generateValidJobRequest() {
        return TestHelpers.readJSONFixture("fixtures/resources/1_valid-job-request-against-spec.json", APIJobRequest.class);
    }

    @Test
    public void testSubmitJobReturnsAResponseContainingTheIDReturnedByTheDAO() throws IOException {
        final JobId jobId = TestHelpers.generateJobId();
        final Pair<JobId, CancelablePromise<FinalizedJob>> managerRet = Pair.of(jobId, new SimpleCancelablePromise<>());
        final JobManagerActions jobManagerActions = mockJobManagerThatReturns(managerRet);
        final JobDAO jobDAO = mock(JobDAO.class);
        final JobSpec jobSpec = generateValidJobSpec();
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mockJobSpecDAOThatReturns(jobSpec);

        final JobResource jobResource = new JobResource(
                jobManagerActions,
                jobDAO,
                jobSpecConfigurationDAO,
                Constants.DEFAULT_PAGE_SIZE);

        final APIJobCreatedResponse resp =
                jobResource.submitJob(TestHelpers.generateSecureSecurityContext(), generateValidJobRequest());

        assertThat(resp.getId()).isEqualTo(jobId);
    }

    @Test(expected = WebApplicationException.class)
    public void testSubmitJobThrowsWebApplicationErrorIfTheRequestProducedValidationErrors() throws IOException {
        final JobManagerActions jobManagerActions = mockJobManagerThatReturns(typicalSubmissionReturn());
        final JobDAO jobDAO = mock(JobDAO.class);
        final JobSpec jobSpec = generateValidJobSpec();
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mockJobSpecDAOThatReturns(jobSpec);

        final JobResource jobResource = new JobResource(
                jobManagerActions,
                jobDAO,
                jobSpecConfigurationDAO,
                Constants.DEFAULT_PAGE_SIZE);

        jobResource.submitJob(
                TestHelpers.generateSecureSecurityContext(),
                generateInvalidJobRequest());
    }

    private APIJobRequest generateInvalidJobRequest() {
        return TestHelpers.readJSONFixture("fixtures/resources/1_invalid-job-request-against-spec.json", APIJobRequest.class);
    }

    @Test
    public void testSubmitJobResolvesDefaultValuesInTheRequestIfNotProvidedInTheSubmissionRequest() throws IOException {
        final JobManagerActions jobManagerActions = mockJobManagerThatReturns(typicalSubmissionReturn());
        final JobDAO jobDAO = mock(JobDAO.class);
        final JobSpec jobSpec = generateValidJobSpec();
        final JobSpecConfigurationDAO jobSpecConfigurationDAO = mockJobSpecDAOThatReturns(jobSpec);

        final JobResource jobResource = new JobResource(
                jobManagerActions,
                jobDAO,
                jobSpecConfigurationDAO,
                Constants.DEFAULT_PAGE_SIZE);

        jobResource.submitJob(
                TestHelpers.generateSecureSecurityContext(),
                getJobRequestWithMissingButDefaultedArg());

        final ArgumentCaptor<ValidJobRequest> captor =
                ArgumentCaptor.forClass(ValidJobRequest.class);

        verify(jobManagerActions, times(1))
                .submit(captor.capture());

        final ValidJobRequest validatedJobRequest = captor.getValue();

        final JobExpectedInputId defaultedInputId = new JobExpectedInputId("foo");
        final JobInput jobInput = validatedJobRequest.getInputs().get(defaultedInputId);

        assertThat(jobInput).isNotNull();
        assertThat(jobInput.getClass()).isEqualTo(SelectInput.class);
        assertThat(((SelectInput)jobInput).getValue()).isEqualTo("a");
    }

    private APIJobRequest getJobRequestWithMissingButDefaultedArg() {
        return TestHelpers.readJSONFixture(
                "fixtures/resources/2_valid-job-request-without-defaulted-arg.json",
                APIJobRequest.class);
    }




    @Test(expected = WebApplicationException.class)
    public void testAbortJobThrowsWebApplicationExceptionIfJobIdIsNull() {
        final JobResource jobResource = mockedJobResource();
        jobResource.abortJob(TestHelpers.generateSecureSecurityContext(), null);
    }

    @Test(expected = WebApplicationException.class)
    public void testAbortJobThrowsIfJobManagerReturnsFalseForAbort() throws IOException {
        final JobManagerActions jobManager = mock(JobManagerActions.class);
        when(jobManager.tryAbort(any())).thenReturn(false);

        final JobResource jobResource = resourceThatUses(jobManager);

        jobResource.abortJob(TestHelpers.generateSecureSecurityContext(), TestHelpers.generateJobId());
    }

    private JobResource resourceThatUses(JobManagerActions jobManagerActions) {
        return new JobResource(
                jobManagerActions,
                mock(JobDAO.class),
                mock(JobSpecConfigurationDAO.class),
                Constants.DEFAULT_PAGE_SIZE);
    }

    @Test
    public void testAbortJobCallsAbortJobInTheDAOWithTheID() throws IOException {
        final JobManagerActions jobManager = mock(JobManagerActions.class);
        when(jobManager.tryAbort(any())).thenReturn(true);
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);

        final JobResource jobResource = resourceThatUses(jobManager, jobDAO);
        final JobId jobId = TestHelpers.generateJobId();

        jobResource.abortJob(TestHelpers.generateSecureSecurityContext(), jobId);

        verify(jobManager, times(1)).tryAbort(jobId);
    }

    private JobResource resourceThatUses(JobManagerActions jobManagerActions, JobDAO jobDAO) {
        return new JobResource(
                jobManagerActions,
                jobDAO,
                mock(JobSpecConfigurationDAO.class),
                Constants.DEFAULT_PAGE_SIZE);
    }



    @Test(expected = WebApplicationException.class)
    public void testGetJobStdoutByIdThrowsWebApplicationExceptionIfNoJobIdIsProvided() {
        final JobResource jobResource = mockedJobResource();
        jobResource.fetchJobStdoutById(TestHelpers.generateSecureSecurityContext(), null);
    }

    @Test
    public void testGetJobStdoutByIdReturns404NotFoundIfDAOReturnsEmptyOptional() throws IOException {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getStdout(any())).thenReturn(Optional.empty());

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response jobStdoutResponse =
                jobResource.fetchJobStdoutById(TestHelpers.generateSecureSecurityContext(), TestHelpers.generateJobId());

        assertThat(jobStdoutResponse.getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void testGetJobStdoutByIdReturnsA200ResponseAndTheDataIfTheDAOHasStdoutData() throws IOException {
        final byte[] stdoutRawData = TestHelpers.generateRandomBytes();
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getStdout(any()))
                .thenReturn(Optional.of(BinaryData.wrap(stdoutRawData)));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response response = jobResource.fetchJobStdoutById(
                TestHelpers.generateSecureSecurityContext(),
                TestHelpers.generateJobId());

        assertThat(response.getStatus()).isEqualTo(HttpStatusCodes.OK);
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/octet-stream");
        assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(stdoutRawData.length));
        assertThat(readAsByteArray(response)).isEqualTo(stdoutRawData);
    }

    private byte[] readAsByteArray(Response response) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ((StreamingOutput)response.getEntity()).write(outputStream);
        return outputStream.toByteArray();
    }



    @Test(expected = WebApplicationException.class)
    public void testGetJobStderrByIdThrowsWebApplicationExceptionIfNoJobIdProvided() {
        final JobResource jobResource = mockedJobResource();
        jobResource.fetchJobStderrById(TestHelpers.generateSecureSecurityContext(), null);
    }

    @Test
    public void testGetJobStderrByIdReturns404NotFoundIfDAOReturnsEmptyOptional() throws IOException {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getStderr(any())).thenReturn(Optional.empty());
        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response response =
                jobResource.fetchJobStderrById(TestHelpers.generateSecureSecurityContext(), TestHelpers.generateJobId());

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND);
    }

    @Test
    public void testGetJobStderrByIdReturnsJobStderrIfPresent() throws IOException {
        final byte[] stderrRawData = TestHelpers.generateRandomBytes();
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.getStderr(any()))
                .thenReturn(Optional.of(BinaryData.wrap(stderrRawData)));
        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response response =
                jobResource.fetchJobStderrById(TestHelpers.generateSecureSecurityContext(), TestHelpers.generateJobId());

        assertThat(response.getStatus()).isEqualTo(HttpStatusCodes.OK);
        assertThat(response.getHeaderString("Content-Type")).isEqualTo("application/octet-stream");
        assertThat(response.getHeaderString("Content-Length")).isEqualTo(Long.toString(stderrRawData.length));
        assertThat(readAsByteArray(response)).isEqualTo(stderrRawData);
    }


    @Test
    public void testFetchJobSpecJobWasSubmittedAgainstReturnsEmptyIfIdDoesNotExist() {
        final JobDAO jobDAO = mockJobDAOThatReturnsSpec(Optional.empty());
        final JobResource jobResource = resourceThatUses(jobDAO);

        assertThat(
                jobResource.fetchJobSpecJobWasSubmittedAgainst(generateSecureSecurityContext(), generateJobId()))
                .isNotPresent();
    }

    @Test
    public void testFetchJobSpecJobWasSubmittedAgainstReturnsTheJobSpecIfIdDoesExist() {
        final JobSpec jobSpec = generateValidJobSpec();
        final JobDAO jobDAO = mockJobDAOThatReturnsSpec(Optional.of(jobSpec));
        final JobResource jobResource = resourceThatUses(jobDAO);

        assertThat(
                jobResource.fetchJobSpecJobWasSubmittedAgainst(generateSecureSecurityContext(), generateJobId()))
                .isPresent();
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobOutputsThrows404ExceptionIfJobDoesNotExist() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(false);
        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.fetchJobOutputs(generateSecureSecurityContext(), generateJobId());
    }

    @Test
    public void testFetchJobOutputsReturnsEmptyMapIfDAOReturnsEmptyMap() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        when(jobDAO.getJobOutputs(any())).thenReturn(emptyList());

        final JobResource jobResource = resourceThatUses(jobDAO);

        final APIJobOutputCollection ret =
                jobResource.fetchJobOutputs(generateSecureSecurityContext(), generateJobId());

        assertThat(ret.getEntries()).isEmpty();
    }

    @Test
    public void testFetchJobOutputsReturnsMapOfOutputsReturnedFromDAO() {
        final List<JobOutputDetails> outputsFromDAO = generateRandomList(
                10,
                20,
                TestHelpers::generateJobOutputDetails);

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        when(jobDAO.getJobOutputs(any())).thenReturn(outputsFromDAO);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final JobId jobId = generateJobId();

        final APIJobOutputCollection ret =
                jobResource.fetchJobOutputs(generateSecureSecurityContext(), jobId);

        assertThat(ret.getEntries().size()).isEqualTo(outputsFromDAO.size());
        assertThat(ret.getEntries().stream().map(APIJobOutput::getId).collect(Collectors.toList()))
                .isEqualTo(outputsFromDAO.stream().map(JobOutputDetails::getId).collect(Collectors.toList()));

        for (APIJobOutput returnedOutput : ret.getEntries()) {
            final JobOutputDetails outputFromDAO =
                    outputsFromDAO
                            .stream()
                            .filter(jobOutput -> jobOutput.getId().equals(returnedOutput.getId()))
                            .findFirst()
                            .get();

            assertThat(returnedOutput.getMimeType()).isEqualTo(outputFromDAO.getMimeType());
            assertThat(returnedOutput.getName()).isEqualTo(outputFromDAO.getName());
            assertThat(returnedOutput.getDescription()).isEqualTo(outputFromDAO.getDescription());
            assertThat(returnedOutput.getMetadata()).isEqualTo(outputFromDAO.getMetadata());
            assertThat(returnedOutput.getHref()).contains("/jobs/" + jobId + "/outputs/" + returnedOutput.getId());
        }
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobOutputReturns404IfJobDoesNotExist() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(false);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response ret =
                jobResource.fetchJobOutput(generateSecureSecurityContext(), generateJobId(), generateJobOutputId());
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobOutputReturns404IfOutputDoesNotExist() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        when(jobDAO.getOutput(any(), any())).thenReturn(Optional.empty());

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response ret =
                jobResource.fetchJobOutput(generateSecureSecurityContext(), generateJobId(),  generateJobOutputId());
    }

    @Test
    public void testFetchJobOutputReturns200IfOutputExists() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        final BinaryData bd = generateRandomBinaryData();
        when(jobDAO.getOutput(any(), any())).thenReturn(Optional.of(bd));

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response ret =
                jobResource.fetchJobOutput(generateSecureSecurityContext(), generateJobId(), generateJobOutputId());

        assertThat(ret.getStatus()).isEqualTo(200);
    }

    @Test
    public void testFetchJobOutputContentTypeMatchesOutputContentType() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        final String mimeType = "application/x-test-type";
        final BinaryData bd = generateRandomBinaryData().withMimeType(mimeType);
        when(jobDAO.getOutput(any(), any())).thenReturn(Optional.of(bd));

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response ret =
                jobResource.fetchJobOutput(generateSecureSecurityContext(), generateJobId(), generateJobOutputId());

        assertThat(ret.getHeaderString("Content-Type")).isEqualTo(mimeType);
    }

    @Test
    public void testFetchJobOutputSetsContentEncodingToIdentityIfAboveBreakpoint() {
        // *Large* job outputs (i.e. bigger than a breakpoint) should not be
        // compressed on the fly. It is known to cause huge CPU and memory spikes
        // in the server, which might be unpredictable for devs.
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        final int breakpoint = Constants.MAX_JOB_OUTPUT_SIZE_IN_BYTES_BEFORE_DISABLING_COMPRESSION;
        final byte data[] = TestHelpers.generateRandomBytes(breakpoint + 1);
        final BinaryData bd = BinaryData.wrap(data);
        when(jobDAO.getOutput(any(), any())).thenReturn(Optional.of(bd));

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Response ret =
                jobResource.fetchJobOutput(generateSecureSecurityContext(), generateJobId(), generateJobOutputId());

        assertThat(ret.getHeaderString("Content-Encoding")).isEqualTo("identity");
    }

    @Test
    public void testFetchJobInputsThrows404IfJobDoesNotExist() {
        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(false);
        when(jobDAO.getJobInputs(any())).thenReturn(Optional.empty());

        final JobResource jobResource = resourceThatUses(jobDAO);

        assertThat(
                jobResource.fetchJobInputs(generateSecureSecurityContext(), generateJobId())
        ).isNotPresent();
    }

    @Test
    public void testFetchJobInputsReturnsJobInputsFromDAOIfJobExists() {
        final Map<JobExpectedInputId, JsonNode> inputs = new HashMap<>();

        final JobDAO jobDAO = mock(JobDAO.class);
        when(jobDAO.jobExists(any())).thenReturn(true);
        when(jobDAO.getJobInputs(any())).thenReturn(Optional.of(inputs));

        final JobResource jobResource = resourceThatUses(jobDAO);

        final Optional<Map<JobExpectedInputId, JsonNode>> ret =
                jobResource.fetchJobInputs(generateSecureSecurityContext(), generateJobId());

        assertThat(ret).isPresent();
        assertThat(ret.get()).isEqualTo(inputs);
    }

    @Test(expected = WebApplicationException.class)
    public void testDeleteJobThrowsBadRequestIfJobIdIsNull() {
        final JobDAO jobDAO = mock(JobDAO.class);

        final JobResource jobResource = resourceThatUses(jobDAO);

        jobResource.deleteJob(generateSecureSecurityContext(), null);
    }

    @Test
    public void testDeleteJobCallsJobDAORemove() {
        final JobDAO jobDAO = mock(JobDAO.class);

        final JobResource jobResource = resourceThatUses(jobDAO);

        final JobId jobId = generateJobId();

        jobResource.deleteJob(generateSecureSecurityContext(), jobId);

        verify(jobDAO, times(1)).remove(jobId);
    }

    @Test
    public void testDeleteJobCallsAbortOnJob() {
        final JobManagerActions actions = mock(JobManagerActions.class);
        final JobDAO jobDAO = mock(JobDAO.class);

        final JobResource jobResource = resourceThatUses(actions, jobDAO);

        final JobId jobId = generateJobId();
        when(actions.tryAbort(jobId)).thenReturn(true);

        jobResource.deleteJob(generateSecureSecurityContext(), jobId);

        verify(actions, times(1)).tryAbort(jobId);
    }
}