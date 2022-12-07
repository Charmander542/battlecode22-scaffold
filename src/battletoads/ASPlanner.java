package battletoads;

import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.Stack;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import battletoads.Logging;
import battletoads.Utils;
import battletoads.ASComparator;
import battletoads.ASLocation;

/**
 * A* planning
 */
public class ASPlanner {

    // Min heap for locations to search
    private PriorityQueue<ASLocation> locations;

    // Set of visited nodes
    private Set<MapLocation> visited;

    // Final path
    private Stack<MapLocation> path;

    // Robot controller
    private RobotController robotController;

    // Goal and start
    MapLocation s_goal;
    MapLocation s_start;

    /**
     * Constructor to initialize planner with surrounding tiles
     */
    public ASPlanner( RobotController robotController ) {
        // Set robot controller
        this.robotController = robotController;
    }

    /**
     * Initializes required data structures
     */
    private void initialize() {
        // Initialize priority queue
        locations = new PriorityQueue<ASLocation>( 10, new ASComparator() );
        visited = new HashSet<MapLocation>();
        path = new Stack<MapLocation>();
    }

    /**
     * Finds path from start to end
     *  adds map locations to stack
     *
     * @param ASLocation goal ASLocation found by A*
     */
    private void retracePath( ASLocation targetLocation ) {
        ASLocation currLocation = targetLocation;

        while ( currLocation.getLocation() != s_start ) {
            path.push( currLocation.getLocation() );
            currLocation = currLocation.getParent();
        }
    }

    /**
     * A* code
     *
     * @returns true if found path
     */
    private boolean computeShortestPath() throws GameActionException {
        ASLocation start = new ASLocation( s_start,
                                           robotController.senseRubble( s_start ),
                                           0,
                                           s_start.distanceSquaredTo( s_goal ) );
        locations.add( start );

        // Find path
        while ( !locations.isEmpty() ) {
            // Get next best location
            ASLocation currLocation = locations.poll();
            visited.add( currLocation.getLocation() );

            // If this is the one quit
            if ( s_goal.equals( currLocation.getLocation() ) ) {
                retracePath( currLocation );
                return ( true );
            }

            // Add adj locations
            MapLocation currMapLocation = currLocation.getLocation();
            for ( Direction direction : Utils.directions ) {
                MapLocation adjLoc = currMapLocation.add( direction );

                if ( !visited.contains( adjLoc )
                     && robotController.canSenseLocation( adjLoc )
                     && !robotController.isLocationOccupied( adjLoc )
                ) {
                    double passability = robotController.senseRubble( adjLoc );
                    double g = s_start.distanceSquaredTo( adjLoc );
                    double h = s_goal.distanceSquaredTo( adjLoc );

                    ASLocation newLocation = new ASLocation( adjLoc, passability, g, h );
                    newLocation.setParent( currLocation );
                    locations.add( newLocation );
                }
            }
        }

        return ( false );
    }

    /**
     * Get path stack
     * Top of stack is beginning of path
     *
     * @return stack of map locations
     */
    public Stack<MapLocation> getPath() {
        return ( path );
    }

    /**
     * Find path to target
     *
     * @return direction to next location in path
     */
    public Direction plan( MapLocation destination ) throws GameActionException {
        // Default direction to center
        Direction nextDirection = Direction.CENTER;

        // Set goal and start
        s_goal = destination;
        s_start = robotController.getLocation();

        // Project goal onto start radius if too far away
        if ( !robotController.canSenseLocation( s_goal ) ) {
            s_goal = Utils.project( s_start, s_goal, robotController.getType().visionRadiusSquared );
        }

        // Init stuff
        initialize();

        boolean res = computeShortestPath();
        if ( res ) {
            if ( !path.empty() ) {
                nextDirection = s_start.directionTo( path.peek() );
            } else {
                Logging.warn( "ASPlanner found path, but queue is empty!" );
            }
        }

        return ( nextDirection );
    }
}
