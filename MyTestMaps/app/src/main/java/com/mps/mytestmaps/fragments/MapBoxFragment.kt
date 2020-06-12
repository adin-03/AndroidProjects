package com.mps.mytestmaps.fragments

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions
import com.mps.mytestmaps.R
import kotlinx.android.synthetic.main.fragment_dashboard.*
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import java.util.logging.Handler


class MapBoxFragment : Fragment(), OnMapReadyCallback {
    private val REQUEST_CODE_AUTOCOMPLETE = 1;
    private val REQUEST_CODE_AUTOCOMPLETE_DESTINATION = 1;
    //private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private val geojsonSourceLayerId = "geojsonSourceLayerId"
    private val symbolIconId = "symbolIconId";
    private val tegal: LatLng = LatLng(-6.879704, 109.125595)
    private var pickUpLocation = LatLng(0.0, 0.0)
    private var destinationLocation = LatLng(0.0, 0.0)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        mapboxMap.setStyle(
            Style.MAPBOX_STREETS, Style.OnStyleLoaded {
                initsearch()
                it.addImage(
                    symbolIconId, BitmapFactory.decodeResource(
                        requireActivity().resources, R.drawable.mapbox_marker_icon_default
                    )
                );

                it.addSource(GeoJsonSource(geojsonSourceLayerId))
                setupLayer(it)
            })
        mapboxMap.addOnMapClickListener { point ->
            mapboxMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(
                            LatLng(
                                point.latitude,
                                point.longitude
                            )
                        )
                        .zoom(14.0)
                        .build()
                ), 2000
            )
            true
        }
    }

    private fun initsearch() {
        fab_location_search.setOnClickListener {
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

    private fun getDestinationLocation() {
        val intent1 = PlaceAutocomplete.IntentBuilder()
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
        startActivityForResult(intent1, 666)
    }

    private fun setupLayer(loadedMapStyle: Style) {
        loadedMapStyle.addLayer(
            SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
                iconImage(symbolIconId),
                iconOffset(arrayOf(0f, -8f))
            )
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data!!)
            val style = mapboxMap.style
            if (style != null) {
                val source = style.getSourceAs<GeoJsonSource>(geojsonSourceLayerId)
                source?.setGeoJson(
                    FeatureCollection.fromFeatures(
                        arrayOf(
                            Feature.fromJson(
                                selectedCarmenFeature.toJson()
                            )
                        )
                    )
                )
                pickUpLocation = LatLng(
                    (selectedCarmenFeature.geometry() as Point).latitude(),
                    (selectedCarmenFeature.geometry() as Point).longitude()
                )
                getDestinationLocation()

                // Move map camera to the selected location
//                mapboxMap.animateCamera(
//                    CameraUpdateFactory.newCameraPosition(
//                        CameraPosition.Builder()
//                            .target(
//                                LatLng(
//                                    (selectedCarmenFeature.geometry() as Point).latitude(),
//                                    (selectedCarmenFeature.geometry() as Point).longitude()
//                                )
//                            )
//                            .zoom(14.0)
//                            .build()
//                    ), 4000
//                )
            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == 666) {
            val selectedCarmenFeature = PlaceAutocomplete.getPlace(data!!)
            destinationLocation = LatLng(
                (selectedCarmenFeature.geometry() as Point).latitude(),
                (selectedCarmenFeature.geometry() as Point).longitude()
            )
        }
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
}



