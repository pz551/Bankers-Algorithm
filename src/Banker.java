import java.io.*;
import java.util.*;
import java.lang.Math;
/**
 *  The class created for storing the information for each activity. It contains all the detailed information for
 *  each activity.
 * 
 */
// the class for the each activities including initiate/request/release and terminate.
class Activity {
	String type; // store the names of types of activities
	int delay; // counter for the delay time
	int resource; // store the number of resources
	int amount; // store number of resources

	public Activity(String type, int delay, int resource, int amount) {
		this.type = type;
		this.delay = delay;
		this.resource = resource;
		this.amount = amount;
	}

}

/**
 *  The class created for storing the information for each task. It contains all the detailed information for
 *  each task.
 * 
 */
// the class for the tasks and its information
class Task {
	public static boolean isBlocked;
	ArrayList<Activity> activities; // store all the activities for task
	int currentActNum; // to make sure fifo
	int id; // task id
	int claim; // store initial claims
	int finishTime; // store finishing time
	int waitTime; // store waiting time
	double percentage; // store percentage of time spent waiting
	boolean aborted; // keep track of status
	int cycleAborted; // keep track of cycle at which task was aborted
	int tempResource;
	int tempAmount;

	public Task(int id) {
		this.activities = new ArrayList<Activity>();
		this.currentActNum = 0;
		this.id = id;
		this.claim = -1;
		this.finishTime = -1;
		this.waitTime = 0;
		this.aborted = false;
		//	this.isBlocked = false;
		this.cycleAborted = -1;
		this.tempResource = 0;
		this.tempAmount = 0;
	}
}

// so that we can use Collection.sort to get the tasks in ID order.
class CompId implements Comparator<Task> {
	// this comparator compares tasks based on their id
	public int compare(Task t1, Task t2) {
		if (t1.id > t2.id) {
			return 1;
		} else { // two tasks will never have the same id
			return -1;
		}
	}
}

public class Banker {
	static int taskNum; // number of tasks
	static int resNum; // number of resource types
	static CompId compId = new CompId(); // comparator to compare by task id
	static int[] resources;
	static ArrayList<Task> tasks = new ArrayList<Task>();
	static int[][] occupiedTable, claimTable;
	private static int[][] availResCycle;


	// main function 
	public static void main(String[] args) throws IOException {
		String input = args[args.length - 1];
		//	for (int i=1; i<=13; i++) { //#
		//		if (i<10)//#
		//			input="input-0"+i+".txt";//#
		//		else//#
		//			input="input-"+i+".txt";//#

		FileReader file = new FileReader(input);
		getInput(file);
		// call FIFO
		FIFO(tasks, resources, occupiedTable, claimTable);

		// call Banker's
		file = new FileReader(input);
		getInput(file);

		bankers(tasks, resources, occupiedTable, claimTable);
		file.close();
		//		}//#
	}

	//get input from the input file
	private static void getInput(FileReader file) {
		Scanner scanner = new Scanner(file);
		taskNum = scanner.nextInt();
		resNum = scanner.nextInt();

		// store total number of recourses of each type
		resources = new int[resNum];
		availResCycle = new int[resNum][1000];
		// initialize resources array with number of total resources by type
		for (int i = 0; i < resNum; i++) {
			int currResources = scanner.nextInt();
			resources[i] = currResources;
		}

		// initialize lists of tasks
		tasks = new ArrayList<Task>();

		for (int i = 0; i < taskNum; i++) {
			tasks.add(new Task(i + 1));
		}

		// add all activities into each task
		while (scanner.hasNext()) {

			Activity act;

			String currType = scanner.next();
			int currId = scanner.nextInt();
			int currDelay = scanner.nextInt();
			int currResource = scanner.nextInt();
			int currNum = scanner.nextInt();

			act = new Activity(currType, currDelay, currResource, currNum);

			tasks.get(currId - 1).activities.add(act);
		}

		// table to store allocated resources by task
		occupiedTable = new int[taskNum][resNum];

		// table to store claimed resources by task
		// initialize claim table with initial claims
		claimTable = new int[taskNum][resNum];
		for (int i=0; i<tasks.size(); i++) {
			Task currItTask=tasks.get(i);
			for (int j=0; j<currItTask.activities.size(); j++) {
				Activity currItAct=currItTask.activities.get(j);
				if (currItAct.type.equals("initiate")) {
					claimTable[currItTask.id - 1][currItAct.resource - 1] = currItAct.amount;
				}
			}
		}	

	}

