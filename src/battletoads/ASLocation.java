package battletoads;

import battlecode.common.MapLocation;

/**
 * Location class to store information about a MapLocation
 */
public class ASLocation {
    // Map Location
    private MapLocation location;
    private ASLocation parent;

    // Passability
    private double passability;

    private double m_g;
    private double m_h;

    /**
     * Constructor to initialize a location
     */
    public ASLocation( MapLocation location, double passability, double g, double h ) {
        this.location = location;
        this.passability = passability;

        parent = null;

        m_g = g;
        m_h = h;
    }

    /**
     * Get MapLocation object
     *
     * @return MapLocation
     */
    public MapLocation getLocation() {
        return ( location );
    }

    /**
     * Get passability value of location
     *
     * @return passability double [0.1, 1.0]
     */
    public double getPassability() {
        return ( passability );
    }

    /**
     * Get g value
     *
     * @return g value from A*
     */
    public double g() {
        return ( m_g );
    }

    /**
     * Get h value
     *
     * @return rhs value from D* lite
     */
    public double h() {
        return ( m_h );
    }

    /**
     * Set parent
     *
     * @param MapLocation of parent
     */
    public void setParent( ASLocation parent ) {
        this.parent = parent;
    }

    /**
     * Get Parent location
     *
     * @return parent ASLocation
     **/
    public ASLocation getParent() {
        return ( parent );
    }
}
