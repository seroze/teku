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

package tech.pegasys.teku.reference.phase0.epoch_processing;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.pegasys.teku.reference.phase0.TestDataUtils.loadStateFromSsz;

import com.google.common.collect.ImmutableMap;
import tech.pegasys.teku.ethtests.finder.TestDefinition;
import tech.pegasys.teku.reference.phase0.TestExecutor;
import tech.pegasys.teku.reference.phase0.epoch_processing.EpochProcessingExecutor.Operation;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.BeaconState;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.EpochProcessor;
import tech.pegasys.teku.spec.logic.common.statetransition.epoch.status.ValidatorStatusFactory;

public class EpochProcessingTestExecutor implements TestExecutor {

  public static ImmutableMap<String, TestExecutor> EPOCH_PROCESSING_TEST_TYPES =
      ImmutableMap.<String, TestExecutor>builder()
          .put(
              "epoch_processing/slashings",
              new EpochProcessingTestExecutor(Operation.PROCESS_SLASHINGS))
          .put(
              "epoch_processing/registry_updates",
              new EpochProcessingTestExecutor(Operation.PROCESS_REGISTRY_UPDATES))
          .put(
              "epoch_processing/rewards_and_penalties",
              new EpochProcessingTestExecutor(Operation.PROCESS_REWARDS_AND_PENALTIES))
          .put(
              "epoch_processing/justification_and_finalization",
              new EpochProcessingTestExecutor(Operation.PROCESS_JUSTIFICATION_AND_FINALIZATION))
          .put(
              "epoch_processing/effective_balance_updates",
              new EpochProcessingTestExecutor(Operation.PROCESS_EFFECTIVE_BALANCE_UPDATES))
          .put(
              "epoch_processing/eth1_data_reset",
              new EpochProcessingTestExecutor(Operation.PROCESS_ETH1_DATA_RESET))
          .put(
              "epoch_processing/participation_record_updates",
              new EpochProcessingTestExecutor(Operation.PROCESS_PARTICIPATION_RECORD_UPDATES))
          .put(
              "epoch_processing/randao_mixes_reset",
              new EpochProcessingTestExecutor(Operation.PROCESS_RANDAO_MIXES_RESET))
          .put(
              "epoch_processing/historical_roots_update",
              new EpochProcessingTestExecutor(Operation.PROCESS_HISTORICAL_ROOTS_UPDATE))
          .put(
              "epoch_processing/slashings_reset",
              new EpochProcessingTestExecutor(Operation.PROCESS_SLASHINGS_RESET))
          .build();

  private final Operation operation;

  public EpochProcessingTestExecutor(final Operation operation) {
    this.operation = operation;
  }

  @Override
  public void runTest(final TestDefinition testDefinition) throws Exception {
    final BeaconState preState = loadStateFromSsz(testDefinition, "pre.ssz_snappy");
    final BeaconState expectedPostState = loadStateFromSsz(testDefinition, "post.ssz_snappy");

    final SpecVersion genesisSpec = testDefinition.getSpec().getGenesisSpec();
    final EpochProcessor epochProcessor = genesisSpec.getEpochProcessor();
    final ValidatorStatusFactory validatorStatusFactory = genesisSpec.getValidatorStatusFactory();
    final EpochProcessingExecutor processor =
        new DefaultEpochProcessingExecutor(epochProcessor, validatorStatusFactory);
    final BeaconState result =
        preState.updated(state -> processor.executeOperation(operation, state));
    assertThat(result).isEqualTo(expectedPostState);
  }
}