	// private static void addToTasks(Scanner in) {
	// TODO Auto-generated method stub

	// }

	/**
	 * FIFO method which includes its algorithm and output to the screen.
	 * Satisfy a request if possible, if not make the task wait; when a release occurs, try to satisfy pending requests in a FIFO manner.
	 **/
	public static void FIFO(ArrayList<Task> tasks, int[] availRes, int[][] occupiedTable, int[][] claimTable) {
		ArrayList<Task> blocked = new ArrayList<Task>(); // list of blocked tasks
		ArrayList<Task> terminated = new ArrayList<Task>(); // list of terminated tasks
		ArrayList<Task> aborted = new ArrayList<Task>(); // list of aborted tasks
		// initialize list for tasks that will be moved from blocked to tasks
		int [] releaseList= new int[resNum];
		ArrayList<Task> addBack = new ArrayList<Task>();
		boolean  isDeadlocked = true;
		int cycle = 0;

		while (!blocked.isEmpty() || !tasks.isEmpty()) {
			Collections.sort(tasks, compId); // sort tasks by id

			// check blocked tasks first
			if (!blocked.isEmpty()) {
				// update waiting time
				for (int i = 0; i < blocked.size(); i++) {
					blocked.get(i).waitTime++;
				}

				for (int i = 0; i < blocked.size(); i++) {
					if (blocked.get(i).currentActNum < blocked.get(i).activities.size()) {

						Activity currBlockedAct = blocked.get(i).activities.get(blocked.get(i).currentActNum);// current
						// Activity

						// processing only when there's no delay time.
						if (currBlockedAct.delay > 0) {
							currBlockedAct.delay--;
							continue;
						} 

						if (!currBlockedAct.type.equals("request")) {
							continue;
						}
						// processing blocked list only if its request
						// if the task requests less than are available, grant request

						if (currBlockedAct.amount <= availRes[currBlockedAct.resource - 1]) {

							// update available resource and allocated resource tables
							availRes[currBlockedAct.resource - 1] -= currBlockedAct.amount;
							occupiedTable[blocked.get(i).id - 1][currBlockedAct.resource
							                                     - 1] += currBlockedAct.amount;
							blocked.get(i).currentActNum++;
							// remove task from blocked and add to addBack list
							addBack.add(blocked.remove(i)); 
							i--; 
						} else {
							// not granted
							// update request table for each blocked task
							blocked.get(i).tempResource = currBlockedAct.resource;
							blocked.get(i).tempAmount = currBlockedAct.amount;
						}
					}


				}
			}

			// check the rest of the tasks
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).currentActNum < tasks.get(i).activities.size()) {
					Activity currAct = tasks.get(i).activities.get(tasks.get(i).currentActNum);// current Activity
					// check if the activity has a delay
					if (currAct.delay > 0) {
						currAct.delay--;
						continue;
					} 


					if (currAct.type.equals("initiate")) {
						tasks.get(i).currentActNum++;
					} else if (currAct.type.equals("request")) {
						// if the task requests less than are available, grant request
						if (currAct.amount <= availRes[currAct.resource - 1]) {

							availRes[currAct.resource - 1] -= currAct.amount;
							occupiedTable[tasks.get(i).id - 1][currAct.resource - 1] += currAct.amount;

							tasks.get(i).currentActNum++;
						} else {

							// update request table for each blocked task
							tasks.get(i).tempResource = currAct.resource;
							tasks.get(i).tempAmount = currAct.amount;
							blocked.add(tasks.get(i)); // add task to blocked
							tasks.remove(i); // remove task from task list
							i--; // decrement index because task list size has also been decremented
						}
					} else if (currAct.type.equals("release")) {

						// update released resource and allocated resource tables
						releaseList[currAct.resource - 1] = currAct.amount;
						occupiedTable[tasks.get(i).id - 1][currAct.resource - 1] -= currAct.amount;
						tasks.get(i).currentActNum++;
						currAct = tasks.get(i).activities.get(tasks.get(i).currentActNum);

						// check if task will terminate
						if (currAct.type.equals("terminate")) {
							if (currAct.delay>0) {
								currAct.delay--;
							} else {
								tasks.get(i).finishTime = cycle + 1;
								terminated.add(tasks.remove(i));
								i--; 
							}
						}
					} else if (currAct.type.equals("terminate")) {
						tasks.get(i).finishTime = cycle + 1;
						terminated.add(tasks.remove(i));
						i--; 
					}

				}
			}

			// add tasks back into task list from blocked
			for (int i = 0; i < addBack.size(); i++) {
				tasks.add(addBack.remove(i));
				i--; 
			}

			for (int i = 0; i < releaseList.length; i++) {
				availRes[i] += releaseList[i];
				releaseList[i] = 0;	// reset released array
			}

			// check for deadlock
			if (tasks.isEmpty() && !blocked.isEmpty()) {
				for (Task b : blocked) {
					if (availRes[b.tempResource - 1] >= b.tempAmount) {
						isDeadlocked = false;
					}
				}


				//abort the task if neccassary
				if (isDeadlocked) {	
					boolean enoughReleased = false;
					while (!enoughReleased && !blocked.isEmpty()) {
						// find the task in blocked list with smallest id
						Task min = new Task(taskNum + 1);
						for (Task t : blocked) {
							if (t.id <= min.id) {
								min = t;
							}
						}
						blocked.remove(min); // abort min task
						aborted.add(min); // add to aborted list
						min.aborted = true;
						// release min's resources
						for (int i = 0; i < resNum; i++) {
							availRes[i] += occupiedTable[min.id - 1][i];
							occupiedTable[min.id - 1][i] = 0;
						}

						for (Task b : blocked) {
							if (availRes[b.tempResource - 1] >= b.tempAmount) {
								enoughReleased = true;
							}
						}
					}
				}
				isDeadlocked = true;
			}
			//isDeadlocked = true;
			cycle++;

		}

		// output part*
		int totalTime = 0,
				totalWait = 0;
		int totalPercentage;
		for (Task t : terminated) {
			tasks.add(t);
		}
		for (Task t : aborted) {
			tasks.add(t);
		}
		Collections.sort(tasks, compId);
		System.out.printf("               FIFO\n");
		for (Task t : tasks) {
			t.percentage = (double) t.waitTime * 100 / (double) t.finishTime;
			t.percentage = (int) Math.round (t.percentage);

			System.out.printf("     %s %d\t", "Task", t.id);
			if (t.aborted) {
				System.out.printf(" aborted\n");
			} else {
				System.out.printf("%2d  %2d \t %d%%\n", t.finishTime,t.waitTime, (int)t.percentage);
				totalWait += t.waitTime;
				totalTime += t.finishTime;
			}
		}

		double doublePercent = (double) totalWait * 100 / (double) totalTime;
		totalPercentage = (int) Math.round (doublePercent);
		System.out.printf("     total\t");
		System.out.printf(" %d   %d \t %d%%\n\n", totalTime, totalWait, totalPercentage);
	}

	//abort the locked ones when necessary
	public static void abort(ArrayList<Task> blocked, ArrayList<Task> aborted, int[] availRes, int[][] occupiedTable) {
		boolean enoughReleased = false;
		while (!enoughReleased && !blocked.isEmpty()) {
			// find the task in blocked list with smallest id
			Task min = new Task(taskNum + 1);
			for (Task t : blocked) {
				if (t.id <= min.id) {
					min = t;
				}
			}
			blocked.remove(min); // abort min task
			aborted.add(min); // add to aborted list
			min.aborted = true;
			// release min's resources
			for (int i = 0; i < resNum; i++) {
				availRes[i] += occupiedTable[min.id - 1][i];
				occupiedTable[min.id - 1][i] = 0;
			}

			for (Task b : blocked) {
				if (availRes[b.tempResource - 1] >= b.tempAmount) {
					enoughReleased = true;
				}
			}
		}
	}

	//check if the banker's available resources all are less than the firstly claimed values

	public static boolean safe(int index, int[] availRes, int[][] claimTable, int[][] occupiedTable) {
		boolean res = true;
		// check that there are enough resources available for the given task
		for (int i = 0; i < availRes.length; i++) {
			if (availRes[i] < claimTable[index - 1][i] - occupiedTable[index - 1][i]) {
				res = false;
			}
		}
		return res;
	}


	/**
	 * the banker algorithm as well as the output. each iteration of a loop represents one circle and 
	 * we deal with the locked in advance just the same as we deal with FIFO method
	 */

	public static void bankers(ArrayList<Task> tasks, int[] availRes, int[][] occupiedTable, int[][] claimTable) {
		ArrayList<Task> blocked = new ArrayList<Task>(); // list of blocked tasks
		ArrayList<Task> terminated = new ArrayList<Task>(); // list of terminated tasks
		ArrayList<Task> aborted = new ArrayList<Task>(); // list of aborted tasks
		// initialize list for tasks that will be moved from blocked to tasks
		ArrayList<Task> addBack = new ArrayList<Task>();
		boolean isDeadlocked = true;
		boolean granted=false;
		// array of released resources at the end of each cycle
		int[] released = new int[resNum];
		int cycle = 0;

		while (!blocked.isEmpty() || !tasks.isEmpty()) {
			Collections.sort(tasks, compId); // sort tasks by id


			for (int i = 0; i < availRes.length; i++) 
				availResCycle[i][cycle] =availRes[i];




			// check blocked tasks first
			if (!blocked.isEmpty()) {
				// update waiting time
				for (int i = 0; i < blocked.size(); i++) {
					blocked.get(i).waitTime++;
				}

				for (int i = 0; i < blocked.size(); i++) {

					if (blocked.get(i).currentActNum < blocked.get(i).activities.size()) {
						Activity currBlockedAct = blocked.get(i).activities.get(blocked.get(i).currentActNum);// current
						// Activity
						// check if the activity has a delay

						if (currBlockedAct.delay > 0) {
							currBlockedAct.delay--;
							continue;
						} else {
							if (currBlockedAct.type.equals("request")) {
								// check if the task's claim is larger than the resources available
								if (currBlockedAct.amount + occupiedTable[blocked.get(i).id - 1][currBlockedAct.resource
								                                                                 - 1] > claimTable[blocked.get(i).id - 1][currBlockedAct.resource - 1]) {

									// abort task and release resources
									released[currBlockedAct.resource - 1] += currBlockedAct.amount;
									occupiedTable[blocked.get(i).id - 1][currBlockedAct.resource
									                                     - 1] -= currBlockedAct.amount;
									blocked.get(i).aborted = true;
									blocked.get(i).cycleAborted = cycle;
									aborted.add(blocked.remove(i));
									i--;

								} else {

									// if the task requests less than or all the available resources, check if
									// granting the request is safe
									if (currBlockedAct.amount <= availRes[currBlockedAct.resource - 1]) {
										int claimX=blocked.get(i).id;
										granted=true;

										for (int k = 0; k < availRes.length; k++) {
											if (availRes[k] < claimTable[claimX - 1][k] - occupiedTable[claimX - 1][k]) {
												granted = false;
											}
										}

									}
									if (granted) {

										// update available resource and allocated resource tables
										availRes[currBlockedAct.resource - 1] -= currBlockedAct.amount;
										occupiedTable[blocked.get(i).id - 1][currBlockedAct.resource
										                                     - 1] += currBlockedAct.amount;
										blocked.get(i).currentActNum++;
										// remove task from blocked and add to addBack list
										addBack.add(blocked.remove(i));
										i--; // decrement index because blocked list size has also been decremented
									} else {

										// update request table for each blocked task
										blocked.get(i).tempResource = currBlockedAct.resource;
										blocked.get(i).tempAmount = currBlockedAct.amount;
									}
								}
								granted = false; // reset
							}
						}
					}
				}
			}

			// check the rest of the tasks
			for (int i = 0; i < tasks.size(); i++) {
				if (tasks.get(i).currentActNum < tasks.get(i).activities.size()) {
					Activity currAct = tasks.get(i).activities.get(tasks.get(i).currentActNum);// current Activity
					// check if the activity has a delay
					if (currAct.delay > 0) {
						currAct.delay--;
						continue;
					} else {
						if (currAct.type.equals("initiate")) {
							if (currAct.amount <= resources[currAct.resource - 1]) {

								tasks.get(i).currentActNum++;
							} else {
								// if the initial claim is larger than the available resources, abort the task

								tasks.get(i).aborted = true;
								tasks.get(i).cycleAborted = cycle;
								aborted.add(tasks.remove(i));
								i--;
							}
						} else if (currAct.type.equals("request")) {
							// check if the claim is larger than the available resources
							if (currAct.amount + occupiedTable[tasks.get(i).id - 1][currAct.resource
							                                                        - 1] > claimTable[tasks.get(i).id - 1][currAct.resource - 1]) {

								// release any resources and abort task
								released[currAct.resource - 1] += currAct.amount;
								occupiedTable[tasks.get(i).id - 1][currAct.resource - 1] -= currAct.amount;
								tasks.get(i).aborted = true;
								tasks.get(i).cycleAborted = cycle;
								aborted.add(tasks.remove(i));
								i--;

							} else {
								// if the request is smaller/equal to the available resources, check if granting
								// is safe
								if (currAct.amount <= availRes[currAct.resource - 1]) {
									granted = safe(tasks.get(i).id, availRes, claimTable, occupiedTable);
								}

								if (granted) {

									// update available resource and allocated resource tables
									availRes[currAct.resource - 1] -= currAct.amount;
									occupiedTable[tasks.get(i).id - 1][currAct.resource - 1] += currAct.amount;
									tasks.get(i).currentActNum++;
								} else {

									// update request table for each blocked task
									tasks.get(i).tempResource = tasks.get(i).activities
											.get(tasks.get(i).currentActNum).resource;
									tasks.get(i).tempAmount = tasks.get(i).activities
											.get(tasks.get(i).currentActNum).amount;
									blocked.add(tasks.get(i)); // add task to blocked
									tasks.remove(i); // remove task from task list
									i--; // decrement index because task list size has also been decremented
								}
							}
							granted = false;
						} else if (currAct.type.equals("release")) {

							// update released resource and allocated resource tables
							released[currAct.resource - 1] = currAct.amount;
							occupiedTable[tasks.get(i).id - 1][currAct.resource - 1] -= currAct.amount;
							tasks.get(i).currentActNum++;
							// check if task will terminate
							currAct = tasks.get(i).activities.get(tasks.get(i).currentActNum);

							// check if task will terminate
							if (currAct.type.equals("terminate")) {
								if (currAct.delay>0) {
									currAct.delay--;
								} else {
									tasks.get(i).finishTime = cycle + 1;
									terminated.add(tasks.remove(i));
									i--; 
								}
							}
						} else if (currAct.type.equals("terminate")) {
							tasks.get(i).finishTime = cycle + 1;
							terminated.add(tasks.remove(i));
							i--; // decrement index because task list size has also been decremented
						}
					}
				}
			}

			// add tasks back into task list from blocked
			for (int i = 0; i < addBack.size(); i++) {
				tasks.add(addBack.remove(i));

				i--; // decrement index because list size has also been decremented
			}

			// update available resource table with released resources
			for (int i = 0; i < released.length; i++) {
				availRes[i] += released[i];
				released[i] = 0; // reset released array
			}

			// check for deadlock
			if (tasks.isEmpty() && !blocked.isEmpty()) {
				for (Task b : blocked) {
					if (availRes[b.tempResource - 1] >= b.tempAmount) {
						isDeadlocked = false;
					}
				}
				if (isDeadlocked) {

					abort(blocked, aborted, availRes, occupiedTable);
				}
				isDeadlocked = true;
			} 
			cycle++;

		}

		// printBankers
		int totalTime = 0, totalWait = 0;
		int totalPercentage;
		for (Task t : terminated) {
			tasks.add(t);
		}

		boolean before = false;
		for (int i = 0; i < aborted.size(); i++) {
			if (aborted.get(i).cycleAborted == 0) {
				before = true;
			}
		}

		if (before) {
			System.out.print("Banker aborts task ");
			for (int i = 0; i < aborted.size(); i++) {
				if (aborted.get(i).cycleAborted == 0) {
					if (i != aborted.size() - 1) {
						System.out.print(aborted.get(i).id + ", ");
					} else {
						System.out.print(aborted.get(i).id);
					}
				}
			}
			System.out.print(" before run begins:\n");
		}
		for(int i = 0; i < aborted.size(); i++) {
			if (aborted.get(i).cycleAborted == 0) {
				System.out.printf("   Task %d: claim for resource %d (%d) exceeds number of units present (%d)\n", aborted.get(i).id, aborted.get(i).activities.get(0).resource, aborted.get(i).activities.get(0).amount, resources[aborted.get(i).activities.get(0).resource - 1]);
			} else {
				System.out.printf("During cycle %d-%d of Banker's algorithms\n", aborted.get(i).cycleAborted, aborted.get(i).cycleAborted+1 );
				System.out.printf("   Task %d's request for resource exceeds its claim; aborted.\n", 
						aborted.get(i).id 
						//,aborted.get(i).activities.get(0).resource, aborted.get(i).activities.get(0).amount,claimTable[aborted.get(i).id-1][aborted.get(i).activities.get(0).resource - 1]
						);
				//System.out.printf(" %d units available next cycle\n", availResCycle[aborted.get(i).activities.get(0).resource-1][aborted.get(i).cycleAborted+2]);

			}
		}

		for (Task t : aborted) {
			tasks.add(t);
		}
		System.out.println();
		Collections.sort(tasks, compId);
		System.out.printf("             BANKER'S\n");
		for (Task t : tasks) {
			t.percentage = (double) t.waitTime * 100 / (double) t.finishTime;
			t.percentage = (int) Math.round (t.percentage);

			System.out.printf("     %s %d\t", "Task", t.id);
			if (t.aborted) {
				System.out.printf(" aborted\n");
			} else {
				System.out.printf("%2d  %2d \t %d%%\n", t.finishTime,t.waitTime, (int)t.percentage);
				totalWait += t.waitTime;
				totalTime += t.finishTime;
			}
		}

		double doublePercent = (double) totalWait * 100 / (double) totalTime;
		totalPercentage = (int) Math.round (doublePercent);
		System.out.printf("     total \t");
		System.out.printf("%d   %d \t %d%%\n\n", totalTime, totalWait, totalPercentage);

	}




}
