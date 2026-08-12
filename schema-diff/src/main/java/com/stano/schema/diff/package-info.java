/**
 * Compares two {@link com.stano.schema.model.Schema} objects and produces a {@link
 * com.stano.schema.diff.ChangeSet} describing the structural differences between them.
 *
 * <p>{@link com.stano.schema.diff.SchemaDiffEngine} is the entry point: it walks the tables,
 * columns, keys, constraints, relations, views, functions, and procedures of an old and a new
 * schema and reports what was added, dropped, or modified as a sequence of {@link
 * com.stano.schema.diff.SchemaChange} instances, in an order suitable for applying the changes to a
 * live database. The concrete change types themselves live in the sibling {@link
 * com.stano.schema.diff.change} package.
 */
package com.stano.schema.diff;
