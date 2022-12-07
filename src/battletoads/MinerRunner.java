package battletoads;

import java.util.HashSet;
import java.util.Set;

import battlecode.common.*;
import battletoads.Logging;
import battletoads.RobotUtils;
import battletoads.Utils;
import battletoads.RobotUtils.MoveToResult;

public class MinerRunner {

    // Store sense points here, cuts down on bytecode
    private static final int[][] SENSE_POINTS = Utils.CIRCLE_POINTS.get(RobotType.MINER.visionRadiusSquared);

    private static enum MinerState {
        SEARCHING,
        MOVING,
        MINING
    }

    private static MinerState currState = MinerState.SEARCHING;
    private static MapLocation mineLoc;

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    public static void run(RobotController rc) throws GameActionException {
        boolean done = false;
        while (!done) {
            switch (currState) {
                case SEARCHING: done |= searchAction(rc); break;
                case MOVING: done |= moveAction(rc); break;
                case MINING: done |= mineAction(rc); break;
            }
        }
    }

    /**
     * Runs the search action
     * Finds the next location to move to based on where there is stuff to mine
     * @return True if the robot's move should be done
     */
    private static boolean searchAction(RobotController rc) throws GameActionException {
        mineLoc = findNextMiningSpot(rc);
        if (mineLoc != null) {
            rc.setIndicatorString(String.format("Claimed [%d, %d], heading there", mineLoc.x, mineLoc.y));
        }
        currState = MinerState.MOVING;
        return true; // Always return true because the finding algorithm is very bytecode intensive
    }

    /**
     * Runs the move action
     * Navigates to the lead location if it exists
     * Otherwise, walks randomly to try to find a new location
     * @return True if the robot's move should be done
     */
    private static boolean moveAction(RobotController rc) throws GameActionException {
        if (mineLoc != null) {
            // TODO should navigate to the square adjacent to the mining location, since it can still mine from there
            // TODO should verify that no one is currently mining the lead it is going for
            // TODO Anomalies can make the robot lose its target

            // if (RobotUtils.moveTo(rc, mineLoc)) {
            //     currState = MinerState.MINING;
            //     return false;
            // }

            MoveToResult res;
            do {
                res = RobotUtils.moveToVerbose(rc, mineLoc);
            }
            while (res == MoveToResult.MOVED);
            if (res == MoveToResult.TARGET_REACHED) {
                currState = MinerState.MINING;
                return false;
            }
        }
        else {
            // TODO This could be updated so that it's not walking in a random direction but instead navigating somewhere more advantageous
            Direction dir = Utils.directions[Utils.rng.nextInt(Utils.directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            currState = MinerState.SEARCHING;
            return false; // Causes the robot to search on the same turn
        }
        return true;
    }

    /**
     * Runs the mine action
     * Mines the desired location
     * @return True if the robot's move should be done
     */
    private static boolean mineAction(RobotController rc) throws GameActionException {
        // TODO Add ability to mine gold
        // TODO Add ability to mine around the lead
        while (rc.canMineLead(mineLoc)) {
            rc.mineLead(mineLoc);
        }

        // When we are done, go back to searching for the next lead
        if (rc.senseLead(mineLoc) == 0) {
            if (minerArrIdx >= 0) {
                rc.writeSharedArray(Utils.ARR_IDX_MINER_1 + minerArrIdx, 0xFFF);
            }

            currState = MinerState.SEARCHING;
            return false;
        }
        return true;
    }

    private static final int X_OFFSET = 6;
    private static final int X_MASK = 0x3F;

    private static int minerArrIdx = -1;

    /**
     * Finds the closest location for mining
     * @return
     */
    private static MapLocation findNextMiningSpot(RobotController rc) {
        try {
            MapLocation[] potentialLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.MINER.visionRadiusSquared);

            Set<MapLocation> takenLocs = new HashSet<>();
            int[] takenLocsArr = {rc.readSharedArray(Utils.ARR_IDX_MINER_1), rc.readSharedArray(Utils.ARR_IDX_MINER_2),
                rc.readSharedArray(Utils.ARR_IDX_MINER_3), rc.readSharedArray(Utils.ARR_IDX_MINER_4),
                rc.readSharedArray(Utils.ARR_IDX_MINER_5), rc.readSharedArray(Utils.ARR_IDX_MINER_6)};

            minerArrIdx = -1;
            for (int i = 0; i < takenLocsArr.length; i++) {
                int locRaw = takenLocsArr[i];
                if (locRaw != 0xFFF) {
                    takenLocs.add(new MapLocation((locRaw >> X_OFFSET) & X_MASK, locRaw & X_MASK));
                }
                else if (minerArrIdx < 0) {
                    minerArrIdx = i;
                }
            }
            // System.out.println(String.format("[%d, %d], [%d, %d], [%d, %d], [%d, %d], [%d, %d], [%d, %d]",
            //     (takenLocsArr[0] >> X_OFFSET) & X_MASK, takenLocsArr[0] & X_MASK,
            //     (takenLocsArr[1] >> X_OFFSET) & X_MASK, takenLocsArr[1] & X_MASK,
            //     (takenLocsArr[2] >> X_OFFSET) & X_MASK, takenLocsArr[2] & X_MASK,
            //     (takenLocsArr[3] >> X_OFFSET) & X_MASK, takenLocsArr[3] & X_MASK,
            //     (takenLocsArr[4] >> X_OFFSET) & X_MASK, takenLocsArr[4] & X_MASK,
            //     (takenLocsArr[5] >> X_OFFSET) & X_MASK, takenLocsArr[5] & X_MASK));

            // Look through all points around current location that the robot can sense
            for (MapLocation loc : potentialLocs) {
                try {
                    // If there is lead here
                    if (rc.senseLead(loc) > 0 && !takenLocs.contains(loc)) {
                        // System.out.println(String.format("Claimed [%d, %d] - %d", loc.x, loc.y, minerArrIdx));
                        if (minerArrIdx >= 0) {
                            rc.writeSharedArray(Utils.ARR_IDX_MINER_1 + minerArrIdx, ((loc.x << X_OFFSET) | loc.y));
                        }
                        else {
                            Logging.warn(String.format("Miner locs filled, robot [%d] couldn't get a spot", rc.getID()));
                        }
                        return loc;
                    }
                }
                // This should never throw an exception
                catch (GameActionException ex) {
                    Logging.error(String.format("Robot [%d] threw error while finding next lead at {%d, %d}: %s", rc.getID(), loc.x, loc.y, ex.getMessage()));
                }

            }
        }
        // We should never reach here, all places where GameActionException can be thrown should be handled
        catch (GameActionException ex) {}

        return null;
    }
}
