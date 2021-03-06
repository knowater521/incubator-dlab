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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

import { ApplicationServiceFacade } from './applicationServiceFacade.service';
import { ErrorUtils } from '../util';

@Injectable()
export class BackupService {
  inProgress: boolean;

  constructor(private applicationServiceFacade: ApplicationServiceFacade) {}

  set creatingBackup(data) {
    this.inProgress = data.status !== 'CREATED' && data.status !== 'FAILED';
  }

  public createBackup(data): Observable<any> {
    return this.applicationServiceFacade
      .buildCreateBackupRequest(data)
      .pipe(
        map(response => response),
        catchError(ErrorUtils.handleServiceError));
  }

  public getBackupStatus(uuid): Observable<{}> {
    const body = `/${uuid}`;
    return this.applicationServiceFacade
      .buildGetBackupStatusRequest(body)
      .pipe(
        map(response => response),
        map(data => (this.creatingBackup = data)),
        catchError(ErrorUtils.handleServiceError));
  }
}
