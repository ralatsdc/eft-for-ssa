#!/usr/bin/env python

from argparse import ArgumentParser
import matplotlib.pyplot as plt
import numpy as np
import os
import re
import datetime

import hhh


# import time


def count_number_of_orbits(options):
    # Define patterns to identify events required for analysis
    created_ptn = re.compile('Created orbitId')
    # refined_ptn = re.compile('Created refined orbitId')
    # saved_ptn = re.compile('Saved orbitId')
    cleared_ptn = re.compile('Cleared orbitId')

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
                if cleared_ptn.search(line) is not None:
                    num_orb -= 1
                try:
                    flds = line.split()
                    date = flds[0].replace("[", "")
                    y, m, d = date.split("-")
                    time = flds[1].replace("]", "")
                    H, M, S = time.split(":")
                    msg = "{0} {1} {2} {3} {4} {5:.3f} {6}".format(y, m, d, H, M, float(S), num_orb)
                    # print(msg)
                    ofp.write(msg + '\n')
                    # time.sleep(0.001)
                    num_orb_list.append(num_orb)
                    date_time_list.append(date + "T" + time)
                except:
                    continue

    # Convert date and time to seconds in the run, and numpy arrays
    date_time = np.array(date_time_list, dtype='datetime64[ms]')
    seconds = (date_time - date_time[0]).astype('timedelta64[s]')
    number_of_orbits = np.array(num_orb_list)

    return seconds, number_of_orbits


def run_time_processing(options):
    # Define patterns to identify events required for analysis
    track_ptn = re.compile('Created trackId')
    created_ptn = re.compile('Created orbitId')
    refined_ptn = re.compile('Refined orbitIds')
    correlated_ptn = re.compile('Correlated orbitIds')
    cleared_ptn = re.compile('Cleared orbitId')

    # Count number of orbits which exist after each event analyzed,
    # and print along with date and time components for analysis
    num_orb = 0
    num_orb_message_dict = {}
    num_orb_list = []
    processing_time_list = []
    date_time_list = []

    # Key: orbit_id Value: track associated with that orbit
    orbit_track_dict = {}
    track_messages_dict = {}
    out_file_path = options.log_file_path.replace(".log", ".dat")
    with open(out_file_path, 'w') as ofp:
        with open(options.log_file_path, 'r') as ifp:
            while True:
                line = ifp.readline()
                # print(line)
                if not line:
                    # print(track_messages_dict)
                    break
                if track_ptn.search(line) is not None:
                    track_id = re.search("(?<=Created track for id )(\d+)", line).group(0)

                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if created_ptn.search(line) is not None:
                    num_orb += 1
                    orbit_id = re.search("(?<=Created orbit for id )(\d+)", line).group(0)
                    track_id = re.search("(?<=from track with id )(\d+)", line).group(0)

                    orbit_track_dict[orbit_id] = track_id

                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if correlated_ptn.search(line) is not None:
                    orbit_id = re.search("(?<=Correlated orbits with ids )(\d+)", line).group(0)

                    # get track associated with orbit_id
                    try:
                        track_id = orbit_track_dict[orbit_id]
                    except:
                        print(
                            "Cannot add track_id " + track_id + " to orbit_id " + orbit_id + " because orbit_id " + orbit_id + " has not been set. Ensure log file is ordered.")

                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if refined_ptn.search(line) is not None:
                    num_orb += 1
                    orbit_id = re.search("(?<=Refined orbits with ids )(\d+)", line).group(0)
                    new_orbit_id = re.search("(?<=create orbit with id )(\d+)", line).group(0)

                    # get track associated with orbit_id
                    track_id = orbit_track_dict[orbit_id]

                    # add new_orbit_id to track_orbit_ids_dict
                    orbit_track_dict[new_orbit_id] = track_id

                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if cleared_ptn.search(line) is not None:
                    num_orb -= 1

                num_orb_message_dict[line] = num_orb

            for message_list in track_messages_dict.values():
                # print(message_list)

                earliest_message = message_list[0]
                latest_message = message_list[1]

                num_orb_list.append(num_orb_message_dict[earliest_message])

                flds = earliest_message.split()
                date = flds[0].replace("[", "")
                y, m, d = date.split("-")
                time = flds[1].replace("]", "")
                H, M, float_S = time.split(":")
                US = (float(float_S) * 1000000) % 1000000
                S = int(float(float_S))
                early_msg = "{0} {1} {2} {3} {4} {5:.3f} {6}".format(y, m, d, H, M, float(float_S), num_orb)

                dt_early = datetime.datetime(int(y), int(m), int(d), int(H), int(M), int(S), int(US))

                date_time_list.append(date + "T" + time)

                flds = latest_message.split()
                date = flds[0].replace("[", "")
                y, m, d = date.split("-")
                time = flds[1].replace("]", "")
                H, M, float_S = time.split(":")
                US = (float(float_S) * 1000000) % 1000000
                S = int(float(float_S))
                late_msg = "{0} {1} {2} {3} {4} {5:.3f} {6}".format(y, m, d, H, M, float(float_S), num_orb)

                dt_late = datetime.datetime(int(y), int(m), int(d), int(H), int(M), int(S), int(US))

                processing_time = dt_late - dt_early
                ofp.write(str(dt_early) + " " + str(processing_time) + '\n')
                # print(processing_time)
                # time.sleep(0.001)
                processing_time_list.append(processing_time)

    # Convert date and time to seconds in the run, and numpy arrays
    date_time = np.array(date_time_list, dtype='datetime64[ms]')
    seconds = (date_time - date_time[0]).astype('timedelta64[s]')
    processing_time = np.array(processing_time_list, dtype='timedelta64[ms]')
    number_of_orbits = np.array(num_orb_list)

    return seconds, processing_time, number_of_orbits


