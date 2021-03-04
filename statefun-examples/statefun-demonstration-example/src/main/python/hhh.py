import numpy as np
from matplotlib import pyplot as plt


# calculates expected number of states, per track, for simulations run in eft-for-ssa
# n, track cutoff value - maximum number of tracks a state is kept with before being deleted
# v, number of intervals track is kept before deleting
def run(n, v):
    # 2-dimensional array to contain tracks
    tracks = np.array([[]])

    # number of states over a period of time
    state_number = []

    # input first track
    i = 0
    tracks, state_number = track(n, v, i, tracks, state_number)

    # input second track
    i = i + 1
    tracks, state_number = track(n, v, i, tracks, state_number)

    # produce tracks until state_number repeats itself (flattens out), or until the number of states gets too large
    while state_number[i] - state_number[i - 1] != 0 and state_number[i] < 2 ** 12:
        print(state_number[i])
        i = i + 1
        tracks, state_number = track(n, v, i, tracks, state_number)

    return tracks, state_number


# replicates adding a new track to the system
def track(n, v, i, t, s_n):
    # print('t', t)

    # if t is not empty, delete those values that have expired
    if t.size != 0:
        t = np.delete(t, t[:, 0] <= (i - v), 0)

    # if t is empty, initialize it with a single track
    if t.size == 0:
        t = np.array([[i, 1]])

    # add a track to the next row
    else:
        t = np.append(t, [[i, 1]], 0)

    # correlate tracks into states
    s = len(t[:, 0]) - 1
    for j in range(0, s):
        t = np.append(t, [[i, t[j, 1] + 1]], 0)

    # delete states that exceed the track cutoff value, excluding the last ones added
    t = np.delete(t, np.logical_and(n < t[:, 1], t[:, 1] < (i + 1)), 0)

    # append number of states to the list of state numbers
    s_n.append(len(t[:, 0]))

    return t, s_n

# for n in [4, 2, 1]:
#     for v in [8, 4, 2]:
#         t, s_n = run(n, v)
#         l = len(s_n)
#
#         plt.plot(range(0, l), s_n, label=f'n = {n}, v = {v}')
#
#     plt.xlabel('Track Interval')
#     plt.ylabel('Total States per Object')
#     # plt.title(f'n = {n}, v = {v}')
#     plt.legend(loc='best')
#     plt.show()
