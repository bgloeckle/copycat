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
package io.atomix.copycat.client.response;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.SerializeWith;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;
import io.atomix.copycat.client.error.RaftError;

import java.util.Objects;

/**
 * Protocol publish response.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
@SerializeWith(id=196)
public class PublishResponse extends SessionResponse<PublishResponse> {

  /**
   * Returns a new publish response builder.
   *
   * @return A new publish response builder.
   */
  public static Builder builder() {
    return new Builder(new PublishResponse());
  }

  /**
   * Returns a publish response builder for an existing response.
   *
   * @param response The response to build.
   * @return The publish response builder.
   * @throws NullPointerException if {@code response} is null
   */
  public static Builder builder(PublishResponse response) {
    return new Builder(response);
  }

  private long version;

  /**
   * Returns the event version number.
   *
   * @return The event version number.
   */
  public long version() {
    return version;
  }

  @Override
  public void readObject(BufferInput<?> buffer, Serializer serializer) {
    status = Status.forId(buffer.readByte());
    if (status == Status.OK) {
      error = null;
    } else if (buffer.readBoolean()) {
      error = RaftError.forId(buffer.readByte());
    }
    version = buffer.readLong();
  }

  @Override
  public void writeObject(BufferOutput<?> buffer, Serializer serializer) {
    buffer.writeByte(status.id());
    if (status == Status.ERROR) {
      if (error != null) {
        buffer.writeBoolean(true).writeByte(error.id());
      } else {
        buffer.writeBoolean(false);
      }
    }
    buffer.writeLong(version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass(), status);
  }

  @Override
  public boolean equals(Object object) {
    if (object instanceof PublishResponse) {
      PublishResponse response = (PublishResponse) object;
      return response.status == status
        && response.version == version;
    }
    return false;
  }

  @Override
  public String toString() {
    return String.format("%s[status=%s, version=%d]", getClass().getSimpleName(), status, version);
  }

  /**
   * Publish response builder.
   */
  public static class Builder extends SessionResponse.Builder<Builder, PublishResponse> {
    protected Builder(PublishResponse response) {
      super(response);
    }

    /**
     * Sets the event version number.
     *
     * @param version The event version number.
     * @return The response builder.
     * @throws IllegalArgumentException if {@code version} is less than {@code 1}
     */
    public Builder withVersion(long version) {
      response.version = Assert.argNot(version, version < 0, "version cannot be less than 0");
      return this;
    }

    /**
     * @throws IllegalStateException if sequence is less than 1
     */
    @Override
    public PublishResponse build() {
      super.build();
      Assert.stateNot(response.version < 0, "version cannot be less than 0");
      return response;
    }
  }

}
