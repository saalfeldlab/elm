package elm;

import static net.imglib2.cache.img.AccessFlags.VOLATILE;
import static net.imglib2.cache.img.PrimitiveType.BYTE;
import static net.imglib2.cache.img.PrimitiveType.DOUBLE;
import static net.imglib2.cache.img.PrimitiveType.FLOAT;
import static net.imglib2.cache.img.PrimitiveType.INT;
import static net.imglib2.cache.img.PrimitiveType.LONG;
import static net.imglib2.cache.img.PrimitiveType.SHORT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.imglib2.N5CellLoader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.imglib2.RandomAccessibleLoader;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import bdv.BigDataViewer;
import bdv.SpimSource;
import bdv.VolatileSpimSource;
import bdv.cache.CacheControl;
import bdv.cache.CacheControl.CacheControls;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.tools.transformation.TransformedSource;
import bdv.util.RandomAccessibleIntervalMipmapSource;
import bdv.util.volatiles.SharedQueue;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import bigwarp.BigWarp.BigWarpViewerOptions;
import bigwarp.loader.ImagePlusLoader;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.ArrayDataAccessFactory;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.display.ScaledARGBConverter;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.img.basictypeaccess.array.LongArray;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.type.numeric.integer.UnsignedLongType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ELMv14_n5
{
	
	final static String SCALE_KEY = "downsamplingFactors";

	public static void main( String[] args ) throws IOException
	{
		final String landmarkPath = "/groups/saalfeld/home/bogovicj/dev/bdv/elm/lm-em-landmarks.csv";
		final String n5Path = "/nrs/saalfeld/FAFB00/v14_align_tps_20170818_dmg.n5";
		final String datasetName = "/volumes/raw";
//		final VoxelDimensions voxelDimensions = new FinalVoxelDimensions("nm", new double[]{4, 4, 4});
		final VoxelDimensions voxelDimensions = new FinalVoxelDimensions("nm", new double[]{1, 1, 1});
		
		
		final AffineTransform3D resolutionXfm = new AffineTransform3D();
		resolutionXfm.set( 4,  0,  0,  0, 
					       0,  4,  0,  0,
					       0,  0, 40,  40 );
		
		final boolean useVolatile = true; 

		final N5FSReader n5 =  new N5FSReader( n5Path, new GsonBuilder());

		final SharedQueue queue = new SharedQueue( Math.min( 8, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)), 17 );

		List<Source<?>> srclist = new ArrayList<Source<?>>(); 
		List<CacheControl> cachelist = new ArrayList<CacheControl>(); 

		final int numScales = n5.list(datasetName).length;
		System.out.println( "num scales: " + numScales );

		@SuppressWarnings("unchecked")
		final RandomAccessibleInterval<UnsignedByteType>[] mipmaps = (RandomAccessibleInterval<UnsignedByteType>[])new RandomAccessibleInterval[numScales];
		final double[][] scales = new double[numScales][];

		for (int s = 0; s < numScales; ++s)
		{
			DatasetAttributes attr = n5.getDatasetAttributes( datasetName + "/s" + s );
			
			
			// need more gymnastics to get scales
			HashMap< String, JsonElement > attrMap = n5.getAttributes( datasetName + "/s" + s  );

			double[] scale;
			if( !attrMap.keySet().contains( SCALE_KEY ))
			{
				//System.out.println( "no dsfactor key" );
				scale = new double[]{ 1.0, 1.0, 1.0 };
			}
			else
			{
				scale = new double[]{
						attrMap.get( SCALE_KEY ).getAsJsonArray().get( 0 ).getAsDouble(),
						attrMap.get( SCALE_KEY ).getAsJsonArray().get( 1 ).getAsDouble(),
						attrMap.get( SCALE_KEY ).getAsJsonArray().get( 2 ).getAsDouble()
				};
				//System.out.println( "scale : " + Arrays.toString( scale ));
			}
			
			final RandomAccessibleInterval<UnsignedByteType> source = N5Utils.openVolatile(n5, datasetName + "/s" + s);

			final RandomAccessibleInterval<UnsignedByteType> cachedSource = wrapAsVolatileCachedCellImg(
					source, attr.getBlockSize());

			mipmaps[s] = cachedSource;
			scales[s] = scale;
		}
		
		final RandomAccessibleIntervalMipmapSource<UnsignedByteType> mipmapSource =
				new RandomAccessibleIntervalMipmapSource<>(
						mipmaps,
						new UnsignedByteType(),
						scales,
						voxelDimensions,
						datasetName);

		final Source<?> volatileMipmapSource;
		if (useVolatile)
		{
			System.out.println( "volatile" );
			volatileMipmapSource = mipmapSource.asVolatile( queue );
		}
		else
			volatileMipmapSource = mipmapSource;

		final TransformedSource<?> transformedVolatileMipmapSource = new TransformedSource<>( volatileMipmapSource );
		transformedVolatileMipmapSource.setFixedTransform( resolutionXfm );
		
		
		
//		srclist.add( volatileMipmapSource );
		srclist.add( transformedVolatileMipmapSource );
		
		final int numTargetSources = srclist.size();
		
		final ImagePlus imp = IJ.openImage( "/groups/saalfeld/public/fly-template/JFRC2013.tif" );
		final ImagePlusLoader loaderP = new ImagePlusLoader( imp );

		final AbstractSpimData< ? >[] spimDataP = loaderP.loadAll( 0 );
		final int numMovingChannels = loaderP.numChannels();
		final AbstractSequenceDescription< ?, ?, ? > seqP = spimDataP[0].getSequenceDescription();
		final int numMovingSources = seqP.getViewSetups().size();

		
		/*
		 * Set up data sources
		 */
		int setupId = 5;

		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		final ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();

		Data data = new Data( converterSetups, sources );
		BigDataViewer.initSetups( spimDataP[0], (List< ConverterSetup >)data.converterSetups, (List< SourceAndConverter<?> >)data.sources );
		
		for( Source s : srclist )
			data = add( s, setupId++, 1, data );
		

		int[] movingSourceIndices = ImagePlusLoader.range( 0, numMovingSources );
		int[] targetSourceIndices = ImagePlusLoader.range( numMovingSources, numTargetSources );
		
		System.out.println( "moving idxs: " + Arrays.toString( movingSourceIndices ));
		System.out.println( "target idxs: " + Arrays.toString( targetSourceIndices ));

		try
		{
			BigWarp bw = data.getBigWarp( movingSourceIndices, targetSourceIndices );

			// open landmarks
			bw.getLandmarkPanel().getTableModel().load( new File( landmarkPath ) );

//			(0.0021197047724571217, 0.0, 0.0, 37.12661378279256, 0.0, 0.0021197047724571217, 0.0, 29.564038470996337, 0.0, 0.0, 0.0021197047724571217, -7.499484211109663)
//			final AffineTransform3D viewerQTransform = new AffineTransform3D();
//			bw.getViewerFrameQ().getViewerPanel().setCurrentViewerTransform( viewerQTransform );
		}
		catch ( SpimDataException e )
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final <T extends NativeType<T>> RandomAccessibleInterval<T> wrapAsVolatileCachedCellImg(
			final RandomAccessibleInterval<T> source,
			final int[] blockSize) throws IOException {

		final long[] dimensions = Intervals.dimensionsAsLongArray(source);
		final CellGrid grid = new CellGrid(dimensions, blockSize);

		final RandomAccessibleLoader<T> loader = new RandomAccessibleLoader<T>(Views.zeroMin(source));

		final T type = Util.getTypeFromInterval(source);

		final CachedCellImg<T, ?> img;
		final Cache<Long, Cell<?>> cache =
				new SoftRefLoaderCache().withLoader(LoadedCellCacheLoader.get(grid, loader, type, VOLATILE));

		if (GenericByteType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(BYTE, VOLATILE));
		} else if (GenericShortType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(SHORT, VOLATILE));
		} else if (GenericIntType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(INT, VOLATILE));
		} else if (GenericLongType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(LONG, VOLATILE));
		} else if (FloatType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(FLOAT, VOLATILE));
		} else if (DoubleType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(DOUBLE, VOLATILE));
		} else {
			img = null;
		}

		return img;
	}
	
	public static <T> Data add(
			AbstractSpimData<?> d,
			int setupId,
			int numTimepoints,
			Data data )
	{
		return null;
	}
	
	public static < T > Data add( 
			Source< T > s, 
			int setupId,
			int numTimepoints, 
			Data data )
	{
		final T type = s.getType();
		ArrayList< ConverterSetup > converterSetups = null;
		ArrayList< SourceAndConverter< T > > sources = null;

		if( data == null )
		{
			converterSetups = new ArrayList<>();
			sources = new ArrayList< SourceAndConverter<T> >();
		}
		else
		{
			converterSetups = data.converterSetups;
			sources = data.sources;
		}
		
		addSourceToListsGenericType( s, setupId++, numTimepoints, type, converterSetups, sources );

		return new Data( converterSetups, sources );
	}
	
	public static < T > Data < ? > add( 
			List<Source > slist, 
			int setupId,
			int numTimepoints )
	{
		final Object type = slist.get(0).getType();
		Data data = add( slist.get(0), setupId, numTimepoints, null );
		
		final ArrayList< ConverterSetup > converterSetups = new ArrayList<>();
		final ArrayList< SourceAndConverter< T > > sources = new ArrayList<>();
		
		for( int i = 1; i < slist.size(); i++ )
			data = add( slist.get(i), setupId++, numTimepoints, data );

		return new Data( converterSetups, sources );
	}
	
	public static class Data<T> 
	{
		public final ArrayList< ConverterSetup > converterSetups;
		public final ArrayList< SourceAndConverter< ? > > sources;
		
		public Data(
				final ArrayList< ConverterSetup > converterSetups,
				final ArrayList< SourceAndConverter< ? > > sources )
		{
			this.converterSetups = converterSetups;
			this.sources = sources;
		}
		
		public BigWarp getBigWarp( int[] movingIdxs, int[] targetIdxs ) throws SpimDataException
		{
			BigWarpViewerOptions opts = new BigWarpViewerOptions(false);
			CacheControls cc = new CacheControls();
			return new BigWarp( sources, converterSetups, cc,
					movingIdxs, targetIdxs,
					"bw", opts, null );
		}
		
	};
	
	private static < T > void addSourceToListsGenericType(
			final Source< T > source,
			final int setupId,
			final int numTimepoints,
			final T type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		if ( type instanceof RealType )
			addSourceToListsRealType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else if ( type instanceof ARGBType )
			addSourceToListsARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else if ( type instanceof VolatileARGBType )
			addSourceToListsVolatileARGBType( ( Source ) source, setupId, ( List ) converterSetups, ( List ) sources );
		else
			throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
	}
	
	public static final class RAIandLoader< T extends NativeType< T > >
	{
		final N5CellLoader< T > loader;
		final RandomAccessibleInterval<T> rai;

		public RAIandLoader( N5CellLoader<T> loader, RandomAccessibleInterval<T> rai )
		{
			this.loader = loader;
			this.rai = rai;
		}
	}


	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public static final < T extends NativeType< T > > RAIandLoader< T > openWithLoader(
			final N5Reader n5,
			final String dataset ) throws IOException
	{
		final DatasetAttributes attributes = n5.getDatasetAttributes( dataset );
		final long[] dimensions = attributes.getDimensions();
		final int[] blockSize = attributes.getBlockSize();

		final N5CellLoader< T > loader = new N5CellLoader<>( n5, dataset, blockSize );

		final CellGrid grid = new CellGrid( dimensions, blockSize );

		final CachedCellImg< T, ? > img;
		final T type;
		final Cache< Long, Cell< ? > > cache;

		switch ( attributes.getDataType() )
		{
		case INT8:
			type = ( T )new ByteType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< ByteArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( BYTE ) );
			break;
		case UINT8:
			type = ( T )new UnsignedByteType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< ByteArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid,type, cache, ArrayDataAccessFactory.get( BYTE ) );
			break;
		case INT16:
			type = ( T )new ShortType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< ShortArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( SHORT ) );
			break;
		case UINT16:
			type = ( T )new UnsignedShortType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< ShortArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( SHORT ) );
			break;
		case INT32:
			type = ( T )new IntType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< IntArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( INT ) );
			break;
		case UINT32:
			type = ( T )new UnsignedIntType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< IntArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( INT ) );
			break;
		case INT64:
			type = ( T )new LongType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< LongArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( LONG ) );
			break;
		case UINT64:
			type = ( T )new UnsignedLongType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< LongArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( LONG ) );
			break;
		case FLOAT32:
			type = ( T )new FloatType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< FloatArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( FLOAT ) );
			break;
		case FLOAT64:
			type = ( T )new DoubleType();
			cache = ( Cache )new SoftRefLoaderCache< Long, Cell< DoubleArray > >()
					.withLoader( LoadedCellCacheLoader.get( grid, loader, type ) );
			img = new CachedCellImg( grid, type, cache, ArrayDataAccessFactory.get( DOUBLE ) );
			break;
		default:
			img = null;
		}

		return new RAIandLoader(loader, img);
	}

	private static < T extends RealType< T >, V extends Volatile< T > & RealType< V > > void initSetupRealType(
			final AbstractSpimData< ? > spimData,
			final BasicViewSetup setup,
			final T type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		if ( spimData.getSequenceDescription().getImgLoader() instanceof WrapBasicImgLoader )
		{
			initSetupRealTypeNonVolatile( spimData, setup, type, converterSetups, sources );
			return;
		}
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< V > vconverter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
		vconverter.setColor( new ARGBType( 0xffffffff ) );
		final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );

		final int setupId = setup.getId();
		final String setupName = createSetupName( setup );
		final VolatileSpimSource< T, V > vs = new VolatileSpimSource<>( spimData, setupId, setupName );
		final SpimSource< T > s = vs.nonVolatile();

		// Decorate each source with an extra transformation, that can be
		// edited manually in this viewer.
		final TransformedSource< V > tvs = new TransformedSource<>( vs );
		final TransformedSource< T > ts = new TransformedSource<>( s, tvs );

		final SourceAndConverter< V > vsoc = new SourceAndConverter<>( tvs, vconverter );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( ts, converter, vsoc );

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter, vconverter ) );
	}

	private static < T extends RealType< T > > void initSetupRealTypeNonVolatile(
			final AbstractSpimData< ? > spimData,
			final BasicViewSetup setup,
			final T type,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ? > > sources )
	{
		final double typeMin = type.getMinValue();
		final double typeMax = type.getMaxValue();
		final RealARGBColorConverter< T > converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );

		final int setupId = setup.getId();
		final String setupName = createSetupName( setup );
		final SpimSource< T > s = new SpimSource<>( spimData, setupId, setupName );

		// Decorate each source with an extra transformation, that can be
		// edited manually in this viewer.
		final TransformedSource< T > ts = new TransformedSource<>( s );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( ts, converter );

		sources.add( soc );
		converterSetups.add( new RealARGBColorConverterSetup( setupId, converter ) );
	}
	
	private static String createSetupName( final BasicViewSetup setup )
	{
		if ( setup.hasName() )
			return setup.getName();

		String name = "";

		final Angle angle = setup.getAttribute( Angle.class );
		if ( angle != null )
			name += ( name.isEmpty() ? "" : " " ) + "a " + angle.getName();

		final Channel channel = setup.getAttribute( Channel.class );
		if ( channel != null )
			name += ( name.isEmpty() ? "" : " " ) + "c " + channel.getName();

		return name;
	}

	
	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link RealARGBColorConverter} and into
	 * a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static < T extends RealType< T > > void addSourceToListsRealType(
			final Source< T > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< T > > sources )
	{
		final T type = Util.getTypeFromInterval( source.getSource( 0, 0 ) );
		final double typeMin = Math.max( 0, Math.min( type.getMinValue(), 65535 ) );
		final double typeMax = Math.max( 0, Math.min( type.getMaxValue(), 65535 ) );
		final RealARGBColorConverter< T > converter ;
		if ( source.getType() instanceof Volatile )
			converter = new RealARGBColorConverter.Imp0<>( typeMin, typeMax );
		else
			converter = new RealARGBColorConverter.Imp1<>( typeMin, typeMax );
		converter.setColor( new ARGBType( 0xffffffff ) );

		final TransformedSource< T > ts = new TransformedSource<>( source );
		final SourceAndConverter< T > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}
	
	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link ScaledARGBConverter.ARGB} and
	 * into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static void addSourceToListsARGBType(
			final Source< ARGBType > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< ARGBType > > sources )
	{
		final TransformedSource< ARGBType > ts = new TransformedSource<>( source );
		final ScaledARGBConverter.ARGB converter = new ScaledARGBConverter.ARGB( 0, 255 );
		final SourceAndConverter< ARGBType > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}
	
	/**
	 * Add the given {@code source} to the lists of {@code converterSetups}
	 * (using specified {@code setupId}) and {@code sources}. For this, the
	 * {@code source} is wrapped with a {@link ScaledARGBConverter.ARGB} and
	 * into a {@link TransformedSource}.
	 *
	 * @param source
	 *            source to add.
	 * @param setupId
	 *            id of the new source for use in {@link SetupAssignments}.
	 * @param converterSetups
	 *            list of {@link ConverterSetup}s to which the source should be
	 *            added.
	 * @param sources
	 *            list of {@link SourceAndConverter}s to which the source should
	 *            be added.
	 */
	private static void addSourceToListsVolatileARGBType(
			final Source< VolatileARGBType > source,
			final int setupId,
			final List< ConverterSetup > converterSetups,
			final List< SourceAndConverter< VolatileARGBType > > sources )
	{
		final TransformedSource< VolatileARGBType > ts = new TransformedSource<>( source );
		final ScaledARGBConverter.VolatileARGB converter = new ScaledARGBConverter.VolatileARGB( 0, 255 );
		final SourceAndConverter< VolatileARGBType > soc = new SourceAndConverter<>( ts, converter );

		final RealARGBColorConverterSetup setup = new RealARGBColorConverterSetup( setupId, converter );

		converterSetups.add( setup );
		sources.add( soc );
	}
}
