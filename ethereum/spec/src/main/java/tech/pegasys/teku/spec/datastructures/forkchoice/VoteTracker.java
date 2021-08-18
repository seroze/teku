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

package tech.pegasys.teku.spec.datastructures.forkchoice;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.teku.infrastructure.unsigned.UInt64;

public class VoteTracker {

  public static final VoteTracker DEFAULT =
      new VoteTracker(Bytes32.ZERO, Bytes32.ZERO, UInt64.ZERO);

  private final Bytes32 currentRoot;
  private final Bytes32 nextRoot;
  private final UInt64 nextEpoch;

  public VoteTracker(final Bytes32 currentRoot, final Bytes32 nextRoot, final UInt64 nextEpoch) {
    this.currentRoot = currentRoot;
    this.nextRoot = nextRoot;
    this.nextEpoch = nextEpoch;
  }

  public Bytes32 getCurrentRoot() {
    return currentRoot;
  }

  public Bytes32 getNextRoot() {
    return nextRoot;
  }

  public UInt64 getNextEpoch() {
    return nextEpoch;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VoteTracker that = (VoteTracker) o;
    return Objects.equals(currentRoot, that.currentRoot)
        && Objects.equals(nextRoot, that.nextRoot)
        && Objects.equals(nextEpoch, that.nextEpoch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentRoot, nextRoot, nextEpoch);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("currentRoot", currentRoot)
        .add("nextRoot", nextRoot)
        .add("nextEpoch", nextEpoch)
        .toString();
  }
}
