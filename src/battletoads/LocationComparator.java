package battletoads;

import java.util.Comparator;

public class LocationComparator implements Comparator<Location> {
    public int compare( Location l1, Location l2 ) {
        double l1Cost = l1.getDistance() + l1.getPassability() / 10;
        double l2Cost = l2.getDistance() + l2.getPassability() / 10;

        return ( (int) ( l1Cost - l2Cost ) );
    }
}
