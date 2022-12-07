package battletoads;

import java.util.Arrays;

import battlecode.common.*;
import battletoads.Utils;

public class BuilderRunner {

    private static MapLocation enemyArchonLocation;

    /**
     * Run a single turn for an BUILDER.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void run(RobotController rc) throws GameActionException {
        // This means the builder was just spawned
        if (enemyArchonLocation == null) {
            // TODO: Does not take into account if next to multiple archons
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getLocation(), 2, rc.getTeam());
            RobotInfo archon = Arrays.stream(nearbyRobots).filter(ri -> ri.getType() == RobotType.ARCHON).findFirst().get();
            MapLocation archonLocation = archon.getLocation();
            Direction enemyArchonDirection = archonLocation.directionTo(rc.getLocation());
            // Radial symmetry
            if (!Utils.CARDINAL_DIRECTIONS.contains(enemyArchonDirection)) {
                enemyArchonLocation = new MapLocation(rc.getMapWidth() - archonLocation.x - 1, rc.getMapHeight() - archonLocation.y - 1);
            }
            // Vertical symmetry
            else if (enemyArchonDirection == Direction.NORTH || enemyArchonDirection == Direction.SOUTH) {
                enemyArchonLocation = new MapLocation(archonLocation.x, rc.getMapHeight() - archonLocation.y - 1);
            }
            // Horizontal symmetry
            else {
                enemyArchonLocation = new MapLocation(rc.getMapWidth() - archonLocation.x - 1, archonLocation.y);
            }
        }
    
        Direction dir = rc.getLocation().directionTo(enemyArchonLocation);
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
