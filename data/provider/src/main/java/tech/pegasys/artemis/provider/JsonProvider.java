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

package tech.pegasys.artemis.provider;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import tech.pegasys.artemis.datastructures.state.BeaconState;
import tech.pegasys.artemis.datastructures.state.BeaconStateWithCache;
import tech.pegasys.artemis.util.SSZTypes.Bitvector;
import tech.pegasys.artemis.util.SSZTypes.Bytes4;
import tech.pegasys.artemis.util.bls.BLSPublicKey;

public class JsonProvider {

  public static String printBeaconState(BeaconState state) {

    ExclusionStrategy strategy =
        new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(FieldAttributes field) {
            if (field.getDeclaringClass() == BeaconStateWithCache.class) {
              return true;
            }

            return false;
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
            return false;
          }
        };

    GsonBuilder builder = new GsonBuilder().addSerializationExclusionStrategy(strategy);

    builder.registerTypeAdapter(
        UnsignedLong.class,
        (JsonSerializer<UnsignedLong>)
            (src, typeOfSrc, context) -> new JsonPrimitive(src.bigIntegerValue()));
    builder.registerTypeAdapter(
        UnsignedLong.class,
        (JsonDeserializer<UnsignedLong>)
            (json, typeOfT, context) -> UnsignedLong.valueOf(json.getAsLong()));

    builder.registerTypeAdapter(
        Bytes.class,
        (JsonSerializer<Bytes>)
            (src, typeOfSrc, context) -> new JsonPrimitive(src.toHexString().toLowerCase()));
    builder.registerTypeAdapter(
        Bytes.class,
        (JsonDeserializer<Bytes>)
            (json, typeOfT, context) -> Bytes.fromHexString(json.getAsString()));

    builder.registerTypeAdapter(
        Bytes32.class,
        (JsonSerializer<Bytes32>)
            (src, typeOfSrc, context) -> new JsonPrimitive(src.toHexString().toLowerCase()));
    builder.registerTypeAdapter(
        Bytes32.class,
        (JsonDeserializer<Bytes32>)
            (json, typeOfT, context) -> Bytes32.fromHexString(json.getAsString()));

    builder.registerTypeAdapter(
        Bytes4.class,
        (JsonSerializer<Bytes4>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(src.getWrappedBytes().toHexString().toLowerCase()));
    builder.registerTypeAdapter(
        Bytes32.class,
        (JsonDeserializer<Bytes4>)
            (json, typeOfT, context) -> new Bytes4(Bytes.fromHexString(json.getAsString())));

    builder.registerTypeAdapter(
        BLSPublicKey.class,
        (JsonSerializer<BLSPublicKey>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toBytes().toHexString().toLowerCase()));
    builder.registerTypeAdapter(
        BLSPublicKey.class,
        (JsonDeserializer<BLSPublicKey>)
            (json, typeOfT, context) ->
                BLSPublicKey.fromBytes(Bytes32.fromHexString(json.getAsString())));

    builder.registerTypeAdapter(
        Bitvector.class,
        (JsonSerializer<Bitvector>)
            (src, typeOfSrc, context) ->
                new JsonPrimitive(Bytes.wrap(src.getByteArray()).toHexString().toLowerCase()));
    builder.registerTypeAdapter(
        Bitvector.class,
        (JsonDeserializer<Bitvector>)
            (json, typeOfT, context) -> {
              Bytes bytes = Bytes.fromHexString(json.getAsString());
              int length = bytes.bitLength();
              return Bitvector.fromBytes(bytes, length);
            });

    Gson gson = builder.create();
    return gson.toJson(state);
  }
}
