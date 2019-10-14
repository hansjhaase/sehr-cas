/*
 * (C) IFETH
 */
package org.ifeth.sehr.p1507291734.web.beans;

/**
 * Geolocation object for processing 'address' on 'OSM' map.
 * <p>
 * Details see http://nominatim.openstreetmap.org
 * </p>
 * <pre>
 * "place_id": "2618114952", "licence": "Data © OpenStreetMap contributors,
 * ODbL 1.0. http://www.openstreetmap.org/copyright", "osm_type": "way",
 * "osm_id": "421803180", "boundingbox": [ "53.5149622", "53.5150559",
 * "10.2591357", "10.2593516" ], "lat": "53.51500905", "lon":
 * "10.2592436320014", "display_name": "21, Lindenstraße, Schönningstedt,
 * Reinbek, Kreis Stormarn, Schleswig-Holstein, 21465, Germany", "class":
 * "building", "type": "yes", "importance": "0.111", "address": {
 * "house_number": "21", "road": "Lindenstraße", "suburb": "Schönningstedt",
 * "village": "Reinbek", "county": "Kreis Stormarn", "state":
 * "Schleswig-Holstein", "postcode": "21465", "country": "Germany",
 * "country_code": "de" }, "svg": "M 10.2591357 -53.515035400000002 L
 * 10.259329599999999 -53.5150559 10.2593516 -53.514982699999997 10.2591576
 * -53.514962199999999 Z"
 * </pre>
 *    
 * @deprecated use GeoLocation from sehr-core
 */
public class GeoLocation {

  private Long place_id = 2618114952L;
  private Double lon = 10.2592436;
  private Double lat = 53.5150090;
  private Long osm_id = 421803180L;

  public GeoLocation() {
  }

  public GeoLocation(Long place_id, Double lon, Double lat) {
    this.place_id = place_id;
    this.lon = lon;
    this.lat = lat;
  }

  /**
   * @return the place_id
   */
  public Long getPlace_id() {
    return place_id;
  }

  /**
   * @param place_id the place_id to set
   */
  public void setPlace_id(Long place_id) {
    this.place_id = place_id;
  }

  /**
   * @return the lon
   */
  public Double getLon() {
    return lon;
  }

  /**
   * @param lon the lon to set
   */
  public void setLon(Double lon) {
    this.lon = lon;
  }

  /**
   * @return the lat
   */
  public Double getLat() {
    return lat;
  }

  /**
   * @param lat the lat to set
   */
  public void setLat(Double lat) {
    this.lat = lat;
  }

  /**
   * @return the osm_id
   */
  public Long getOsm_id() {
    return osm_id;
  }

  /**
   * @param osm_id the osm_id to set
   */
  public void setOsm_id(Long osm_id) {
    this.osm_id = osm_id;
  }
}
