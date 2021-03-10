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

package tech.pegasys.teku.api.schema;

import static tech.pegasys.teku.api.schema.SchemaConstants.DESCRIPTION_BYTES32;
import static tech.pegasys.teku.api.schema.SchemaConstants.DESCRIPTION_BYTES96;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.spec.SpecVersion;
import tech.pegasys.teku.spec.constants.SpecConstants;
import tech.pegasys.teku.ssz.SSZTypes.SSZList;

public class BeaconBlockBody {
  @Schema(type = "string", format = "byte", description = DESCRIPTION_BYTES96)
  public final BLSSignature randao_reveal;

  public final Eth1Data eth1_data;

  @Schema(type = "string", format = "byte", description = DESCRIPTION_BYTES32)
  public final Bytes32 graffiti;

  public final List<ProposerSlashing> proposer_slashings;
  public final List<AttesterSlashing> attester_slashings;
  public final List<Attestation> attestations;
  public final List<Deposit> deposits;
  public final List<SignedVoluntaryExit> voluntary_exits;

  @JsonCreator
  public BeaconBlockBody(
      @JsonProperty("randao_reveal") final BLSSignature randao_reveal,
      @JsonProperty("eth1_data") final Eth1Data eth1_data,
      @JsonProperty("graffiti") final Bytes32 graffiti,
      @JsonProperty("proposer_slashings") final List<ProposerSlashing> proposer_slashings,
      @JsonProperty("attester_slashings") final List<AttesterSlashing> attester_slashings,
      @JsonProperty("attestations") final List<Attestation> attestations,
      @JsonProperty("deposits") final List<Deposit> deposits,
      @JsonProperty("voluntary_exits") final List<SignedVoluntaryExit> voluntary_exits) {
    this.randao_reveal = randao_reveal;
    this.eth1_data = eth1_data;
    this.graffiti = graffiti;
    this.proposer_slashings = proposer_slashings;
    this.attester_slashings = attester_slashings;
    this.attestations = attestations;
    this.deposits = deposits;
    this.voluntary_exits = voluntary_exits;
  }

  public BeaconBlockBody(
      tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody body) {
    this.randao_reveal = new BLSSignature(body.getRandao_reveal().toSSZBytes());
    this.eth1_data = new Eth1Data(body.getEth1_data());
    this.graffiti = body.getGraffiti();
    this.proposer_slashings =
        body.getProposer_slashings().stream()
            .map(ProposerSlashing::new)
            .collect(Collectors.toList());
    this.attester_slashings =
        body.getAttester_slashings().stream()
            .map(AttesterSlashing::new)
            .collect(Collectors.toList());
    this.attestations =
        body.getAttestations().stream().map(Attestation::new).collect(Collectors.toList());
    this.deposits = body.getDeposits().stream().map(Deposit::new).collect(Collectors.toList());
    this.voluntary_exits =
        body.getVoluntary_exits().stream()
            .map(SignedVoluntaryExit::new)
            .collect(Collectors.toList());
  }

  public tech.pegasys.teku.spec.datastructures.blocks.blockbody.BeaconBlockBody
      asInternalBeaconBlockBody(final SpecVersion spec) {
    final SpecConstants constants = spec.getConstants();
    return spec.getSchemaDefinitions()
        .getBeaconBlockBodySchema()
        .createBlockBody(
            randao_reveal.asInternalBLSSignature(),
            new tech.pegasys.teku.spec.datastructures.blocks.Eth1Data(
                eth1_data.deposit_root, eth1_data.deposit_count, eth1_data.block_hash),
            graffiti,
            SSZList.createMutable(
                proposer_slashings.stream().map(ProposerSlashing::asInternalProposerSlashing),
                constants.getMaxProposerSlashings(),
                tech.pegasys.teku.spec.datastructures.operations.ProposerSlashing.class),
            SSZList.createMutable(
                attester_slashings.stream().map(AttesterSlashing::asInternalAttesterSlashing),
                constants.getMaxAttesterSlashings(),
                tech.pegasys.teku.spec.datastructures.operations.AttesterSlashing.class),
            SSZList.createMutable(
                attestations.stream().map(Attestation::asInternalAttestation),
                constants.getMaxAttestations(),
                tech.pegasys.teku.spec.datastructures.operations.Attestation.class),
            SSZList.createMutable(
                deposits.stream().map(Deposit::asInternalDeposit),
                constants.getMaxDeposits(),
                tech.pegasys.teku.spec.datastructures.operations.Deposit.class),
            SSZList.createMutable(
                voluntary_exits.stream().map(SignedVoluntaryExit::asInternalSignedVoluntaryExit),
                constants.getMaxVoluntaryExits(),
                tech.pegasys.teku.spec.datastructures.operations.SignedVoluntaryExit.class));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BeaconBlockBody)) return false;
    BeaconBlockBody that = (BeaconBlockBody) o;
    return Objects.equals(randao_reveal, that.randao_reveal)
        && Objects.equals(eth1_data, that.eth1_data)
        && Objects.equals(graffiti, that.graffiti)
        && Objects.equals(proposer_slashings, that.proposer_slashings)
        && Objects.equals(attester_slashings, that.attester_slashings)
        && Objects.equals(attestations, that.attestations)
        && Objects.equals(deposits, that.deposits)
        && Objects.equals(voluntary_exits, that.voluntary_exits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        randao_reveal,
        eth1_data,
        graffiti,
        proposer_slashings,
        attester_slashings,
        attestations,
        deposits,
        voluntary_exits);
  }
}
