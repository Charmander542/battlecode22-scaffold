package battletoads;

import battlecode.common.*;
import battletoads.*;

/**
 * Utility functions that are used to manipulate a RobotController
 */
public class RobotUtils {

    // Used to define how the moveTo function ended
    public static enum MoveToResult {
        /* The following means we did move */
        TARGET_REACHED, // Success! We successfully made it to the target
        MOVED, // We moved closer to the target

        /* The following means we didn't move */
        TARGET_BLOCKED, // If the target location is blocked and the robot is next to the target
        COOLDOWN_REACHED, // If the robot cannot move any longer
        PATH_BLOCKED, // If there is a robot in the way
    }

    private static GreedyPlanner planner;

    /**
     * Returns whether or not a move was successful based on the move result
     * Anytime an enum is added to MoveToResult, it should be added here
     * @param res The result to test
     * @return True if move was successful
     */
    public static boolean moveToSuccessful(MoveToResult res) {
        switch (res) {
            case TARGET_REACHED:
            case MOVED:
                return true;
            case TARGET_BLOCKED:
            case COOLDOWN_REACHED:
            case PATH_BLOCKED:
        }
        return false;
    }

    /**
     * Moves a given robot to a given location
     * TODO Check bytecode usage
     * @param rc Robot to move
     * @param loc The location to move to
     * @return True if the robot has reached the destination
     */
    public static boolean moveTo(RobotController rc, MapLocation loc) throws GameActionException {
        //Direction dir = rc.getLocation().directionTo(loc);
        if (planner == null) {
            planner = new GreedyPlanner(rc);
        }
        Direction dir = planner.plan(loc);

        if (rc.canMove(dir)) {
            rc.move(dir);
        }
        return rc.getLocation().equals(loc);
    }

    /**
     * Moves a given robot to a given location
     * This returns a more explicit answer to how a move was made
     * TODO Check bytecode usage
     * @param rc Robot to move
     * @param loc The location to move to
     * @return The result of the attempted move
     */
    public static MoveToResult moveToVerbose(RobotController rc, MapLocation loc) throws GameActionException {
        if (planner == null) {
            planner = new GreedyPlanner(rc);
        }

        if (!rc.isMovementReady()) {
            return MoveToResult.COOLDOWN_REACHED;
        }

        Direction dir = planner.plan(loc);
        // Direction dir = rc.getLocation().directionTo(loc);
        MapLocation nextLocation = rc.getLocation().add(dir);

        // If next direction is center, planning found no move poassible
        if (dir == Direction.CENTER || !rc.canMove(dir)) {
            if (rc.getLocation().isAdjacentTo(loc)) {
                return MoveToResult.TARGET_BLOCKED;
            }

            return MoveToResult.PATH_BLOCKED;
        }

        // Move the robot, IDEALLY shouldn't throw an exception
        rc.move(dir);
        if (nextLocation.equals(loc)) {
            return MoveToResult.TARGET_REACHED;
        }
        return MoveToResult.MOVED;
    }
}
