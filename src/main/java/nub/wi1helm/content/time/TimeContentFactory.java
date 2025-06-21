// src/main/java/nub/wi1helm/content/time/TimeContentFactory.java
package nub.wi1helm.content.time;

import net.minestom.server.instance.Instance; // You'll likely need the player's instance to create content

/**
 * An interface for creating new instances of TimeContent.
 * Each factory represents a 'blueprint' for a type of content.
 */
public interface TimeContentFactory {
    /**
     * Creates a new, unique instance of TimeContent.
     * This method will be called for each player that should receive this content.
     *
     * @param instance The Minestom Instance where the content will exist.
     * This is crucial for entities/blocks to be properly placed.
     * @return A new instance of TimeContent.
     */
    TimeContent create(Instance instance);
}