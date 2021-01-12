import re
import time

# inp_fnm = 'consumer-14s-2020-11-02T23:57+00:00.log'
# inp_fnm = 'consumer-42s-2020-11-03T00:08+00:00.log'
# inp_fnm = 'leo-tle-2020-11-03.log'
# inp_fnm = 'leo-tle-2020-11-04.log'
# inp_fnm = 'leo-tle-2020-11-05.log'
# inp_fnm = 'leo-tle-2020-11-06.log'
# inp_fnm = 'consumer-2020-12-29-001.log'
# inp_fnm = 'consumer-2020-12-29-002.log'
# inp_fnm = 'consumer-2020-12-29-003.log'
# inp_fnm = 'consumer-2020-12-29-004.log'
inp_fnm = 'consumer-2020-12-29-008.log'
# inp_fnm = 'consumer-2020-12-29-032.log'
#inp_fnm = 'consumer-2020-12-29-320.log'

out_fnm = inp_fnm.replace('.log', '.dat')

created_ptn = re.compile('Created orbit for id')
refined_ptn = re.compile('Refined orbits with ids')
cleared_ptn = re.compile('Cleared orbit for id')

num_orb = 0
with open(out_fnm, 'w') as ofp:
    with open(inp_fnm, 'r') as ifp:
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
