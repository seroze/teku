/*
 * Copyright 2019 ConsenSys AG.
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

package tech.pegasys.artemis.statetransition;

import static tech.pegasys.artemis.datastructures.Constants.EPOCH_LENGTH;

import com.google.common.primitives.UnsignedLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.pegasys.artemis.datastructures.blocks.BeaconBlock;
import tech.pegasys.artemis.statetransition.util.BlockProcessorUtil;
import tech.pegasys.artemis.statetransition.util.EpochProcessorUtil;
import tech.pegasys.artemis.statetransition.util.SlotProcessorUtil;

public class StateTransition {

  private static final Logger LOG = LogManager.getLogger();

  public StateTransition() {}

  public void initiate(BeaconState state, BeaconBlock block) {

    try {
      // per-slot processing
      slotProcessor(state, block);
      // per-block processing
      if (block != null) {
        blockProcessor(state, block);
      }
      // per-epoch processing
      if (state
          .getSlot()
          .plus(UnsignedLong.ONE)
          .mod(UnsignedLong.valueOf(EPOCH_LENGTH))
          .equals(UnsignedLong.ZERO)) {
        epochProcessor(state);
      }
    } catch (Exception e) {
      LOG.warn(e.toString());
    }
  }

  protected void slotProcessor(BeaconState state, BeaconBlock block) throws Exception {
    state.incrementSlot();
    LOG.info("Processing new slot: " + state.getSlot());
    // Slots the proposer has skipped (i.e. layers of RANDAO expected)
    // should be in Validator.randao_skips
    SlotProcessorUtil.updateLatestRandaoMixes(state);
    SlotProcessorUtil.updateRecentBlockHashes(state, block);
  }

  protected void blockProcessor(BeaconState state, BeaconBlock block) {
    LOG.info("Processing new block in slot: " + block.getSlot());
    // block header
    BlockProcessorUtil.verify_signature(state, block);
    // verifyAndUpdateRandao(state, block);

    // block body operations
    // processAttestations(state, block);
  }

  protected void epochProcessor(BeaconState state) throws Exception {
    LOG.info("Processing new epoch in slot: " + state.getSlot());
    EpochProcessorUtil.updateJustification(state);
    EpochProcessorUtil.updateCrosslinks(state);
    EpochProcessorUtil.finalBookKeeping(state);
  }
}
