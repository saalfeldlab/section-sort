/**
 * 
 */
package org.janelia.sort.tsp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
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
	
	
	/**
	 * Clean similarity matrix from "empty" sections 
	 * @param matrix input matrix
	 * @param removedIndices output parameter for removed row/column indices
	 * @param keptIndices output parameter for remaining row/column indices
	 * @return matrix without holes. If the original matrix does not have any holes, return original matrix.
	 */
	public static < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval< T > cleanMatrix(
			final RandomAccessibleInterval< T > matrix,
			final ArrayList< Long > removedIndices,
			final ArrayList< Long > keptIndices
			)
	{
		return cleanMatrix(matrix, removedIndices, keptIndices, new ArrayImgFactory<T>() );
	}
	
	/**
	 * Clean similarity matrix from "empty" sections 
	 * @param matrix input matrix
	 * @param removedIndices output parameter for removed row/column indices
	 * @param keptIndices output parameter for remaining row/column indices
	 * @param factory ImgFactory used for creating output matrix.
	 * @return matrix without holes. If the original matrix does not have any holes, return original matrix.
	 */
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
		// loop over all rows and
		// if row is entirely NaN/0.0:
		//    store index for removal
		// else:
		//    store index for result matrix
		for ( long i = 0; i < n; ++i ) {
			final Cursor<T> row = Views.flatIterable( Views.hyperSlice( matrix, 1, i ) ).cursor();
			boolean isBad = true;
			while ( row.hasNext() ) {
				final double val = row.next().getRealDouble();
				// as soon as a non-zero value is also not NaN, break and store index
				if ( !Double.isNaN( val ) && val != 0.0 ) {
					isBad = false;
					keptIndices.add( i );
					break;
				}
			}
			// if only zero or NaN values, remove index
			if ( isBad ) {
				removedIndices.add( i );
			}
		}
		
		
		// if nothing needs to be removed, return original matrix, else create matrix w/o removed indices
		if ( removedIndices.size() > 0 ) {
			final long[] newDimension = new long[] { n - removedIndices.size(), n - removedIndices.size() };
			final Img<T> result     = factory.create( newDimension, matrix.randomAccess().get() );
			final Cursor<T> c       = Views.flatIterable( result ).cursor();
			final RandomAccess<T> r = matrix.randomAccess();
			while( c.hasNext() ) {
				c.fwd();
				// get coordinates of target images and look up the corresponding indices of the original matrix
				// in the list of saved coordinates (keptIndices)
				final Long xTrans = keptIndices.get( c.getIntPosition( 0 ) );
				final Long yTrans = keptIndices.get( c.getIntPosition( 1 ) );
				r.setPosition( xTrans, 0 );
				r.setPosition( yTrans, 1 );
				// write value of the corresponding position within the old matrix into the current position
				// of the result matrix
				c.get().set( r.get() );
			}
			return result;
		}
		else {
			return matrix;
		}
		
	}
	
	
	public static void runConcordeTSPSolverWithDefaultConcorde( 
			final String inputFileName, 
			final String outputFileName
			) throws IOException {
		runConcordeTSPSolverWithDefaultConcorde( inputFileName, outputFileName, "" );
	}
	
	
	public static void runConcordeTSPSolverWithDefaultConcorde( 
			final String inputFileName, 
			final String outputFileName, 
			final String additionalArgument 
			) throws IOException {
		runConcordeTSPSolver( "concorde", inputFileName, outputFileName, additionalArgument );
	}
	
	
	public static void runConcordeTSPSolver( 
			final String concordeExecutablePath, 
			final String inputFileName, 
			final String outputFileName
			) throws IOException {
		runConcordeTSPSolver( concordeExecutablePath, inputFileName, outputFileName, "" );
	}
	
	
	public static void runConcordeTSPSolver( 
			final String concordeExecutablePath, 
			final String inputFileName, 
			final String outputFileName, 
			final String additionalArgument
			) throws IOException {
		final String command = String.format( "%s %s -o %s %s", concordeExecutablePath, additionalArgument, outputFileName, inputFileName );
		Runtime.getRuntime().exec( command );
	}
	
	public static int[] tspResultToArray( final String tspResultFileName, final int n ) {
		return tspResultToArray(tspResultFileName, n, Charset.defaultCharset() );
	}
	
	public static int[] tspResultToArray( final String tspResultFileName, final int n, final Charset cs ) {
		final int[] result = new int[ n ];
		try {
			final List<String> lines = Files.readAllLines( Paths.get( tspResultFileName), cs);
			final int nVariables = Integer.parseInt( lines.get( 0 ) );
			if ( nVariables != n+1 )
				return null;
			int targetIndex = 0;
			for ( int listIndex = 1; listIndex < lines.size(); ++listIndex ) {
				final String[] currSplit = lines.get( listIndex ).split( " " );
				for ( final String s : currSplit ) {
					final int val = Integer.parseInt( s );
					if ( val == n )
						continue;
					result[targetIndex] = val;
					++targetIndex;
				}
			}
		} catch (final IOException e) {
			return null;
		}
		return result;
	}
	
	
	public static void main(final String[] args) throws IOException {
		
	}
	
}







