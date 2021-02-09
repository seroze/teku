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

package tech.pegasys.teku.networking.eth2.rpc.core;

import java.util.Optional;
import tech.pegasys.teku.infrastructure.async.SafeFuture;

public interface ResponseStream<O> {
  default SafeFuture<O> expectSingleResponse() {
    return expectOptionalResponse()
        .thenApply(
            maybeResult ->
                maybeResult.orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No response received when single response expected")));
  }

  SafeFuture<Optional<O>> expectOptionalResponse();

  SafeFuture<Void> expectNoResponse();

  SafeFuture<Void> expectMultipleResponses(ResponseStreamListener<O> listener);
}
