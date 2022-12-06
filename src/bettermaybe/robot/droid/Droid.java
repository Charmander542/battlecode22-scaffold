package bettermaybe.robot.droid;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra;
import bettermaybe.robot.Robot;

public abstract class Droid extends Robot {
    public Droid(RobotController rc, RobotType type, Dijkstra dijkstra) {
        super(rc, type, dijkstra);
    }

    @Override
    public void run() throws GameActionException {
        super.run();
    }
}
