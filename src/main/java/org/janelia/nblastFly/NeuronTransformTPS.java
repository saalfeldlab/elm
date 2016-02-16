package org.janelia.nblastFly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import jitk.spline.XfmUtils;

import org.janelia.io.LandmarkTableModelLight;
import org.janelia.io.NeuronReaderWriterSWC;
import org.janelia.struct.NeuronNode;

public class NeuronTransformTPS
{

	public static final String resMovingTag = "-rm";
	public static final String resTargetTag = "-rt";

	public static void main( String[] args ) throws IOException
	{
		String landmarkFile = args[ 0 ];
		String swcPath = args[ 1 ];
		
		int k = 2;
		String swcPathOut;
		if( args.length >= (k+1) && !isResTag( args[ k ]))
		{
			swcPathOut = args[ k ];
			k++;
		}
		else
			swcPathOut = swcPath.replaceFirst( ".swc", "_xfm.swc" );


		double[] resMoving = fetchRes( args, resMovingTag );
		double[] resTarget = fetchRes( args, resTargetTag );
		System.out.println( "resMoving " +  XfmUtils.printArray( resMoving ));
		System.out.println( "resTarget " +  XfmUtils.printArray( resTarget ));

		System.out.println( "reading");
		ArrayList< NeuronNode > skeleton = NeuronReaderWriterSWC.read( swcPath );

		/* Read landmarks and build the transform.
		 * 
		 * Note: loading the landmarks with the LandmarkTableModel builds a transform
		 * that takes a point in the fixed image space and outputs a point in the moving 
		 * image space.
		 */
		LandmarkTableModelLight ltm = new LandmarkTableModelLight( 3 ); // 3-D images
		ltm.load( new File( landmarkFile ) );
		ThinPlateR2LogRSplineKernelTransform xfm = ltm.getTransform();
		
		/* transform the points in the input skeleton and drop them in a new one */
		ArrayList< NeuronNode > skeletonXfm = new ArrayList< NeuronNode >();
		System.out.println( "transforming");
		for( NeuronNode n : skeleton )
		{
			double[] ptxfm = xfm.apply( new double[]{
					resMoving[ 0 ] * n.x,
					resMoving[ 1 ] * n.y,
					resMoving[ 2 ] * n.z } );

			skeletonXfm.add( new NeuronNode( n,
					resTarget[ 0 ] * ptxfm[ 0 ],
					resTarget[ 1 ] * ptxfm[ 1 ],
					resTarget[ 2 ] * ptxfm[ 2 ] ));
		}

		System.out.println( "writing");
		NeuronReaderWriterSWC.write( swcPathOut, skeletonXfm );

		System.out.println( "done");
		System.exit( 0 );
	}

	public static boolean isResTag( String s )
	{
		return s.equals( resMovingTag ) || s.equals( resTargetTag );
	}

	public static double[] fetchRes( String[] args, String tag )
	{
		int N = args.length;
		for( int i = 0; i < N; i++ )
		{
			if( args[ i ].equals( tag ))
			{
				return fetchRes( args[ i + 1 ]);
			}
		}
		return new double[]{ 1.0, 1.0, 1.0};
	}

	public static double[] fetchRes( String s )
	{
		String[] resStr = s.split( "," );
		double[] res = new double[ resStr.length ];
		for( int i = 0; i < resStr.length; i++ )
			res[ i ] = Double.parseDouble( resStr[ i ] );

		return res;
	}
}
