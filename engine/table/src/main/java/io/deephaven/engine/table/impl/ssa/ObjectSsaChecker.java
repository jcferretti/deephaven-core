//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
// ****** AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY
// ****** Edit CharSsaChecker and run "./gradlew replicateSegmentedSortedArray" to regenerate
//
// @formatter:off
package io.deephaven.engine.table.impl.ssa;

import java.util.Objects;
import io.deephaven.util.compare.ObjectComparisons;

import io.deephaven.base.verify.Assert;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.LongChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.WritableLongChunk;
import io.deephaven.chunk.util.hashing.ObjectChunkEquals;
import io.deephaven.chunk.util.hashing.LongChunkEquals;
import io.deephaven.engine.rowset.chunkattributes.RowKeys;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.engine.table.impl.util.ChunkUtils;

public class ObjectSsaChecker implements SsaChecker {
    static ObjectSsaChecker INSTANCE = new ObjectSsaChecker();

    private ObjectSsaChecker() {} // static use only

    @Override
    public void checkSsa(SegmentedSortedArray ssa, Chunk<? extends Values> valueChunk,
            LongChunk<? extends RowKeys> tableIndexChunk) {
        checkSsa((ObjectSegmentedSortedArray) ssa, valueChunk.asObjectChunk(), tableIndexChunk);
    }

    static void checkSsa(ObjectSegmentedSortedArray ssa, ObjectChunk<Object, ? extends Values> valueChunk,
            LongChunk<? extends RowKeys> tableIndexChunk) {
        ssa.validateInternal();

        // noinspection unchecked
        try (final WritableObjectChunk<Object, Values> resultChunk = (WritableObjectChunk) ssa.asObjectChunk();
                final WritableLongChunk<RowKeys> indexChunk = ssa.rowKeysChunk()) {

            Assert.eq(valueChunk.size(), "valueChunk.size()", resultChunk.size(), "resultChunk.size()");
            Assert.eq(tableIndexChunk.size(), "tableIndexChunk.size()", indexChunk.size(), "indexChunk.size()");

            if (!ObjectChunkEquals.equalReduce(resultChunk, valueChunk)) {
                final StringBuilder messageBuilder = new StringBuilder("Values do not match:\n");
                messageBuilder.append("Result Values:\n").append(ChunkUtils.dumpChunk(resultChunk)).append("\n");
                messageBuilder.append("Table Values:\n").append(ChunkUtils.dumpChunk(valueChunk)).append("\n");

                for (int ii = 0; ii < resultChunk.size(); ++ii) {
                    if (!eq(resultChunk.get(ii), valueChunk.get(ii))) {
                        messageBuilder.append("First difference at ").append(ii).append(("\n"));
                        break;
                    }
                }

                throw new SsaCheckException(messageBuilder.toString());
            }
            if (!LongChunkEquals.equalReduce(indexChunk, tableIndexChunk)) {
                final StringBuilder messageBuilder = new StringBuilder("Values do not match:\n");
                messageBuilder.append("Result:\n").append(ChunkUtils.dumpChunk(resultChunk)).append("\n");
                messageBuilder.append("Values:\n").append(ChunkUtils.dumpChunk(valueChunk)).append("\n");

                messageBuilder.append("Result row keys:\n").append(ChunkUtils.dumpChunk(indexChunk)).append("\n");
                messageBuilder.append("Table row keys:\n").append(ChunkUtils.dumpChunk(tableIndexChunk)).append("\n");

                for (int ii = 0; ii < indexChunk.size(); ++ii) {
                    if (indexChunk.get(ii) != tableIndexChunk.get(ii)) {
                        messageBuilder.append("First difference at ").append(ii).append(("\n"));
                        break;
                    }
                }

                throw new SsaCheckException(messageBuilder.toString());
            }
        }
    }

    private static boolean eq(Object lhs, Object rhs) {
        // region equality function
        return Objects.equals(lhs, rhs);
        // endregion equality function
    }
}
