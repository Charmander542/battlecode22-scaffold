package player35;

import battlecode.common.*;

public strictfp class RobotPlayer {

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Statics.init(rc);

        while (true) {
            try {
                switch (rc.getType()) {
                    case ARCHON:
                        ArchonStrategy.run(rc);
                        break;
                    case MINER:
                        MinerStrategy.run(rc);
                        break;
                    case SOLDIER:
                        SoldierStrategy.run(rc);
                        break;
                    case LABORATORY:
                        LaboratoryStrategy.run(rc);
                        break;
                    case WATCHTOWER:
                        WatchtowerStrategy.run(rc);
                        break;
                    case BUILDER:
                        BuilderStrategy.run(rc);
                        break;
                    case SAGE:
                        SageStrategy.run(rc);
                        break;
                }
            } catch (GameActionException e) {
                //e.printStackTrace();
            } catch (NullPointerException e) {
                //e.printStackTrace();
            } catch (Exception e) {
                //e.printStackTrace();
                // rc.resign(); // ! For debugging purposes only!
            } finally {
                Clock.yield();
            }
        }
    }
}
