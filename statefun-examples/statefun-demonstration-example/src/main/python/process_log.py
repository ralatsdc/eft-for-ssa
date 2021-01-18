#!/usr/bin/env python

from argparse import ArgumentParser
import re
# import time


def main():

    # Add and parse arguments
    parser = ArgumentParser()
    parser.add_argument(
        "-l",
        "--log-file-path",
        default="consumer.log",
        help="consumer log file path",
    )
    options = parser.parse_args()

    # Define patterns to identify events required for analysis
    created_ptn = re.compile('Created orbit for id')
    refined_ptn = re.compile('Refined orbits with ids')
    cleared_ptn = re.compile('Cleared orbit for id')

    # Count number of orbits which exist after each event analyzed,
    # and print along with date and time components for analysis
    num_orb = 0
    out_file_path = options.log_file_path.replace('.log', '.dat')
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
                y, m, d = flds[0].replace('[', '').split('-')
                H, M, S = flds[1].replace(']', '').split(':')
                msg = "{0} {1} {2} {3} {4} {5:.3f} {6}".format(y, m, d, H, M, float(S), num_orb)
                print(msg)
                ofp.write(msg + '\n')
                # time.sleep(0.001)


if __name__ == "__main__":
    main()
