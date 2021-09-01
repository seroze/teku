/*
 * Copyright 2021 ConsenSys AG.
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

package tech.pegasys.teku.validator.client.duties.attestations;

import java.util.List;
import java.util.stream.Stream;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.spec.datastructures.operations.Attestation;
import tech.pegasys.teku.validator.api.ValidatorApiChannel;
import tech.pegasys.teku.validator.client.duties.DutyResult;
import tech.pegasys.teku.validator.client.duties.ProductionResult;

public class BatchAttestationSendingStrategy implements AttestationSendingStrategy {
  private final ValidatorApiChannel validatorApiChannel;

  public BatchAttestationSendingStrategy(final ValidatorApiChannel validatorApiChannel) {
    this.validatorApiChannel = validatorApiChannel;
  }

  @Override
  public SafeFuture<DutyResult> sendAttestations(
      final Stream<SafeFuture<ProductionResult<Attestation>>> attestations) {
    return SafeFuture.collectAll(attestations).thenCompose(this::sendAttestationsAsBatch);
  }

  private SafeFuture<DutyResult> sendAttestationsAsBatch(
      final List<ProductionResult<Attestation>> results) {
    return ProductionResult.send(results, validatorApiChannel::sendSignedAttestations);
  }
}
