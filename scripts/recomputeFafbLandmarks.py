import urllib2
import json
import numpy as np

OWNER = "flyTEM"
PROJECT = "FAFB00"
STACK = "v9_align_tps_2"

offset = np.array( [ 133937, 63254, 22880 ] )

WORLD_TO_LOCAL_URL = "http://tem-services.int.janelia.org:8080/render-ws/v1/owner/{:s}/project/{:s}/stack/{:s}/z/{:.0f}/world-to-local-coordinates/{:f},{:f}"

LOCAL_TO_WORLD_URL = "http://tem-services.int.janelia.org:8080/render-ws/v1/owner/{:s}/project/{:s}/stack/{:s}/tile/{:s}/local-to-world-coordinates/{:f},{:f}"

conv = {i: lambda x: x.replace('\"', '') for i in (5,6,7) }
pts = np.loadtxt( '../lm-em-landmarks.csv', delimiter=",", 
        usecols = (5,6,7), converters=conv)

pts += offset
pts[:,2] /= 10

worldPts = np.zeros( pts.shape )

i = 0
for row in pts:
    # send request, world to local coordinates
    localResult = urllib2.urlopen( WORLD_TO_LOCAL_URL.format( OWNER, PROJECT, STACK, row[ 2 ], row[ 0 ], row[ 1 ]) ).read()
    localJson = json.loads( localResult )
    localPt = localJson[0]['local']
    localTile = localJson[0]['tileId']
    print localPt

    # Push the local point through to the new world coordinates
    worldResult = urllib2.urlopen( LOCAL_TO_WORLD_URL.format( OWNER, PROJECT, STACK, localTile, localPt[ 0 ], localPt[ 1 ]) ).read()
    worldJson = json.loads( worldResult )
    worldPt = worldJson[ 'world' ]
    worldPt

    worldPts[i,:] = worldPt    
    i+=1

# Write to a new file
np.savetxt( 'world_tmp.txt', worldPts, '%10.5f', delimiter="," )

