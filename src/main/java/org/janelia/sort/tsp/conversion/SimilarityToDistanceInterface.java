/**
 * 
 */
package org.janelia.sort.tsp.conversion;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface SimilarityToDistanceInterface {
	double convert( double similarity );
}