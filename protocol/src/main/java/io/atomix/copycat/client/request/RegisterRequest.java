/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.copycat.client.request;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.SerializeWith;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;

import java.util.Objects;
import java.util.UUID;

/**
 * Protocol register client request.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@SerializeWith(id=199)
public class RegisterRequest extends AbstractRequest<RegisterRequest> {

  /**
   * Returns a new register client request builder.
   *
   * @return A new register client request builder.
   */
  public static Builder builder() {
    return new Builder(new RegisterRequest());
  }

  /**
   * Returns a register client request builder for an existing request.
   *
   * @param request The request to build.
   * @return The register client request builder.
   * @throws NullPointerException if {@code request} is null
   */
  public static Builder builder(RegisterRequest request) {
    return new Builder(request);
  }

  private UUID client;

  /**
   * Returns the client ID.
   *
   * @return The client ID.
   */
  public UUID client() {
    return client;
  }

  @Override
  public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
    serializer.writeObject(client, buffer);
  }

  @Override
  public void readObject(BufferInput<?> buffer, Serializer serializer) {
    client = serializer.readObject(buffer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), client);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof RegisterRequest) {
      RegisterRequest request = (RegisterRequest) object;
      return request.client.equals(client);
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s", getClass().getSimpleName());
  }

  /**
   * Register client request builder.
   */
  public static class Builder extends AbstractRequest.Builder<Builder, RegisterRequest> {
    protected Builder(RegisterRequest request) {
      super(request);
    }

    /**
     * Sets the client ID.
     *
     * @param client The client ID.
     * @return The request builder.
     * @throws NullPointerException if {@code client} is null
     */
    public Builder withClient(UUID client) {
      request.client = Assert.notNull(client, "client");
      return this;
    }

    /**
     * @throws IllegalStateException if client is null
     */
    @Override
    public RegisterRequest build() {
      super.build();
      Assert.stateNot(request.client == null, "client");
      return request;
    }
  }

}
