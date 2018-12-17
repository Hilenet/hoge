package hoge.marker;

import java.util.HashMap;

/**
 * Store MarkerInfos.
 *
 */
public class MarkerRoot {
	HashMap<String, HashMap<String, MarkerInfo>> markers; /* <fqcn, <signature, entity>> */

	public MarkerRoot() {
		markers = new HashMap<String, HashMap<String, MarkerInfo>>();
	}

	/**
	 * Store markerInfo.
	 * 
	 * @param markerInfo
	 * @return markers
	 */
	public HashMap<String, HashMap<String, MarkerInfo>> addMarkerInfo(MarkerInfo markerInfo) {
		HashMap<String, MarkerInfo> classMarkers = markers.get(markerInfo.fqcn);
		if (classMarkers == null) {
			classMarkers = new HashMap<String, MarkerInfo>();
			markers.put(markerInfo.fqcn, classMarkers);
		}
		if (!classMarkers.containsKey(markerInfo.toSignatureString())) {
			classMarkers.put(markerInfo.toSignatureString(), markerInfo);
		}

		return markers;
	}

	/**
	 * @return stored marker set
	 */
	public HashMap<String, HashMap<String, MarkerInfo>> getMarkers() {
		return markers;
	}

}
