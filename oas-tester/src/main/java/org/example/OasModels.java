package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OasModels {

    public static class FieldDetails {
        public String jaywayPath;
        public String attrType; // Populated only for ATTR (e.g., "Special" or "Normal")

        public FieldDetails(String jaywayPath, String attrType) {
            this.jaywayPath = jaywayPath;
            this.attrType = attrType;
        }

        @Override
        public String toString() {
            return attrType != null ? jaywayPath + " (" + attrType + ")" : jaywayPath;
        }
    }

    public static class BucketData {
        public List<Object> requestFields = new ArrayList<>();
        public List<Object> responseFields = new ArrayList<>();
        public List<Object> uriParam = new ArrayList<>();
        public List<Object> queryParam = new ArrayList<>();
    }

    public static class OasFields {
        // Maps FIELDSPECIFICNAME (ATTR, MASKED, RENDER) to its 4 buckets
        public Map<String, BucketData> fieldSpecificBuckets = new HashMap<>();

        public OasFields() {
            fieldSpecificBuckets.put("ATTR", new BucketData());
            fieldSpecificBuckets.put("MASKED", new BucketData());
            fieldSpecificBuckets.put("RENDER", new BucketData());
        }
    }
}