package org.janelia.nblastFly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

import org.janelia.io.LandmarkTableModelLight;
import org.janelia.io.NeuronReaderWriterSWC;
import org.janelia.struct.NeuronNode;

public class NeuronTransformTPS
{

	public static void main( String[] args ) throws IOException
	{
		String swcPathOut = args[ 0 ];
		String swcPath = args[ 1 ];
		String landmarkFile = args[ 2 ];

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
		System.out.println( xfm );
		
		/* transform the points in the input skeleton and drop them in a new one */
		ArrayList< NeuronNode > skeletonXfm = new ArrayList< NeuronNode >();
		System.out.println( "transforming");
		for( NeuronNode n : skeleton )
		{
			double[] ptxfm = xfm.apply( new double[]{ n.x, n.y, n.z } );
			skeletonXfm.add( new NeuronNode( n, ptxfm[ 0 ], ptxfm[ 1 ], ptxfm[ 2 ] ));
		}
		
		System.out.println( "writing");
		NeuronReaderWriterSWC.write( swcPathOut, skeletonXfm );
		
		System.out.println( "done");
		System.exit( 0 );
	}
}
