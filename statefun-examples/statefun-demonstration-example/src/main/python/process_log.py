#!/usr/bin/env python

from argparse import ArgumentParser
import matplotlib.pyplot as plt
import numpy as np
import os
import re
# import time


def count_number_of_orbits(options):

    # Define patterns to identify events required for analysis
    created_ptn = re.compile('Created orbit for id')
    refined_ptn = re.compile('Refined orbits with ids')
    cleared_ptn = re.compile('Cleared orbit for id')

    # Count number of orbits which exist after each event analyzed,
    # and print along with date and time components for analysis
    num_orb = 0
    num_orb_list = []
    date_time_list = []
    out_file_path = options.log_file_path.replace(".log", ".dat")
    with open(out_file_path, 'w') as ofp:
        with open(options.log_file_path, 'r') as ifp:
            while True:
                line = ifp.readline()
                # print(line)
                if not line:
                    break
                if created_ptn.search(line) is not None:
                    num_orb += 1
                if refined_ptn.search(line) is not None:
                    num_orb += 1
                if cleared_ptn.search(line) is not None:
                    num_orb -= 1
                flds = line.split()
                date = flds[0].replace("[", "")
                y, m, d = date.split("-")
                time = flds[1].replace("]", "")
                H, M, S = time.split(":")
                msg = "{0} {1} {2} {3} {4} {5:.3f} {6}".format(y, m, d, H, M, float(S), num_orb)
                print(msg)
                ofp.write(msg + '\n')
                # time.sleep(0.001)
                num_orb_list.append(num_orb)
                date_time_list.append(date + "T" + time)

    # Convert date and time to seconds in the run, and numpy arrays
    date_time = np.array(date_time_list, dtype='datetime64[ms]')
    seconds = (date_time - date_time[0]).astype('timedelta64[s]')
    number_of_orbits = np.array(num_orb_list)

    return seconds, number_of_orbits


def plot_number_of_orbits(options, seconds, number_of_orbits):

    # Plot number of orbits as a function of run seconds
    fig, ax = plt.subplots()
    ax.plot(seconds, number_of_orbits)
    head, file_name = os.path.split(options.log_file_path)
    head, file_dir = os.path.split(head)
    ax.set_title(os.path.join(file_dir, file_name))
    ax.set_xlabel("Run time [s]")
    ax.set_ylabel("Number of Orbits")
    plt_file_path = options.log_file_path.replace(".log", ".png")
    plt.savefig(plt_file_path)
    plt.show()


def main():

    # Add and parse arguments
    parser = ArgumentParser()
    parser.add_argument(
        "-l",
        "--log-file-path",
        default="consumer.log",
        help="consumer log file path",
    )
    parser.add_argument(
        "-c",
        "--count-number-of-orbits",
        action="store_true",
        help="count and plot number of orbits as a function of run seconds",
    )
    options = parser.parse_args()

    # Process arguments
    if options.count_number_of_orbits:
        seconds, number_of_orbits = count_number_of_orbits(options)
        plot_number_of_orbits(options, seconds, number_of_orbits)


if __name__ == "__main__":
    main()
