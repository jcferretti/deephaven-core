//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
// ****** AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY
// ****** Edit TupleSourceCodeGenerator and run "./gradlew replicateTupleSources" to regenerate
//
// @formatter:off
package io.deephaven.engine.table.impl.tuplesource.generated;

import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.DoubleChunk;
import io.deephaven.chunk.LongChunk;
import io.deephaven.chunk.ObjectChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.chunk.WritableObjectChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.TupleSource;
import io.deephaven.engine.table.WritableColumnSource;
import io.deephaven.engine.table.impl.tuplesource.AbstractTupleSource;
import io.deephaven.engine.table.impl.tuplesource.ThreeColumnTupleSourceFactory;
import io.deephaven.tuple.generated.ByteDoubleLongTuple;
import io.deephaven.util.BooleanUtils;
import io.deephaven.util.type.TypeUtils;
import org.jetbrains.annotations.NotNull;


/**
 * <p>{@link TupleSource} that produces key column values from {@link ColumnSource} types Boolean, Double, and Long.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class BooleanDoubleLongColumnTupleSource extends AbstractTupleSource<ByteDoubleLongTuple> {

    /** {@link ThreeColumnTupleSourceFactory} instance to create instances of {@link BooleanDoubleLongColumnTupleSource}. **/
    public static final ThreeColumnTupleSourceFactory<ByteDoubleLongTuple, Boolean, Double, Long> FACTORY = new Factory();

    private final ColumnSource<Boolean> columnSource1;
    private final ColumnSource<Double> columnSource2;
    private final ColumnSource<Long> columnSource3;

    public BooleanDoubleLongColumnTupleSource(
            @NotNull final ColumnSource<Boolean> columnSource1,
            @NotNull final ColumnSource<Double> columnSource2,
            @NotNull final ColumnSource<Long> columnSource3
    ) {
        super(columnSource1, columnSource2, columnSource3);
        this.columnSource1 = columnSource1;
        this.columnSource2 = columnSource2;
        this.columnSource3 = columnSource3;
    }

    @Override
    public final ByteDoubleLongTuple createTuple(final long rowKey) {
        return new ByteDoubleLongTuple(
                BooleanUtils.booleanAsByte(columnSource1.getBoolean(rowKey)),
                columnSource2.getDouble(rowKey),
                columnSource3.getLong(rowKey)
        );
    }

    @Override
    public final ByteDoubleLongTuple createPreviousTuple(final long rowKey) {
        return new ByteDoubleLongTuple(
                BooleanUtils.booleanAsByte(columnSource1.getPrevBoolean(rowKey)),
                columnSource2.getPrevDouble(rowKey),
                columnSource3.getPrevLong(rowKey)
        );
    }

    @Override
    public final ByteDoubleLongTuple createTupleFromValues(@NotNull final Object... values) {
        return new ByteDoubleLongTuple(
                BooleanUtils.booleanAsByte((Boolean)values[0]),
                TypeUtils.unbox((Double)values[1]),
                TypeUtils.unbox((Long)values[2])
        );
    }

    @Override
    public final ByteDoubleLongTuple createTupleFromReinterpretedValues(@NotNull final Object... values) {
        return new ByteDoubleLongTuple(
                BooleanUtils.booleanAsByte((Boolean)values[0]),
                TypeUtils.unbox((Double)values[1]),
                TypeUtils.unbox((Long)values[2])
        );
    }

    @Override
    public final int tupleLength() {
        return 3;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <ELEMENT_TYPE> void exportElement(@NotNull final ByteDoubleLongTuple tuple, final int elementIndex, @NotNull final WritableColumnSource<ELEMENT_TYPE> writableSource, final long destinationRowKey) {
        if (elementIndex == 0) {
            writableSource.set(destinationRowKey, (ELEMENT_TYPE) BooleanUtils.byteAsBoolean(tuple.getFirstElement()));
            return;
        }
        if (elementIndex == 1) {
            writableSource.set(destinationRowKey, tuple.getSecondElement());
            return;
        }
        if (elementIndex == 2) {
            writableSource.set(destinationRowKey, tuple.getThirdElement());
            return;
        }
        throw new IndexOutOfBoundsException("Invalid element index " + elementIndex + " for export");
    }

    @Override
    public final Object exportElement(@NotNull final ByteDoubleLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }

    @Override
    public final void exportAllTo(final Object @NotNull [] dest, @NotNull final ByteDoubleLongTuple tuple) {
        dest[0] = BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        dest[1] = TypeUtils.box(tuple.getSecondElement());
        dest[2] = TypeUtils.box(tuple.getThirdElement());
    }

    @Override
    public final void exportAllTo(final Object @NotNull [] dest, @NotNull final ByteDoubleLongTuple tuple, final int @NotNull [] map) {
        dest[map[0]] = BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        dest[map[1]] = TypeUtils.box(tuple.getSecondElement());
        dest[map[2]] = TypeUtils.box(tuple.getThirdElement());
    }

    @Override
    public final Object exportElementReinterpreted(@NotNull final ByteDoubleLongTuple tuple, int elementIndex) {
        if (elementIndex == 0) {
            return BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        }
        if (elementIndex == 1) {
            return TypeUtils.box(tuple.getSecondElement());
        }
        if (elementIndex == 2) {
            return TypeUtils.box(tuple.getThirdElement());
        }
        throw new IllegalArgumentException("Bad elementIndex for 3 element tuple: " + elementIndex);
    }
    @Override
    public final void exportAllReinterpretedTo(final Object @NotNull [] dest, @NotNull final ByteDoubleLongTuple tuple) {
        dest[0] = BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        dest[1] = TypeUtils.box(tuple.getSecondElement());
        dest[2] = TypeUtils.box(tuple.getThirdElement());
    }

    @Override
    public final void exportAllReinterpretedTo(final Object @NotNull [] dest, @NotNull final ByteDoubleLongTuple tuple, final int @NotNull [] map) {
        dest[map[0]] = BooleanUtils.byteAsBoolean(tuple.getFirstElement());
        dest[map[1]] = TypeUtils.box(tuple.getSecondElement());
        dest[map[2]] = TypeUtils.box(tuple.getThirdElement());
    }


    @Override
    protected void convertChunks(@NotNull WritableChunk<? super Values> destination, int chunkSize, Chunk<? extends Values> [] chunks) {
        WritableObjectChunk<ByteDoubleLongTuple, ? super Values> destinationObjectChunk = destination.asWritableObjectChunk();
        ObjectChunk<Boolean, ? extends Values> chunk1 = chunks[0].asObjectChunk();
        DoubleChunk<? extends Values> chunk2 = chunks[1].asDoubleChunk();
        LongChunk<? extends Values> chunk3 = chunks[2].asLongChunk();
        for (int ii = 0; ii < chunkSize; ++ii) {
            destinationObjectChunk.set(ii, new ByteDoubleLongTuple(BooleanUtils.booleanAsByte(chunk1.get(ii)), chunk2.get(ii), chunk3.get(ii)));
        }
        destinationObjectChunk.setSize(chunkSize);
    }

    /** {@link ThreeColumnTupleSourceFactory} for instances of {@link BooleanDoubleLongColumnTupleSource}. **/
    private static final class Factory implements ThreeColumnTupleSourceFactory<ByteDoubleLongTuple, Boolean, Double, Long> {

        private Factory() {
        }

        @Override
        public TupleSource<ByteDoubleLongTuple> create(
                @NotNull final ColumnSource<Boolean> columnSource1,
                @NotNull final ColumnSource<Double> columnSource2,
                @NotNull final ColumnSource<Long> columnSource3
        ) {
            return new BooleanDoubleLongColumnTupleSource(
                    columnSource1,
                    columnSource2,
                    columnSource3
            );
        }
    }
}
