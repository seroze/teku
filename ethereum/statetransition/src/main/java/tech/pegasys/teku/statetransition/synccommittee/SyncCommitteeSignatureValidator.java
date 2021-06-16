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

package tech.pegasys.teku.statetransition.synccommittee;

import static java.util.stream.Collectors.toList;
import static tech.pegasys.teku.infrastructure.time.TimeUtilities.secondsToMillis;
import static tech.pegasys.teku.statetransition.validation.InternalValidationResult.ACCEPT;
import static tech.pegasys.teku.statetransition.validation.InternalValidationResult.IGNORE;
import static tech.pegasys.teku.statetransition.validation.InternalValidationResult.REJECT;
import static tech.pegasys.teku.util.config.Constants.MAXIMUM_GOSSIP_CLOCK_DISPARITY;
import static tech.pegasys.teku.util.config.Constants.VALID_SYNC_COMMITTEE_SIGNATURE_SET_SIZE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.bls.BLS;
import tech.pegasys.teku.bls.BLSPublicKey;
import tech.pegasys.teku.infrastructure.async.SafeFuture;
import tech.pegasys.teku.infrastructure.collections.LimitedSet;
import tech.pegasys.teku.infrastructure.time.TimeProvider;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;
import tech.pegasys.teku.spec.Spec;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.SyncCommitteeSignature;
import tech.pegasys.teku.spec.datastructures.operations.versions.altair.ValidateableSyncCommitteeSignature;
import tech.pegasys.teku.spec.datastructures.state.beaconstate.versions.altair.BeaconStateAltair;
import tech.pegasys.teku.spec.datastructures.util.SyncSubcommitteeAssignments;
import tech.pegasys.teku.spec.logic.common.util.SyncCommitteeUtil;
import tech.pegasys.teku.statetransition.validation.InternalValidationResult;
import tech.pegasys.teku.storage.client.RecentChainData;

public class SyncCommitteeSignatureValidator {
  private static final Logger LOG = LogManager.getLogger();
  private final Set<UniquenessKey> seenIndices =
      LimitedSet.create(VALID_SYNC_COMMITTEE_SIGNATURE_SET_SIZE);
  private final Spec spec;
  private final RecentChainData recentChainData;
  private final SyncCommitteeStateUtils syncCommitteeStateUtils;
  private final TimeProvider timeProvider;

  public SyncCommitteeSignatureValidator(
      final Spec spec,
      final RecentChainData recentChainData,
      final SyncCommitteeStateUtils syncCommitteeStateUtils,
      final TimeProvider timeProvider) {
    this.spec = spec;
    this.recentChainData = recentChainData;
    this.syncCommitteeStateUtils = syncCommitteeStateUtils;
    this.timeProvider = timeProvider;
  }

  public SafeFuture<InternalValidationResult> validate(
      final ValidateableSyncCommitteeSignature validateableSignature) {

    final SyncCommitteeSignature signature = validateableSignature.getSignature();

    final Optional<SyncCommitteeUtil> maybeSyncCommitteeUtil =
        spec.getSyncCommitteeUtil(signature.getSlot());
    if (maybeSyncCommitteeUtil.isEmpty()) {
      LOG.trace(
          "Rejecting sync committee signature because the fork active at slot {} does not support sync committees",
          signature.getSlot());
      return SafeFuture.completedFuture(REJECT);
    }
    final SyncCommitteeUtil syncCommitteeUtil = maybeSyncCommitteeUtil.get();

    // [IGNORE] The signature's slot is for the current slot(with a MAXIMUM_GOSSIP_CLOCK_DISPARITY
    // allowance),
    // i.e. sync_committee_signature.slot == current_slot.
    if (!isSignatureForCurrentSlot(signature.getSlot())) {
      LOG.trace("Ignoring sync committee signature because it is not from the current slot");
      return SafeFuture.completedFuture(IGNORE);
    }

    // [IGNORE] There has been no other valid sync committee signature for the declared slot for the
    // validator referenced by sync_committee_signature.validator_index.
    final Optional<UniquenessKey> uniquenessKey;
    if (validateableSignature.getReceivedSubnetId().isPresent()) {
      final UniquenessKey key =
          getUniquenessKey(signature, validateableSignature.getReceivedSubnetId().getAsInt());
      if (seenIndices.contains(key)) {
        return SafeFuture.completedFuture(IGNORE);
      }
      uniquenessKey = Optional.of(key);
    } else {
      uniquenessKey = Optional.empty();
    }

    // [IGNORE] The block being signed over (sync_committee_signature.beacon_block_root) has been
    // seen (via both gossip and non-gossip sources).
    if (!recentChainData.containsBlock(signature.getBeaconBlockRoot())) {
      LOG.trace("Ignoring sync committee signature because beacon block is not known");
      return SafeFuture.completedFuture(IGNORE);
    }

    return syncCommitteeStateUtils
        .getStateForSyncCommittee(signature.getSlot(), signature.getBeaconBlockRoot())
        .thenApply(
            maybeState -> {
              if (maybeState.isEmpty()) {
                LOG.trace(
                    "Ignoring sync committee signature because state is not available or not from Altair fork");
                return IGNORE;
              }
              final BeaconStateAltair state = maybeState.get();
              return validateWithState(
                  validateableSignature, signature, syncCommitteeUtil, state, uniquenessKey);
            });
  }

