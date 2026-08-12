/**
 * Dialect-agnostic SQL generation framework.
 *
 * <p>This package defines the interfaces and support classes shared by all per-dialect DDL
 * generator implementations: the {@link com.stano.schema.gensql.impl.common.SQLGenerator} base
 * class that each dialect extends, the {@link
 * com.stano.schema.gensql.impl.common.SQLGeneratorFactory} used to select the correct generator for
 * a given {@link com.stano.schema.model.DatabaseType}, the {@link
 * com.stano.schema.gensql.impl.common.SQLGeneratorOptions} that bundle generation settings, and the
 * {@link com.stano.schema.gensql.impl.common.OutputMode} enum that controls which parts of a schema
 * are output.
 *
 * <p>Concrete generator implementations live in sibling sub-packages: {@code
 * com.stano.schema.gensql.impl.postgresql}, {@code com.stano.schema.gensql.impl.sqlserver}, and
 * {@code com.stano.schema.gensql.impl.h2}.
 */
package com.stano.schema.gensql.impl.common;
