package test_bot_CoreyRoberts;

import battlecode.common.*;
import test_bot_CoreyRoberts.Components.*;


//TODO change type of gardener based on how many I have and how many of each I need.
// Farmers = 6 trees surrounding.  Unless I change to a more mobile format.  Place in back, tightly clustered
// Builder = 4 trees and two open slots for soldiers, tanks, etc.  Middle placement, leaving room for tanks to move.
// TODO build one scout IMMEDIATELY to get a head start on collecting bullets. Then build trees.  Then more robots.
class Gardener extends Robot {
    private boolean settled;
    private int maxTreePlots;

    private BroadcastAntenna broadcastAntenna;
    private SensorArray sensorArray;
    private NavigationSystem  navigationSystem;
    private MapLocation currentLocation;

    //TODO this may need to be moved to its own class for use in archon
    //Add new Subtypes package maybe
    public static class GardenerType {
        private static int Unsettled = 1;
        private static int Farmer = 2;
        private static int Builder = 3;
    }

    public Gardener() {
        settled = false;
        maxTreePlots = 4;

        broadcastAntenna = new BroadcastAntenna(robotController);
        sensorArray = new SensorArray(robotController, broadcastAntenna);
        navigationSystem = new NavigationSystem(robotController);
    }
    public void onUpdate() {
        while (true) {
            try {
                //TODO consolidate location into a single component instead of each one tracking it.
                //Probably sensor array or navigation
                currentLocation = robotController.getLocation();
                sensorArray.reset();

                if (!settled) {
                    navigationSystem.tryMove(randomDirection());
                    trySettle();
                }

                if(!settled) {
                    broadcastAntenna.addGardener(GardenerType.Unsettled);
                }else {
                    broadcastAntenna.addGardener(GardenerType.Builder);
                    tryPlantingTrees();
                    tryBuildRobot();
                }
                tryWateringTrees();
                tryShakeTree();

                Clock.yield();
            } catch (Exception e) {
                System.out.println("A Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    public void trySettle() throws GameActionException {
        //Leave enough room from the edge of the map to build trees all around it
        if(!isAwayFromMapEdge(currentLocation,3f)) {
            return;
        }

        //Do not build too close to own trees.  Ignore neutral trees, they'll likely be destroyed soon.
        if(robotController.senseNearbyTrees(2, myTeam).length > 0) {
            return;
        }

        //TODO move to sensor
        //Ignore other bots.  Don't build too close to other gardeners or the Archon
        for (RobotInfo robot: robotController.senseNearbyRobots(5, myTeam)) {
            if(robot.type == RobotType.GARDENER || robot.type == RobotType.ARCHON) {
                return;
            }
        }

        settled = true;
    }

    //TODO create priority build system.
    //Move outside of "if settled" for first scout creation, then only build more after there are enough trees
    //to ensure there are enough bullets to continue
    private void tryBuildRobot() throws GameActionException {
        tryBuildScout();
        tryBuildSoldier();
    }

    public void tryPlantingTrees() throws GameActionException {
        for(int i = 0; i < maxTreePlots; i++) {
            Direction direction = new Direction(i * 1.0472f);

            if (robotController.canPlantTree(direction)) {
                robotController.plantTree(direction);
                return;
            }
        }
    }

    public void tryWateringTrees() throws GameActionException {
        //TODO move to sensor
        TreeInfo[] trees = robotController.senseNearbyTrees(2, myTeam);

        TreeInfo minHealthTree = null;
        for (TreeInfo tree : trees) {
            if (tree.health < 95) {
                if (minHealthTree == null || tree.health < minHealthTree.health) {
                    minHealthTree = tree;
                }
            }
        }
        if (minHealthTree != null) {
            robotController.water(minHealthTree.ID);
        }
    }

    private void tryBuildScout() throws GameActionException {
        int scoutsToHire = broadcastAntenna.getScoutsToHire();
        if(scoutsToHire <= 0) {
            return;
        }

        //Build in the direction of the intentional opening
        Direction direction = new Direction(5.236f);
        if (robotController.canBuildRobot(RobotType.SCOUT, direction)) {
            robotController.buildRobot(RobotType.SCOUT, direction);
            broadcastAntenna.setHireCount(RobotType.SCOUT, scoutsToHire - 1);
        }
    }

    //TODO split between different soldier types
    // Guards and Hunters
    public void tryBuildSoldier() throws GameActionException {
        int soldiersToHire = broadcastAntenna.getSoldiersToHire();
        if(soldiersToHire == 0) {
            return;
        }

        //Build in the direction of the intentional opening
        Direction direction = new Direction(5.236f);
        if (robotController.canBuildRobot(RobotType.SOLDIER, direction)) {
            robotController.buildRobot(RobotType.SOLDIER, direction);
            broadcastAntenna.setHireCount(RobotType.SOLDIER, soldiersToHire - 1);
        }
    }
}