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

package tech.pegasys.artemis.datastructures.state;

import com.google.common.primitives.UnsignedLong;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.SSZ;

import java.util.Objects;

public class Fork {

  // TODO previous_version should be a bytes4 (this has serialization impacts)
  private Bytes previous_version;
  // TODO current_version should be a bytes4 (this has serialization impacts)
  private Bytes current_version;
  private UnsignedLong epoch;

  public Fork(Bytes previous_version, Bytes current_version, UnsignedLong epoch) {
    this.previous_version = previous_version;
    this.current_version = current_version;
    this.epoch = epoch;
  }

  public Fork(Fork fork) {
    this.previous_version = fork.getPrevious_version();
    this.current_version = fork.getCurrent_version();
    this.epoch = fork.getEpoch();
  }

  public static Fork fromBytes(Bytes bytes) {
    return SSZ.decode(
        bytes,
        reader ->
            new Fork(
                Bytes.wrap(reader.readBytes()),
                Bytes.wrap(reader.readBytes()),
                UnsignedLong.fromLongBits(reader.readUInt64())));
  }

  public Bytes toBytes() {
    return SSZ.encode(
        writer -> {
          writer.writeBytes(previous_version);
          writer.writeBytes(current_version);
          writer.writeUInt64(epoch.longValue());
        });
  }

  @Override
  public int hashCode() {
    return Objects.hash(previous_version, current_version, epoch);
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof Fork)) {
      return false;
    }

    Fork other = (Fork) obj;
    return Objects.equals(this.getPrevious_version(), other.getPrevious_version())
        && Objects.equals(this.getCurrent_version(), other.getCurrent_version())
        && Objects.equals(this.getEpoch(), other.getEpoch());
  }

  /** ******************* * GETTERS & SETTERS * * ******************* */
  public Bytes getPrevious_version() {
    return previous_version;
  }

  public void setPrevious_version(Bytes previous_version) {
    this.previous_version = previous_version;
  }

  public Bytes getCurrent_version() {
    return current_version;
  }

  public void setCurrent_version(Bytes current_version) {
    this.current_version = current_version;
  }

  public UnsignedLong getEpoch() {
    return epoch;
  }

  public void setEpoch(UnsignedLong epoch) {
    this.epoch = epoch;
  }
}
