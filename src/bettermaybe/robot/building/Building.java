package bettermaybe.robot.building;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import bettermaybe.dijkstra.Dijkstra;
import bettermaybe.robot.Robot;

public abstract class Building extends Robot {
    public Building(RobotController rc, RobotType type, Dijkstra dijkstra) {
        super(rc, type, dijkstra);
    }

    @Override
    public void run() throws GameActionException {
        super.run();
    }

    protected boolean tryTransform() throws GameActionException {
        if (rc.canTransform()) {
            rc.transform();
            return true;
        }

        return false;
    }
}
