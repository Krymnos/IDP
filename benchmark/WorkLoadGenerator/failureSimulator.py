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

def generateAllLinkFailure():
	start = time.time()
	rows = session.execute("SELECT * FROM node")
	for node_row in rows:
		if node_row.successor != node_row.id:
			heartBeatRows = session.execute("update heartbeat set sentneighbourid='[]' where id in('"+node_row.successor+"')")

generateAllLinkFailure()