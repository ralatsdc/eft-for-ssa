#!/usr/bin/env python

from argparse import ArgumentParser
import matplotlib.pyplot as plt
import numpy as np
import os
import re
import datetime
import json

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
    track_ptn = re.compile('Created trackId [\d]+ from')
    created_ptn = re.compile('Created orbitId')
    propagated_ptn = re.compile('Propagated orbitId')
    from_tracks_ptn = re.compile('Created refined orbitId \d+ from tracks')
    refined_ptn = re.compile('Refined orbitId')
    correlated_ptn = re.compile('Correlated orbitIds')
    cleared_ptn = re.compile('Cleared orbitId')

    # Count number of orbits which exist after each event analyzed,
    # and print along with date and time components for analysis
    num_orb = 0
    num_orb_message_dict = {}
    num_orb_list = []
    processing_time_list = []
    date_time_list = []

    # Keep track of orbits
    orbit_details = []

    # Keep track of tracks
    track_details = []

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
                    track_id = re.search("(?<=Created trackId )(\d+)", line).group(0)

                    # Add message to track_details
                    track_details.insert(int(track_id), {"orbit_ids": [], "messages": [line]})

                    # TODO: reimplement and delete old processing formatting
                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if created_ptn.search(line) is not None:
                    num_orb += 1
                    orbit_id = re.search("(?<=Created orbitId )(\d+)", line).group(0)
                    # add orbit creation time
                    orbit_details.insert(int(orbit_id), {"created": line})

                if propagated_ptn.search(line) is not None:
                    orbit_id = re.search("(?<=orbitId )(\d+)", line).group(0)
                    track_id = re.search("(?<=trackId )(\d+)", line).group(0)

                    # add track number to orbit_details
                    orbit_details[int(orbit_id)]["track_ids"] = [track_id]

                    # add mapping between track and orbit
                    orbit_track_dict[orbit_id] = track_id

                    # Add orbit id and message to track_details
                    orbit_ids = track_details[int(track_id)].get("orbit_ids", [])
                    orbit_ids.append(orbit_id)
                    track_details[int(track_id)]["orbit_ids"] = orbit_ids

                    messages = track_details[int(track_id)].get("messages", [])
                    messages.append(line)
                    track_details[int(track_id)]["messages"] = messages

                    # TODO: change this older logic to use new details dictionaries
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

                    # Add message to track_details
                    append_message_to_track_details(track_details, orbit_details, line, orbit_id)

                    # add message to track_messages_dict
                    track_messages = track_messages_dict.get(track_id, [])
                    track_messages.append(line)
                    track_messages_dict[track_id] = track_messages

                if from_tracks_ptn.search(line) is not None:
                    orbit_id = re.search("(?<=orbitId )(\d+)", line).group(0)
                    tracks_string = re.search("(?<=tracks: )\[(\d.*)\]", line).group(0)

                    tracks = tracks_string.strip("[]").split(", ")

                    # add track number to orbit_details
                    orbit_details[int(orbit_id)]["track_ids"] = tracks

                    # add orbit number and message to track_details
                    for track_id in tracks:
                        orbit_ids = track_details[int(track_id)].get("orbit_ids", [])
                        orbit_ids.append(orbit_id)
                        track_details[int(track_id)]["orbit_ids"] = orbit_ids

                        messages = track_details[int(track_id)].get("messages", [])
                        messages.append(line)
                        track_details[int(track_id)]["messages"] = messages

                if refined_ptn.search(line) is not None:
                    orbit_ids = re.findall("(?<=orbitId )(\d+)", line)

                    # add info to orbit_details
                    orbit0_refined_to = orbit_details[int(orbit_ids[0])].get("refined_to", [])
                    orbit0_refined_to.append(orbit_ids[2])
                    orbit_details[int(orbit_ids[0])]["refined_to"] = orbit0_refined_to

                    orbit1_refined_to = orbit_details[int(orbit_ids[1])].get("refined_to", [])
                    orbit1_refined_to.append(orbit_ids[2])
                    orbit_details[int(orbit_ids[1])]["refined_to"] = orbit1_refined_to

                    orbit_details[int(orbit_ids[2])]["refined_from"] = [orbit_ids[0], orbit_ids[1]]

                    # Add message to track_details, using refined_from ids
                    append_message_to_track_details(track_details, orbit_details, line, orbit_ids[0])
                    append_message_to_track_details(track_details, orbit_details, line, orbit_ids[1])

                    ## TODO: revisit this logic - with addition of orbit_details, this process can be made more streamlined
                    # get track associated with orbit_id
                    # track_id = orbit_track_dict[orbit_ids[2]]

                    # add new_orbit_id to track_orbit_ids_dict
                    # orbit_track_dict[orbit_ids[2]] = track_id

                    # add message to track_messages_dict
                    # track_messages = track_messages_dict.get(track_id, [])
                    # track_messages.append(line)
                    # track_messages_dict[track_id] = track_messages

                if cleared_ptn.search(line) is not None:
                    num_orb -= 1
                    orbit_id = re.search("(?<=Cleared orbitId )(\d+)", line).group(0)

                    creation_time = datetime.datetime.strptime(
                        orbit_details[int(orbit_id)]["created"].split("]")[0].strip("[]"), "%Y-%m-%d %H:%M:%S.%f")
                    deletion_time = datetime.datetime.strptime(line.split("]")[0].strip("[]"), "%Y-%m-%d %H:%M:%S.%f")
                    orbit_lifespan = deletion_time - creation_time

                    # add details
                    orbit_details[int(orbit_id)]["cleared"] = line
                    orbit_details[int(orbit_id)]["lifespan"] = orbit_lifespan

                    # Add message to track_details
                    append_message_to_track_details(track_details, orbit_details, line, orbit_id)

    num_orb_message_dict[line] = num_orb

    for message_list in track_messages_dict.values():
        # print(message_list)

        earliest_message = message_list[0]
        latest_message = message_list[1]

        # num_orb_list.append(num_orb_message_dict[earliest_message])

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
        # ofp.write(str(dt_early) + " " + str(processing_time) + '\n')
        # print(processing_time)
        # time.sleep(0.001)
        processing_time_list.append(processing_time)

    # Convert date and time to seconds in the run, and numpy arrays
    date_time = np.array(date_time_list, dtype='datetime64[ms]')
    seconds = (date_time - date_time[0]).astype('timedelta64[s]')
    processing_time = np.array(processing_time_list, dtype='timedelta64[ms]')
    number_of_orbits = np.array(num_orb_list)

    # Dump orbit details into json
    json_orbit_file_path = options.log_file_path.replace(".log", "_orbit_details.json")
    with open(json_orbit_file_path, 'w') as fp:
        json.dump(orbit_details, fp, sort_keys=True, default=str)

    json_track_file_path = options.log_file_path.replace(".log", "_track_details.json")
    with open(json_track_file_path, 'w') as fp:
        json.dump(track_details, fp, sort_keys=True, default=str)

    return seconds, processing_time, number_of_orbits, orbit_details


