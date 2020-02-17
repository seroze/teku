/*
 * Copyright 2020 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package tech.pegasys.artemis.beaconrestapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.eventbus.EventBus;
import io.javalin.Javalin;
import io.javalin.core.JavalinServer;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.beaconrestapi.beaconhandlers.GenesisTimeHandler;
import tech.pegasys.artemis.beaconrestapi.beaconhandlers.VersionHandler;
import tech.pegasys.artemis.storage.ChainStorageClient;

class BeaconRestApiTest {
  private final ChainStorageClient storageClient =
      ChainStorageClient.memoryOnlyClient(new EventBus());
  private final JavalinServer mockServer = mock(JavalinServer.class);
  private final Javalin mockApp = mock(Javalin.class);
  private static final Integer THE_PORT = 12345;

  @Test
  public void RestApiShouldHaveServerPortSet() {
    when(mockApp.server()).thenReturn(mockServer);
    new BeaconRestApi(storageClient, null, null, THE_PORT, mockApp);

    verify(mockServer).setServerPort(THE_PORT);
  }

  @Test
  public void RestApiShouldHaveGenesisTimeEndpoint() throws Exception {
    when(mockApp.server()).thenReturn(mockServer);
    new BeaconRestApi(storageClient, null, null, THE_PORT, mockApp);

    verify(mockApp).get(eq(GenesisTimeHandler.ROUTE), any(GenesisTimeHandler.class));
  }

  @Test
  public void RestApiShouldHaveVersionEndpoint() throws Exception {
    when(mockApp.server()).thenReturn(mockServer);
    new BeaconRestApi(storageClient, null, null, THE_PORT, mockApp);

    verify(mockApp).get(eq(VersionHandler.ROUTE), any(VersionHandler.class));
  }
}
