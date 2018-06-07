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

import com.github.jobson.TestHelpers;
import com.github.jobson.api.v1.APIJobSpec;
import com.github.jobson.api.v1.APIJobSpecSummary;
import com.github.jobson.api.v1.APIJobSpecSummaryCollection;
import com.github.jobson.dao.specs.JobSpecDAO;
import com.github.jobson.dao.specs.JobSpecSummary;
import com.github.jobson.specs.JobSpec;
import com.github.jobson.specs.JobSpecId;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.github.jobson.Constants.HTTP_SPECS_PATH;
import static com.github.jobson.TestHelpers.*;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public final class JobSpecResourceTest {

    private static int DEFAULT_PAGE_SIZE = 20;



    @Test(expected = NullPointerException.class)
    public void testCtorThrowsNullPointerExceptionIfArgumentIsNull() {
        new JobSpecResource(null, DEFAULT_PAGE_SIZE);
    }

    @Test(expected = RuntimeException.class)
    public void testCtorThrowsRuntimeExceptionIfDefaultPageSizeIsNegative() {
        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);

        new JobSpecResource(jobSpecDAO, -1);
    }





    @Test
    public void testFetchJobSpecSummariesReturnsAResponseIfCalledWithOnlyEmptyOptionals() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        final APIJobSpecSummaryCollection apiJobSpecSummaryCollection =
                jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(apiJobSpecSummaryCollection).isNotNull();
        assertThat(
                apiJobSpecSummaryCollection.getEntries().stream().map(APIJobSpecSummary::toJobSpecSummary).collect(toList()))
                .isEqualTo(jobSpecSummariesReturnedByDAO);
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobSpecSummariesThrowsExceptionIfPageSizeIsNegative() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        // Should throw
        final APIJobSpecSummaryCollection APIJobSpecSummaryCollection =
                jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.of(-1), Optional.empty());
    }

    @Test
    public void testFetchJobSpecSummariesCallsTheDAOWithPage0AndDefaultPageSize() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.empty(), Optional.empty());

        verify(jobSpecDAO, times(1)).getJobSpecSummaries(DEFAULT_PAGE_SIZE, 0);
    }

    @Test
    public void testFetchJobSpecSummariesCallsTheDAOWithTheSpecifiedPageSize() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();
        final int specifiedPageSize = TestHelpers.randomIntBetween(10, 30);

        jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.of(specifiedPageSize), Optional.empty());

        verify(jobSpecDAO, times(1)).getJobSpecSummaries(specifiedPageSize, 0);
    }

    @Test
    public void testFetchJobSpecSummariesCallsTheDAOWithThePageIfSpecified() throws IOException {
        // 50-100 pages
        final int numEntries = TestHelpers.randomIntBetween(50 * DEFAULT_PAGE_SIZE, 100 * DEFAULT_PAGE_SIZE);

        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(numEntries);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();
        final int pageRequested = TestHelpers.randomIntBetween(15, 70);

        jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.of(pageRequested), Optional.empty(), Optional.empty());

        verify(jobSpecDAO, times(1)).getJobSpecSummaries(DEFAULT_PAGE_SIZE, pageRequested);
    }

    @Test(expected = WebApplicationException.class)
    public void testFetchJobSpecSummariesThrowsWebApplicationExceptionIfPageIsNegative() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        // Should throw
        jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.of(-1), Optional.empty(), Optional.empty());
    }

    @Test
    public void testFetchJobSpecSummariesCallsTheDAOWithTheQuerySpecified() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();
        final String query = TestHelpers.generateRandomString();

        jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.empty(), Optional.of(query));

        verify(jobSpecDAO, times(1)).getJobSpecSummaries(DEFAULT_PAGE_SIZE, 0, query);
    }

    @Test
    public void testFetchJobSpecSummariesContainsLinkToSummaryDetails() throws IOException {
        final List<JobSpecSummary> jobSpecSummariesReturnedByDAO = generateNJobSpecSummaries(5);

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecSummaries(anyInt(), anyInt())).thenReturn(jobSpecSummariesReturnedByDAO);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        final APIJobSpecSummaryCollection APIJobSpecSummaryCollection =
                jobSpecResource.fetchJobSpecSummaries(securityContext, Optional.empty(), Optional.empty(), Optional.empty());

        for (APIJobSpecSummary APIJobSpecSummary : APIJobSpecSummaryCollection.getEntries()) {
            assertThat(APIJobSpecSummary.getLinks().containsKey("details")).isTrue();
            assertThat(APIJobSpecSummary.getLinks().get("details").getHref().toString())
                    .contains(HTTP_SPECS_PATH + "/" + APIJobSpecSummary.getId().toString());
        }
    }





    @Test(expected = WebApplicationException.class)
    public void testFetchJobSpecDetailsByIdThrowsWebApplicationExceptionIfJobSpecIdIsNull() throws IOException {
        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();

        final Optional<APIJobSpec> response = jobSpecResource.fetchJobSpecDetailsById(securityContext, null);
    }

    @Test
    public void testFetchJobSpecDetailsByIdReturnsWhateverTheDAOReturns() throws IOException {
        final JobSpec jobSpecFromDAO = generateJobSpec();

        final JobSpecDAO jobSpecDAO = mock(JobSpecDAO.class);
        when(jobSpecDAO.getJobSpecById(any())).thenReturn(Optional.of(jobSpecFromDAO));

        final JobSpecResource jobSpecResource = new JobSpecResource(jobSpecDAO, DEFAULT_PAGE_SIZE);

        final SecurityContext securityContext = generateSecureSecurityContext();
        final JobSpecId jobSpecId = jobSpecFromDAO.getId();

        final Optional<APIJobSpec> maybeJobSpecFromResource =
                jobSpecResource.fetchJobSpecDetailsById(securityContext, jobSpecId);

        assertThat(maybeJobSpecFromResource).isPresent();
        assertThat(maybeJobSpecFromResource.get()).isEqualTo(APIJobSpec.fromJobSpec(jobSpecFromDAO));
    }
}