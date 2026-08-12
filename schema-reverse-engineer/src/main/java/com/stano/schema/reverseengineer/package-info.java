/**
 * Reverse-engineers a live database's schema into the vendor-neutral XML schema definition format
 * used by the rest of this library.
 *
 * <p>{@link com.stano.schema.reverseengineer.SchemaReverseEngineer} is the command-line entry
 * point: it connects to a database via JDBC, reads its metadata into a {@link
 * com.stano.schema.model.Schema} model, and writes that model out as XML using {@link
 * com.stano.schema.reverseengineer.SchemaWriter}.
 */
package com.stano.schema.reverseengineer;
