package com.stano.schema.model;

/** The kind of consistency check performed when verifying aggregated/derived data. */
public enum VerifyType {
  DATE, // only 1 allowed
  SUM,
  GROUP_BY
}
