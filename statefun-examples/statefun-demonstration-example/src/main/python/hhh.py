import numpy as np
from matplotlib import pyplot as plt


def run(n, v):
    t = np.array([[]])
    s_n = []

    i = 0
    t, s_n = track(n, v, i, t, s_n)

    i = i + 1
    t, s_n = track(n, v, i, t, s_n)


    while s_n[i] - s_n[i - 1] != 0 and s_n[i] < 2**12:
        print(s_n[i])
        i = i + 1
        t, s_n = track(n, v, i, t, s_n)

    return t, s_n


def track(n, v, i, t, s_n):

    # print('t', t)

    if t.size != 0:
        t = np.delete(t, t[:, 0] <= (i - v), 0)

    if t.size == 0:
        t = np.array([[i, 1]])

    else:
        t = np.append(t, [[i, 1]], 0)

    s = len(t[:, 0]) - 1
    for j in range(0, s):
        t = np.append(t, [[i, t[j, 1] + 1]], 0)

    t = np.delete(t, np.logical_and(n < t[:, 1], t[:, 1] < i), 0)

    s_n.append(len(t[:, 0]))

    return t, s_n


# for n in [4, 2, 1]:
#     for v in [8, 4, 2]:
#         t, s_n = run(n, v)
#         l = len(s_n)
#
#         plt.plot(range(0, l), s_n)
#         plt.xlabel('Track Interval')
#         plt.ylabel('Total States per Object')
#         plt.title(f'n = {n}, v = {v}')
#
#         plt.show()

run(4, 2)