def append_message_to_track_details(track_details, orbit_details, message, orbit_id):
    track_ids = orbit_details[int(orbit_id)]["track_ids"]
    for track_id in track_ids:
        messages = track_details[int(track_id)].get("messages", [])
        messages.append(message)
        track_details[int(track_id)]["messages"] = messages


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


def plot_slowdown(options, orbit_details):
    timedeltas = []
    start_time = None
    processing_time = []

    for orbit in orbit_details:
        creation_time = datetime.datetime.strptime(orbit["created"].split("]")[0].strip("[]"), "%Y-%m-%d %H:%M:%S.%f")
        deletion_time = datetime.datetime.strptime(orbit["cleared"].split("]")[0].strip("[]"), "%Y-%m-%d %H:%M:%S.%f")
        orbit_lifespan = deletion_time - creation_time

        if start_time is None: start_time = creation_time

        timedeltas.append(orbit["lifespan"].total_seconds())
        processing_time.append((creation_time - start_time).total_seconds())

    fig, ax = plt.subplots()
    ax.scatter(processing_time, timedeltas)
    head, file_name = os.path.split(options.log_file_path)
    head, file_dir = os.path.split(head)
    ax.set_title(os.path.join(file_dir, file_name))
    ax.set_ylabel("Orbit Lifespan (s)")
    ax.set_xlabel("Processing Time (s)")

    plt_file_path = options.log_file_path.replace(".log", "_slowdown.png")
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
        "--plot-slowdown",
        action="store_true",
        help="measure timing of orbit deletions over course of simulation. Proxy for machine not keeping up with simulation",
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
        seconds, processing_time, number_of_orbits, orbit_details = run_time_processing(options)
        plot_processing_time(options, seconds, processing_time)

    if options.run_time_processing_as_function_of_count:
        seconds, processing_time, number_of_orbits, orbit_details = run_time_processing(options)
        plot_processing_time_as_count(options, number_of_orbits, processing_time)

    if options.plot_slowdown:
        seconds, processing_time, number_of_orbits, orbit_details = run_time_processing(options)
        plot_slowdown(options, orbit_details)


if __name__ == "__main__":
    main()
