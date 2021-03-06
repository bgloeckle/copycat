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
package io.atomix.copycat.server.storage;

import io.atomix.catalyst.buffer.PooledDirectAllocator;
import io.atomix.catalyst.serializer.Serializer;
import io.atomix.catalyst.util.Assert;

import java.io.File;
import java.time.Duration;

/**
 * Immutable log configuration and {@link Log} factory.
 * <p>
 * This class provides a factory for {@link Log} objects. {@code Storage} objects are immutable and
 * can be created only via the {@link Storage.Builder}. To create a new
 * {@code Storage.Builder}, use the static {@link #builder()} factory method:
 * <pre>
 *   {@code
 *     Storage storage = Storage.builder()
 *       .withDirectory(new File("logs"))
 *       .withPersistenceLevel(PersistenceLevel.DISK)
 *       .build();
 *   }
 * </pre>
 * Users can also configure a number of options related to how {@link Log logs} are constructed and managed.
 * Most notable of the configuration options is the number of {@link #compactionThreads()}, which specifies the
 * number of background threads to use to clean log {@link Segment segments}. The parallelism of the log
 * compaction algorithm will be limited by the number of {@link #compactionThreads()}.
 *
 * @see Log
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
public class Storage {

  /**
   * Returns a new storage builder.
   *
   * @return A new storage builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  private static final String DEFAULT_DIRECTORY = System.getProperty("user.dir");
  private static final int DEFAULT_MAX_SEGMENT_SIZE = 1024 * 1024 * 32;
  private static final int DEFAULT_MAX_ENTRIES_PER_SEGMENT = 1024 * 1024;
  private static final int DEFAULT_COMPACTION_THREADS = Runtime.getRuntime().availableProcessors() / 2;
  private static final Duration DEFAULT_MINOR_COMPACTION_INTERVAL = Duration.ofMinutes(1);
  private static final Duration DEFAULT_MAJOR_COMPACTION_INTERVAL = Duration.ofHours(1);
  private static final double DEFAULT_COMPACTION_THRESHOLD = 0.5;

  private StorageLevel storageLevel = StorageLevel.DISK;
  private Serializer serializer = new Serializer(new PooledDirectAllocator());
  private File directory = new File(DEFAULT_DIRECTORY);
  private int maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
  private int maxEntriesPerSegment = DEFAULT_MAX_ENTRIES_PER_SEGMENT;
  private int compactionThreads = DEFAULT_COMPACTION_THREADS;
  private Duration minorCompactionInterval = DEFAULT_MINOR_COMPACTION_INTERVAL;
  private Duration majorCompactionInterval = DEFAULT_MAJOR_COMPACTION_INTERVAL;
  private double compactionThreshold = DEFAULT_COMPACTION_THRESHOLD;

  public Storage() {
  }

  public Storage(StorageLevel storageLevel) {
    this.storageLevel = Assert.notNull(storageLevel, "storageLevel");
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(String directory) {
    this(new File(Assert.notNull(directory, "directory")));
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(File directory) {
    this(directory, StorageLevel.DISK);
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(Serializer serializer) {
    this(StorageLevel.DISK, serializer);
  }

  /**
   * @throws NullPointerException if {@code directory} or {@code serializer} are null
   */
  public Storage(String directory, Serializer serializer) {
    this(new File(Assert.notNull(directory, "directory")), serializer);
  }

  /**
   * @throws NullPointerException if {@code directory} or {@code serializer} are null
   */
  public Storage(File directory, Serializer serializer) {
    this(directory, StorageLevel.DISK, serializer);
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(String directory, StorageLevel storageLevel) {
    this(new File(Assert.notNull(directory, "directory")), storageLevel);
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(File directory, StorageLevel storageLevel) {
    this.directory = Assert.notNull(directory, "directory");
    this.storageLevel = Assert.notNull(storageLevel, "storageLevel");
  }

  /**
   * @throws NullPointerException if {@code directory} is null
   */
  public Storage(StorageLevel storageLevel, Serializer serializer) {
    this.storageLevel = Assert.notNull(storageLevel, "storageLevel");
    this.serializer = Assert.notNull(serializer, "serializer");
  }

  /**
   * @throws NullPointerException if {@code directory} or {@code serializer} are null
   */
  public Storage(String directory, StorageLevel storageLevel, Serializer serializer) {
    this(new File(Assert.notNull(directory, "directory")), storageLevel, serializer);
  }

  /**
   * @throws NullPointerException if {@code directory} or {@code serializer} are null
   */
  public Storage(File directory, StorageLevel storageLevel, Serializer serializer) {
    this.directory = Assert.notNull(directory, "directory");
    this.storageLevel = Assert.notNull(storageLevel, "storageLevel");
    this.serializer = Assert.notNull(serializer, "serializer");
  }

  /**
   * Returns the storage serializer.
   * <p>
   * The serializer is be used to serialize and deserialize entries written to the log. Entries written
   * to the log must be recognized by the {@link Serializer} either by implementing {@link java.io.Serializable}
   * or {@link io.atomix.catalyst.serializer.CatalystSerializable} or by registering a custom
   * {@link io.atomix.catalyst.serializer.TypeSerializer} with the serializer.
   *
   * @return The storage serializer.
   */
  public Serializer serializer() {
    return serializer;
  }

  /**
   * Returns the storage directory.
   * <p>
   * The storage directory is the directory to which all {@link Log}s write {@link Segment} files. Segment files
   * for multiple logs may be stored in the storage directory, and files for each log instance will be identified
   * by the {@code name} provided when the log is {@link #openLog(String) opened}.
   *
   * @return The storage directory.
   */
  public File directory() {
    return directory;
  }

  /**
   * Returns the storage level.
   * <p>
   * The storage level dictates how entries within individual log {@link Segment}s should be stored.
   *
   * @return The storage level.
   */
  public StorageLevel level() {
    return storageLevel;
  }

  /**
   * Returns the maximum log segment size.
   * <p>
   * The maximum segment size dictates the maximum size any {@link Segment} in a {@link Log} may consume
   * in bytes.
   *
   * @return The maximum segment size in bytes.
   */
  public int maxSegmentSize() {
    return maxSegmentSize;
  }

  /**
   * Returns the maximum number of entries per segment.
   * <p>
   * The maximum entries per segment dictates the maximum number of {@link io.atomix.copycat.server.storage.entry.Entry entries}
   * that are allowed to be stored in any {@link Segment} in a {@link Log}.
   *
   * @return The maximum number of entries per segment.
   */
  public int maxEntriesPerSegment() {
    return maxEntriesPerSegment;
  }

  /**
   * Returns the number of log compaction threads.
   * <p>
   * The compaction thread count dictates the parallelism with which the log
   * {@link io.atomix.copycat.server.storage.compaction.Compactor} can rewrite segments in the log.
   *
   * @return The number of log compaction threads.
   */
  public int compactionThreads() {
    return compactionThreads;
  }

  /**
   * Returns the minor compaction interval.
   * <p>
   * The minor compaction interval dictates the interval at which the
   * {@link io.atomix.copycat.server.storage.compaction.MinorCompactionManager} should evaluate {@link Segment}s
   * in the log for minor compaction.
   *
   * @return The minor compaction interval.
   */
  public Duration minorCompactionInterval() {
    return minorCompactionInterval;
  }

  /**
   * Returns the major compaction interval.
   * <p>
   * The major compaction interval dictates the interval at which the
   * {@link io.atomix.copycat.server.storage.compaction.MajorCompactionManager} should evaluate {@link Segment}s
   * in the log for major compaction.
   *
   * @return The major compaction interval.
   */
  public Duration majorCompactionInterval() {
    return majorCompactionInterval;
  }

  /**
   * Returns the compaction threshold.
   * <p>
   * The compaction threshold is used during {@link io.atomix.copycat.server.storage.compaction.Compaction#MINOR minor compaction}
   * to determine the set of segments to compact.
   *
   * @return The compaction threshold.
   */
  public double compactionThreshold() {
    return compactionThreshold;
  }

  /**
   * Opens a new {@link MetaStore}.
   *
   * @param name The metastore name.
   * @return The metastore.
   */
  public MetaStore openMetaStore(String name) {
    return new MetaStore(name, this);
  }

  /**
   * Opens a new {@link Log}.
   * <p>
   * When a log is opened, the log will attempt to load {@link Segment}s from the storage {@link #directory()}
   * according to the provided log {@code name}. If segments for the given log name are present on disk, segments
   * will be loaded and indexes will be rebuilt from disk. If no segments are found, an empty log will be created.
   *
   * @return The opened log.
   */
  public Log openLog(String name) {
    return new Log(name, this);
  }

  @Override
  public String toString() {
    return String.format("%s[directory=%s]", getClass().getSimpleName(), directory);
  }

  /**
   * Builds a {@link Storage} configuration.
   * <p>
   * The storage builder provides simplifies building more complex {@link Storage} configurations. To
   * create a storage builder, use the {@link #builder()} factory method. Set properties of the configured
   * {@code Storage} object with the various {@code with*} methods. Once the storage has been configured,
   * call {@link #build()} to build the object.
   * <pre>
   *   {@code
   *   Storage storage = Storage.builder()
   *     .withDirectory(new File("logs"))
   *     .withPersistenceLevel(PersistenceLevel.DISK)
   *     .build();
   *   }
   * </pre>
   */
  public static class Builder extends io.atomix.catalyst.util.Builder<Storage> {
    private final Storage storage = new Storage();

    private Builder() {
    }

    /**
     * Sets the log storage level, returning the builder for method chaining.
     * <p>
     * The storage level indicates how individual {@link io.atomix.copycat.server.storage.entry.Entry entries}
     * should be persisted in the log.
     *
     * @param storageLevel The log storage level.
     * @return The storage builder.
     */
    public Builder withStorageLevel(StorageLevel storageLevel) {
      storage.storageLevel = Assert.notNull(storageLevel, "storageLevel");
      return this;
    }

    /**
     * Sets the log entry {@link Serializer}, returning the builder for method chaining.
     * <p>
     * The serializer will be used to serialize and deserialize entries written to the log. Entries written
     * to the log must be recognized by the provided {@link Serializer} either by implementing {@link java.io.Serializable}
     * or {@link io.atomix.catalyst.serializer.CatalystSerializable} or by registering a custom
     * {@link io.atomix.catalyst.serializer.TypeSerializer} with the serializer.
     *
     * @param serializer The log entry serializer.
     * @return The storage builder.
     * @throws NullPointerException If the serializer is {@code null}
     */
    public Builder withSerializer(Serializer serializer) {
      storage.serializer = Assert.notNull(serializer, "serializer");
      return this;
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory. If multiple {@link Storage} objects are located
     * on the same machine, they write logs to different directories.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder withDirectory(String directory) {
      return withDirectory(new File(Assert.notNull(directory, "directory")));
    }

    /**
     * Sets the log directory, returning the builder for method chaining.
     * <p>
     * The log will write segment files into the provided directory. If multiple {@link Storage} objects are located
     * on the same machine, they write logs to different directories.
     *
     * @param directory The log directory.
     * @return The storage builder.
     * @throws NullPointerException If the {@code directory} is {@code null}
     */
    public Builder withDirectory(File directory) {
      storage.directory = Assert.notNull(directory, "directory");
      return this;
    }

    /**
     * Sets the maximum segment size in bytes, returning the builder for method chaining.
     * <p>
     * The maximum segment size dictates when logs should roll over to new segments. As entries are written to
     * a segment of the log, once the size of the segment surpasses the configured maximum segment size, the
     * log will create a new segment and append new entries to that segment.
     * <p>
     * By default, the maximum segment size is {@code 1024 * 1024 * 32}.
     *
     * @param maxSegmentSize The maximum segment size in bytes.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxSegmentSize} is not positive
     */
    public Builder withMaxSegmentSize(int maxSegmentSize) {
      Assert.arg(maxSegmentSize > SegmentDescriptor.BYTES, "maxSegmentSize must be greater than " + SegmentDescriptor.BYTES);
      storage.maxSegmentSize = maxSegmentSize;
      return this;
    }

    /**
     * Sets the maximum number of allows entries per segment, returning the builder for method chaining.
     * <p>
     * The maximum entry count dictates when logs should roll over to new segments. As entries are written to
     * a segment of the log, if the entry count in that segment meets the configured maximum entry count, the
     * log will create a new segment and append new entries to that segment.
     * <p>
     * By default, the maximum entries per segment is {@code 1024 * 1024}.
     *
     * @param maxEntriesPerSegment The maximum number of entries allowed per segment.
     * @return The storage builder.
     * @throws IllegalArgumentException If the {@code maxEntriesPerSegment} not greater than the default max entries per
     * segment
     */
    public Builder withMaxEntriesPerSegment(int maxEntriesPerSegment) {
      Assert.argNot(maxEntriesPerSegment > DEFAULT_MAX_ENTRIES_PER_SEGMENT,
          "max entries per segment cannot be greater than " + DEFAULT_MAX_ENTRIES_PER_SEGMENT);
      storage.maxEntriesPerSegment = maxEntriesPerSegment;
      return this;
    }

    /**
     * Sets the number of log compaction threads, returning the builder for method chaining.
     * <p>
     * The compaction thread count dictates the parallelism with which the log
     * {@link io.atomix.copycat.server.storage.compaction.Compactor} can rewrite segments in the log. By default,
     * the log uses {@code Runtime.getRuntime().availableProcessors() / 2} compaction threads.
     *
     * @param compactionThreads The number of log compaction threads.
     * @return The storage builder.
     * @throws IllegalArgumentException if {@code compactionThreads} is not positive
     */
    public Builder withCompactionThreads(int compactionThreads) {
      storage.compactionThreads = Assert.arg(compactionThreads, compactionThreads > 0, "compactionThreads must be positive");
      return this;
    }

    /**
     * Sets the minor compaction interval, returning the builder for method chaining.
     * <p>
     * The minor compaction interval dictates the interval at which the
     * {@link io.atomix.copycat.server.storage.compaction.MinorCompactionManager} should evaluate {@link Segment}s
     * in the log for minor compaction. It is recommended that the minor compaction interval be at least an order
     * of magnitude smaller than the major compaction interval.
     *
     * @see io.atomix.copycat.server.storage.compaction.MinorCompactionManager
     * @see io.atomix.copycat.server.storage.compaction.MinorCompactionTask
     *
     * @param interval The minor compaction interval.
     * @return The storage builder.
     */
    public Builder withMinorCompactionInterval(Duration interval) {
      storage.minorCompactionInterval = Assert.notNull(interval, "interval");
      return this;
    }

    /**
     * Sets the major compaction interval, returning the builder for method chaining.
     * <p>
     * The major compaction interval dictates the interval at which the
     * {@link io.atomix.copycat.server.storage.compaction.MajorCompactionManager} should evaluate {@link Segment}s
     * in the log for major compaction. Because of the performance costs of major compaction, it is recommended that
     * the major compaction interval be at least an order of magnitude greater than the minor compaction interval.
     *
     * @see io.atomix.copycat.server.storage.compaction.MajorCompactionManager
     * @see io.atomix.copycat.server.storage.compaction.MajorCompactionTask
     *
     * @param interval The major compaction interval.
     * @return The storage builder.
     */
    public Builder withMajorCompactionInterval(Duration interval) {
      storage.majorCompactionInterval = Assert.notNull(interval, "interval");
      return this;
    }

    /**
     * Sets the percentage of entries in the segment that must be cleaned before a segment can be compacted,
     * returning the builder for method chaining.
     * <p>
     * The compaction threshold is used during {@link io.atomix.copycat.server.storage.compaction.Compaction#MINOR minor compaction}
     * to determine the set of segments to compact. By default, the compaction threshold is {@code 0.5}. Increasing the
     * compaction threshold will increase the number of {@link io.atomix.copycat.server.storage.entry.Entry entries} that
     * must be cleaned from the segment before compaction and thus decrease the likelihood that a segment will be compacted.
     * Conversely, decreasing the compaction threshold will increase the frequency of compaction at the cost of unnecessary
     * I/O.
     *
     * @see io.atomix.copycat.server.storage.compaction.MinorCompactionManager
     *
     * @param threshold The segment compact threshold.
     * @return The storage builder.
     */
    public Builder withCompactionThreshold(double threshold) {
      storage.compactionThreshold = Assert.argNot(threshold, threshold <= 0, "threshold must be positive");
      return this;
    }

    /**
     * Builds the {@link Storage} object.
     *
     * @return The built storage configuration.
     */
    @Override
    public Storage build() {
      return storage;
    }
  }

}
