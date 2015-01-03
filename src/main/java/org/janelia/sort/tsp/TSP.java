/**
 * 
 */
package org.janelia.sort.tsp;

import ij.ImagePlus;

import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
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
	
	
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > cleanMatrix(
			final RandomAccessibleInterval< T > matrix,
			final ArrayList< Long > removedIndices,
			final ArrayList< Long > keptIndices
			)
	{
		return cleanMatrix(matrix, removedIndices, keptIndices, new ArrayImgFactory<T>() );
	}
	
	
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > cleanMatrix(
			final RandomAccessibleInterval< T > matrix,
			final ArrayList< Long > removedIndices,
			final ArrayList< Long > keptIndices,
			final ImgFactory< T > factory
			)
	{
		assert matrix.numDimensions() == 2: "Need two-dimensional matrix";
		assert matrix.dimension( 0 ) == matrix.dimension( 1 ): "Matrix needs to be quadratic";
		
		final long n = matrix.dimension( 0 );
		
		removedIndices.clear(); // maybe do not call clear?
		keptIndices.clear(); // maybe do not call clear?
		for ( long i = 0; i < n; ++i ) {
			final Cursor<T> row = Views.flatIterable( Views.hyperSlice( matrix, 1, i ) ).cursor();
			boolean isBad = true;
			while ( row.hasNext() ) {
				final double val = row.next().getRealDouble();
				if ( !Double.isNaN( val ) && val != 0.0 ) {
					isBad = false;
					keptIndices.add( i );
					break;
				}
			}
			if ( isBad ) {
				removedIndices.add( i );
			}
		}
		
		if ( removedIndices.size() > 0 ) {
			final long[] newDimension = new long[] { n - removedIndices.size(), n - removedIndices.size() };
			final Img<T> result     = factory.create( newDimension, matrix.randomAccess().get() );
			final Cursor<T> c       = Views.flatIterable( result ).cursor();
			final RandomAccess<T> r = matrix.randomAccess();
			while( c.hasNext() ) {
				c.fwd();
				final Long xTrans = keptIndices.get( c.getIntPosition( 0 ) );
				final Long yTrans = keptIndices.get( c.getIntPosition( 1 ) );
				r.setPosition( xTrans, 0 );
				r.setPosition( yTrans, 1 );
				c.get().set( r.get() );
			}
			return result;
		}
		else {
			return matrix;
		}
		
	}
	
	public static void main(final String[] args) {
		final String fn = "src/test/java/org/janelia/sort/tsp/AVG_inlier ratio matrix-excerpt.tif";
		final ImagePlus imp = new ImagePlus( fn );
		final FloatImagePlus<FloatType> img = ImagePlusAdapter.wrapFloat( imp );
		final ArrayList<Long> removedIndices = new ArrayList< Long >();
		final ArrayList<Long> keptIndices    = new ArrayList< Long >();
		
		final RandomAccessibleInterval<FloatType> result = cleanMatrix( img, removedIndices, keptIndices );
		System.out.println( removedIndices );
		System.out.println( keptIndices );
	}
	

}