def plot_count(options, seconds, number_of_orbits):
    # Plot number of orbits as a function of run seconds
    fig, ax = plt.subplots()
    ax.plot(seconds, number_of_orbits, label='actual')
    head, file_name = os.path.split(options.log_file_path)
    head, file_dir = os.path.split(head)
    ax.set_title(os.path.join(file_dir, file_name))
    ax.set_xlabel("Time (s)")
    plt.ylabel('Total States')

    if options.plot_with_simulation:
        interval, track_number = options.plot_with_simulation.split(',')
        t, s_n = hhh.run(int(track_number), int(interval))

        # calculate time step for simulated values, based on the track interval and the number of objects used
        step = int(options.track_interval_time) * int(options.object_number)

        end_time = seconds[-1].astype(int)

        times = range(0, end_time + step, step)

        # modify s_n to match scale of docker simulation
        s_n = [int(s) * int(options.object_number) for s in s_n]

        # find the number of constant data points after stabilization
        data_difference = times[-1] // step - len(s_n)

        # ensure line starts at 0
        s_n.insert(0, 0)

        # add constant data points to end of simulated values
        for i in range(0, data_difference):
            s_n.append(s_n[-1])

        plt.plot(
            times,
            s_n, label="expected")
        plt.legend(loc='best', frameon=False)

    if options.show_tracks:
        interval = int(options.track_interval_time)
        time = range(0, seconds[-1].astype(int), interval)
        tracks = range(0, len(time), 1)
        plt.plot(time, tracks, label="Data")
        plt.legend(loc='best', frameon=False)
        plt.legend().get_texts()[0].set_text("Stored States")

    plt_file_path = options.log_file_path.replace(".log", "_c.png")
    plt.savefig(plt_file_path)
    plt.xlim(xmin=0)
    plt.ylim(ymin=0)
    plt.show()


def plot_processing_time(options, seconds, processing_time):
    # Plot number of orbits as a function of run seconds
    fig, ax = plt.subplots()
    ax.plot(seconds, processing_time)
    head, file_name = os.path.split(options.log_file_path)
    head, file_dir = os.path.split(head)
    ax.set_title(os.path.join(file_dir, file_name))
    ax.set_xlabel("Time (s)")
    ax.set_ylabel("Processing Time (ms)")
    plt_file_path = options.log_file_path.replace(".log", "_t.png")
    plt.savefig(plt_file_path)
    plt.show()


def plot_processing_time_as_count(options, number_of_orbits, processing_time):
    # Plot number of orbits as a function of run seconds
    fig, ax = plt.subplots()
    ax.scatter(number_of_orbits, processing_time)
    head, file_name = os.path.split(options.log_file_path)
    head, file_dir = os.path.split(head)
    ax.set_title(os.path.join(file_dir, file_name))
    ax.set_xlabel("Total States")
    ax.set_ylabel("Processing Time (s)")
    plt_file_path = options.log_file_path.replace(".log", "_tc.png")
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
    parser.add_argument(
        "-t",
        "--run-time-processing",
        action="store_true",
        help="plot time to complete track processing as a function of run time",
    )
    parser.add_argument(
        "-tc",
        "--run-time-processing-as-function-of-count",
        action="store_true",
        help="plot time to complete track processing as a function of run time",
    )
    parser.add_argument(
        "-s",
        "--plot-with-simulation",
        # default="4,2",
        help="plot processed log next to simulated expected values. Values are expiration and delete interval, comma separated: i.e.: 4,2 represents 4 delete interval, 2 track number",
    )

    parser.add_argument(
        "-i",
        "--track-interval-time",
        default="10",
        help="interval in which tracks are produced in the simulation, in seconds. Include this with the -s argument",
    )

    parser.add_argument(
        "-n",
        "--object-number",
        default="1",
        help="number of objects included in the simulation. Include this with the -s argument",
    )

    parser.add_argument(
        "--show-tracks",
        action="store_true",
        help="show tracks going into simulation as a function of time",
    )
    options = parser.parse_args()

    # Process arguments
    if options.count_number_of_orbits:
        seconds, number_of_orbits = count_number_of_orbits(options)
        plot_count(options, seconds, number_of_orbits)

    if options.run_time_processing:
        seconds, processing_time, number_of_orbits = run_time_processing(options)
        plot_processing_time(options, seconds, processing_time)

    if options.run_time_processing_as_function_of_count:
        seconds, processing_time, number_of_orbits = run_time_processing(options)
        plot_processing_time_as_count(options, number_of_orbits, processing_time)


if __name__ == "__main__":
    main()
