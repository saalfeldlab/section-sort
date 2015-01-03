package org.janelia.sort.tsp.conversion;

public class SimilarityToDistanceSigmoid implements
		SimilarityToDistanceInterface {
	
	private final double factor;
	private final double summand;
	private final double nanReplacement;

	/**
	 * @param factor
	 * @param summand
	 * @param nanReplacement
	 */
	public SimilarityToDistanceSigmoid(final double factor, final double summand,
			final double nanReplacement) {
		super();
		this.factor = factor;
		this.summand = summand;
		this.nanReplacement = nanReplacement;
	}
	
	

	/**
	 * @param factor
	 */
	public SimilarityToDistanceSigmoid( final double factor ) {
		this( factor, 0.0, 0.0 );
	}


	@Override
	public double convert(final double similarity) {
		if ( Double.isNaN( similarity ) )
            return this.nanReplacement;
		else {
			final double absDiff = Math.abs( 1.0 - similarity );
			return this.factor * 1.0 / ( this.summand + Math.exp( -absDiff ) );
		}
	}

}
