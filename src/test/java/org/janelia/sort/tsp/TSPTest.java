package org.janelia.sort.tsp;

import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import org.janelia.sort.tsp.conversion.DataToStringFullMatrixTSPLIB;
import org.janelia.sort.tsp.conversion.SimilarityToDistanceSigmoid;
import org.junit.Assert;
import org.junit.Test;

public class TSPTest {
	
	final String path                                      = "src/test/java/org/janelia/sort/tsp/AVG_inlier ratio matrix-excerpt.tif";
	final DataToStringFullMatrixTSPLIB converter           = new DataToStringFullMatrixTSPLIB();
	final SimilarityToDistanceSigmoid similarityToDistance = new SimilarityToDistanceSigmoid( 1000.0, 0.0, 1000000.0 );
	final String referencePath                             = "src/test/java/org/janelia/sort/tsp/excerpt-tsp.dat";
	
	final String matrixWithHolesPath    = "src/test/java/org/janelia/sort/tsp/AVG_inlier ratio matrix.tif";
	final String matrixWithoutHolesPath = "src/test/java/org/janelia/sort/tsp/AVG-no-empty.tif";
	
	// TODO for now, concorde needs to be present in some directory in PATH env variable;
	// TODO set PATH variable in eclipse by 
	// TODO Run -> Run Configurations -> Environment -> New { "Variable" : "PATH", "Value" : "/path/to/directory/containing/concorde" }
	// TODO this might not be platform robust and needs to be replaced by sth. else
	final String concordeExecutablePath = "concorde";
	final String inputFileName          = "src/test/java/org/janelia/sort/tsp/excerpt-tsp.dat";
	final String outputFileName         = "src/test/java/org/janelia/sort/tsp/excerpt-result.txt";
	final String referenceMatrixPath    = "src/test/java/org/janelia/sort/tsp/excerpt-result.tif";
	
	final int concordeSeed     = 10;
	// final int[] orderReference = new int[]{0, 21, 20, 19, 18, 17, 7, 6, 5, 10, 9, 8, 13, 12, 11, 16, 15, 14, 4, 3, 2, 1};
	final int[] orderReference = new int[]{0, 1, 2, 3, 4, 14, 15, 16, 11, 12, 13, 8, 9, 10, 5, 6, 7, 17, 18, 19, 20, 21};
	

	@Test
	public void testConvertMatrix() {
		final ImagePlus imp      = new ImagePlus( path );
		final Img<FloatType> img = ImageJFunctions.wrapFloat( imp );
		final String s           = TSP.convertMatrix( img, converter, similarityToDistance );
		final String[] split     = s.split( System.getProperty("line.separator") );
		
		LineNumberReader lnr;
		try {
			lnr = new LineNumberReader(new FileReader(new File( referencePath )));
			lnr.skip(Long.MAX_VALUE);
			Assert.assertEquals( split.length, lnr.getLineNumber()+1 );
			lnr.close();
		} catch (final FileNotFoundException e) {
			Assert.fail( "Could not find file " + referencePath );
		} catch ( final IOException e ) {
			Assert.fail( "Error accessing file " + referencePath );
		}
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader( new File( referencePath ) ) );
		} catch (final FileNotFoundException e1) {
			br = null;
			Assert.fail( "Could not find file " + referencePath );
		}
		
		String line = null;
		try {
			for (int index = 0; (line = br.readLine()) != null; ++index )  
			{
				Assert.assertEquals( split[index], line );
			}
		} catch (final IOException e) {
			Assert.fail( "Error accessing file " + referencePath );
		} 
		
	}
	
	@Test
	public void testCleanMatrix() {
		final FloatImagePlus<FloatType> matrixWithHoles             = ImagePlusAdapter.wrapFloat( new ImagePlus( matrixWithHolesPath ) );
		final FloatImagePlus<FloatType> matrixWithoutHolesReference = ImagePlusAdapter.wrapFloat( new ImagePlus( matrixWithoutHolesPath ) );
		

		
		final ArrayList<Long> keptIndices              = new ArrayList< Long >();
		final ArrayList<Long> removedIndices           = new ArrayList< Long >();
		final ArrayList<ArrayList<Long>> badSuccessors = new ArrayList< ArrayList< Long > >();
		final boolean[] sectionStatus                  = new boolean[ (int) matrixWithHoles.dimension( 0 ) ];
		
		final RandomAccessibleInterval<FloatType> matrixWithoutHoles = TSP.cleanMatrix( matrixWithHoles, removedIndices, keptIndices, badSuccessors, sectionStatus );
		
		Assert.assertEquals( matrixWithoutHolesReference.dimension( 0 ), keptIndices.size() );
		
		Assert.assertEquals( matrixWithHoles.dimension( 0 ), keptIndices.size() + removedIndices.size() );
		
		for ( int d = 0; d < matrixWithoutHoles.numDimensions(); ++d )
			Assert.assertEquals( matrixWithoutHoles.dimension( d ), matrixWithoutHolesReference.dimension( d ) );
		
		final Cursor<FloatType> ref = Views.flatIterable( matrixWithoutHolesReference ).cursor();
		final Cursor<FloatType> res = Views.flatIterable( matrixWithoutHoles ).cursor();
		while( ref.hasNext() ) {
			Assert.assertEquals( res.next(), ref.next() );
		}
	}
	
	@Test
	public void testTSPSolver() {
		
		try {
			TSP.runConcordeTSPSolver( concordeExecutablePath, inputFileName, outputFileName, String.format("-s %d", concordeSeed ) );
		} catch (final IOException e) {
			Assert.fail();
		}
		final int[] result = TSP.tspResultToArray( outputFileName, 22 );
		Assert.assertArrayEquals( orderReference, result );
	}
	
	
	@Test
	public void testRearrangement() {
		
		final FloatImagePlus<FloatType> input            = ImagePlusAdapter.wrapFloat( new ImagePlus( path ) );
		final FloatImagePlus<FloatType> reference        = ImagePlusAdapter.wrapFloat( new ImagePlus( referenceMatrixPath) );
		final RandomAccessibleInterval<FloatType> output = TSP.rearrangeMatrix( input, orderReference );
		
		final Cursor<FloatType> o = Views.flatIterable( output ).cursor();
		final Cursor<FloatType> r = Views.flatIterable( reference ).cursor();
		
		while( o.hasNext() ) {
			Assert.assertEquals( r.next(), o.next() );
		}
		
	}

}
