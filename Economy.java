// import the API.
// See xxx for the javadocs.
import MapTools.*;
import bc.*;
import java.util.*;

class Econ {
	static Direction[] dirs = Direction.values();
	
	static boolean allreachableKarb = false;

	//initial factory stuff
	static int totalRocket = 0;

	//normal stuff
	static int[][] karbMapBFS;
	static ArrayList<MapLocation> dest = new ArrayList<MapLocation>();

	static void turn(GameController gc) {
		//System.out.println(dest.size());
		Team otherTeam = null;
		if( gc.team() == Team.Blue )
			otherTeam = Team.Red;
		else
			otherTeam = Team.Blue;
		//overall structure:
//		loop factories
//		loop workers
//          initialFactory OR
//          normalCode
		long round = Player.gc.round();
		long karb = gc.karbonite();    //NOTE: global update after every action that affects it
		//System.out.println(karb);
		int madeRocket = 0;
		HashSet<Integer> stayFactory = new HashSet<Integer>(); //these units will not go out to look for karbonite/make new factories
		HashSet<Integer> stayRocket = new HashSet<Integer>();
		//FACTORIES
		Map<Integer, Integer> initCpy = new HashMap<>(Rollout.initIds);
		for (Unit u : Player.factory) {
			MapLocation mapLoc = u.location().mapLocation();
			int comp = UnionFind.id(Planet.Earth, mapLoc.getX(), mapLoc.getY());
			if(initCpy.containsKey(comp)) {
				initCpy.remove(comp);
				if (u.structureIsBuilt() == 1) {
					Rollout.initIds.remove(comp);
					Rollout.factTime.remove(comp);
					Rollout.factLocs.remove(comp);
					Rollout.pathDist.remove(comp);
					Rollout.workNum.remove(comp);
					System.out.println("round " + round + ": Built factory in component " + comp + "!");
				}
			}
			else {
				VecUnit wF = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Worker);
				for (int i = 0; i < wF.size() && i < 1; i++) {
					int temp = wF.get(i).id(); 
					stayFactory.add(temp);
					if (u.health() == u.maxHealth()) {
						for (int k = 0; k < 8; k++) {
							if (/*round%50==4 && */gc.canReplicate(temp, dirs[k])) {
								//NOTE: ^^^ contains an artificial cap; remove later
								gc.replicate(temp, dirs[k]);
								karb = gc.karbonite();
							}
						}
					} else {
						if (gc.canBuild(temp, u.id()))
							gc.build(temp, u.id());
					}
				}
			}
			if( !(round > 100 && Player.factory.size() < 3) && !(round > 300 && totalRocket == 0) && !(round > 500 && totalRocket < 4) || gc.karbonite() > 150) {
				if(!allreachableKarb && Player.worker.size() == 0 ) {
					if (gc.canProduceRobot(u.id(), UnitType.Worker))
						gc.produceRobot(u.id(), UnitType.Worker);
				}
				if(Player.ranger.size()/6 > Player.healer.size()){
					if (gc.canProduceRobot(u.id(), UnitType.Healer))
						gc.produceRobot(u.id(), UnitType.Healer);
				}
				else{
					if (gc.canProduceRobot(u.id(), UnitType.Ranger))
						gc.produceRobot(u.id(), UnitType.Ranger);
				}
				karb = gc.karbonite();
				for (int k = 0; k < 8; k++)
					if (gc.canUnload(u.id(), dirs[k])) gc.unload(u.id(), dirs[k]);
			}
			else {
				//System.out.println("Shutdown");
			}
		}
		for( Unit u: Player.rocket) {
			VecUnit wF = gc.senseNearbyUnitsByType(u.location().mapLocation(), 2, UnitType.Worker);
			for (int i = 0; i < wF.size() && i < 1; i++) {
				int temp = wF.get(i).id(); 
				stayRocket.add(temp);
				boolean workinRock = false;
				for(int c = 0; c < u.structureGarrison().size(); c++) {
					if(gc.unit(u.structureGarrison().get(c)).unitType() == UnitType.Worker) {
						workinRock = true;
						break;
					}
				}
				if (gc.canLoad(u.id(), temp) && !workinRock) {
					gc.load(u.id(), temp);
					/*for (int k = 0; k < 8; k++) {
						if (gc.canReplicate(temp, dirs[k])) {
							gc.replicate(temp, dirs[k]);
							karb = gc.karbonite();
						}
					}*/
				} else {
					if (gc.canBuild(temp, u.id()))
						gc.build(temp, u.id());
				}
			}
		}
		for(int comp : initCpy.keySet()){
			System.out.println("round " + round + ": Construction of factory in component " + comp + " failed.");
			Rollout.initIds.remove(comp);
			Rollout.factTime.remove(comp);
			Rollout.factLocs.remove(comp);
			Rollout.pathDist.remove(comp);
			Rollout.workNum.remove(comp);
		}

