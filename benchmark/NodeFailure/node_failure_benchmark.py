import json
import urllib2
from cassandra.cluster import Cluster
import time
from matplotlib import pyplot
import numpy as np
import timeit
from functools import partial
import random

cluster = Cluster(['122.129.79.66'],port=9042)
#cluster = Cluster()
session = cluster.connect("provenancekeytest")

def findNodeFailure():
	start = time.time()
	rows = session.execute("SELECT * FROM node")
	for node_row in rows:
		node1Active = False
		heartBeatRows = session.execute("SELECT * FROM heartbeat where id='"+node_row.id+"'")
		for heartbeat_row in heartBeatRows:
			if heartbeat_row.id == node_row.id:
				node1Active = True
		if node1Active == False:
			print("Node "+node_row.id+" may have failed.")
		else:
			print("Node "+node_row.id+" active.")
	end = time.time()
	print("Total Time taken: " + str(end - start))

#while True:
#	findLinkFailure()
#	time.sleep(5)

def plotTC(fn, nMin, nMax, nInc, nTests):
    x = []
    y = []
    for i in range(nMin, nMax, nInc):
        testNTimer = timeit.Timer(partial(fn))
        t = testNTimer.timeit(number=nTests)
        x.append(i)
        y.append(t)
    p1 = pyplot.plot(x, y, 'o')

def main():
    print('Analyzing Provenance system for link failures...')

    plotTC(findNodeFailure, 10, 1000, 10, 10)
    pyplot.show()

# call main
if __name__ == '__main__':
	main()