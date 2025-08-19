package com.example.finalprojectappraisal.model;

import static com.example.finalprojectappraisal.utils.Constants.VALUE_EXISTS;
import static com.example.finalprojectappraisal.utils.Constants.VALUE_NOT_EXISTS;

import java.util.Objects;

/**
 * The `AiImageTag` class represents the tags and features detected by the AI system in an image.
 * The class supports two main types of features:
 * 1. Binary features (exists/does not exist) such as elevator, storage, parking.
 * 2. Categorized features with various values such as floor type, kitchen condition, etc.
 */
class AiImageTag {
    private String type;      // Feature type (e.g., floorType, kitchenState)
    private String value;     // Feature value (e.g., ceramic, modern)
    private double confidence; // Confidence level of the AI recognition (between 0 and 1)
    private boolean isBinary; // Whether the tag is binary (exists/does not exist)

    /**
     * Constructor to create an AI tag with a value and a flag indicating if it's binary.
     *
     * @param type The feature type (e.g., floorType, kitchenState)
     * @param value The feature value (e.g., ceramic, modern)
     * @param confidence The confidence level of the AI recognition (between 0 and 1)
     * @param isBinary Flag indicating if the tag is binary (true for binary tags)
     */
    public AiImageTag(String type, String value, double confidence, boolean isBinary) {
        // Validate confidence range
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }

        this.type = type;
        this.value = value;
        this.confidence = confidence;
        this.isBinary = isBinary;
    }

    /**
     * Constructor for creating a non-binary AI tag.
     */
    public AiImageTag(String type, String value, double confidence) {
        this(type, value, confidence, false); // Default non-binary tags
    }

    /**
     * Static method to create a binary feature tag (exists/not exists).
     *
     * @param type The feature type (e.g., elevator, storage)
     * @param exists Whether the feature exists or not
     * @param confidence The confidence level of the AI recognition
     * @return A new binary feature tag
     */
    public static AiImageTag createBinary(String type, boolean exists, double confidence) {
        String value = exists ? VALUE_EXISTS : VALUE_NOT_EXISTS;
        return new AiImageTag(type, value, confidence, true);
    }

    /**
     * Checks whether the feature exists (only for binary tags).
     *
     * @return true if the feature exists, false if it does not exist or if the tag is not binary
     */
    public boolean exists() {
        if (!isBinary) {
            return false;
        }
        return VALUE_EXISTS.equals(value); // Check if the value is "exists"
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public double getConfidence() {
        return confidence;
    }

    public boolean isBinary() {
        return isBinary;
    }

    // Setters
    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        this.confidence = confidence;
    }

    /**
     * Checks if the confidence level is above a certain threshold.
     *
     * @param threshold The confidence threshold
     * @return true if the confidence is higher than the threshold, false otherwise
     */
    public boolean isConfident(double threshold) {
        return this.confidence >= threshold;
    }

    /**
     * Compares equality between two AiImageTag objects.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiImageTag that = (AiImageTag) o;
        return isBinary == that.isBinary &&
                Objects.equals(type, that.type) &&
                Objects.equals(value, that.value);
    }

    /**
     * Generates a hash value for the AiImageTag object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, value, isBinary);
    }

    /**
     * Converts the AiImageTag to a JSON-like string for easy representation.
     */
    @Override
    public String toString() {
        return "{\"type\":\"" + type +
                "\",\"value\":\"" + value +
                "\",\"confidence\":" + confidence +
                ",\"isBinary\":" + isBinary + "}";
    }
}