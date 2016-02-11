package org.janelia.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.janelia.struct.NeuronNode;


public class NeuronReaderWriterSWC
{
	public static final String NEWLINE = "\r\n";
	
	public static final String HDR = 
			"# SWC format file"+ NEWLINE + 
			"# based on specifications at http://research.mssm.edu/cnic/swc.html" + NEWLINE +  
			"# Created by NeuronReaderWriterSWC" + NEWLINE + 
			"# PointNo Label X Y Z Radius Parent";

	String path;

	public NeuronReaderWriterSWC( )
	{}
	
	public NeuronReaderWriterSWC( String path )
	{
		this.path = path;
	}
	
	public static ArrayList< NeuronNode > read( String path ) throws IOException
	{
		ArrayList< NeuronNode > skeleton = new ArrayList< NeuronNode >();
		List< String > lines = Files.readAllLines( Paths.get( new File( path ).getAbsolutePath() ) );

		for ( String s : lines )
		{
			if ( s.startsWith( "#" ) )
				continue;

			String[] cols = s.split( "\\s" );
			skeleton.add( new NeuronNode( 
					Integer.parseInt(   cols[ 0 ] ), 
					Integer.parseInt(   cols[ 1 ] ),
					Double.parseDouble( cols[ 2 ] ),
					Double.parseDouble( cols[ 3 ]  ),
					Double.parseDouble( cols[ 4 ] ),
					Integer.parseInt(   cols[ 5 ] ),
					Integer.parseInt(   cols[ 6 ] )));
		}

		return skeleton;
	}
	
	public static Path write( String path, ArrayList< NeuronNode > skeleton ) throws IOException
	{
		String out = HDR + NEWLINE;

		for( NeuronNode n : skeleton )
		{
			out +=
					Integer.toString( n.index ) + " " +
					Integer.toString( n.label ) + " " +
					Double.toString( n.x ) + " " +
					Double.toString( n.y ) + " " + 
					Double.toString( n.z ) + " " + 
					Integer.toString( n.radius ) + " " +
					Integer.toString( n.parent ) + 
					NEWLINE;
		}
		return Files.write( Paths.get( new File( path ).getAbsolutePath() ), out.getBytes() );
		
	}
}
