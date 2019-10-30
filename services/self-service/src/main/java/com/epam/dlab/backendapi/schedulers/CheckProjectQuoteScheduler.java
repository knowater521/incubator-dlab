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

package com.epam.dlab.backendapi.schedulers;

import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.schedulers.internal.Scheduled;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Scheduled("checkProjectQuoteScheduler")
@Slf4j
public class CheckProjectQuoteScheduler implements Job {

	@Inject
	private BillingDAO billingDAO;
	@Inject
	private EnvironmentService environmentService;
	@Inject
	private ProjectService projectService;

	@Override
	public void execute(JobExecutionContext context) {
		projectService.getProjects()
				.stream()
				.map(ProjectDTO::getName)
				.filter(billingDAO::isProjectQuoteReached)
				.peek(p -> log.debug("Stopping {} project env because of reaching user billing quote", p))
				.forEach(environmentService::stopProjectEnvironment);
	}
}
