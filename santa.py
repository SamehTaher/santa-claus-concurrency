import threading
import time
import random

# -----------------------------------------------------------
# Santa Claus Problem - Semaphore Implementation (Python)
# COIS 3320 - Project 1
#
# This implementation demonstrates multi-party synchronization
# between Santa and his 9 reindeer with multiple elves. Reindeer have
# priority over elves, and elves are helped strictly in groups
# of three. Semaphores enforce all synchronization rules.
#
# -----------------------------------------------------------

# Global counters
reindeer_count = 0
elf_count = 0


# Semaphores / Locks
mutex = threading.Semaphore(1)         # Protects shared counters
santa_sem = threading.Semaphore(0)     # Santa sleeps on this
reindeer_sem = threading.Semaphore(0)  # Reindeer wait to be harnessed
elf_sem = threading.Semaphore(0)       # Elves wait to see Santa
elf_queue_lock = threading.Semaphore(1)  # Prevent more than 3 elves forming

# Barrier so Santa waits until all 9 reindeer finish
reindeer_finish_sem = threading.Semaphore(0)

# Total number of reindeer (assignment uses 9)
NUM_REINDEER = 9



# Santa Thread

def santa():
    global reindeer_count, elf_count

    while True:
        # Sleep until elves or reindeer wake Santa
        santa_sem.acquire()

        mutex.acquire()
        if reindeer_count == NUM_REINDEER:
            # Priority: Reindeer first
            print("\n🎅 Santa: ALL reindeer returned — preparing the sleigh.")

            # Harness all reindeer
            for _ in range(NUM_REINDEER):
                reindeer_sem.release()

            mutex.release()

            # Wait for reindeer delivery to finish
            for _ in range(NUM_REINDEER):
                reindeer_finish_sem.acquire()

            print("🎅 Santa: Reindeer finished delivery — back to sleep.\n")

        elif elf_count == 3:
            print("\n🎅 Santa: A group of 3 elves needs help.")

            # Allow 3 elves inside
            for _ in range(3):
                elf_sem.release()

            mutex.release()

            # Simulate helping elves
            time.sleep(1)
            print("🎅 Santa: Done helping this group of elves — waiting again.\n")

        else:
            # Should never occur due to controlled wake-up conditions
            mutex.release()



# Reindeer Thread
def reindeer_thread(id):
    global reindeer_count

    while True:
        # Reindeer go on vacation
        time.sleep(random.uniform(1, 3))

        mutex.acquire()
        reindeer_count += 1
        print(f"🦌 Reindeer {id} returned. Count = {reindeer_count}")

        # Last reindeer wakes Santa
        if reindeer_count == NUM_REINDEER:
            print("🦌 All reindeer returned — waking Santa!")
            santa_sem.release()

        mutex.release()

        # Wait to be harnessed
        reindeer_sem.acquire()
        print(f"🦌 Reindeer {id} is being harnessed.")

        # Deliver presents
        time.sleep(random.uniform(0.5, 1.5))
        print(f"🦌 Reindeer {id} finished delivery.")

        # Signal Santa that this reindeer is done
        reindeer_finish_sem.release()



# Elf Thread

def elf_thread(id):
    global elf_count

    while True:
        # Elf works independently
        time.sleep(random.uniform(1, 4))

        elf_queue_lock.acquire()  # Only one group of 3 allowed

        mutex.acquire()
        elf_count += 1
        print(f"🧝 Elf {id} needs help. Current waiting = {elf_count}")

        if elf_count == 3:
            print("🧝 A group of 3 elves formed — waking Santa!")
            santa_sem.release()

        mutex.release()

        # Wait until Santa invites this elf in
        elf_sem.acquire()

        # Elf receives help
        print(f"🧝 Elf {id} is being helped by Santa.")
        time.sleep(0.7)

        # After help, update count and possibly release next group
        mutex.acquire()
        elf_count -= 1

        if elf_count == 0:
            # Last elf from group — allow next 3 to form
            elf_queue_lock.release()

        mutex.release()



# Main Program: Start all threads

def main():
    # Start Santa thread
    threading.Thread(target=santa, daemon=True).start()

    # Start reindeer threads
    for i in range(1, NUM_REINDEER + 1):
        threading.Thread(target=reindeer_thread, args=(i,), daemon=True).start()

    # Start elf threads
    for i in range(1, 20):  # You can adjust number of elves
        threading.Thread(target=elf_thread, args=(i,), daemon=True).start()

    # Keep main thread alive forever
    while True:
        time.sleep(1)


if __name__ == "__main__":
    main()
