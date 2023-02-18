#!/usr/bin/env python

import os
import sys
import time
import threading
import argparse

from threading import Thread

ready_count = 0

def t1Handler(event1, event2, file1, syncFlag):
    global ready_count

    os.mkfifo(file1)
    fifo = open(file1, "w")
    fifo.write("abcdefghijklmnopqrstuvwxyzabcdef")
   
    print("t1: wrote first 32 bytes to file")

    if syncFlag:
        event1.clear()
        ready_count += 1

        if ready_count < 2:
            if event1.wait(2) == False:
                print("t1: timeout occured waiting for t2 to start")
            else:
                print("ALL: threads synchronized")
        
        event1.set() 
    else:
        time.sleep(2)
        print("t1: no synchronization, continuing")

    try:
        fifo.write("abcdefghijklmnopqrstuvwxyzabcdef")
        fifo.close()
    except IOError, e:
        fifo.close()
    os.remove(file1)
    
    print("t1: wrote second 32 bytes to file")

    if syncFlag:
        event2.set()

def t2Handler(event1, event2, file2, syncFlag):
    global ready_count

    os.mkfifo(file2)
    fifo = open(file2, "w")
    fifo.write("12345678901234567890123456789012")
    
    print("t2: wrote first 32 bytes to file")

    if syncFlag:
        event1.clear()
        ready_count += 1

        if ready_count < 2:
            if event1.wait(2) == False:
                print("t2: timeout occured waiting for t1 to start")
            else:
                print("ALL: threads synchronized")
        
        event1.set()

        if event2.wait(2) == False:
            print("t2: timeout occured waiting for t1 to finish")
        else:
            print("t2: t1 finished")
            time.sleep(2)
        
    else:
        time.sleep(2)
        print("t2: no synchronization, continuing")

    try:
        fifo.write("12345678901234567890123456789012")
        fifo.close()
    except IOError, e:
        fifo.close()

    print("t2: wrote second 32 bytes to file")

    os.remove(file2)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("fifo", nargs="+", help="Fifo files")
    parser.add_argument("--nosync", dest='syncFlag', action='store_false', required=False, help="Flag for thread synchronization")
    parser.set_defaults(syncFlag=True)
    args = parser.parse_args()

    event1 = threading.Event()
    event2 = threading.Event()

    t1 = Thread(target=t1Handler, args=(event1, event2, args.fifo[0], args.syncFlag))
    if len(args.fifo) > 1:
        t2 = Thread(target=t2Handler, args=(event1, event2, args.fifo[1], args.syncFlag))
    
    t1.start()
    if len(args.fifo) > 1:
        t2.start()

    t1.join()
    if len(args.fifo) > 1:
        t2.join()

if __name__ == "__main__":
    main()
