from __future__ import print_function

import random
import time

import grpc

import pipeline_pb2
import pipeline_pb2_grpc

def make_grid_data_message(meterid,metricid,timestamp,values,meterlocation,meterfreqpack,metercapturefreq):
    return pipeline_pb2.Grid_data(
        measurement = pipeline_pb2.measurement_message(meter_id=meterid,metric_id=metricid, timestamp=timestamp, values=values),
        meter = pipeline_pb2.meter_message(meter_id=meterid, meter_location=meterlocation, meter_freq_pack=meterfreqpack, meter_capture_freq=metercapturefreq))

def generate_iterator(grid_data_list):
    for i in grid_data_list:
        yield i

def gateway_push_data(stub):
    grid_data_list = [make_grid_data_message(1,2,1645,"99","berlin","5",5),make_grid_data_message(2,2,1646,"90","Duesseldorf","5",5)]

    data_iterator = generate_iterator(grid_data_list)
    data_push_response = stub.push_data(data_iterator)
    print ("data pushed")
    print ("Response Code: %s " % data_push_response.response_code)

def run():
    channel = grpc.insecure_channel('localhost:50051')
    stub = pipeline_pb2_grpc.gatewayStub(channel)
    print ("----push_data----")
    gateway_push_data(stub)

if __name__ == '__main__':
    run()
