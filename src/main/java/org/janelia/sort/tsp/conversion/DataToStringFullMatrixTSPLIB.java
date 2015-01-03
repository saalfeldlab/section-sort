/**
 * 
 */
package org.janelia.sort.tsp.conversion;


/**
 * @author Philipp Hanslovsky <hanslovskyp@janelia.hhmi.org>
 *
 */
public class DataToStringFullMatrixTSPLIB implements DataToStringInterface {
	
	private int n;
	private int dummyIndex;
	
	private final String baseString;
	
	/**
	 * @param baseString
	 */
	public DataToStringFullMatrixTSPLIB(final String baseString) {
		super();
		this.baseString = baseString;
	}
	

	/**
	 * 
	 */
	public DataToStringFullMatrixTSPLIB() {
		this(DataToStringFullMatrixTSPLIB.createBaseStringWithComment( "" )	);
	}

	public static String createBaseStringWithComment( final String comment ) {
		return "NAME: SORT" + System.getProperty("line.separator")
		+ "TYPE: TSP" + System.getProperty("line.separator")
		+ "COMMENT: " + comment + System.getProperty("line.separator")
		+ "DIMENSION: %d" + System.getProperty("line.separator")
		+ "EDGE_WEIGHT_TYPE: EXPLICIT" + System.getProperty("line.separator")
		+ "EDGE_DATA_FORMAT: EDGE_LIST" + System.getProperty("line.separator")
		+ "EDGE_WEIGHT_FORMAT: FULL_MATRIX" + System.getProperty("line.separator")
		+ "NODE_COORD_TYPE: NO_COORDS" + System.getProperty("line.separator")
		+ "DISPLAY_DATA_TYPE: NO_DISPLAY" + System.getProperty("line.separator")
		+ "EDGE_WEIGHT_SECTION" + System.getProperty("line.separator");
	}

	private double[][] similarities;
	private double[] currentRow;
	
	private final static double DUMMY_VALUE = 0.0;

	@Override
	public void initialize(final int n) {
		this.n = n + 1;
		this.dummyIndex = n;
		similarities = new double[ this.n ][ this.n ];
		currentRow = similarities[0];
	}

	@Override
	public void addSimilarity(final int index1, final int index2, final double value) {
		similarities[index1][index2] = value;
	}

	@Override
	public void addDummy(final int index, final double value) {
		similarities[ index ][ this.dummyIndex ] = DataToStringFullMatrixTSPLIB.DUMMY_VALUE;
	}

	@Override
	public String close() {
		final String base      = String.format( this.baseString, this.n );
		final StringBuilder sb = new StringBuilder( base );
		for ( int i = 0; i < dummyIndex; ++i ) {
			final double[] row = similarities[i];
			for ( int j = 0; j < dummyIndex; ++j ) { 
				sb.append( (int)row[j] ).append( " " );
			}
			sb.append( (int)row[dummyIndex] ).append( "\n" );
		}
		final double[] row = similarities[dummyIndex];
		for ( int j = 0; j < dummyIndex; ++j ) {
			sb.append( (int)row[j] ).append(" ");
		}
		sb.append((int)row[dummyIndex]).append("\n").append("EOF");
		return sb.toString();
	}

}