		//WORKERS
		int unreachable = 0;
		for(Unit u : Rollout.normWork){
			if (!u.location().isOnMap()) continue;
			MapLocation mapLoc = u.location().mapLocation();
			boolean doneAction = false;

			//normal code
			if( !allreachableKarb )
				karbBFS(round);
			VecUnit nearFac = gc.senseNearbyUnitsByType(mapLoc, 9, UnitType.Factory);
			//if(stayFactory.contains(u.id())) //System.out.println("Staying by factory");
			if(!stayFactory.contains(u.id()) && !stayRocket.contains(u.id())) {
				//System.out.println("Normal");
				doneAction = false;
				VecUnit nearRock = gc.senseNearbyUnitsByType(mapLoc, 2, UnitType.Rocket);
				if (round > 250 && nearRock.size() != 0) {
					if (gc.canBuild(u.id(), nearRock.get(0).id())) {
						gc.build(u.id(), nearRock.get(0).id());
						doneAction = true;
					} /*else if (gc.canLoad(nearRock.get(0).id(), u.id())) {
						gc.load(nearRock.get(0).id(), u.id());
						//System.out.println("worker loaded");
						doneAction = true;
					}*/
				}
				//VecUnit nearRan = gc.senseNearbyUnitsByType(mapLoc, 36, UnitType.Ranger);
				//if ((nearRan.size() > 2 || round > 600) && round > 250 && madeRocket < 3 && Player.rocket.size() < 3 && !doneAction && (nearFac.size() == 0 || round >500) ) {
				if (round > 250 && madeRocket < 3 && Player.rocket.size() < 3 && !doneAction && (nearFac.size() == 0 || round >500) ) {
					//move from factories
					//System.out.println("Trying to make rocket");
					for (int k = 0; k < 8; k++) {
						if (gc.canBlueprint(u.id(), UnitType.Rocket, dirs[k])) {
							gc.blueprint(u.id(), UnitType.Rocket, dirs[k]);
							karb = gc.karbonite();
							madeRocket++;
							totalRocket++;
							doneAction = true;
							break;
						}
					}
				}
				//if ( nearFac.size() == 0 ) {
				//System.out.println(dest.size());
				if( /*(gc.karbonite() < 100) &&*/ dest.size() > 0 && !allreachableKarb ) {
					int min = 9999;
					int min2 = 9999;
					int dire = -1;
					int dire2 = -1;
					for (int k = 0; k < 8; k++) {
						MapLocation temp = u.location().mapLocation().add(dirs[k]);
						if( temp.getX() < 0 || temp.getX() >= Player.mapEarth.getWidth() || temp.getY() >= Player.mapEarth.getHeight() || temp.getY() < 0 ) continue;
						int movetemp = karbMapBFS[temp.getY()][temp.getX()];
						if( movetemp == -1 ) {
							//System.out.println("-1???");
							unreachable++;
							continue;
						}
						if(Math.min(movetemp, min) != min) {
							dire = k;
							min2 = min;
							min = Math.min(movetemp, min);
						}
						else if( Math.min(movetemp, min2) != min2 ) {
							dire2 = k;
							min2 = Math.min(movetemp, min2);
							if( min2 < min ) {
								int t = min2;
								min2 = min;
								min = t;
								int td = dire2;
								dire2 = dire;
								dire = td;
							}
						}
					}
					//System.out.println("Min: " + min + " Min2: " + min2);
					if (min == 0 ) { //0 or 1??
						if(gc.canHarvest(u.id(), dirs[dire])) {
							//if (round > 1 && karb < 300) {
								//System.out.println("Harvesting karbonite");
								gc.harvest(u.id(), dirs[dire]);
								karb = gc.karbonite();
							//}
						}
						else {
							//Probably make BFS take a hashset??
							//Remove - it's empty
							ArrayList<MapLocation> temp = new ArrayList<MapLocation>();
							for( MapLocation m: dest) {
								if( !m.equals(u.location().mapLocation().add(dirs[dire])) )
									temp.add(m);
							}
							dest = temp;
						}
					}
					else if( (min != 9999 && min != 0) && dire != -1 ) {
						if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire]) ) {
							gc.moveRobot(u.id(), dirs[dire]);
							//System.out.println("Moving to karbonite");
						}
					}
					else if( min2 != 9999 && dire2 != -1 ) {
						if( min2 != 0 ) {
							if( gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[dire2]) ) {
								gc.moveRobot(u.id(), dirs[dire2]);
								//System.out.println("Moving to karbonite");
							}
						}
						else {
							if(gc.canHarvest(u.id(), dirs[dire2])) {
								//if (round > 1 && karb < 300) {
									//System.out.println("Harvesting karbonite");
									gc.harvest(u.id(), dirs[dire]);
									karb = gc.karbonite();
								//}
							}
						}
					}
				}
				//System.out.println("unreachable" + unreachable);
				if( unreachable >= dest.size() ) {
					allreachableKarb = true;
				}
				if (Player.worker.size() < 4 || Rollout.purchQ.isEmpty() && Player.worker.size() < 10 && Player.worker.size() < Player.ranger.size() && Player.worker.size() < Player.healer.size()) {
					for (int k = 0; k < 8; k++) {
						if (/*round%50==4 && */gc.canReplicate(u.id(), dirs[k])) {
							//NOTE: ^^^ contains an artificial cap; remove later
							gc.replicate(u.id(), dirs[k]);
							karb = gc.karbonite();
						}
					}
				}
				if(Rollout.purchQ.isEmpty() && Player.factory.size() < 5 ) {
					for (int k = 0; k < 8; k++) {
						if (gc.canBlueprint(u.id(), UnitType.Factory, dirs[k])) {
							gc.blueprint(u.id(), UnitType.Factory, dirs[k]);
							karb = gc.karbonite();
							doneAction = false;
							break;
						}
					}
				}

				//}
				if (nearFac.size() != 0) {
					Unit fac = nearFac.get(0);
					Direction avoid = mapLoc.directionTo(fac.location().mapLocation());
					for (int k = 0; k < 8; k++) {
						if (!dirs[k].equals(avoid)) {
							if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k])) {
								gc.moveRobot(u.id(), dirs[k]);
								break;
							}
						}
					}
				}
				if( allreachableKarb ) {
					VecUnit enemy = gc.senseNearbyUnitsByTeam(u.location().mapLocation(), 50, otherTeam);
					if( enemy.size() > 0 ) {
						Direction avoid = mapLoc.directionTo(enemy.get(0).location().mapLocation());
						for (int k = 0; k < 8; k++) {
							if (!dirs[k].equals(avoid)) {
								if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k])) {
									gc.moveRobot(u.id(), dirs[k]);
									//System.out.println("move away");
									break;
								}
							}
						}
					}
					else {
						for (int k = 0; k < 8; k++) {
							if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), dirs[k])) {
								gc.moveRobot(u.id(), dirs[k]);
								//System.out.println("move away");
								break;
							}
						}
					}
				}
			}
		}
	}
	private static boolean stayByRocket(GameController gc, Unit u, Unit r) {
		if (!u.location().isOnMap()){
			//System.out.println("ERROR: Attempted call to stayByRocket on a unit in space/garrison");
			return false;
		}
		MapLocation mapLoc = u.location().mapLocation();
		if (!mapLoc.isAdjacentTo(r.location().mapLocation())) {
			if (gc.isMoveReady(u.id()) && gc.canMove(u.id(), mapLoc.directionTo(r.location().mapLocation()))) {
				gc.moveRobot(u.id(), mapLoc.directionTo(r.location().mapLocation()));
				return false;
			}
			return true;
		}
		return true;
	}
	//[y][x] = [height][width]
	public static void karbBFS(long r) {
		if( dest.size() == 0 && r < 50 ) {
			System.out.println("Karbonite Map initiated");
			PlanetMap earth = Player.gc.startingMap(Planet.Earth);
			int[][] karb = Karbonite.matrix(Planet.Earth, 1);
			for(int y = 0; y < earth.getHeight(); y++){
	            for(int x = 0; x < earth.getWidth(); x++){
	                long k = karb[y][x];
	                if( k > 0 ) {
	                	dest.add(new MapLocation(Planet.Earth, x, y));
	                }
	            }
			}
		}
		karbMapBFS = MapAnalysis.BFS(dest);
	}
}