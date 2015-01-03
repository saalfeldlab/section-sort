/**
 * 
 */
package org.janelia.similarity;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.AffineModel2D;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * @author Stephan Saalfeld <saalfelds@janelia.hhmi.org>
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class SiftPairwiseSimilarity {
	
	/**
	 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
	 * Helper class that holds parameters for pairwise sift feature extraction
	 */
	public static class Param {
		public FloatArray2DSIFT.Param p;
		public int maxSteps;
		public float rod;
		public float maxEpsilon;
		public float minInlierRatio;
		public int minNumInliers;
		public int nThreads;
		public boolean showProgress;
		public int range;
	}
	
	public static Param generateDefaultParameters() {
		final Param p = new Param();
		final FloatArray2DSIFT.Param siftp = new FloatArray2DSIFT.Param();
		siftp.fdSize = 4;
		siftp.maxOctaveSize = 1024;
		siftp.minOctaveSize = 1000;
		
		p.p              = siftp;
		p.maxSteps       = 3;
		p.rod            = 0.92f;
		p.maxEpsilon     = 50f;
		p.minInlierRatio = 0.05f;
		p.minNumInliers  = 10;
		p.nThreads       = Runtime.getRuntime().availableProcessors();
		p.showProgress   = true;
		p.range          = 50;
		
		return p;
	}
	
	
	private final Param p;
	
	
	/**
	 * @param p
	 * helper
	 */
	public SiftPairwiseSimilarity(final Param p) {
		super();
		this.p = p;
	}


	/**
	 * @param ijSIFT
	 * @param ip
	 * @return
	 * helper
	 */
	public static ArrayList< Feature > extract( final SIFT ijSIFT, final ImageProcessor ip ) {
		final ArrayList<Feature> features = new ArrayList< Feature >();
		ijSIFT.extractFeatures( ip, features );
		return features;
	}
	
	
	/**
	 * @param n
	 * @param featuresList
	 * @return
	 * helper
	 */
	public static FloatProcessor generateMatrix( final int n, final ArrayList< List< Feature > > featuresList ) {
		final FloatProcessor matrix = new FloatProcessor( n, n );
		matrix.add( Double.NaN );
		for ( int i = 0; i < n; ++i ) {
			if ( featuresList.get( i ).size() > 0 )
				matrix.setf( i, i, 1.0f );
			else
				matrix.setf( i, i, 0.0f );
		}
		matrix.setMinAndMax( 0.0, 1.0 );
		return matrix;
	}
	
	
	/**
	 * @param model
	 * @param features1
	 * @param features2
	 * @return
	 * helper
	 */
	public < M extends Model< M > > double match( final M model, final List< Feature > features1, final List< Feature > features2 ) {
		
		final ArrayList<PointMatch> candidates = new ArrayList< PointMatch >();
		final ArrayList<PointMatch> inliers = new ArrayList< PointMatch >();
		double inlierRatio = 0.0;
		
		if ( features1.size() > 0 && features2.size() > 0 ) {
			FeatureTransform.matchFeatures( features1, features2, candidates, p.rod );
			
			boolean modelFound = false;
			try {
				modelFound = model.filterRansac(
					candidates,
					inliers,
					1000,
					p.maxEpsilon,
					p.minInlierRatio,
					p.minNumInliers,
					3);
			}
			catch (final NotEnoughDataPointsException e) {
				modelFound = false;
			}
		
			if (modelFound)
				inlierRatio = (double)inliers.size() / candidates.size();
		}
		
		return inlierRatio;
	}
	
	
	public ArrayList< List< Feature > > extractFeatures( final ImagePlus imp ) {
		final ImageStack stack = imp.getStack();
		final int n = stack.getSize();
		final ArrayList< List< Feature > > featuresList = new ArrayList< List < Feature > >( n );
		for ( int k = 0; k < n; ++k )
			featuresList.add( null );
		final AtomicInteger i = new AtomicInteger(0);
		final ArrayList<Thread> threads = new ArrayList< Thread >();
		for (int t = 0; t < p.nThreads; ++t) {
			final Thread thread = new Thread(
				new Runnable(){
					@Override
					public void run(){
						final FloatArray2DSIFT sift = new FloatArray2DSIFT( p.p );
						final SIFT ijSIFT = new SIFT(sift);
						for (int k = i.getAndIncrement(); k < n; k = i.getAndIncrement()) {
							final ArrayList< Feature > features = extract(ijSIFT, stack.getProcessor(k + 1));
							IJ.log( k + ": " + features.size() + " features extracted" );
							featuresList.set( k, features );
						}
					}
				}
			);
			threads.add(thread);
			thread.start();
		}
		for (final Thread t : threads)
			try {
				t.join();
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return featuresList;
	}
	
	
	public < M extends Model< M > > ImagePlus matchFeaturesAndCalculateSimilarities( 
			final ArrayList< List< Feature > > featuresList,
			final M model) {
		final ArrayList<Thread> threads = new ArrayList< Thread >();
		final int n = featuresList.size();
		final FloatProcessor matrix = generateMatrix( n, featuresList );
		final ImagePlus impMatrix = new ImagePlus("inlier ratio matrix", matrix);
		if ( p.showProgress )
			impMatrix.show();
		for (int i = 0; i < n; ++i) {
			final int fi = i;
			final List<Feature> f1 = featuresList.get( fi );
			final AtomicInteger j = new AtomicInteger(fi + 1);
			for (int t = 0; t < p.nThreads; ++t) {
				final Thread thread = new Thread(
					new Runnable(){
						@Override
						public void run(){
							for (int k = j.getAndIncrement(); k < n && k < fi + p.range; k = j.getAndIncrement()) {
								final List<Feature> f2 = featuresList.get( k );
								final float inlierRatio = (float)match( model, f1, f2 );
								matrix.setf(fi, k, inlierRatio);
								matrix.setf(k, fi, inlierRatio);
								impMatrix.updateAndDraw();
							}
						}
					}
				);
				threads.add(thread);
				thread.start();
			}
			for (final Thread t : threads)
				try {
					t.join();
				} catch (final InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return impMatrix;
	}
	
	public < M extends Model< M > > ImagePlus calculateSimilarityMatrix( final ImagePlus imp, final M model ) {
		final ArrayList<List<Feature>> featuresList = extractFeatures( imp );
		final ImagePlus impMatrix = matchFeaturesAndCalculateSimilarities( featuresList, model );
		return impMatrix;
	}
	
	
	public static void main(final String[] args) {
		
		final String filename = System.getProperty( "user.dir" ) + "/test_data_features.tif";
		final ImagePlus imp = new ImagePlus( filename );
		new ImageJ();
		imp.show();
		final FloatArray2DSIFT.Param siftp = new FloatArray2DSIFT.Param();
		siftp.fdSize = 4;
		siftp.maxOctaveSize = 1024;
		siftp.minOctaveSize = 64;
		final Param p = generateDefaultParameters();
		p.range        = 5;
		p.showProgress = true;
		p.p = siftp;
		final SiftPairwiseSimilarity SPS = new SiftPairwiseSimilarity( p );
		final AffineModel2D model = new AffineModel2D();
		final ImagePlus impMatrix = SPS.calculateSimilarityMatrix(imp, model);
	}
	
}