  boolean isSignatureForCurrentSlot(final UInt64 slot) {
    if (recentChainData.getCurrentSlot().isEmpty()) {
      return false;
    }
    final UInt64 slotMillis = secondsToMillis(spec.atSlot(slot).getConfig().getSecondsPerSlot());
    final UInt64 slotStartTimeMillis =
        secondsToMillis(spec.getSlotStartTime(slot, recentChainData.getGenesisTime()));
    final UInt64 slotEndTimeMillis = slotStartTimeMillis.plus(slotMillis);
    final UInt64 currentTimeMillis = timeProvider.getTimeInMillis();

    return currentTimeMillis.isGreaterThanOrEqualTo(
            slotStartTimeMillis.minusMinZero(MAXIMUM_GOSSIP_CLOCK_DISPARITY))
        && currentTimeMillis.isLessThanOrEqualTo(
            slotEndTimeMillis.plus(MAXIMUM_GOSSIP_CLOCK_DISPARITY));
  }

  private InternalValidationResult validateWithState(
      final ValidateableSyncCommitteeSignature validateableSignature,
      final SyncCommitteeSignature signature,
      final SyncCommitteeUtil syncCommitteeUtil,
      final BeaconStateAltair state,
      final Optional<UniquenessKey> maybeUniquenessKey) {
    final UInt64 signatureEpoch = spec.computeEpochAtSlot(signature.getSlot());

    // Always calculate the applicable subcommittees to ensure they are cached and can be used to
    // send the gossip.
    final SyncSubcommitteeAssignments assignedSubcommittees =
        validateableSignature.calculateAssignments(spec, state);

    // [REJECT] The validator producing this sync_committee_signature is in the current sync
    // committee, i.e. state.validators[sync_committee_signature.validator_index].pubkey in
    // state.current_sync_committee.pubkeys.
    if (assignedSubcommittees.isEmpty()) {
      LOG.trace(
          "Rejecting sync committee signature because validator is not in the sync committee");
      return REJECT;
    }

    // For signatures received via gossip, it has to be unique based on the subnet it was on
    // For locally produced signatures we should accept it if it hasn't been seen on any subnet
    final List<UniquenessKey> uniquenessKeys =
        maybeUniquenessKey
            .map(List::of)
            .orElseGet(
                () ->
                    assignedSubcommittees.getAssignedSubcommittees().stream()
                        .map(subnetId -> getUniquenessKey(signature, subnetId))
                        .collect(toList()));

    // [IGNORE] There has been no other valid sync committee signature for the declared slot for the
    // validator referenced by sync_committee_signature.validator_index.
    if (seenIndices.containsAll(uniquenessKeys)) {
      return IGNORE;
    }

    // [REJECT] The subnet_id is correct, i.e. subnet_id in
    // compute_subnets_for_sync_committee(state, sync_committee_signature.validator_index).
    if (validateableSignature.getReceivedSubnetId().isPresent()
        && !assignedSubcommittees
            .getAssignedSubcommittees()
            .contains(validateableSignature.getReceivedSubnetId().getAsInt())) {
      LOG.trace("Rejecting sync committee signature because subnet id is incorrect");
      return REJECT;
    }

    final Optional<BLSPublicKey> maybeValidatorPublicKey =
        spec.getValidatorPubKey(state, signature.getValidatorIndex());
    if (maybeValidatorPublicKey.isEmpty()) {
      LOG.trace("Rejecting sync committee signature because the validator index is unknown");
      return REJECT;
    }

    // [REJECT] The signature is valid for the message beacon_block_root for the validator
    // referenced by validator_index.
    final Bytes32 signingRoot =
        syncCommitteeUtil.getSyncCommitteeSignatureSigningRoot(
            signature.getBeaconBlockRoot(), signatureEpoch, state.getForkInfo());
    if (!BLS.verify(maybeValidatorPublicKey.get(), signingRoot, signature.getSignature())) {
      LOG.trace("Rejecting sync committee signature because the signature is invalid");
      return REJECT;
    }

    if (!seenIndices.addAll(uniquenessKeys)) {
      LOG.trace("Ignoring sync committee signature as a duplicate was processed during validation");
      return IGNORE;
    }
    return ACCEPT;
  }

  private UniquenessKey getUniquenessKey(
      final SyncCommitteeSignature signature, final int subnetId) {
    return new UniquenessKey(signature.getValidatorIndex(), signature.getSlot(), subnetId);
  }

  private static class UniquenessKey {
    private final UInt64 validatorIndex;
    private final UInt64 slot;
    private final int subnetId;

    private UniquenessKey(final UInt64 validatorIndex, final UInt64 slot, final int subnetId) {
      this.validatorIndex = validatorIndex;
      this.slot = slot;
      this.subnetId = subnetId;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final UniquenessKey that = (UniquenessKey) o;
      return subnetId == that.subnetId
          && Objects.equals(validatorIndex, that.validatorIndex)
          && Objects.equals(slot, that.slot);
    }

    @Override
    public int hashCode() {
      return Objects.hash(validatorIndex, slot, subnetId);
    }
  }
}
