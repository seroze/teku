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

package tech.pegasys.teku.beaconrestapi.handlers.v1.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static tech.pegasys.teku.datastructures.util.BeaconStateUtil.compute_start_slot_at_epoch;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.teku.api.response.v1.validator.GetProposerDutiesResponse;
import tech.pegasys.teku.api.response.v1.validator.ProposerDuty;
import tech.pegasys.teku.api.schema.BLSPubKey;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class GetProposerDutiesTest extends AbstractValidatorApiTest {
  @BeforeEach
  public void setup() {
    handler =
        new GetProposerDuties(
            chainDataProvider, syncDataProvider, validatorDataProvider, jsonProvider);
  }

  @Test
  public void shouldGetProposerDuties() throws Exception {
    when(validatorDataProvider.isStoreAvailable()).thenReturn(true);
    when(syncService.isSyncActive()).thenReturn(false);
    when(chainDataProvider.getCurrentEpoch()).thenReturn(UInt64.valueOf(100));
    when(context.pathParamMap()).thenReturn(Map.of("epoch", "100"));

    List<ProposerDuty> duties =
        List.of(getProposerDuty(2, compute_start_slot_at_epoch(UInt64.valueOf(100))));
    when(validatorDataProvider.getProposerDuties(eq(UInt64.valueOf(100))))
        .thenReturn(SafeFuture.completedFuture(Optional.of(duties)));

    handler.handle(context);
    GetProposerDutiesResponse response = getResponseFromFuture(GetProposerDutiesResponse.class);
    assertThat(response.data).isEqualTo(duties);
  }

  private ProposerDuty getProposerDuty(final int validatorIndex, final UInt64 slot) {
    return new ProposerDuty(new BLSPubKey(BLSPublicKey.random(1)), validatorIndex, slot);
  }
}
