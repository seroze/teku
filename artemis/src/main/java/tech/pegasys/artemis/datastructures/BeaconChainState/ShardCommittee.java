/*
 * Copyright 2018 ConsenSys AG.
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

package tech.pegasys.artemis.datastructures.BeaconChainState;

import tech.pegasys.artemis.util.uint.UInt64;

public class ShardCommittee {

  public UInt64 shard;
  public int[] committee;
  private UInt64 total_validator_count;

  public ShardCommittee(UInt64 shard, int[] committee, UInt64 total_validator_count) {
    this.shard = shard;
    this.committee = committee;
    this.total_validator_count = total_validator_count;
  }

}
