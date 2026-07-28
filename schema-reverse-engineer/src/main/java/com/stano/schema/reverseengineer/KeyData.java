package com.stano.schema.reverseengineer;

import java.util.List;

public record KeyData(
    String tableName, String indexName, boolean nonUnique, List<KeyDataColumn> columns) {}
