/**
 * Concrete {@link com.stano.schema.diff.SchemaChange} implementations produced by {@link
 * com.stano.schema.diff.SchemaDiffEngine}.
 *
 * <p>Each class is a small, immutable value object representing one structural difference between
 * two schemas: an addition ({@code Add*Change}), a removal ({@code Drop*Change}), a
 * definition-level change ({@code ModifyColumnChange}), or a rename ({@code RenameColumnChange},
 * {@code RenameTableChange}) covering tables, columns, keys, constraints, relations, views,
 * functions, and procedures. Instances carry only the data needed to describe or apply the change —
 * such as the affected table's name and, where relevant, the full {@code com.stano.schema.model}
 * definition involved.
 */
package com.stano.schema.diff.change;
