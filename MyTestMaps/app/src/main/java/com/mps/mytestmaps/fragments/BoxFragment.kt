package com.mps.mytestmaps.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.core.exceptions.ServicesException
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mps.mytestmaps.R
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.mapView
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.fragment_notifications.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class BoxFragment : Fragment(), OnMapReadyCallback{

    private val REQUEST_CODE_AUTOCOMPLETE = 1;
    private lateinit var mapboxMap: MapboxMap
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId";
    private val tegal: LatLng = LatLng(-6.879704, 109.125595)
    private var pickUpLocation = LatLng(0.0, 0.0)
    private var destinationLocation = LatLng(0.0, 0.0)
    private var marker : MarkerView? = null
    private var markerViewManager : MarkerViewManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Mapbox.getInstance(requireActivity(), getString(R.string.map_box_access_token));
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tegal, 8.5));
        markerViewManager = MarkerViewManager(mapView, mapboxMap)
        mapboxMap.addOnMapClickListener { point ->
            marker?.let {
                markerViewManager?.removeMarker(it)
            }
            val imageView = ImageView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setImageBitmap(BitmapFactory.decodeResource(requireActivity().resources, R.drawable.mapbox_marker_icon_default))
            }
            marker = MarkerView(point, imageView)
            markerViewManager?.addMarker(marker!!)
            true
        }
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            initsearch()
            it.addSource(GeoJsonSource(geojsonSourceLayerId))
            setupLayer(it)
        }
    }

    private fun initsearch() {
        requireView().fab_location_search.setOnClickListener {
            val getPickupLocation = PlaceAutocomplete.IntentBuilder()
                .accessToken(
                    if (Mapbox.getAccessToken() != null)
                        Mapbox.getAccessToken().toString()
                    else
                        getString(R.string.map_box_access_token)
                )
                .placeOptions(
                    PlaceOptions.builder()
                        .backgroundColor(Color.parseColor("#EEEEEE"))
                        .limit(10)
                        .build(PlaceOptions.MODE_CARDS)
                )
                .build(requireActivity())
            startActivityForResult(getPickupLocation, REQUEST_CODE_AUTOCOMPLETE)
        }

        requireView().fab_location_search.setOnLongClickListener {
            Toast.makeText(
                requireActivity(),
                "Pickup location ${pickUpLocation.latitude} and ${pickUpLocation.longitude}",
                Toast.LENGTH_LONG
            ).show()
            android.os.Handler().postDelayed(Runnable {
                Toast.makeText(
                    requireActivity(),
                    "Pickup location ${destinationLocation.latitude} and ${destinationLocation.longitude}",
                    Toast.LENGTH_LONG
                ).show()
            }, 2000)
            return@setOnLongClickListener false
        }
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                PropertyFactory.iconImage(symbolIconId),
                PropertyFactory.iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            val selectedPoint = PlaceAutocomplete.getPlace(data!!)
            val style = mapboxMap.style
            if (style != null) {
                val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                source?.setGeoJson(FeatureCollection.fromFeatures(arrayOf(Feature.fromJson(selectedPoint.toJson()))))
                pickUpLocation = LatLng((selectedPoint.geometry() as Point).latitude(), (selectedPoint.geometry() as Point).longitude())
                //getDestinationLocation()

                mapboxMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng((selectedPoint.geometry() as Point).latitude(), (selectedPoint.geometry() as Point).longitude()))
                            .zoom(14.0)
                            .build()
                    ), 4000
                )
            }
        }
    }


    private fun toast(message : String) = Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).apply {
        setGravity(Gravity.CENTER, 0,0)
        show()
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private val DROPPED_MARKER_LAYER_ID = "DROPPED_MARKER_LAYER_ID"
    }

}