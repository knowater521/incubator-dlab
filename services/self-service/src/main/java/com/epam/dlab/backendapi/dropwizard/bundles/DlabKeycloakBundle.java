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

package com.epam.dlab.backendapi.dropwizard.bundles;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.auth.KeycloakAuthenticator;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.google.inject.Inject;
import de.ahus1.keycloak.dropwizard.KeycloakBundle;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.setup.Environment;

import java.security.Principal;

public class DlabKeycloakBundle extends KeycloakBundle<SelfServiceApplicationConfiguration> {

	@Inject
	private KeycloakAuthenticator authenticator;

	@Override
	protected KeycloakConfiguration getKeycloakConfiguration(SelfServiceApplicationConfiguration configuration) {
		return configuration.getKeycloakConfiguration();
	}

	@Override
	protected Class<? extends Principal> getUserClass() {
		return UserInfo.class;
	}

	@Override
	protected Authorizer createAuthorizer() {
		return new SelfServiceSecurityAuthorizer();
	}

	@Override
	protected Authenticator createAuthenticator(KeycloakConfiguration configuration) {
		return new KeycloakAuthenticator(configuration);
	}
}
