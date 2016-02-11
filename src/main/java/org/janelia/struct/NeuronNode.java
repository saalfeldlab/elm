package org.janelia.struct;

public class NeuronNode
{
	public final int index;
	public final int label;
	public final double x;
	public final double y;
	public final double z;
	public final int radius;
	public final int parent;
	
	public NeuronNode( int parent, int r, double x, double y, double z, int k, int child )
	{
		this.index = parent;
		this.label = r;
		this.x = x;
		this.y = y;
		this.z = z;
		this.radius = k;
		this.parent = child;
	}
	
	public NeuronNode( NeuronNode node, double x, double y, double z )
	{
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.index = node.index;
		this.label = node.label;
		this.radius = node.radius;
		this.parent = node.parent;
	}
	
}
