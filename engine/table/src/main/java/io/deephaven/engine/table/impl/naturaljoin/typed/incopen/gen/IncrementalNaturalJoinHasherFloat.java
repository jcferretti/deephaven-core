//
// Copyright (c) 2016-2025 Deephaven Data Labs and Patent Pending
//
// ****** AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY
// ****** Run ReplicateTypedHashers or ./gradlew replicateTypedHashers to regenerate
//
// @formatter:off
package io.deephaven.engine.table.impl.naturaljoin.typed.incopen.gen;

import static io.deephaven.util.compare.FloatComparisons.eq;

import io.deephaven.api.NaturalJoinType;
import io.deephaven.base.verify.Assert;
import io.deephaven.chunk.Chunk;
import io.deephaven.chunk.FloatChunk;
import io.deephaven.chunk.LongChunk;
import io.deephaven.chunk.attributes.Values;
import io.deephaven.chunk.util.hashing.FloatChunkHasher;
import io.deephaven.engine.rowset.RowSequence;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.rowset.RowSetFactory;
import io.deephaven.engine.rowset.WritableRowSet;
import io.deephaven.engine.rowset.chunkattributes.OrderedRowKeys;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.impl.NaturalJoinModifiedSlotTracker;
import io.deephaven.engine.table.impl.by.alternatingcolumnsource.AlternatingColumnSource;
import io.deephaven.engine.table.impl.naturaljoin.IncrementalNaturalJoinStateManagerTypedBase;
import io.deephaven.engine.table.impl.sources.LongArraySource;
import io.deephaven.engine.table.impl.sources.immutable.ImmutableFloatArraySource;
import java.lang.Override;
import java.util.Arrays;

final class IncrementalNaturalJoinHasherFloat extends IncrementalNaturalJoinStateManagerTypedBase {
    private ImmutableFloatArraySource mainKeySource0;

    private ImmutableFloatArraySource alternateKeySource0;

    public IncrementalNaturalJoinHasherFloat(ColumnSource[] tableKeySources,
            ColumnSource[] originalTableKeySources, int tableSize, double maximumLoadFactor,
            double targetLoadFactor, NaturalJoinType joinType, boolean addOnly) {
        super(tableKeySources, originalTableKeySources, tableSize, maximumLoadFactor, joinType, addOnly);
        this.mainKeySource0 = (ImmutableFloatArraySource) super.mainKeySources[0];
        this.mainKeySource0.ensureCapacity(tableSize);
    }

    private int nextTableLocation(int tableLocation) {
        return (tableLocation + 1) & (tableSize - 1);
    }

    private int alternateNextTableLocation(int tableLocation) {
        return (tableLocation + 1) & (alternateTableSize - 1);
    }

