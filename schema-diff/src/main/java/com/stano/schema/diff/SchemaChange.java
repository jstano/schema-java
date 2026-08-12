package com.stano.schema.diff;

/**
 * Marker interface implemented by every structural change type in the {@code
 * com.stano.schema.diff.change} package (e.g. {@code AddTableChange}, {@code DropColumnChange},
 * {@code ModifyColumnChange}). It carries no behavior of its own; it exists so that {@link
 * SchemaDiffEngine} and {@link ChangeSet} can treat all change types uniformly as members of a
 * single {@link ChangeSet}, leaving callers to distinguish between concrete change types (e.g. via
 * {@code instanceof}) when applying or rendering them.
 */
public interface SchemaChange {}
