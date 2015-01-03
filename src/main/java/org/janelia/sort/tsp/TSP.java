/**
 * 
 */
package org.janelia.sort.tsp;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.janelia.sort.tsp.conversion.DataToStringInterface;
import org.janelia.sort.tsp.conversion.SimilarityToDistanceInterface;

/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 * Collection of convenience functions for transferring a similarity matrix into a
 * traveling salesman problem (TSP) and rewriting the matrix according to the result.
 * 
 */
public class TSP {
	
	/**
	 * @param matrix 2D {@link RandomAccessibleInterval} containing the similarity matrix
	 * @param converter {@link DataToStringInterface} determining the rule for converting data to string
	 * @param similarityToDistance {@link SimilarityToDistanceInterface} for converting similarities into distances
	 * @return {@link String} that contains all the information necessary for TSP solver
	 */
	public static < T extends RealType<T> & NativeType< T > > String convertMatrix( 
			final RandomAccessibleInterval< T > matrix,
			final DataToStringInterface converter,
			final SimilarityToDistanceInterface similarityToDistance ) {
		
		assert matrix.numDimensions() == 2: "Need two-dimensional matrix";
		assert matrix.dimension( 0 ) == matrix.dimension( 1 ): "Matrix needs to be quadratic";
		
		final long n = matrix.dimension( 0 );
		
		// initialize converter with number of nodes
		converter.initialize( (int) n );
		
		// for each row convert each column and 
		for ( int i = 0; i < n; ++i ) {
			final IntervalView<T> row = Views.hyperSlice( matrix, 1, i );
			final Cursor<T> r         = Views.flatIterable( row ).cursor();
			for ( int j = 0; r.hasNext(); ++j ) {
				converter.addSimilarity( i, j, similarityToDistance.convert( r.next().getRealDouble() ) );
			}
			// add zero distance dummy to transfer TSP into sorting problem
			converter.addDummy( i, 0.0 );
		}
		
		// close out converter and obtain string
		return converter.close();
		
	}
	

}