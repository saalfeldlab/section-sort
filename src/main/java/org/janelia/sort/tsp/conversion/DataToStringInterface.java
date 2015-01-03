/**
 * 
 */
package org.janelia.sort.tsp.conversion;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public interface DataToStringInterface {
	
	public void initialize( int n );
	
	public void addSimilarity( int index1, int index2, double value );
	
	public void addDummy( int index, double value );
	
	public String close();
	
}
