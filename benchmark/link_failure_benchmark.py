import json
import urllib2
from cassandra.cluster import Cluster
import time
from matplotlib import pyplot
import numpy as np
import timeit
from functools import partial
import random

#cluster = Cluster(['122.129.79.66'],port=9042)
cluster = Cluster()
session = cluster.connect("provenancekey")

def findLinkFailure():
	start = time.time()
	rows = session.execute("SELECT * FROM node")
	for node_row in rows:
		heartBeatrows = session.execute("SELECT * FROM heartbeat where id='"+node_row.id+"'")
		nodeActive = False
		nodeLinkActive = False
		sameNode = False
		for heartbeat_row in heartBeatrows:
			if heartbeat_row.sentneighbourid == node_row.id:
				nodeActive = True
			if heartbeat_row.sentneighbourid == node_row.successor:
				nodeLinkActive = True
				if node_row.id == node_row.successor:
					sameNode = True
		if nodeActive == True:
			print("Node:"+ node_row.id + " Active")
		else:
			print("Node:"+ node_row.id + " InActive")
			end = time.time()
			print("Detected in : " + str(end - start)+" seconds")
		if nodeLinkActive == True:
			if sameNode == False:
				print("Node:"+ node_row.id + " and successor "+ node_row.successor +" link remains connected")
				end = time.time()
				print("Detected in : " + str(end - start)+" seconds")
		else:
			print "Node:"+ node_row.id + " and successor "+ node_row.successor +" link disconnection found"
			end = time.time()
			print("Detected in : " + str(end - start)+" seconds")
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

    plotTC(findLinkFailure, 10, 1000, 10, 10)
    pyplot.show()

# call main
if __name__ == '__main__':
	main()