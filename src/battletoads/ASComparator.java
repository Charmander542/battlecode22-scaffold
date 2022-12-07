package battletoads;

import java.util.Comparator;

public class ASComparator implements Comparator<ASLocation> {
    public int compare( ASLocation l1, ASLocation l2 ) {
        double l1Cost = l1.g() + l1.h() + l1.getPassability() / 10;
        double l2Cost = l2.g() + l2.h() + l2.getPassability() / 10;

        return ( (int) ( l1Cost - l2Cost ) );
    }
}
