/**
 * SQL generation entry point.
 *
 * <p>This package contains {@link com.stano.schema.gensql.GenSQL}, the main entry point for turning
 * a parsed {@link com.stano.schema.model.Schema} into dialect-specific SQL DDL. It can be used as a
 * plain library class or run as a command-line tool.
 *
 * <p>The dialect-agnostic generation framework used by {@code GenSQL} lives in the {@link
 * com.stano.schema.gensql.impl.common} sub-package, with concrete per-dialect implementations in
 * {@code com.stano.schema.gensql.impl.postgresql}, {@code com.stano.schema.gensql.impl.sqlserver},
 * and {@code com.stano.schema.gensql.impl.h2}.
 */
package com.stano.schema.gensql;
