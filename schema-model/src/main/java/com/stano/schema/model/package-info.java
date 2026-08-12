/**
 * Core, vendor-neutral domain model for a relational database schema.
 *
 * <p>Classes in this package represent the object graph produced when an XML schema definition file
 * (rooted at a {@code <database>} element) is parsed by {@code schema-parser}: {@link
 * com.stano.schema.model.Schema} at the root, containing {@link com.stano.schema.model.Table},
 * {@link com.stano.schema.model.View}, {@link com.stano.schema.model.Function}, {@link
 * com.stano.schema.model.Procedure}, {@link com.stano.schema.model.EnumType}, and {@link
 * com.stano.schema.model.OtherSql} definitions. Tables in turn own {@link
 * com.stano.schema.model.Column}s, {@link com.stano.schema.model.Key}s, {@link
 * com.stano.schema.model.Relation}s, {@link com.stano.schema.model.Trigger}s, {@link
 * com.stano.schema.model.Constraint}s, {@link com.stano.schema.model.InitialData}, and {@link
 * com.stano.schema.model.Aggregation} rollups.
 *
 * <p>This model is intentionally database-agnostic: it captures schema structure and intent, not
 * SQL syntax. Database-specific SQL DDL is produced from this model by the {@code
 * schema-sql-generator} module, which dispatches on the {@link com.stano.schema.model.DatabaseType}
 * enum and the {@link com.stano.schema.model.ForeignKeyMode} / {@link
 * com.stano.schema.model.BooleanMode} generation settings carried on {@link
 * com.stano.schema.model.Schema}. The classes here are plain, mostly-immutable Java objects with
 * getters (and, where mutation after construction is needed, setters), designed to be minimal in
 * their dependencies so they can be shared across the parser, SQL generator, installer, migration,
 * reverse-engineering, and diagram-generation modules.
 */
package com.stano.schema.model;
