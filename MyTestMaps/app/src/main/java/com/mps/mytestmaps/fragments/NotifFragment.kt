package com.mps.mytestmaps.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mps.mytestmaps.R
import kotlinx.android.synthetic.main.fragment_notifications.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class NotifFragment : Fragment(), OnMapReadyCallback{

    //private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var selectLocationButton: Button? = null
    private var permissionsManager: PermissionsManager? = null
    private var hoveringMarker: ImageView? = null
    private var droppedMarkerLayer: Layer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Mapbox.getInstance(requireActivity(), getString(R.string.map_box_access_token));
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            //enableLocationPlugin(style)
            /*hoveringMarker = ImageView(requireActivity())
            hoveringMarker!!.setImageResource(R.drawable.mapbox_marker_icon_default)
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER
            )
            hoveringMarker!!.layoutParams = params
            mapView!!.addView(hoveringMarker)*/

            // Initialize, but don't show, a SymbolLayer for the marker icon which will represent a selected location.
            initDroppedMarker(style)

            // Button for user to drop marker or to pick marker back up.
            select_location_button.setOnClickListener {
                buttonSelected(style)
            }
        }
    }

    private fun buttonSelected(style : Style){
        if (hoveringMarker!!.visibility == View.VISIBLE) {

            // Use the map target's coordinates to make a reverse geocoding search
            val mapTargetLatLng = mapboxMap!!.cameraPosition.target

            // Hide the hovering red hovering ImageView marker
            hoveringMarker!!.visibility = View.INVISIBLE

            // Transform the appearance of the button to become the cancel button
            select_location_button.setBackgroundColor(
                ContextCompat.getColor(requireActivity(), R.color.colorAccent)
            )
            select_location_button.text = "Batal"

            // Show the SymbolLayer icon to represent the selected map location
            if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                val source = style.getSourceAs<GeoJsonSource>("dropped-marker-source-id")
                source?.setGeoJson(Point.fromLngLat(mapTargetLatLng.longitude, mapTargetLatLng.latitude))
                droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
                if (droppedMarkerLayer != null) {
                    droppedMarkerLayer!!.setProperties(PropertyFactory.visibility(Property.VISIBLE))
                }
            }

            // Use the map camera target's coordinates to make a reverse geocoding search
            reverseGeocode(
                Point.fromLngLat(
                    mapTargetLatLng.longitude,
                    mapTargetLatLng.latitude
                )
            )

        } else {

            // Switch the button appearance back to select a location.
            select_location_button.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.colorPrimary))
            select_location_button.text = "selected"
            // Show the red hovering ImageView marker
            hoveringMarker!!.visibility = View.VISIBLE

            // Hide the selected location SymbolLayer
            droppedMarkerLayer = style.getLayer(DROPPED_MARKER_LAYER_ID)
            if (droppedMarkerLayer != null) {
                droppedMarkerLayer!!.setProperties(PropertyFactory.visibility(Property.NONE))
            }
        }
    }

    private fun initDroppedMarker(loadedMapStyle: Style) {
        // Add the marker image to map
        loadedMapStyle.addImage("dropped-icon-image", BitmapFactory.decodeResource(resources, R.drawable.mapbox_marker_icon_default))
        loadedMapStyle.addSource(GeoJsonSource("dropped-marker-source-id"))
        loadedMapStyle.addLayer(
            SymbolLayer(DROPPED_MARKER_LAYER_ID, "dropped-marker-source-id").withProperties(
                PropertyFactory.iconImage("dropped-icon-image"),
                PropertyFactory.visibility(Property.NONE),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()

        mapView!!.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager!!.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

     /*override fun onExplanationNeeded(permissionsToExplain: List<String>) {
         Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG)
             .show()
     }

     override fun onPermissionResult(granted: Boolean) {
         if (granted && mapboxMap != null) {
             val style = mapboxMap!!.style
             if (style != null) {
                 enableLocationPlugin(style)
             }
         } else {
             Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG)
                 .show()
             finish()
         }
     }*/

    private fun reverseGeocode(point: Point) {
        try {
            val client = MapboxGeocoding.builder()
                .accessToken(getString(R.string.map_box_access_token))
                .query(Point.fromLngLat(point.longitude(), point.latitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build()
            client.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    Timber.e("Geocoding Failure: %s", t.message)
                }

                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                    if (response.body() != null) {
                        val results = response.body()!!.features()
                        if (results.size > 0) {
                            val feature = results.get(0)

                            // If the geocoder returns a result, we take the first in the list and show a Toast with the place name.
                            mapboxMap!!.getStyle { style ->
                                if (style.getLayer(DROPPED_MARKER_LAYER_ID) != null) {
                                    Toast.makeText(requireActivity(), "location place name ${feature.placeName()}", Toast.LENGTH_SHORT).show()
                                }
                            }

                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "lokasi tidak ada hasil",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            })
        } catch (servicesException: ServicesException) {
            Timber.e("Error geocoding: %s", servicesException.toString())
            servicesException.printStackTrace()
        }

    }

   /* private fun enableLocationPlugin(loadedMapStyle: Style) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component. Adding in LocationComponentOptions is also an optional
            // parameter
            val locationComponent = mapboxMap!!.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(
                    this, loadedMapStyle
                ).build()
            )
            locationComponent.isLocationComponentEnabled = true

            // Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.NORMAL

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }*/

    companion object {
        private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    }
}