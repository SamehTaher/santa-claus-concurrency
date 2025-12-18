import java.util.Random;

/**
 * Santa Claus Problem - Java Monitor Implementation
 * COIS 3320 - Project 1
 *
 * This version uses Java monitors:
 *   - shared state inside SantaWorkshop
 *   - synchronized methods
 *   - wait() / notifyAll() for coordination
 *
 * Properties:
 *   - 9 reindeer must all return before Santa can deliver toys
 *   - Elves see Santa in groups of exactly 3
 *   - Reindeer have priority over elves when both are ready
 */
public class SantaClaus{

    public static void main(String[] args) {
        SantaWorkshop workshop = new SantaWorkshop();
        int numReindeer = 9;
        int numElves = 15; // adjustable

        // Santa thread
        Thread santa = new Thread(() -> {
            try {
                workshop.santaLoop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Santa");
        santa.start();

        // Reindeer threads
        for (int i = 1; i <= numReindeer; i++) {
            int id = i;
            Thread r = new Thread(() -> {
                try {
                    workshop.reindeerRoutine(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Reindeer-" + id);
            r.start();
        }

        // Elf threads
        for (int i = 1; i <= numElves; i++) {
            int id = i;
            Thread e = new Thread(() -> {
                try {
                    workshop.elfRoutine(id);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            }, "Elf-" + id);
            e.start();
        }
    }
}

/**
 * Monitor encapsulating all shared state and synchronization.
 */
class SantaWorkshop {

    private static final int NUM_REINDEER = 9;
    private static final int ELF_GROUP_SIZE = 3;

    private final Random random = new Random();

    // Shared state
    private int reindeerCount = 0;     // how many reindeer have returned
    private int reindeerHitched = 0;   // how many have been harnessed in current round

    private int elfCount = 0;          // how many elves are currently waiting in group
    private int elvesHelped = 0;       // how many elves have been helped in current group

    private boolean reindeerReady = false; // true when Santa is harnessing/delivering
    private boolean helpingElves = false;  // true when Santa is helping a group of 3 elves

    /**
     * Main loop for Santa.
     * Santa sleeps until either:
     *   - all 9 reindeer have returned, or
     *   - a group of 3 elves is waiting.
     * If both are ready, reindeer are served first.
     */
    public synchronized void santaLoop() throws InterruptedException {
        while (true) {
            // Sleep until some condition requires Santa
            while (reindeerCount < NUM_REINDEER && elfCount < ELF_GROUP_SIZE) {
                wait();
            }

            // Reindeer have priority if all 9 are ready
            if (reindeerCount == NUM_REINDEER) {
                System.out.println("\nSanta: All reindeer returned, preparing the sleigh.");
                reindeerReady = true;
                notifyAll(); // wake all reindeer

                // Wait until all reindeer have been hitched and finished delivery
                while (reindeerHitched < NUM_REINDEER) {
                    wait();
                }

                System.out.println("Santa: Reindeer delivery finished, back to sleep.\n");

                // Reset state for next round
                reindeerReady = false;
                reindeerHitched = 0;
                reindeerCount = 0;
            }
            // Otherwise, if exactly 3 elves are waiting and no reindeer priority
            else if (elfCount == ELF_GROUP_SIZE) {
                System.out.println("\nSanta: A group of 3 elves needs help.");
                helpingElves = true;
                notifyAll(); // wake waiting elves

                // Wait until all 3 elves in this group have been helped
                while (elvesHelped < ELF_GROUP_SIZE) {
                    wait();
                }

                System.out.println("Santa: Done helping this group of elves.\n");

                // Reset elf state for next group
                helpingElves = false;
                elvesHelped = 0;
                elfCount = 0;

                // Allow new elves to form another group
                notifyAll();
            }

            // Loop back to wait for the next event
        }
    }

    /**
     * Reindeer high-level routine:
     *   - Go on vacation (sleep),
     *   - Return to Santa's workshop,
     *   - Wait to be harnessed,
     *   - Deliver presents.
     */
    public void reindeerRoutine(int id) throws InterruptedException {
        while (true) {
            // Simulate vacation
            randomSleep(1000, 3000);

            reindeerBack(id);

            // After being hitched and delivering, loop again
        }
    }

    /**
     * Called by a reindeer when it returns from vacation.
     * This method is synchronized to act as part of the monitor.
     */
    public synchronized void reindeerBack(int id) throws InterruptedException {
        reindeerCount++;
        System.out.println("Reindeer " + id + " returned. Count = " + reindeerCount);

        // If this is the 9th reindeer, wake Santa
        if (reindeerCount == NUM_REINDEER) {
            System.out.println("Reindeer " + id + ": all reindeer are back, waking Santa.");
            notifyAll();
        }

        // Wait until Santa sets reindeerReady
        while (!reindeerReady) {
            wait();
        }

        // Now this reindeer is being harnessed / delivering
        System.out.println("Reindeer " + id + " is being harnessed.");

        // Simulate delivery outside monitor to avoid holding the lock too long
        // But we still coordinate completion inside the monitor
        // Quick sleep inside synchronized is OK for demo; for realism, you would unlock first
        randomSleep(500, 1500);
        System.out.println("Reindeer " + id + " finished delivery.");

        reindeerHitched++;
        if (reindeerHitched == NUM_REINDEER) {
            // Last reindeer to finish notifies Santa
            notifyAll();
        }
    }

    /**
     * Elf high-level routine:
     *   - Work independently,
     *   - Occasionally need help from Santa.
     */
    public void elfRoutine(int id) throws InterruptedException {
        while (true) {
            // Simulate independent work
            randomSleep(1000, 4000);

            elfNeedsHelp(id);
        }
    }

    /**
     * Called when an elf needs help from Santa.
     * Elves are admitted in groups of at most 3 at a time.
     */
    public synchronized void elfNeedsHelp(int id) throws InterruptedException {
        // Wait if a full group of 3 is already waiting or currently being helped
        while (elfCount == ELF_GROUP_SIZE || helpingElves) {
            wait();
        }

        elfCount++;
        System.out.println("Elf " + id + " needs help. Current waiting = " + elfCount);

        // If this elf completes a group of 3 and reindeer are not all back, wake Santa
        if (elfCount == ELF_GROUP_SIZE && reindeerCount < NUM_REINDEER) {
            System.out.println("Elf " + id + ": group of 3 formed, waking Santa.");
            notifyAll();
        }

        // Wait until Santa starts helping elves (helpingElves == true)
        while (!helpingElves) {
            // If reindeer become ready in the meantime, Santa may prioritize them.
            wait();
        }

        // At this point, Santa is helping elves
        System.out.println("Elf " + id + " is being helped by Santa.");

        // Simulate the help time (short)
        randomSleep(500, 1200);

        elvesHelped++;
        if (elvesHelped == ELF_GROUP_SIZE) {
            // Last elf in the group notifies Santa that this group is done
            notifyAll();
        }
    }

    /**
     * Utility: sleep for a random time between min and max milliseconds.
     */
    private void randomSleep(int minMillis, int maxMillis) throws InterruptedException {
        int t = minMillis + random.nextInt(maxMillis - minMillis + 1);
        Thread.sleep(t);
    }
}
