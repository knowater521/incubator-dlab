/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.SparkStandaloneClusterResource;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.*;
import static com.epam.dlab.rest.contracts.ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC;

@Singleton
@Slf4j
public class ComputationalServiceImpl implements ComputationalService {

	private static final String COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT = "Could not update the status of " +
			"computational resource {} for user {}";
	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ComputationalDAO computationalDAO;

	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private SelfServiceApplicationConfiguration configuration;


	@Override
	public boolean createSparkCluster(UserInfo userInfo, SparkStandaloneClusterCreateForm form) {

		validateForm(form);

		if (computationalDAO.addComputational(userInfo.getName(), form.getNotebookName(),
				createInitialComputationalResource(form))) {

			try {
				UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), form
						.getNotebookName());

				ComputationalBase<?> dto = RequestBuilder.newComputationalCreate(userInfo, instance, form);

				String uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK, userInfo
						.getAccessToken(), dto, String.class);
				RequestId.put(userInfo.getName(), uuid);
				return true;
			} catch (RuntimeException e) {
				try {
					updateComputationalStatus(userInfo.getName(), form.getNotebookName(), form.getName(), FAILED);
				} catch (DlabException d) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, form.getName(), userInfo.getName(), d);
				}
				throw e;
			}
		} else {
			log.debug("Computational with name {} is already existing for user {}", form.getName(), userInfo.getName
					());
			return false;
		}
	}

	@Override
	public void terminateComputationalEnvironment(UserInfo userInfo, String exploratoryName, String
			computationalName) {
		try {

			updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, TERMINATING);

			String exploratoryId = exploratoryDAO.fetchExploratoryId(userInfo.getName(), exploratoryName);
			UserComputationalResource computationalResource = computationalDAO.fetchComputationalFields(userInfo
					.getName(), exploratoryName, computationalName);

			ComputationalTerminateDTO dto = RequestBuilder.newComputationalTerminate(userInfo, exploratoryName,
					exploratoryId, computationalName, computationalResource.getComputationalId(),
					DataEngineType.fromDockerImageName(computationalResource.getImageName()));

			String uuid = provisioningService.post(getTerminateUrl(computationalResource), userInfo.getAccessToken(),
					dto, String.class);
			RequestId.put(userInfo.getName(), uuid);
		} catch (RuntimeException re) {

			try {
				updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, FAILED);
			} catch (DlabException e) {
				log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, computationalName, userInfo.getName(), e);
			}

			throw re;
		}
	}

	@Override
	public boolean createDataEngineService(UserInfo userInfo, ComputationalCreateFormDTO formDTO,
										   UserComputationalResource computationalResource) {

		boolean isAdded = computationalDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(),
				computationalResource);

		if (isAdded) {
			try {
				UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO
						.getNotebookName());
				String uuid = provisioningService.post(COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, userInfo.getAccessToken(),
						RequestBuilder.newComputationalCreate(userInfo, instance, formDTO), String.class);
				RequestId.put(userInfo.getName(), uuid);
				return true;
			} catch (Exception t) {
				try {
					updateComputationalStatus(userInfo.getName(), formDTO.getNotebookName(), formDTO.getName(),
							FAILED);
				} catch (DlabException e) {
					log.error(COULD_NOT_UPDATE_THE_STATUS_MSG_FORMAT, formDTO.getName(), userInfo.getName(), e);
				}
				throw new DlabException("Could not send request for creation the computational resource " + formDTO
						.getName() + ": " + t.getLocalizedMessage(), t);
			}
		} else {
			log.debug("Used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
			return false;
		}
	}

	private String getTerminateUrl(UserComputationalResource computationalResource) {

		if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
				== DataEngineType.SPARK_STANDALONE) {

			return ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK;
		} else if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
				== DataEngineType.CLOUD_SERVICE) {

			return ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC;
		} else {
			throw new IllegalArgumentException("Unknown docker image for " + computationalResource);
		}
	}

	/**
	 * Validates if input form is correct
	 *
	 * @param form user input form
	 * @throws IllegalArgumentException if user typed wrong arguments
	 */

	private void validateForm(SparkStandaloneClusterCreateForm form) {

		int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

		if (instanceCount < configuration.getMinSparkInstanceCount()
				|| instanceCount > configuration.getMaxSparkInstanceCount()) {
			throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
					configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
		}

		if (DataEngineType.fromDockerImageName(form.getImage()) != DataEngineType.SPARK_STANDALONE) {
			throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
		}
	}

	/**
	 * Updates the status of computational resource in database.
	 *
	 * @param user              user name.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @param status            status
	 */
	private void updateComputationalStatus(String user, String exploratoryName, String computationalName,
										   UserInstanceStatus status) {
		ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
				.withUser(user)
				.withExploratoryName(exploratoryName)
				.withComputationalName(computationalName)
				.withStatus(status);

		computationalDAO.updateComputationalStatus(computationalStatus);
	}

	private SparkStandaloneClusterResource createInitialComputationalResource(SparkStandaloneClusterCreateForm form) {

		return SparkStandaloneClusterResource.builder()
				.computationalName(form.getName())
				.imageName(form.getImage())
				.templateName(form.getTemplateName())
				.status(CREATING.toString())
				.dataEngineInstanceCount(form.getDataEngineInstanceCount())
				.dataEngineMaster(form.getDataEngineMaster())
				.dataEngineSlave(form.getDataEngineSlave())
				.build();
	}

}
