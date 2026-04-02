package io.github.alejandrodorich.searchengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for cosine similarity.
 */
class CosineTests {
    
    VectorizedForwardIndex vectorizedForwardIndex = new VectorizedForwardIndex();


    @Test
    void equalVectors() {
        // Create two vectors with random positive double values
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.1, 0.2, 0.3, 0.4, 0.5 };

        // Check if the cosine similarity of the two equal vectors is 1.0
        assertEquals(1.0, vectorizedForwardIndex.calculateCosineSimilarity(vectorA, vectorB));
    }

    @Test
    void orthogonalVectors() {
        // Create two orthogonal vectors
        double[] vectorA = { 1.0, 0.0, 0.0 };
        double[] vectorB = { 0.0, 1.0, 0.0 };

        // Check if the cosine similarity of the two orthogonal vectors is 0.0
        assertEquals(0.0, vectorizedForwardIndex.calculateCosineSimilarity(vectorA, vectorB));
    }

    @Test
    void randomVectors() {
        // Create two random vectors
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };

        // Check if the cosine similarity of the two random positive vectors is between 0.0 and 1.0
        assertTrue(vectorizedForwardIndex.calculateCosineSimilarity(vectorA, vectorB) > 0.0);
        assertTrue(vectorizedForwardIndex.calculateCosineSimilarity(vectorA, vectorB) < 1.0);
    }

    @Test
    void specificResults() {
        // Create two vectors with specific values
        double[] vectorA = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        double[] vectorB = { 0.5, 0.4, 0.3, 0.2, 0.1 };

        // Check if the cosine similarity of the two orthogonal vectors is approximately 0.6364
        assertTrue(Math.abs(vectorizedForwardIndex.calculateCosineSimilarity(vectorA, vectorB) - 0.6364) < 0.0001);
    }
}
