//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
// ****** AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY
// ****** Edit CharVectorTransfer and run "./gradlew replicateParquetTransferObjects" to regenerate
//
// @formatter:off
package io.deephaven.parquet.table.transfer;

import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfShort;
import io.deephaven.engine.rowset.RowSequence;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.vector.ShortVector;
import org.jetbrains.annotations.NotNull;

import java.nio.IntBuffer;

final class ShortVectorTransfer extends PrimitiveVectorTransfer<ShortVector, IntBuffer> {
    ShortVectorTransfer(@NotNull final ColumnSource<?> columnSource, @NotNull final RowSequence tableRowSet,
            final int targetPageSizeInBytes) {
        // We encode primitive shorts as primitive ints
        super(columnSource, tableRowSet, targetPageSizeInBytes / Integer.BYTES, targetPageSizeInBytes,
                IntBuffer.allocate(targetPageSizeInBytes / Integer.BYTES), Integer.BYTES);
    }

    @Override
    void resizeBuffer(final int length) {
        buffer = IntBuffer.allocate(length);
    }

    @Override
    void copyToBuffer(@NotNull final EncodedData<ShortVector> data) {
        try (final CloseablePrimitiveIteratorOfShort dataIterator = data.encodedValues.iterator()) {
            dataIterator.forEachRemaining((short value) -> buffer.put(value));
        }
    }
}
