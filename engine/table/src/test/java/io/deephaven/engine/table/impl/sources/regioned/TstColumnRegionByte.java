//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.engine.table.impl.sources.regioned;

import io.deephaven.chunk.attributes.Values;
import io.deephaven.util.QueryConstants;
import io.deephaven.chunk.WritableByteChunk;
import io.deephaven.chunk.WritableChunk;
import io.deephaven.engine.page.Page;
import io.deephaven.engine.rowset.RowSequence;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static org.junit.Assert.assertArrayEquals;

/**
 * Tests for {@link ColumnRegionByte}.
 */
@SuppressWarnings({"AutoBoxing", "JUnit4AnnotatedMethodInJUnit3TestCase"})
public abstract class TstColumnRegionByte {

    static class Identity implements ColumnRegionByte<Values>, Page.WithDefaults<Values> {
        @Override
        public void invalidate() {
            // NOTHNG TO DO
        }

        @Override
        public long mask() {
            return Long.MAX_VALUE;
        }

        @Override
        public byte getByte(long elementIndex) {
            return (byte) elementIndex;
        }

        @Override
        public byte[] getBytes(long firstElementIndex, @NotNull byte[] destination, int destinationOffset, int length) {
            for (int i = 0; i < length; ++i) {
                destination[destinationOffset + i] = (byte) (firstElementIndex + i);
            }

            return destination;
        }

        @Override
        public void fillChunkAppend(@NotNull FillContext context, @NotNull WritableChunk<? super Values> destination,
                @NotNull RowSequence rowSequence) {
            WritableByteChunk<? super Values> charDestination = destination.asWritableByteChunk();
            int size = destination.size();
            int length = (int) rowSequence.size();

            rowSequence.forAllRowKeys(key -> {
                for (int i = 0; i < length; ++i) {
                    charDestination.set(size + i, (byte) key);
                }
            });

            charDestination.setSize(size + length);
        }
    }

    public static class TestNull extends TstColumnRegionPrimative<ColumnRegionByte<Values>> {

        @Override
        public void setUp() throws Exception {
            super.setUp();
            SUT = ColumnRegionByte.createNull(Long.MAX_VALUE);
        }

        @Override
        public void testGet() {
            TestCase.assertEquals(QueryConstants.NULL_BYTE, SUT.getByte(0));
            TestCase.assertEquals(QueryConstants.NULL_BYTE, SUT.getByte(1));
            TestCase.assertEquals(QueryConstants.NULL_BYTE, SUT.getByte(Integer.MAX_VALUE));
            TestCase.assertEquals(QueryConstants.NULL_BYTE, SUT.getByte((1L << 40) - 2));
            TestCase.assertEquals(QueryConstants.NULL_BYTE, SUT.getByte(Long.MAX_VALUE));
        }

        public void testGetBytes() {
            byte[] result = new byte[1024];
            SUT.getBytes(0, result, 0, result.length);
            for (byte b : result) {
                TestCase.assertEquals(QueryConstants.NULL_BYTE, b);
            }
            SUT.getBytes(Integer.MAX_VALUE, result, 100, result.length - 200);
            for (byte b : result) {
                TestCase.assertEquals(QueryConstants.NULL_BYTE, b);
            }
        }
    }

    public static class TestDeferred extends TstColumnRegionPrimative.Deferred<ColumnRegionByte<Values>> {

        @Override
        public void setUp() throws Exception {
            super.setUp();
            // noinspection unchecked
            regionSupplier = mock(Supplier.class, "R1");
            checking(new Expectations() {
                {
                    oneOf(regionSupplier).get();
                    will(returnValue(new TstColumnRegionByte.Identity()));
                }
            });
            SUT = new DeferredColumnRegionByte<>(Long.MAX_VALUE, regionSupplier);
        }

        @Override
        public void testGet() {
            assertEquals((byte) 8, SUT.getByte(8));
            assertIsSatisfied();
            assertEquals((byte) 127, SUT.getByte(127));
            assertIsSatisfied();
        }

        public void testGetBytes() {
            assertArrayEquals(new byte[] {(byte) 0, (byte) 1, (byte) 2}, SUT.getBytes(0, new byte[3], 0, 3));
            assertIsSatisfied();
            assertArrayEquals(new byte[] {(byte) 5, (byte) 6, (byte) 7}, SUT.getBytes(5, new byte[3], 0, 3));
            assertIsSatisfied();
        }
    }
}
