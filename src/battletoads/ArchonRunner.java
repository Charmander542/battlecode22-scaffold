package battletoads;

import battlecode.common.*;
import battletoads.Utils;

public class ArchonRunner {

    private static final int MAX_NUM_MINERS = 3; // Half the max num of miners TODO This doesn't work
    private static int numMiners = 0;

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void run(RobotController rc) throws GameActionException {
        Direction dir = Utils.directions[Utils.rng.nextInt(Utils.directions.length)];
        if (numMiners < MAX_NUM_MINERS) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
                numMiners++;
            }
        }
        else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }
}