    protected void buildFromLeftSide(RowSequence rowSequence, Chunk[] sourceKeyChunks) {
        Assert.eqZero(rehashPointer, "rehashPointer");
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final int chunkSize = keyChunk0.size();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            int tableLocation = firstTableLocation;
            MAIN_SEARCH: while (true) {
                long rightRowKeyForState = mainRightRowKey.getUnsafe(tableLocation);
                if (isStateEmpty(rightRowKeyForState)) {
                    numEntries++;
                    liveEntries++;
                    mainKeySource0.set(tableLocation, k0);
                    mainLeftRowSet.set(tableLocation, RowSetFactory.fromKeys(rowKeyChunk.get(chunkPosition)));
                    mainRightRowKey.set(tableLocation, RowSet.NULL_ROW_KEY);
                    mainModifiedTrackerCookieSource.set(tableLocation, -1L);
                    break;
                } else if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (rightRowKeyForState <= FIRST_DUPLICATE && (joinType == NaturalJoinType.ERROR_ON_DUPLICATE || joinType == NaturalJoinType.EXACTLY_ONE_MATCH)) {
                        throw new IllegalStateException("Natural Join found duplicate right key for " + extractKeyStringFromSourceTable(rowKeyChunk.get(chunkPosition)));
                    }
                    mainLeftRowSet.getUnsafe(tableLocation).insert(rowKeyChunk.get(chunkPosition));
                    break;
                } else {
                    tableLocation = nextTableLocation(tableLocation);
                    Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
                }
            }
        }
    }

    protected void buildFromRightSide(RowSequence rowSequence, Chunk[] sourceKeyChunks) {
        Assert.eqZero(rehashPointer, "rehashPointer");
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final int chunkSize = keyChunk0.size();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            int tableLocation = firstTableLocation;
            MAIN_SEARCH: while (true) {
                long existingRightRowKey = mainRightRowKey.getUnsafe(tableLocation);
                if (isStateEmpty(existingRightRowKey)) {
                    numEntries++;
                    liveEntries++;
                    mainKeySource0.set(tableLocation, k0);
                    mainLeftRowSet.set(tableLocation, RowSetFactory.empty());
                    mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                    mainModifiedTrackerCookieSource.set(tableLocation, -1L);
                    break;
                } else if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (existingRightRowKey == RowSet.NULL_ROW_KEY) {
                        mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                    } else if (existingRightRowKey <= FIRST_DUPLICATE) {
                        final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                        rightSideDuplicateRowSets.getUnsafe(duplicateLocation).insert(rowKeyChunk.get(chunkPosition));
                    } else {
                        if (addOnly && joinType == NaturalJoinType.FIRST_MATCH)  {
                            // nop, we already have the first match;
                        } else if (addOnly && joinType == NaturalJoinType.LAST_MATCH) {
                            // always update the RHS key since this is the last match;
                            mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                        } else {
                            final long duplicateLocation = allocateDuplicateLocation();
                            rightSideDuplicateRowSets.set(duplicateLocation, RowSetFactory.fromKeys(existingRightRowKey, rowKeyChunk.get(chunkPosition)));
                            mainRightRowKey.set(tableLocation, rowKeyFromDuplicateLocation(duplicateLocation));
                        }
                    }
                    break;
                } else {
                    tableLocation = nextTableLocation(tableLocation);
                    Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
                }
            }
        }
    }

    protected void addRightSide(RowSequence rowSequence, Chunk[] sourceKeyChunks,
            NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final int chunkSize = keyChunk0.size();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            int tableLocation = firstTableLocation;
            int firstDeletedLocation = -1;
            MAIN_SEARCH: while (true) {
                long existingRightRowKey = mainRightRowKey.getUnsafe(tableLocation);
                if (firstDeletedLocation < 0 && isStateDeleted(existingRightRowKey)) {
                    firstDeletedLocation = tableLocation;
                }
                if (isStateEmpty(existingRightRowKey)) {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    int alternateTableLocation = firstAlternateTableLocation;
                    while (alternateTableLocation < rehashPointer) {
                        existingRightRowKey = alternateRightRowKey.getUnsafe(alternateTableLocation);
                        if (isStateEmpty(existingRightRowKey)) {
                            break;
                        } else if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                            if (isStateDeleted(existingRightRowKey)) {
                                break;
                            }
                            if (existingRightRowKey == RowSet.NULL_ROW_KEY) {
                                alternateRightRowKey.set(alternateTableLocation, rowKeyChunk.get(chunkPosition));
                                alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                            } else if (existingRightRowKey <= FIRST_DUPLICATE) {
                                final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                                final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                                final long duplicateSize = duplicates.size();
                                final long inputKey = rowKeyChunk.get(chunkPosition);
                                final long newKey = addRightRowKeyToDuplicates(duplicates, inputKey, joinType);
                                Assert.eq(duplicateSize, "duplicateSize", duplicates.size() - 1, "duplicates.size() - 1");
                                final boolean leftEmpty = alternateLeftRowSet.getUnsafe(alternateTableLocation).isEmpty();
                                if (!leftEmpty && inputKey == newKey) {
                                    // we have a new output key for the LHS rows;
                                    alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                }
                            } else {
                                final long inputKey = rowKeyChunk.get(chunkPosition);
                                if (addOnly && joinType == NaturalJoinType.FIRST_MATCH) {
                                    final long newKey = Math.min(existingRightRowKey, inputKey);
                                    if (newKey != existingRightRowKey) {
                                        alternateRightRowKey.set(alternateTableLocation, newKey);
                                        alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                    }
                                } else if (addOnly && joinType == NaturalJoinType.LAST_MATCH) {
                                    final long newKey = Math.max(existingRightRowKey, inputKey);
                                    if (newKey != existingRightRowKey) {
                                        alternateRightRowKey.set(alternateTableLocation, newKey);
                                        alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                    }
                                } else {
                                    final long duplicateLocation = allocateDuplicateLocation();
                                    final WritableRowSet duplicates = RowSetFactory.fromKeys(existingRightRowKey, inputKey);
                                    rightSideDuplicateRowSets.set(duplicateLocation, duplicates);
                                    alternateRightRowKey.set(alternateTableLocation, rowKeyFromDuplicateLocation(duplicateLocation));
                                    alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                }
                            }
                            break MAIN_SEARCH;
                        } else {
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (firstDeletedLocation >= 0) {
                        tableLocation = firstDeletedLocation;
                    } else {
                        numEntries++;
                    }
                    liveEntries++;
                    mainKeySource0.set(tableLocation, k0);
                    mainLeftRowSet.set(tableLocation, RowSetFactory.empty());
                    mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                    mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(-1, mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                    break;
                } else if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(existingRightRowKey)) {
                        tableLocation = firstDeletedLocation;
                        liveEntries++;
                        mainKeySource0.set(tableLocation, k0);
                        mainLeftRowSet.set(tableLocation, RowSetFactory.empty());
                        mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                        mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(-1, mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                        break;
                    }
                    if (existingRightRowKey == RowSet.NULL_ROW_KEY) {
                        mainRightRowKey.set(tableLocation, rowKeyChunk.get(chunkPosition));
                        mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                    } else if (existingRightRowKey <= FIRST_DUPLICATE) {
                        final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                        final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                        final long duplicateSize = duplicates.size();
                        final long inputKey = rowKeyChunk.get(chunkPosition);
                        final long newKey = addRightRowKeyToDuplicates(duplicates, inputKey, joinType);
                        Assert.eq(duplicateSize, "duplicateSize", duplicates.size() - 1, "duplicates.size() - 1");
                        final boolean leftEmpty = mainLeftRowSet.getUnsafe(tableLocation).isEmpty();
                        if (!leftEmpty && inputKey == newKey) {
                            // we have a new output key for the LHS rows;
                            mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                        }
                    } else {
                        final long inputKey = rowKeyChunk.get(chunkPosition);
                        if (addOnly && joinType == NaturalJoinType.FIRST_MATCH) {
                            final long newKey = Math.min(existingRightRowKey, inputKey);
                            if (newKey != existingRightRowKey) {
                                mainRightRowKey.set(tableLocation, newKey);
                                mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                            }
                        } else if (addOnly && joinType == NaturalJoinType.LAST_MATCH) {
                            final long newKey = Math.max(existingRightRowKey, inputKey);
                            if (newKey != existingRightRowKey) {
                                mainRightRowKey.set(tableLocation, newKey);
                                mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                            }
                        } else {
                            final long duplicateLocation = allocateDuplicateLocation();
                            final WritableRowSet duplicates = RowSetFactory.fromKeys(existingRightRowKey, inputKey);
                            rightSideDuplicateRowSets.set(duplicateLocation, duplicates);
                            mainRightRowKey.set(tableLocation, rowKeyFromDuplicateLocation(duplicateLocation));
                            mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                        }
                    }
                    break;
                } else {
                    tableLocation = nextTableLocation(tableLocation);
                    Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
                }
            }
        }
    }

    protected void addLeftSide(RowSequence rowSequence, Chunk[] sourceKeyChunks,
            LongArraySource leftRedirections, long leftRedirectionOffset) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final int chunkSize = keyChunk0.size();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            int tableLocation = firstTableLocation;
            int firstDeletedLocation = -1;
            MAIN_SEARCH: while (true) {
                long rightRowKeyForState = mainRightRowKey.getUnsafe(tableLocation);
                if (firstDeletedLocation < 0 && isStateDeleted(rightRowKeyForState)) {
                    firstDeletedLocation = tableLocation;
                }
                if (isStateEmpty(rightRowKeyForState)) {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    int alternateTableLocation = firstAlternateTableLocation;
                    while (alternateTableLocation < rehashPointer) {
                        rightRowKeyForState = alternateRightRowKey.getUnsafe(alternateTableLocation);
                        if (isStateEmpty(rightRowKeyForState)) {
                            break;
                        } else if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                            if (isStateDeleted(rightRowKeyForState)) {
                                break;
                            }
                            final long rightRowKey;
                            if (rightRowKeyForState <= FIRST_DUPLICATE) {
                                if (joinType == NaturalJoinType.ERROR_ON_DUPLICATE || joinType == NaturalJoinType.EXACTLY_ONE_MATCH) {
                                    throw new IllegalStateException("Natural Join found duplicate right key for " + extractKeyStringFromSourceTable(rightRowKeyForState));
                                }
                                final long duplicateLocation = duplicateLocationFromRowKey(rightRowKeyForState);
                                final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                                rightRowKey = getRightRowKeyFromDuplicates(duplicates, joinType);
                            } else {
                                rightRowKey = rightRowKeyForState;
                            }
                            alternateLeftRowSet.getUnsafe(alternateTableLocation).insert(rowKeyChunk.get(chunkPosition));
                            leftRedirections.set(leftRedirectionOffset++, rightRowKey);
                            break MAIN_SEARCH;
                        } else {
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (firstDeletedLocation >= 0) {
                        tableLocation = firstDeletedLocation;
                    } else {
                        numEntries++;
                    }
                    liveEntries++;
                    mainKeySource0.set(tableLocation, k0);
                    mainLeftRowSet.set(tableLocation, RowSetFactory.fromKeys(rowKeyChunk.get(chunkPosition)));
                    mainRightRowKey.set(tableLocation, RowSet.NULL_ROW_KEY);
                    mainModifiedTrackerCookieSource.set(tableLocation, -1L);
                    leftRedirections.set(leftRedirectionOffset++, RowSet.NULL_ROW_KEY);
                    break;
                } else if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(rightRowKeyForState)) {
                        tableLocation = firstDeletedLocation;
                        liveEntries++;
                        mainKeySource0.set(tableLocation, k0);
                        mainLeftRowSet.set(tableLocation, RowSetFactory.fromKeys(rowKeyChunk.get(chunkPosition)));
                        mainRightRowKey.set(tableLocation, RowSet.NULL_ROW_KEY);
                        mainModifiedTrackerCookieSource.set(tableLocation, -1L);
                        leftRedirections.set(leftRedirectionOffset++, RowSet.NULL_ROW_KEY);
                        break;
                    }
                    final long rightRowKey;
                    if (rightRowKeyForState <= FIRST_DUPLICATE) {
                        if (joinType == NaturalJoinType.ERROR_ON_DUPLICATE || joinType == NaturalJoinType.EXACTLY_ONE_MATCH) {
                            throw new IllegalStateException("Natural Join found duplicate right key for " + extractKeyStringFromSourceTable(rightRowKeyForState));
                        }
                        final long duplicateLocation = duplicateLocationFromRowKey(rightRowKeyForState);
                        final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                        rightRowKey = getRightRowKeyFromDuplicates(duplicates, joinType);
                    } else {
                        rightRowKey = rightRowKeyForState;
                    }
                    mainLeftRowSet.getUnsafe(tableLocation).insert(rowKeyChunk.get(chunkPosition));
                    leftRedirections.set(leftRedirectionOffset++, rightRowKey);
                    break;
                } else {
                    tableLocation = nextTableLocation(tableLocation);
                    Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
                }
            }
        }
    }

    protected void removeRight(RowSequence rowSequence, Chunk[] sourceKeyChunks,
            NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        final int chunkSize = keyChunk0.size();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            boolean found = false;
            boolean searchAlternate = true;
            int tableLocation = firstTableLocation;
            long existingRightRowKey;
            while (!isStateEmpty(existingRightRowKey = mainRightRowKey.getUnsafe(tableLocation))) {
                if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(existingRightRowKey)) {
                        searchAlternate = false;
                        break;
                    }
                    if (existingRightRowKey <= FIRST_DUPLICATE) {
                        final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                        final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                        final long duplicateSize = duplicates.size();
                        final long inputKey = rowKeyChunk.get(chunkPosition);
                        final long originalKey = removeRightRowKeyFromDuplicates(duplicates, inputKey, joinType);
                        Assert.eq(duplicateSize, "duplicateSize", duplicates.size() + 1, "duplicates.size() + 1");
                        final boolean leftEmpty = mainLeftRowSet.getUnsafe(tableLocation).isEmpty();
                        if (!leftEmpty && originalKey == inputKey) {
                            // we have a new output key for the LHS rows;
                            mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                        }
                        if (duplicates.size() == 1) {
                            final long newKey = getRightRowKeyFromDuplicates(duplicates, joinType);;
                            mainRightRowKey.set(tableLocation, newKey);
                            freeDuplicateLocation(duplicateLocation);
                        }
                    } else if (existingRightRowKey != rowKeyChunk.get(chunkPosition)) {
                        Assert.statementNeverExecuted("Could not find existing right row in state");
                    } else {
                        final boolean leftEmpty = mainLeftRowSet.getUnsafe(tableLocation).isEmpty();
                        if (leftEmpty) {
                            mainRightRowKey.set(tableLocation, TOMBSTONE_RIGHT_STATE);
                            liveEntries--;
                        } else {
                            mainRightRowKey.set(tableLocation, RowSet.NULL_ROW_KEY);
                        }
                        mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                    }
                    found = true;
                    break;
                }
                tableLocation = nextTableLocation(tableLocation);
                Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
            }
            if (!found) {
                if (!searchAlternate) {
                    throw Assert.statementNeverExecuted("Could not find existing state for removed right row");
                } else {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    boolean alternateFound = false;
                    if (firstAlternateTableLocation < rehashPointer) {
                        int alternateTableLocation = firstAlternateTableLocation;
                        while (!isStateEmpty(existingRightRowKey = alternateRightRowKey.getUnsafe(alternateTableLocation))) {
                            if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                                if (isStateDeleted(existingRightRowKey)) {
                                    break;
                                }
                                if (existingRightRowKey <= FIRST_DUPLICATE) {
                                    final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                                    final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                                    final long duplicateSize = duplicates.size();
                                    final long inputKey = rowKeyChunk.get(chunkPosition);
                                    final long originalKey = removeRightRowKeyFromDuplicates(duplicates, inputKey, joinType);
                                    Assert.eq(duplicateSize, "duplicateSize", duplicates.size() + 1, "duplicates.size() + 1");
                                    final boolean leftEmpty = alternateLeftRowSet.getUnsafe(alternateTableLocation).isEmpty();
                                    if (!leftEmpty && originalKey == inputKey) {
                                        // we have a new output key for the LHS rows;
                                        alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                    }
                                    if (duplicates.size() == 1) {
                                        final long newKey = getRightRowKeyFromDuplicates(duplicates, joinType);;
                                        alternateRightRowKey.set(alternateTableLocation, newKey);
                                        freeDuplicateLocation(duplicateLocation);
                                    }
                                } else if (existingRightRowKey != rowKeyChunk.get(chunkPosition)) {
                                    Assert.statementNeverExecuted("Could not find existing right row in state");
                                } else {
                                    final boolean leftEmpty = alternateLeftRowSet.getUnsafe(alternateTableLocation).isEmpty();
                                    if (leftEmpty) {
                                        alternateRightRowKey.set(alternateTableLocation, TOMBSTONE_RIGHT_STATE);
                                        liveEntries--;
                                    } else {
                                        alternateRightRowKey.set(alternateTableLocation, RowSet.NULL_ROW_KEY);
                                    }
                                    alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                }
                                alternateFound = true;
                                break;
                            }
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (!alternateFound) {
                        throw Assert.statementNeverExecuted("Could not find existing state for removed right row");
                    }
                }
            }
        }
    }

    protected void modifyByRight(RowSequence rowSequence, Chunk[] sourceKeyChunks,
            NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final int chunkSize = keyChunk0.size();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            boolean found = false;
            boolean searchAlternate = true;
            int tableLocation = firstTableLocation;
            long existingRightRowKey;
            while (!isStateEmpty(existingRightRowKey = mainRightRowKey.getUnsafe(tableLocation))) {
                if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(existingRightRowKey)) {
                        searchAlternate = false;
                        break;
                    }
                    mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                    found = true;
                    break;
                }
                tableLocation = nextTableLocation(tableLocation);
                Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
            }
            if (!found) {
                if (!searchAlternate) {
                    throw Assert.statementNeverExecuted("Could not find existing state for modified right row");
                } else {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    boolean alternateFound = false;
                    if (firstAlternateTableLocation < rehashPointer) {
                        int alternateTableLocation = firstAlternateTableLocation;
                        while (!isStateEmpty(existingRightRowKey = alternateRightRowKey.getUnsafe(alternateTableLocation))) {
                            if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                                if (isStateDeleted(existingRightRowKey)) {
                                    break;
                                }
                                alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_CHANGE));
                                alternateFound = true;
                                break;
                            }
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (!alternateFound) {
                        throw Assert.statementNeverExecuted("Could not find existing state for modified right row");
                    }
                }
            }
        }
    }

    protected void applyRightShift(RowSequence rowSequence, Chunk[] sourceKeyChunks,
            long shiftDelta, NaturalJoinModifiedSlotTracker modifiedSlotTracker,
            IncrementalNaturalJoinStateManagerTypedBase.ProbeContext pc) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        final int chunkSize = keyChunk0.size();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            boolean found = false;
            boolean searchAlternate = true;
            int tableLocation = firstTableLocation;
            long existingRightRowKey;
            while (!isStateEmpty(existingRightRowKey = mainRightRowKey.getUnsafe(tableLocation))) {
                if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(existingRightRowKey)) {
                        searchAlternate = false;
                        break;
                    }
                    final long keyToShift = rowKeyChunk.get(chunkPosition);
                    if (existingRightRowKey == keyToShift - shiftDelta) {
                        mainRightRowKey.set(tableLocation, keyToShift);
                        mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_SHIFT));
                    } else if (existingRightRowKey <= FIRST_DUPLICATE) {
                        final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                        if (shiftDelta < 0) {
                            final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                            shiftOneKey(duplicates, keyToShift, shiftDelta);
                        } else {
                            pc.pendingShifts.set(pc.pendingShiftPointer++, (long)duplicateLocation);
                            pc.pendingShifts.set(pc.pendingShiftPointer++, keyToShift);
                        }
                        final boolean leftEmpty = mainLeftRowSet.getUnsafe(tableLocation).isEmpty();
                        if (!leftEmpty) {
                            // we may have a new output key for the LHS rows;
                            mainModifiedTrackerCookieSource.set(tableLocation, modifiedSlotTracker.addMain(mainModifiedTrackerCookieSource.getUnsafe(tableLocation), mainInsertMask | tableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_SHIFT));
                        }
                    } else {
                        throw Assert.statementNeverExecuted("Could not find existing index for shifted right row");
                    }
                    found = true;
                    break;
                }
                tableLocation = nextTableLocation(tableLocation);
                Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
            }
            if (!found) {
                if (!searchAlternate) {
                    throw Assert.statementNeverExecuted("Could not find existing state for shifted right row");
                } else {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    boolean alternateFound = false;
                    if (firstAlternateTableLocation < rehashPointer) {
                        int alternateTableLocation = firstAlternateTableLocation;
                        while (!isStateEmpty(existingRightRowKey = alternateRightRowKey.getUnsafe(alternateTableLocation))) {
                            if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                                if (isStateDeleted(existingRightRowKey)) {
                                    break;
                                }
                                final long keyToShift = rowKeyChunk.get(chunkPosition);
                                if (existingRightRowKey == keyToShift - shiftDelta) {
                                    alternateRightRowKey.set(alternateTableLocation, keyToShift);
                                    alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_SHIFT));
                                } else if (existingRightRowKey <= FIRST_DUPLICATE) {
                                    final long duplicateLocation = duplicateLocationFromRowKey(existingRightRowKey);
                                    if (shiftDelta < 0) {
                                        final WritableRowSet duplicates = rightSideDuplicateRowSets.getUnsafe(duplicateLocation);
                                        shiftOneKey(duplicates, keyToShift, shiftDelta);
                                    } else {
                                        pc.pendingShifts.set(pc.pendingShiftPointer++, (long)duplicateLocation);
                                        pc.pendingShifts.set(pc.pendingShiftPointer++, keyToShift);
                                    }
                                    final boolean leftEmpty = alternateLeftRowSet.getUnsafe(alternateTableLocation).isEmpty();
                                    if (!leftEmpty) {
                                        // we may have a new output key for the LHS rows;
                                        alternateModifiedTrackerCookieSource.set(alternateTableLocation, modifiedSlotTracker.addMain(alternateModifiedTrackerCookieSource.getUnsafe(alternateTableLocation), alternateInsertMask | alternateTableLocation, existingRightRowKey, NaturalJoinModifiedSlotTracker.FLAG_RIGHT_SHIFT));
                                    }
                                } else {
                                    throw Assert.statementNeverExecuted("Could not find existing index for shifted right row");
                                }
                                alternateFound = true;
                                break;
                            }
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (!alternateFound) {
                        throw Assert.statementNeverExecuted("Could not find existing state for shifted right row");
                    }
                }
            }
        }
    }

    protected void removeLeft(RowSequence rowSequence, Chunk[] sourceKeyChunks) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        final int chunkSize = keyChunk0.size();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            boolean found = false;
            boolean searchAlternate = true;
            int tableLocation = firstTableLocation;
            long rightState;
            while (!isStateEmpty(rightState = mainRightRowKey.getUnsafe(tableLocation))) {
                if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(rightState)) {
                        searchAlternate = false;
                        break;
                    }
                    final WritableRowSet left = mainLeftRowSet.getUnsafe(tableLocation);
                    left.remove(rowKeyChunk.get(chunkPosition));
                    if (left.isEmpty() && rightState == RowSet.NULL_ROW_KEY) {
                        mainRightRowKey.set(tableLocation, TOMBSTONE_RIGHT_STATE);
                        liveEntries--;
                    }
                    found = true;
                    break;
                }
                tableLocation = nextTableLocation(tableLocation);
                Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
            }
            if (!found) {
                if (!searchAlternate) {
                    throw Assert.statementNeverExecuted("Could not find existing state for removed left row");
                } else {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    boolean alternateFound = false;
                    if (firstAlternateTableLocation < rehashPointer) {
                        int alternateTableLocation = firstAlternateTableLocation;
                        while (!isStateEmpty(rightState = alternateRightRowKey.getUnsafe(alternateTableLocation))) {
                            if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                                if (isStateDeleted(rightState)) {
                                    break;
                                }
                                final WritableRowSet left = alternateLeftRowSet.getUnsafe(alternateTableLocation);
                                left.remove(rowKeyChunk.get(chunkPosition));
                                if (left.isEmpty() && rightState == RowSet.NULL_ROW_KEY) {
                                    alternateRightRowKey.set(alternateTableLocation, TOMBSTONE_RIGHT_STATE);
                                    liveEntries--;
                                }
                                alternateFound = true;
                                break;
                            }
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (!alternateFound) {
                        throw Assert.statementNeverExecuted("Could not find existing state for removed left row");
                    }
                }
            }
        }
    }

    protected void applyLeftShift(RowSequence rowSequence, Chunk[] sourceKeyChunks, long shiftDelta,
            IncrementalNaturalJoinStateManagerTypedBase.ProbeContext pc) {
        final FloatChunk<Values> keyChunk0 = sourceKeyChunks[0].asFloatChunk();
        final LongChunk<OrderedRowKeys> rowKeyChunk = rowSequence.asRowKeyChunk();
        final int chunkSize = keyChunk0.size();
        for (int chunkPosition = 0; chunkPosition < chunkSize; ++chunkPosition) {
            final float k0 = keyChunk0.get(chunkPosition);
            final int hash = hash(k0);
            final int firstTableLocation = hashToTableLocation(hash);
            boolean found = false;
            boolean searchAlternate = true;
            int tableLocation = firstTableLocation;
            long stateValue;
            while (!isStateEmpty(stateValue = mainRightRowKey.getUnsafe(tableLocation))) {
                if (eq(mainKeySource0.getUnsafe(tableLocation), k0)) {
                    if (isStateDeleted(stateValue)) {
                        searchAlternate = false;
                        break;
                    }
                    final WritableRowSet leftRowSetForState = mainLeftRowSet.getUnsafe(tableLocation);
                    final long keyToShift = rowKeyChunk.get(chunkPosition);
                    if (shiftDelta < 0) {
                        shiftOneKey(leftRowSetForState, keyToShift, shiftDelta);
                    } else {
                        pc.pendingShifts.set(pc.pendingShiftPointer++, (long)tableLocation);
                        pc.pendingShifts.set(pc.pendingShiftPointer++, keyToShift);
                    }
                    found = true;
                    break;
                }
                tableLocation = nextTableLocation(tableLocation);
                Assert.neq(tableLocation, "tableLocation", firstTableLocation, "firstTableLocation");
            }
            if (!found) {
                if (!searchAlternate) {
                    throw Assert.statementNeverExecuted("Could not find existing state for shifted left row");
                } else {
                    final int firstAlternateTableLocation = hashToTableLocationAlternate(hash);
                    boolean alternateFound = false;
                    if (firstAlternateTableLocation < rehashPointer) {
                        int alternateTableLocation = firstAlternateTableLocation;
                        while (!isStateEmpty(stateValue = alternateRightRowKey.getUnsafe(alternateTableLocation))) {
                            if (eq(alternateKeySource0.getUnsafe(alternateTableLocation), k0)) {
                                if (isStateDeleted(stateValue)) {
                                    break;
                                }
                                final WritableRowSet leftRowSetForState = alternateLeftRowSet.getUnsafe(alternateTableLocation);
                                final long keyToShift = rowKeyChunk.get(chunkPosition);
                                if (shiftDelta < 0) {
                                    shiftOneKey(leftRowSetForState, keyToShift, shiftDelta);
                                } else {
                                    pc.pendingShifts.set(pc.pendingShiftPointer++, (long)(AlternatingColumnSource.ALTERNATE_SWITCH_MASK | alternateTableLocation));
                                    pc.pendingShifts.set(pc.pendingShiftPointer++, keyToShift);
                                }
                                alternateFound = true;
                                break;
                            }
                            alternateTableLocation = alternateNextTableLocation(alternateTableLocation);
                            Assert.neq(alternateTableLocation, "alternateTableLocation", firstAlternateTableLocation, "firstAlternateTableLocation");
                        }
                    }
                    if (!alternateFound) {
                        throw Assert.statementNeverExecuted("Could not find existing state for shifted left row");
                    }
                }
            }
        }
    }

    private static int hash(float k0) {
        int hash = FloatChunkHasher.hashInitialSingle(k0);
        return hash;
    }

    private static boolean isStateEmpty(long state) {
        return state == EMPTY_RIGHT_STATE;
    }

    private static boolean isStateDeleted(long state) {
        return state == TOMBSTONE_RIGHT_STATE;
    }

    private boolean migrateOneLocation(int locationToMigrate, boolean trueOnDeletedEntry,
            NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        final long currentStateValue = alternateRightRowKey.getUnsafe(locationToMigrate);
        if (isStateEmpty(currentStateValue)) {
            return false;
        }
        if (isStateDeleted(currentStateValue)) {
            alternateEntries--;
            alternateRightRowKey.set(locationToMigrate, EMPTY_RIGHT_STATE);
            return trueOnDeletedEntry;
        }
        final float k0 = alternateKeySource0.getUnsafe(locationToMigrate);
        final int hash = hash(k0);
        int destinationTableLocation = hashToTableLocation(hash);
        long candidateState;
        while (!isStateEmpty(candidateState = mainRightRowKey.getUnsafe(destinationTableLocation)) && !isStateDeleted(candidateState)) {
            destinationTableLocation = nextTableLocation(destinationTableLocation);
        }
        mainKeySource0.set(destinationTableLocation, k0);
        mainRightRowKey.set(destinationTableLocation, currentStateValue);
        mainLeftRowSet.set(destinationTableLocation, alternateLeftRowSet.getUnsafe(locationToMigrate));
        alternateLeftRowSet.set(locationToMigrate, null);
        final long cookie  = alternateModifiedTrackerCookieSource.getUnsafe(locationToMigrate);
        mainModifiedTrackerCookieSource.set(destinationTableLocation, cookie);
        alternateModifiedTrackerCookieSource.set(locationToMigrate, -1L);
        modifiedSlotTracker.moveTableLocation(cookie, locationToMigrate, mainInsertMask | destinationTableLocation);;
        alternateRightRowKey.set(locationToMigrate, EMPTY_RIGHT_STATE);
        if (!isStateDeleted(candidateState)) {
            numEntries++;
        }
        alternateEntries--;
        return true;
    }

    @Override
    protected int rehashInternalPartial(int entriesToRehash,
            NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        int rehashedEntries = 0;
        while (rehashPointer > 0 && rehashedEntries < entriesToRehash) {
            if (migrateOneLocation(--rehashPointer, false, modifiedSlotTracker)) {
                rehashedEntries++;
            }
        }
        return rehashedEntries;
    }

    @Override
    protected void adviseNewAlternate() {
        this.mainKeySource0 = (ImmutableFloatArraySource)super.mainKeySources[0];
        this.alternateKeySource0 = (ImmutableFloatArraySource)super.alternateKeySources[0];
    }

    @Override
    protected void clearAlternate() {
        super.clearAlternate();
        this.alternateKeySource0 = null;
    }

    @Override
    protected void migrateFront(NaturalJoinModifiedSlotTracker modifiedSlotTracker) {
        int location = 0;
        while (migrateOneLocation(location++, true, modifiedSlotTracker) && location < alternateTableSize);
    }

    @Override
    protected void rehashInternalFull(final int oldSize) {
        final float[] destKeyArray0 = new float[tableSize];
        final long[] destState = new long[tableSize];
        Arrays.fill(destState, EMPTY_RIGHT_STATE);
        final float [] originalKeyArray0 = mainKeySource0.getArray();
        mainKeySource0.setArray(destKeyArray0);
        final long [] originalStateArray = mainRightRowKey.getArray();
        mainRightRowKey.setArray(destState);
        final Object [] oldLeftRowSet = mainLeftRowSet.getArray();
        final Object [] destLeftRowSet = new Object[tableSize];
        mainLeftRowSet.setArray(destLeftRowSet);
        final long [] oldModifiedCookie = mainModifiedTrackerCookieSource.getArray();
        final long [] destModifiedCookie = new long[tableSize];
        mainModifiedTrackerCookieSource.setArray(destModifiedCookie);
        for (int sourceBucket = 0; sourceBucket < oldSize; ++sourceBucket) {
            final long currentStateValue = originalStateArray[sourceBucket];
            if (isStateEmpty(currentStateValue)) {
                continue;
            }
            final float k0 = originalKeyArray0[sourceBucket];
            final int hash = hash(k0);
            final int firstDestinationTableLocation = hashToTableLocation(hash);
            int destinationTableLocation = firstDestinationTableLocation;
            while (true) {
                if (isStateEmpty(destState[destinationTableLocation])) {
                    destKeyArray0[destinationTableLocation] = k0;
                    destState[destinationTableLocation] = originalStateArray[sourceBucket];
                    destLeftRowSet[destinationTableLocation] = oldLeftRowSet[sourceBucket];
                    destModifiedCookie[destinationTableLocation] = oldModifiedCookie[sourceBucket];
                    break;
                }
                destinationTableLocation = nextTableLocation(destinationTableLocation);
                Assert.neq(destinationTableLocation, "destinationTableLocation", firstDestinationTableLocation, "firstDestinationTableLocation");
            }
        }
    }
}
