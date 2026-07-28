package com.stano.schema.reverseengineer;

import java.util.List;
import java.util.Map;

public record KeyList(Map<String, List<KeyData>> keyData) {}
