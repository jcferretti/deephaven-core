//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
package io.deephaven.engine.table.impl.sources.regioned;

import org.jetbrains.annotations.NotNull;

class PartitioningSourceFactory {

    /**
     * Get a partitioning {@link RegionedColumnSource} for the supplied {@code dataType}.
     *
     * @param manager The {@link RegionedColumnSourceManager} that will manage the new column source
     * @param dataType The data type expected for partition values
     * @return A new partitioning {@link RegionedColumnSource}
     */
    static <DATA_TYPE> RegionedColumnSource<DATA_TYPE> makePartitioningSource(
            @NotNull final RegionedColumnSourceManager manager,
            @NotNull final Class<DATA_TYPE> dataType) {
        final RegionedColumnSource<?> result;
        if (dataType == boolean.class || dataType == Boolean.class) {
            result = new RegionedColumnSourceObject.Partitioning<>(manager, dataType);
        } else if (dataType == char.class || dataType == Character.class) {
            result = new RegionedColumnSourceChar.Partitioning(manager);
        } else if (dataType == byte.class || dataType == Byte.class) {
            result = new RegionedColumnSourceByte.Partitioning(manager);
        } else if (dataType == short.class || dataType == Short.class) {
            result = new RegionedColumnSourceShort.Partitioning(manager);
        } else if (dataType == int.class || dataType == Integer.class) {
            result = new RegionedColumnSourceInt.Partitioning(manager);
        } else if (dataType == long.class || dataType == Long.class) {
            result = new RegionedColumnSourceLong.Partitioning(manager);
        } else if (dataType == float.class || dataType == Float.class) {
            result = new RegionedColumnSourceFloat.Partitioning(manager);
        } else if (dataType == double.class || dataType == Double.class) {
            result = new RegionedColumnSourceDouble.Partitioning(manager);
        } else {
            result = new RegionedColumnSourceObject.Partitioning<>(manager, dataType);
        }
        // noinspection unchecked
        return (RegionedColumnSource<DATA_TYPE>) result;
    }
}
